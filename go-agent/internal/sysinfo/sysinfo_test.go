/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package sysinfo

import (
	"os"
	"path/filepath"
	"runtime"
	"testing"
	"time"

	"github.com/dromara/Jpom/go-agent/internal/app"
	"github.com/dromara/Jpom/go-agent/internal/config"
)

// newTestApp 构造一个只依赖临时目录的 App，供 sysinfo 单测使用。
func newTestApp(t *testing.T) *app.App {
	t.Helper()
	base := t.TempDir()
	a := &app.App{
		Version:           "2.11.13",
		BaseDir:           base,
		ConfPath:          filepath.Join(base, "conf", "application.yml"),
		Config:            config.Default(),
		DataDir:           filepath.Join(base, "data"),
		LogsDir:           filepath.Join(base, "logs"),
		TmpDir:            filepath.Join(base, "tmp"),
		RunDir:            base,
		Executable:        filepath.Join(base, "bin", "agent"),
		GlobalScriptDir:   filepath.Join(base, "data", "global-script"),
		ScriptRunCacheDir: filepath.Join(base, "data", "script_run_cache"),
		TempDir:           filepath.Join(base, "data", "temp"),
		Pid:               4242,
		RandomID:          "random-id",
		InstallID:         "install-id",
		StartAt:           time.Now(),
	}
	for _, dir := range []string{a.DataDir, a.LogsDir, a.TmpDir, a.GlobalScriptDir, a.TempDir} {
		if err := os.MkdirAll(dir, 0o755); err != nil {
			t.Fatal(err)
		}
	}
	return a
}

func TestManifestFieldTypes(t *testing.T) {
	a := newTestApp(t)
	manifest := a.Manifest()
	if manifest["pid"] != "4242" {
		t.Errorf("pid = %v, want string 4242", manifest["pid"])
	}
	if _, ok := manifest["upTime"].(string); !ok {
		t.Errorf("upTime should be a string, got %T", manifest["upTime"])
	}
	if _, ok := manifest["port"].(int); !ok {
		t.Errorf("port should be an int, got %T", manifest["port"])
	}
	if manifest["debug"] != false {
		t.Errorf("debug = %v, want false for release version", manifest["debug"])
	}
	dev := *a
	dev.Version = "dev"
	if dev.Manifest()["debug"] != true {
		t.Errorf("dev version should set debug=true")
	}
	if manifest["type"] != "Agent" {
		t.Errorf("type = %v", manifest["type"])
	}
	// CONTRACT §2：Go Agent manifest 新增 engine/platform/arch，osName 保持 Java 兼容值。
	if manifest["engine"] != "go" {
		t.Errorf("engine = %v, want go", manifest["engine"])
	}
	if manifest["platform"] != runtime.GOOS+"/"+runtime.GOARCH {
		t.Errorf("platform = %v, want %s/%s", manifest["platform"], runtime.GOOS, runtime.GOARCH)
	}
	if manifest["arch"] != runtime.GOARCH {
		t.Errorf("arch = %v, want %s", manifest["arch"], runtime.GOARCH)
	}
	if manifest["osName"] != a.OsName() {
		t.Errorf("osName = %v", manifest["osName"])
	}
}

func TestNetworkFilterAntPatterns(t *testing.T) {
	monitor := &config.Monitor{Network: config.Network{
		StatExcludeNames:      "lo*, docker0",
		StatContainsOnlyNames: "ens*,eth?",
	}}
	filter := NewNetworkFilter(monitor)
	cases := map[string]bool{
		"lo":              false,
		"docker0":         false,
		"ens18":           true,
		"eth0":            true,
		"eth10":           false,
		"docker_gwbridge": false,
	}
	for name, want := range cases {
		if got := filter.Match(name); got != want {
			t.Errorf("Match(%q) = %v, want %v", name, got, want)
		}
	}
	// 未配置 monitor 时全部参与统计。
	if !NewNetworkFilter(nil).Match("lo") {
		t.Error("nil monitor should not filter interfaces")
	}
}

func TestCronExpressionMatch(t *testing.T) {
	expr, err := parseCron("0 0/10 * * * ?")
	if err != nil {
		t.Fatalf("parseCron: %v", err)
	}
	base := time.Date(2026, 11, 9, 6, 30, 0, 0, time.Local)
	if !expr.Match(base, true) {
		t.Error("06:30:00 should match 0 0/10 * * * ?")
	}
	if expr.Match(base.Add(5*time.Minute), true) {
		t.Error("06:35:00 should not match")
	}
	if expr.Match(base.Add(1*time.Second), true) {
		t.Error("06:30:01 should not match")
	}
	every5, err := parseCron("*/5 * * * * ?")
	if err != nil {
		t.Fatalf("parseCron: %v", err)
	}
	if !every5.Match(base.Add(5*time.Second), true) {
		t.Error("every 5 seconds should match")
	}
	// timer-match-second=false：忽略秒字段，只在第 0 秒匹配（Hutool CronTimer 语义）。
	if !expr.Match(base, false) {
		t.Error("matchSecond=false 时应按分钟边界匹配")
	}
	if every5.Match(base.Add(5*time.Second), false) {
		t.Error("matchSecond=false 时不应在第 5 秒触发")
	}
	if !every5.Match(base.Add(1*time.Minute), false) {
		t.Error("matchSecond=false 时下一分钟边界应触发")
	}
	if _, err := parseCron("bad cron"); err == nil {
		t.Error("invalid cron should return error")
	}
}

func TestCronManagerUpsertAndRemove(t *testing.T) {
	a := newTestApp(t)
	manager := NewCronManager(a)
	SetGlobalCron(manager)
	defer SetGlobalCron(nil)
	if err := RegisterCronTask("script:S1", "0 0/5 * * * ?", func() {}); err != nil {
		t.Fatalf("RegisterCronTask: %v", err)
	}
	list := CronList()
	if len(list) != 1 || list[0]["taskId"] != "script:S1" || list[0]["cron"] != "0 0/5 * * * ?" {
		t.Fatalf("taskList = %v", list)
	}
	if err := RemoveCronTask("script:S1"); err != nil {
		t.Fatalf("RemoveCronTask: %v", err)
	}
	if list := CronList(); len(list) != 0 {
		t.Fatalf("remove 后 taskList 应为空, got %v", list)
	}
	// 移除不存在的任务也返回成功（Java CronUtils.remove 幂等）。
	if err := RemoveCronTask("not-exists"); err != nil {
		t.Fatalf("RemoveCronTask(not-exists): %v", err)
	}
	// 未注册调度器时应返回错误而不是 panic。
	SetGlobalCron(nil)
	if err := RemoveCronTask("script:S1"); err == nil {
		t.Error("cron 未启动时 RemoveCronTask 应返回错误")
	}
}

func TestParseDataSize(t *testing.T) {
	cases := map[string]int64{
		"50MB": 50 * 1024 * 1024,
		"1KB":  1024,
		"2GB":  2 * 1024 * 1024 * 1024,
		"100":  100,
		"":     10,
		"x":    10,
	}
	for value, want := range cases {
		if got := parseDataSize(value, 10); got != want {
			t.Errorf("parseDataSize(%q) = %d, want %d", value, got, want)
		}
	}
}

func TestCronHutoolExtensions(t *testing.T) {
	// 1) | 分组 OR：09:00 与 18:00 各触发一次，12:00 不触发。
	groups, err := parseCron("0 0 9 * * ? | 0 0 18 * * ?")
	if err != nil {
		t.Fatalf("parseCron groups: %v", err)
	}
	day := time.Date(2026, 11, 9, 0, 0, 0, 0, time.Local)
	if !groups.Match(day.Add(9*time.Hour), true) {
		t.Error("09:00 应命中 | 分组")
	}
	if !groups.Match(day.Add(18*time.Hour), true) {
		t.Error("18:00 应命中 | 分组")
	}
	if groups.Match(day.Add(12*time.Hour), true) {
		t.Error("12:00 不应命中")
	}

	// 2) 7 段年份：仅匹配指定年份。
	yearExpr, err := parseCron("0 0 9 9 11 ? 2026")
	if err != nil {
		t.Fatalf("parseCron year: %v", err)
	}
	if !yearExpr.Match(time.Date(2026, 11, 9, 9, 0, 0, 0, time.Local), true) {
		t.Error("2026-11-09 09:00 应命中")
	}
	if yearExpr.Match(time.Date(2027, 11, 9, 9, 0, 0, 0, time.Local), true) {
		t.Error("2027 年不应命中 2026 年限定表达式")
	}
	if _, err := parseCron("0 0 9 9 11 ? 2100"); err == nil {
		t.Error("年份超出 1970-2099 应报错")
	}
	// 年份通配
	wildYear, err := parseCron("0 0 9 9 11 ? *")
	if err != nil {
		t.Fatalf("parseCron year wildcard: %v", err)
	}
	if !wildYear.Match(time.Date(2027, 11, 9, 9, 0, 0, 0, time.Local), true) {
		t.Error("年份通配应对任意年份生效")
	}

	// 3) 5 段表达式：秒默认为 0。
	five, err := parseCron("0/10 * * * *")
	if err != nil {
		t.Fatalf("parseCron 5 fields: %v", err)
	}
	if !five.Match(day.Add(10*time.Minute), true) {
		t.Error("5 段 */10 应在第 10 分钟触发")
	}
	if five.Match(day.Add(10*time.Minute).Add(time.Second), true) {
		t.Error("5 段表达式秒固定为 0")
	}

	// 4) Hutool 星期映射：0=周日 ... 6=周六，MON=1。
	monday := day
	for monday.Weekday() != time.Monday {
		monday = monday.AddDate(0, 0, 1)
	}
	monExpr, err := parseCron("0 0 9 * * MON")
	if err != nil {
		t.Fatalf("parseCron MON: %v", err)
	}
	if !monExpr.Match(monday.Add(9*time.Hour), true) {
		t.Error("MON 应命中周一 09:00")
	}
	if monExpr.Match(monday.AddDate(0, 0, 1).Add(9*time.Hour), true) {
		t.Error("MON 不应命中周二")
	}
	// Hutool Part.DAY_OF_WEEK 取值上限为 6（0=Sunday），7 非法。
	if _, err := parseCron("0 0 9 * * 7"); err == nil {
		t.Error("星期 7 超出 Hutool 取值范围应报错")
	}
	// L（字段最大值）与负数取值兼容 Hutool。
	if _, err := parseCron("0 0 9 L * ?"); err != nil {
		t.Errorf("L 应被接受（按字段最大值处理）: %v", err)
	}
	if _, err := parseCron("0 0 9 ? * MON-FRI"); err != nil {
		t.Errorf("星期范围别名应被接受: %v", err)
	}
}
