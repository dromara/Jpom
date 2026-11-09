/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package config

import (
	"os"
	"path/filepath"
	"testing"
)

func TestLoadJavaCompatibleConfig(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "application.yml")
	content := []byte(`
jpom:
  path: /opt/jpom-agent
  authorize:
    agent-name: custom
    agent-pwd: secret
  project:
    status-wait-time: 3
    file-backup-count: 2
    file-backup-suffix: [ '.jar','.html','^.+\\.(?i)(txt)$' ]
    log:
      auto-backup-size: 10MB
      save-days: 3
  init-read-line: 20
  system:
    command-use-sudo: true
    lang: en-US
  monitor:
    network:
      stat-contains-only-names: en*
      stat-exclude-names: [lo*, docker0]
server:
  port: 2124
  address: 127.0.0.1
`)
	if err := os.WriteFile(path, content, 0o644); err != nil {
		t.Fatal(err)
	}
	cfg, err := Load(path)
	if err != nil {
		t.Fatalf("Load: %v", err)
	}
	if cfg.Jpom.Path != "/opt/jpom-agent" {
		t.Errorf("path = %q", cfg.Jpom.Path)
	}
	if cfg.Jpom.Authorize.AgentName != "custom" || cfg.Jpom.Authorize.AgentPwd != "secret" {
		t.Errorf("authorize = %+v", cfg.Jpom.Authorize)
	}
	if cfg.Jpom.Project.StatusWaitTime != 3 || cfg.Jpom.Project.FileBackupCount != 2 {
		t.Errorf("project = %+v", cfg.Jpom.Project)
	}
	if len(cfg.Jpom.Project.FileBackupSuffix) != 3 {
		t.Errorf("suffix = %v", cfg.Jpom.Project.FileBackupSuffix)
	}
	if cfg.Jpom.Project.Log.AutoBackupSize != "10MB" || cfg.Jpom.Project.Log.SaveDays != 3 {
		t.Errorf("project log = %+v", cfg.Jpom.Project.Log)
	}
	if cfg.Jpom.InitReadLine != 20 || !cfg.Jpom.System.CommandUseSudo || cfg.Jpom.System.Lang != "en-US" {
		t.Errorf("init-read-line/system = %+v", cfg.Jpom)
	}
	if cfg.Jpom.Monitor == nil {
		t.Fatal("monitor section should be parsed")
	}
	wantContains := []string{"en*"}
	if got := cfg.Jpom.Monitor.Network.StatContainsOnlyNames.List(); len(got) != 1 || got[0] != wantContains[0] {
		t.Errorf("contains names = %v", got)
	}
	if cfg.Jpom.Monitor.Network.StatContainsOnlyNames.String() != "en*" {
		t.Errorf("contains names raw = %q", cfg.Jpom.Monitor.Network.StatContainsOnlyNames)
	}
	if got := cfg.Jpom.Monitor.Network.StatExcludeNames.List(); len(got) != 2 {
		t.Errorf("exclude names = %v", got)
	}
	if cfg.Server.Port != 2124 || cfg.Server.Address != "127.0.0.1" {
		t.Errorf("server = %+v", cfg.Server)
	}
}

func TestLoadDefaultsOnMissingKeys(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "application.yml")
	if err := os.WriteFile(path, []byte("server:\n  port: 2125\n"), 0o644); err != nil {
		t.Fatal(err)
	}
	cfg, err := Load(path)
	if err != nil {
		t.Fatal(err)
	}
	if cfg.Server.Port != 2125 {
		t.Errorf("port = %d", cfg.Server.Port)
	}
	if cfg.Jpom.Authorize.AgentName != "jpomAgent" {
		t.Errorf("default agent name = %q", cfg.Jpom.Authorize.AgentName)
	}
	if cfg.Jpom.Project.StatusWaitTime != 10 || cfg.Jpom.InitReadLine != 10 {
		t.Errorf("defaults = %+v", cfg.Jpom)
	}
	if cfg.Jpom.Monitor != nil {
		t.Errorf("monitor should be nil when not configured")
	}
}

func TestLoadMissingExplicitFile(t *testing.T) {
	if _, err := Load(filepath.Join(t.TempDir(), "not-exists.yml")); err == nil {
		t.Fatal("expected error for missing explicit config file")
	}
}
