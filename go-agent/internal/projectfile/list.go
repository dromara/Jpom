/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package projectfile

import (
	"os"
	"path/filepath"
	"sort"
	"strconv"
	"strings"
)

// FileInfo is one entry of the file list responses. The field order and the
// names match org.dromara.jpom.util.FileUtils#fileToJson / #parseInfo so the web
// UI keeps working unchanged.
type FileInfo struct {
	IsDirectory    bool   `json:"isDirectory"`
	FileSizeLong   string `json:"fileSizeLong,omitempty"`
	Filename       string `json:"filename"`
	ModifyTimeLong string `json:"modifyTimeLong"`
	LevelName      string `json:"levelName"`
	TextFileEdit   *bool  `json:"textFileEdit,omitempty"`
}

// DisableScanDir reports whether the project disables directory size scanning.
func DisableScanDir(info map[string]any) bool {
	switch value := info["disableScanDir"].(type) {
	case bool:
		return value
	case string:
		return strings.EqualFold(value, "true") || value == "1"
	default:
		return false
	}
}

// dirSize sums the size of every regular file below dir (FileUtil#size), the
// same value the Java agent reports for directory rows.
func dirSize(dir string) int64 {
	var total int64
	_ = filepath.WalkDir(dir, func(path string, entry os.DirEntry, err error) error {
		if err != nil {
			return nil
		}
		if entry.IsDir() {
			return nil
		}
		stat, statErr := os.Stat(path)
		if statErr != nil {
			return nil
		}
		total += stat.Size()
		return nil
	})
	return total
}

// levelName returns the directory of path relative to base with forward slashes,
// mirroring StringUtil#delStartPath(file, base, false).
func levelName(base, dir string) string {
	rel, err := filepath.Rel(base, dir)
	if err != nil {
		return ""
	}
	rel = filepath.ToSlash(rel)
	if rel == "." {
		return ""
	}
	return rel
}

// parseInfo builds the sorted file list of listedDir. startPath is the anchor
// used to compute levelName (the project lib dir or the backup snapshot dir).
func (s *Service) parseInfo(listedDir, startPath string, entries []os.DirEntry, timeSort, disableScanDir bool) []FileInfo {
	files := make([]FileInfo, 0, len(entries))
	parentLevel := levelName(startPath, listedDir)
	for _, entry := range entries {
		full := filepath.Join(listedDir, entry.Name())
		item := FileInfo{Filename: entry.Name()}
		var modifyTime int64
		stat, err := os.Stat(full)
		if err != nil {
			// Broken symbolic link: Java reports it as a plain file with size 0.
			item.IsDirectory = false
			if entryInfo, infoErr := entry.Info(); infoErr == nil {
				modifyTime = entryInfo.ModTime().UnixMilli()
			}
		} else {
			item.IsDirectory = stat.IsDir()
			modifyTime = stat.ModTime().UnixMilli()
			if !item.IsDirectory {
				item.FileSizeLong = strconv.FormatInt(stat.Size(), 10)
			} else if !disableScanDir {
				item.FileSizeLong = strconv.FormatInt(dirSize(full), 10)
			}
		}
		item.ModifyTimeLong = strconv.FormatInt(modifyTime, 10)
		item.LevelName = parentLevel
		files = append(files, item)
	}
	if timeSort {
		sort.SliceStable(files, func(i, j int) bool {
			left, leftErr := strconv.ParseInt(files[i].ModifyTimeLong, 10, 64)
			right, rightErr := strconv.ParseInt(files[j].ModifyTimeLong, 10, 64)
			if leftErr != nil || rightErr != nil {
				return files[i].ModifyTimeLong > files[j].ModifyTimeLong
			}
			return left > right
		})
	} else {
		sort.SliceStable(files, func(i, j int) bool {
			return files[i].Filename < files[j].Filename
		})
	}
	return files
}

// GetFileList implements /manage/file/getFileList: it lists one directory of the
// project and marks every entry that may be edited in the browser.
func (s *Service) GetFileList(id, relativePath string) ([]FileInfo, error) {
	info, err := s.Project(id)
	if err != nil {
		return nil, err
	}
	lib, target, err := s.LibAndTarget(info, relativePath)
	if err != nil {
		return nil, err
	}
	entries, err := os.ReadDir(target)
	if err != nil {
		// Missing/unreadable directory is an empty list in the Java agent.
		return []FileInfo{}, nil
	}
	files := s.parseInfo(target, lib, entries, false, DisableScanDir(info))
	suffixes := s.Whitelist().AllowEditSuffix
	for index := range files {
		editable := checkSilentFileSuffix(suffixes, files[index].Filename)
		files[index].TextFileEdit = &editable
	}
	return files, nil
}

// DiffItem is one entry of DiffFileVo.
type DiffItem struct {
	Name string `json:"name"`
	Sha1 string `json:"sha1"`
}

// DiffFileRequest is the JSON body of /manage/file/diff_file and
// /manage/file/batch_delete (DiffFileVo).
type DiffFileRequest struct {
	ID   string     `json:"id"`
	Data []DiffItem `json:"data"`
	Dir  string     `json:"dir"`
}

// DiffFileResult is the response of /manage/file/diff_file.
type DiffFileResult struct {
	Diff []DiffItem `json:"diff"`
	Del  []DiffItem `json:"del"`
}

// DiffFile compares the local project files with the file signatures supplied by
// the server and returns the files that must be uploaded (diff) and deleted (del).
func (s *Service) DiffFile(request DiffFileRequest) (*DiffFileResult, error) {
	if len(request.Data) == 0 {
		return nil, errComparisonDataMissing
	}
	info, err := s.Project(request.ID)
	if err != nil {
		return nil, err
	}
	if err := s.checkLibAllowedPath(info); err != nil {
		return nil, err
	}
	_, target, err := s.LibAndTarget(info, request.Dir)
	if err != nil {
		return nil, err
	}
	nowMap := map[string]string{}
	nowOrder := make([]string, 0, len(request.Data))
	if stat, statErr := os.Stat(target); statErr == nil && !stat.IsDir() {
		// Degenerate case: dir points to a file, FileUtil#loopFiles returns it.
		sha1Value, hashErr := fileSHA1(target)
		if hashErr != nil {
			return nil, hashErr
		}
		name := levelName(target, target)
		if name == "" {
			name = filepath.Base(target)
		}
		nowMap[name] = sha1Value
		nowOrder = append(nowOrder, name)
	} else {
		_ = filepath.WalkDir(target, func(path string, entry os.DirEntry, walkErr error) error {
			if walkErr != nil {
				return nil
			}
			statValue, statErr := os.Stat(path)
			if statErr != nil || statValue.IsDir() {
				return nil
			}
			name, relErr := filepath.Rel(target, path)
			if relErr != nil {
				return nil
			}
			name = filepath.ToSlash(name)
			sha1Value, hashErr := fileSHA1(path)
			if hashErr != nil {
				return nil
			}
			nowMap[name] = sha1Value
			nowOrder = append(nowOrder, name)
			return nil
		})
	}
	tryMap := make(map[string]string, len(request.Data))
	diff := make([]DiffItem, 0, len(request.Data))
	for _, item := range request.Data {
		tryMap[item.Name] = item.Sha1
		nowSha1, present := nowMap[item.Name]
		if present && nowSha1 == item.Sha1 {
			continue
		}
		diff = append(diff, item)
	}
	del := make([]DiffItem, 0)
	for _, name := range nowOrder {
		if _, present := tryMap[name]; present {
			continue
		}
		del = append(del, DiffItem{Name: name, Sha1: nowMap[name]})
	}
	return &DiffFileResult{Diff: diff, Del: del}, nil
}

// checkLibAllowedPath validates the project directory without resolving the
// target, used by endpoints that only need the lib path.
func (s *Service) checkLibAllowedPath(info map[string]any) error {
	_, err := s.LibPath(info)
	return err
}
