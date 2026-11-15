/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package script

import (
	"fmt"
	"os"
	"path/filepath"
	"regexp"
	"strings"

	"github.com/dromara/Jpom/go-agent/internal/i18n"
)

// referencePattern matches the script library reference G@("tag") used inside
// script templates (Java ScriptLibraryService#pattern).
var referencePattern = regexp.MustCompile(`G@\("(.*?)"\)`)

// GlobalScriptDir returns <data>/global-script.
func (m *Manager) GlobalScriptDir() string { return m.app.GlobalScriptDir }

// LibraryPath returns <data>/global-script/<id>.json.
func (m *Manager) LibraryPath(id string) string {
	return filepath.Join(m.GlobalScriptDir(), id+".json")
}

// LibraryList returns the script library entries. The script content is not
// returned (Java sets script to null before serializing).
func (m *Manager) LibraryList() ([]map[string]any, error) {
	dir := m.GlobalScriptDir()
	// Java FileUtil.loopFiles 使用 File.listFiles()（文件系统遍历序），
	// 这里用 os.Open + Readdirnames 保持同样的目录顺序（os.ReadDir 会按文件名排序）。
	handle, err := os.Open(dir)
	if err != nil {
		if os.IsNotExist(err) {
			return []map[string]any{}, nil
		}
		return nil, err
	}
	defer func() { _ = handle.Close() }()
	names, err := handle.Readdirnames(-1)
	if err != nil {
		return nil, err
	}
	filtered := make([]string, 0, len(names))
	for _, name := range names {
		info, statErr := os.Stat(filepath.Join(dir, name))
		if statErr != nil || info.IsDir() {
			continue
		}
		if !strings.EqualFold(filepath.Ext(name), ".json") {
			continue
		}
		filtered = append(filtered, name)
	}
	names = filtered
	list := make([]map[string]any, 0, len(names))
	for _, name := range names {
		value := map[string]any{}
		if err := readObject(filepath.Join(dir, name), &value); err != nil {
			// Java logs and skips unreadable files.
			continue
		}
		tag := strings.TrimSuffix(name, filepath.Ext(name))
		value["tag"] = tag
		if stringField(value, "id") == "" {
			value["id"] = tag
		}
		delete(value, "script")
		list = append(list, value)
	}
	return list, nil
}

// LibraryGet returns one script library entry with its script content.
func (m *Manager) LibraryGet(id string) (map[string]any, bool, error) {
	if strings.TrimSpace(id) == "" {
		return nil, false, nil
	}
	path := m.LibraryPath(id)
	value := map[string]any{}
	if err := readObject(path, &value); err != nil {
		return nil, false, err
	}
	if len(value) == 0 {
		return nil, false, nil
	}
	value["tag"] = id
	if stringField(value, "id") == "" {
		value["id"] = id
	}
	return value, true, nil
}

// LibrarySave writes <data>/global-script/<id>.json with the Java model field
// order (id, tag, script, description, version); empty optional fields are
// omitted just like fastjson2 does for the Java agent.
func (m *Manager) LibrarySave(id, content, description, version string) error {
	id = strings.TrimSpace(id)
	if id == "" {
		return fmt.Errorf("%s", i18n.T(i18n.DefaultLang(), "i18n.id_cannot_be_empty.8f2c"))
	}
	value := map[string]any{"id": id, "script": content}
	if description != "" {
		value["description"] = description
	}
	if version != "" {
		value["version"] = version
	}
	encoded, err := marshalOrdered(value, libraryFieldOrder)
	if err != nil {
		return err
	}
	return atomicWrite(m.LibraryPath(id), encoded)
}

// LibraryDelete removes one script library file.
func (m *Manager) LibraryDelete(id string) error {
	id = strings.TrimSpace(id)
	if id == "" {
		return nil
	}
	if err := os.Remove(m.LibraryPath(id)); err != nil && !os.IsNotExist(err) {
		return err
	}
	return nil
}

// ReferenceReplace replaces every G@("tag") reference with the script body of
// the matching library entry (Java ScriptLibraryService#referenceReplace).
func (m *Manager) ReferenceReplace(context string) (string, error) {
	if context == "" {
		return context, nil
	}
	matches := referencePattern.FindAllStringSubmatchIndex(context, -1)
	if len(matches) == 0 {
		return context, nil
	}
	cache := make(map[string]string, len(matches))
	var builder strings.Builder
	last := 0
	for _, match := range matches {
		tag := context[match[2]:match[3]]
		script, ok := cache[tag]
		if !ok {
			item, exists, err := m.LibraryGet(tag)
			if err != nil {
				return "", err
			}
			if !exists {
				return "", fmt.Errorf("%s%s", i18n.T(i18n.DefaultLang(), "i18n.missing_script_library_message.be9a"), tag)
			}
			script = stringField(item, "script")
			cache[tag] = script
		}
		builder.WriteString(context[last:match[0]])
		builder.WriteString(script)
		last = match[1]
	}
	builder.WriteString(context[last:])
	return builder.String(), nil
}
