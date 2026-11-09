/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
// Package projectfile implements the agent side of the Jpom project file
// manager: /manage/file/* and /manage/file2/*.
//
// It is a port of the Java agent classes ProjectFileControl,
// FileManageController and ProjectFileBackupController: the sharding upload
// protocol, the file backup layout, the path rules and the JSON field names are
// kept byte compatible with the Java implementation so the same server and web
// UI can drive both agents.
package projectfile

import (
	"fmt"
	"log/slog"
	"os"
	"path/filepath"
	"strings"

	"gopkg.in/yaml.v3"

	"jpom.io/go-agent/internal/app"
	"jpom.io/go-agent/internal/project"
)

// backupDirName is the default file backup root inside the agent data dir
// (data/project_file_backup/<projectId>), same as ProjectFileBackupService.
const backupDirName = "project_file_backup"

// Service owns the project file endpoints of the agent.
type Service struct {
	app      *app.App
	projects *project.Manager
}

// New creates a project file service bound to the agent runtime.
func New(a *app.App) *Service {
	return &Service{
		app:      a,
		projects: project.New(a.BaseDir),
	}
}

// App exposes the runtime for handlers that need to resolve download paths.
func (s *Service) App() *app.App { return s.app }

// Log returns the agent logger.
func (s *Service) Log() *slog.Logger { return s.app.Log() }

// errComparisonDataMissing mirrors i18n.comparison_data_not_found.
var errComparisonDataMissing = invalidf("i18n.comparison_data_not_found.413e")

// Project returns the raw project data by id, mirroring
// BaseAgentController#getProjectInfoModel(String).
func (s *Service) Project(id string) (map[string]any, error) {
	id = strings.TrimSpace(id)
	if id != "" {
		info, ok, err := s.projects.Get(id)
		if err != nil {
			// project.json is corrupt or unreadable: a server side failure (500),
			// not a validation error.
			return nil, failSuffix("i18n.service_exception.3821", err.Error())
		}
		if ok {
			return info, nil
		}
	}
	return nil, invalidSuffix("i18n.get_project_info_failure.ddff", id)
}

// ResolveModel follows a runMode=Link project to the real project, same as
// ProjectInfoService#resolveModel.
func (s *Service) ResolveModel(info map[string]any) (map[string]any, error) {
	if !strings.EqualFold(project.String(info, "runMode"), "Link") {
		return info, nil
	}
	linkID := project.String(info, "linkId")
	linked, ok, err := s.projects.Get(linkID)
	if err != nil {
		return nil, failSuffix("i18n.service_exception.3821", err.Error())
	}
	if !ok || linked == nil {
		return nil, invalidf("i18n.soft_link_project_does_not_exist.4e4f")
	}
	return linked, nil
}

// LibPath returns the absolute project file directory (NodeProjectInfoModel#allLib
// with soft link resolution).
func (s *Service) LibPath(info map[string]any) (string, error) {
	model, err := s.ResolveModel(info)
	if err != nil {
		return "", err
	}
	lib, err := project.ProjectPath(model)
	if err != nil {
		return "", err
	}
	abs, err := filepath.Abs(lib)
	if err != nil {
		return "", err
	}
	abs = filepath.Clean(abs)
	if err := s.checkLibAllowed(abs); err != nil {
		return "", err
	}
	return abs, nil
}

// LibAndTarget resolves the project lib dir and a client supplied relative path
// inside it. Every path taken from a request must go through this method so a
// malicious server client cannot escape the authorized directory.
func (s *Service) LibAndTarget(info map[string]any, parts ...string) (string, string, error) {
	lib, err := s.LibPath(info)
	if err != nil {
		return "", "", err
	}
	target, err := secureJoin(lib, parts...)
	if err != nil {
		return "", "", err
	}
	return lib, target, nil
}

// Whitelist returns the agent whitelist (data/whitelistDirectory.json).
func (s *Service) Whitelist() project.Whitelist {
	whitelist, err := project.ReadWhitelist(s.app.BaseDir)
	if err != nil {
		s.Log().Warn("read whitelist failed", "error", err)
		return project.Whitelist{}
	}
	return whitelist
}

// checkLibAllowed rejects project directories that are outside the authorized
// directories. When no authorization directory is configured yet the check is
// skipped, matching the Java agent which trusts project.whitelistDirectory.
func (s *Service) checkLibAllowed(lib string) error {
	whitelist := s.Whitelist()
	if len(whitelist.Project) == 0 {
		return nil
	}
	if !whitelist.CheckPath(lib) {
		return invalidMessage(fmt.Sprintf("Project path is not in the authorized directory: %s", lib))
	}
	return nil
}

// dslConfig is the subset of the project DSL yml that influences file handling.
type dslConfig struct {
	File *dslFile `yaml:"file"`
}

type dslFile struct {
	BackupCount  *int     `yaml:"backupCount"`
	BackupSuffix []string `yaml:"backupSuffix"`
	BackupPath   string   `yaml:"backupPath"`
	DiffBackup   *bool    `yaml:"diffBackup"`
}

// dsl parses the project dslContent (a YAML document stored inside project.json).
func (s *Service) dsl(model map[string]any) (*dslConfig, error) {
	content := project.String(model, "dslContent")
	if strings.TrimSpace(content) == "" {
		return nil, nil
	}
	config := &dslConfig{}
	if err := yaml.Unmarshal([]byte(content), config); err != nil {
		return nil, failMessage(fmt.Sprintf("Failed to parse project DSL config: %v", err))
	}
	return config, nil
}

// backupSettings are the effective file backup options for one project.
type backupSettings struct {
	// ID is the project id owning the backup directory (the linked project for
	// runMode=Link projects).
	ID string
	// Path is the custom backup path from the DSL config, empty when unset.
	Path string
	// Count is the number of backup directories to keep, <=0 disables backups.
	Count int
	// Suffix are the file name patterns kept in a backup (suffix or regex).
	Suffix []string
	// DiffBackup removes unchanged files from the backup snapshot.
	DiffBackup bool
}

// backupSettings resolves the effective backup options, same as
// ProjectFileBackupService#resolveBackupCount/#resolveBackupPath.
func (s *Service) backupSettings(info map[string]any) (backupSettings, error) {
	model, err := s.ResolveModel(info)
	if err != nil {
		return backupSettings{}, err
	}
	settings := backupSettings{
		ID:         project.String(model, "id"),
		Count:      s.app.Config.Jpom.Project.FileBackupCount,
		Suffix:     s.app.Config.Jpom.Project.FileBackupSuffix,
		DiffBackup: true,
	}
	if settings.ID == "" {
		settings.ID = project.String(info, "id")
	}
	dsl, err := s.dsl(model)
	if err != nil {
		return backupSettings{}, err
	}
	if dsl != nil && dsl.File != nil {
		if dsl.File.BackupCount != nil {
			settings.Count = *dsl.File.BackupCount
		}
		if dsl.File.BackupSuffix != nil {
			settings.Suffix = dsl.File.BackupSuffix
		}
		if dsl.File.DiffBackup != nil {
			settings.DiffBackup = *dsl.File.DiffBackup
		}
		settings.Path = strings.TrimSpace(dsl.File.BackupPath)
	}
	return settings, nil
}

// backupRoot returns the directory holding every backup snapshot of a project.
func (s *Service) backupRoot(settings backupSettings) string {
	if settings.Path == "" {
		return filepath.Join(s.app.DataDir, backupDirName, settings.ID)
	}
	return filepath.Join(settings.Path, settings.ID)
}

// defaultBackupRoot returns the built-in backup directory of a project id.
func (s *Service) defaultBackupRoot(projectID string) string {
	return filepath.Join(s.app.DataDir, backupDirName, projectID)
}

// async runs fn in a goroutine, swallowing (and logging) panics, used for the
// post-write backup comparison which the Java agent also runs asynchronously.
func (s *Service) async(name string, fn func()) {
	go func() {
		defer func() {
			if recovered := recover(); recovered != nil {
				s.Log().Error("project file background task panic", "task", name, "panic", recovered)
			}
		}()
		fn()
	}()
}

// exists reports whether path exists (following symbolic links like Java).
func exists(path string) bool {
	_, err := os.Stat(path)
	return err == nil
}

// isDir reports whether path is an existing directory.
func isDir(path string) bool {
	stat, err := os.Stat(path)
	return err == nil && stat.IsDir()
}
