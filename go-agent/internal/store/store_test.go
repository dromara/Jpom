/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package store

import (
	"path/filepath"
	"testing"
)

func TestWriteReadAtomic(t *testing.T) {
	path := filepath.Join(t.TempDir(), "data", "project.json")
	s := New(path)
	var empty map[string]map[string]any
	if err := s.Read(&empty); err != nil {
		t.Fatalf("read missing: %v", err)
	}
	if empty != nil {
		t.Fatalf("missing file should leave dst untouched")
	}
	value := map[string]map[string]any{"A1": {"id": "A1", "name": "demo"}}
	if err := s.Write(value); err != nil {
		t.Fatalf("write: %v", err)
	}
	var got map[string]map[string]any
	if err := s.Read(&got); err != nil {
		t.Fatalf("read: %v", err)
	}
	if got["A1"]["name"] != "demo" {
		t.Fatalf("got %+v", got)
	}
}

func TestUpdate(t *testing.T) {
	path := filepath.Join(t.TempDir(), "whitelist.json")
	s := New(path)
	type whitelist struct {
		AllowEditSuffix []string `json:"allowEditSuffix"`
		Project         []string `json:"project"`
	}
	err := Update[whitelist](s, func() whitelist {
		return whitelist{AllowEditSuffix: []string{"txt"}}
	}, func(v *whitelist) error {
		v.Project = append(v.Project, "/data/demo")
		return nil
	})
	if err != nil {
		t.Fatalf("update: %v", err)
	}
	var got whitelist
	if err := s.Read(&got); err != nil {
		t.Fatal(err)
	}
	if len(got.Project) != 1 || got.Project[0] != "/data/demo" || len(got.AllowEditSuffix) != 1 {
		t.Fatalf("got %+v", got)
	}
}
