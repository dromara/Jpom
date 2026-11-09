/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

// Package config loads the Jpom agent configuration.
//
// The Go agent deliberately reads the *same* Spring Boot style
// conf/application.yml as the Java agent, so a node can switch between the two
// implementations without touching configuration or data.
package config

import (
	"fmt"
	"os"
	"strconv"
	"strings"

	"gopkg.in/yaml.v3"
)

// Config mirrors the subset of application.yml that is meaningful for an agent.
type Config struct {
	Jpom   Jpom   `yaml:"jpom"`
	Server Server `yaml:"server"`
	Spring Spring `yaml:"spring"`

	// Source is the file the configuration was loaded from ("" for defaults).
	Source string `yaml:"-"`
}

type Jpom struct {
	Path         string    `yaml:"path"`
	Authorize    Authorize `yaml:"authorize"`
	Project      Project   `yaml:"project"`
	System       System    `yaml:"system"`
	Monitor      *Monitor  `yaml:"monitor"`
	InitReadLine int       `yaml:"init-read-line"`
}

type Authorize struct {
	AgentName string `yaml:"agent-name"`
	AgentPwd  string `yaml:"agent-pwd"`
	// Authorize 保留字段仅为兼容旧 Go 实现：Java 插件端不支持
	// jpom.authorize.authorize（AgentAuthorize getter 恒为 null），因此这里显式
	// 忽略该配置键，保证同一份配置在 Java/Go 下鉴权行为一致（P2-2）。
	Authorize string `yaml:"-"`
}

type Project struct {
	StatusWaitTime          int        `yaml:"status-wait-time"`
	FileBackupCount         int        `yaml:"file-backup-count"`
	FileBackupSuffix        []string   `yaml:"file-backup-suffix"`
	StatusDetectionInterval int        `yaml:"status-detection-interval"`
	Log                     ProjectLog `yaml:"log"`
}

type ProjectLog struct {
	AutoBackupConsoleCron string `yaml:"auto-backup-console-cron"`
	AutoBackupSize        string `yaml:"auto-backup-size"`
	AutoBackupToFile      bool   `yaml:"auto-backup-to-file"`
	SaveDays              int    `yaml:"save-days"`
	FileCharset           string `yaml:"file-charset"`
}

type System struct {
	TimerMatchSecond bool   `yaml:"timer-match-second"`
	OldJarsCount     int    `yaml:"old-jars-count"`
	RemoteVersionURL string `yaml:"remote-version-url"`
	LogCharset       string `yaml:"log-charset"`
	ConsoleCharset   string `yaml:"console-charset"`
	AllowedDowngrade bool   `yaml:"allowed-downgrade"`
	CommandUseSudo   bool   `yaml:"command-use-sudo"`
	Lang             string `yaml:"lang"`
}

// Monitor mirrors jpom.monitor. It is a pointer in Config so that the agent can
// tell "not configured" (null in the Java /info response, which Jackson omits)
// apart from an explicitly empty section.
type Monitor struct {
	Network Network `yaml:"network"`
}

// Network mirrors jpom.monitor.network. The Java fields are plain String
// (comma separated), CommaString also accepts a YAML sequence for convenience.
type Network struct {
	StatContainsOnlyNames CommaString `yaml:"stat-contains-only-names"`
	StatExcludeNames      CommaString `yaml:"stat-exclude-names"`
}

// CommaString is a string option that accepts both a scalar ("a,b") and a YAML
// sequence (["a", "b"]); it always renders back as the comma separated form used
// by the Java configuration and /info response.
type CommaString string

// UnmarshalYAML implements yaml.Unmarshaler.
func (s *CommaString) UnmarshalYAML(node *yaml.Node) error {
	if node.Kind == yaml.SequenceNode {
		var values []string
		if err := node.Decode(&values); err != nil {
			return err
		}
		*s = CommaString(strings.Join(values, ","))
		return nil
	}
	var value string
	if err := node.Decode(&value); err != nil {
		return err
	}
	*s = CommaString(value)
	return nil
}

// String returns the raw comma separated value.
func (s CommaString) String() string { return string(s) }

// List splits the value on commas and trims every item.
func (s CommaString) List() []string { return SplitComma(string(s)) }

type Server struct {
	Port    int    `yaml:"port"`
	Address string `yaml:"address"`
}

// Spring mirrors the Spring Boot configuration subset the agent reads.
type Spring struct {
	Servlet Servlet `yaml:"servlet"`
}

// Servlet mirrors spring.servlet.
type Servlet struct {
	Multipart Multipart `yaml:"multipart"`
}

// Multipart mirrors spring.servlet.multipart (upload size limits).
type Multipart struct {
	MaxFileSize    string `yaml:"max-file-size"`
	MaxRequestSize string `yaml:"max-request-size"`
}

// 上传大小默认值（与 Java 插件端 config_default/application.yml 一致）。
const (
	DefaultMaxFileSize    int64 = 5 << 20
	DefaultMaxRequestSize int64 = 20 << 20
)

// MaxFileSize returns spring.servlet.multipart.max-file-size (default 5MB).
func (c *Config) MaxFileSize() int64 {
	return ParseDataSize(c.Spring.Servlet.Multipart.MaxFileSize, DefaultMaxFileSize)
}

// MaxRequestSize returns spring.servlet.multipart.max-request-size (default 20MB).
func (c *Config) MaxRequestSize() int64 {
	return ParseDataSize(c.Spring.Servlet.Multipart.MaxRequestSize, DefaultMaxRequestSize)
}

// ParseDataSize parses a Spring DataSize value such as "5MB", "12KB", "20MB",
// "1GB" or a plain byte count. An empty or invalid value yields fallback.
func ParseDataSize(value string, fallback int64) int64 {
	text := strings.ToUpper(strings.TrimSpace(value))
	if text == "" {
		return fallback
	}
	units := []struct {
		suffix string
		scale  int64
	}{
		{"TB", 1 << 40},
		{"GB", 1 << 30},
		{"MB", 1 << 20},
		{"KB", 1 << 10},
		{"B", 1},
	}
	scale := int64(1)
	for _, unit := range units {
		if strings.HasSuffix(text, unit.suffix) {
			scale = unit.scale
			text = strings.TrimSpace(strings.TrimSuffix(text, unit.suffix))
			break
		}
	}
	parsed, err := strconv.ParseFloat(text, 64)
	if err != nil || parsed < 0 {
		return fallback
	}
	return int64(parsed * float64(scale))
}

// StringList accepts both a comma separated scalar and a YAML sequence, because
// the same option has been documented both ways over the project lifetime.
type StringList []string

// UnmarshalYAML implements yaml.Unmarshaler.
func (l *StringList) UnmarshalYAML(node *yaml.Node) error {
	if node.Kind == yaml.SequenceNode {
		var values []string
		if err := node.Decode(&values); err != nil {
			return err
		}
		*l = values
		return nil
	}
	var value string
	if err := node.Decode(&value); err != nil {
		return err
	}
	*l = SplitComma(value)
	return nil
}

// SplitComma splits a comma separated option value and trims every item.
func SplitComma(value string) []string {
	if strings.TrimSpace(value) == "" {
		return nil
	}
	parts := strings.Split(value, ",")
	out := make([]string, 0, len(parts))
	for _, part := range parts {
		part = strings.TrimSpace(part)
		if part != "" {
			out = append(out, part)
		}
	}
	return out
}

// Default returns the same defaults as the Java agent's config_default/application.yml.
func Default() *Config {
	return &Config{
		Jpom: Jpom{
			Authorize: Authorize{AgentName: "jpomAgent"},
			Project: Project{
				StatusWaitTime:          10,
				FileBackupCount:         0,
				FileBackupSuffix:        []string{".jar", ".html", `^.+\.(?i)(txt)$`},
				StatusDetectionInterval: 500,
				Log: ProjectLog{
					AutoBackupConsoleCron: "0 0/10 * * * ?",
					AutoBackupSize:        "50MB",
					AutoBackupToFile:      true,
					SaveDays:              7,
				},
			},
			InitReadLine: 10,
			System: System{
				OldJarsCount: 2,
				LogCharset:   "UTF-8",
				Lang:         "",
			},
		},
		Server: Server{Port: 2123},
		Spring: Spring{
			Servlet: Servlet{
				Multipart: Multipart{
					MaxFileSize:    "5MB",
					MaxRequestSize: "20MB",
				},
			},
		},
	}
}

// Load reads path over the built-in defaults. A missing file is only an error
// when the caller explicitly asked for it (source is returned empty otherwise).
func Load(path string) (*Config, error) {
	cfg := Default()
	if strings.TrimSpace(path) == "" {
		return cfg, nil
	}
	content, err := os.ReadFile(path)
	if err != nil {
		if os.IsNotExist(err) {
			return nil, fmt.Errorf("config file not found: %s", path)
		}
		return nil, err
	}
	if err := yaml.Unmarshal(content, cfg); err != nil {
		return nil, fmt.Errorf("parse config file failed %s: %w", path, err)
	}
	cfg.Source = path
	if cfg.Server.Port <= 0 {
		cfg.Server.Port = 2123
	}
	if strings.TrimSpace(cfg.Jpom.Authorize.AgentName) == "" {
		cfg.Jpom.Authorize.AgentName = "jpomAgent"
	}
	return cfg, nil
}
