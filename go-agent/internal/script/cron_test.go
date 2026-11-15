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
	"os"
	"sync"
	"testing"
	"time"
)

func mustCron(t *testing.T, expression string) *CronPattern {
	t.Helper()
	pattern, err := ParseCron(expression)
	if err != nil {
		t.Fatalf("parse %q: %v", expression, err)
	}
	return pattern
}

// fakeCronRegistry emulates sysinfo.CronManager for unit tests.
type fakeCronRegistry struct {
	mu    sync.Mutex
	tasks map[string]fakeCronTask
}

type fakeCronTask struct {
	expression string
	fn         func()
}

func newFakeCronRegistry() *fakeCronRegistry {
	return &fakeCronRegistry{tasks: map[string]fakeCronTask{}}
}

func (r *fakeCronRegistry) Register(id, expression string, fn func()) error {
	r.mu.Lock()
	defer r.mu.Unlock()
	r.tasks[id] = fakeCronTask{expression: expression, fn: fn}
	return nil
}

func (r *fakeCronRegistry) Remove(id string) error {
	r.mu.Lock()
	defer r.mu.Unlock()
	delete(r.tasks, id)
	return nil
}

func (r *fakeCronRegistry) get(id string) (fakeCronTask, bool) {
	r.mu.Lock()
	defer r.mu.Unlock()
	task, ok := r.tasks[id]
	return task, ok
}

func (r *fakeCronRegistry) size() int {
	r.mu.Lock()
	defer r.mu.Unlock()
	return len(r.tasks)
}

func TestParseCron(t *testing.T) {
	valid := []string{
		"0 0/5 * * * ?",
		"0 0 0/1 * * ?",
		"0 0 0 1 * ?",
		"0 0 0 * * 1",
		"0 0 0 * * *",
		"*/10 * * * * ?",
		"0 0 12 * * MON-FRI",
		"0 0 12 1 JAN,DEC ?",
		"0 0 0 1 1 ? 2027",
		"0 0 9-18 * * ?",
		"0 0 0 * * ? | 0 0 12 * * ?",
	}
	for _, expression := range valid {
		if _, err := ParseCron(expression); err != nil {
			t.Fatalf("expected valid: %q: %v", expression, err)
		}
	}
	invalid := []string{"", "* * *", "60 * * * * ?", "0 0 0 32 * ?", "0 0 0 * 13 ?", "0 0 0 * * 7", "a b c d e f", "0 0 0 * * ? 1960"}
	for _, expression := range invalid {
		if _, err := ParseCron(expression); err == nil {
			t.Fatalf("expected invalid: %q", expression)
		}
	}
}

func TestCronMatchMinuteLevel(t *testing.T) {
	pattern := mustCron(t, "0 0/5 * * * ?")
	at := func(minute, second int) time.Time {
		return time.Date(2026, 11, 9, 10, minute, second, 0, time.Local)
	}
	if !pattern.Match(at(5, 37), false) {
		t.Fatal("minute 5 must match when seconds are ignored")
	}
	if pattern.Match(at(6, 0), false) {
		t.Fatal("minute 6 must not match")
	}
	if !pattern.Match(at(10, 0), false) {
		t.Fatal("minute 10 must match")
	}
	if pattern.Match(at(5, 37), true) {
		t.Fatal("second 37 must not match the seconds field 0")
	}
	if !pattern.Match(at(5, 0), true) {
		t.Fatal("second 0 must match")
	}
}

func TestCronMatchSecondStep(t *testing.T) {
	pattern := mustCron(t, "*/10 * * * * ?")
	base := time.Date(2026, 11, 9, 10, 0, 0, 0, time.Local)
	for _, second := range []int{0, 10, 20, 30, 40, 50} {
		if !pattern.Match(base.Add(time.Duration(second)*time.Second), true) {
			t.Fatalf("second %d must match */10", second)
		}
	}
	for _, second := range []int{1, 9, 15, 59} {
		if pattern.Match(base.Add(time.Duration(second)*time.Second), true) {
			t.Fatalf("second %d must not match */10", second)
		}
	}
}

func TestCronMatchDayOfWeekAndMonth(t *testing.T) {
	monday := time.Date(2026, 11, 9, 12, 0, 0, 0, time.Local)
	if monday.Weekday() != time.Monday {
		t.Fatalf("fixture is not a Monday: %v", monday.Weekday())
	}
	pattern := mustCron(t, "0 0 12 * * 1")
	if !pattern.Match(monday, true) {
		t.Fatal("Monday must match dayOfWeek 1")
	}
	if pattern.Match(monday.AddDate(0, 0, 1), true) {
		t.Fatal("Tuesday must not match dayOfWeek 1")
	}
	december := time.Date(2026, 12, 1, 12, 0, 0, 0, time.Local)
	if !mustCron(t, "0 0 12 1 DEC ?").Match(december, true) {
		t.Fatal("DEC alias must match December")
	}
	both := mustCron(t, "0 0 12 1 * 1")
	if !both.Match(monday, true) {
		t.Fatal("Monday must match the dayOfWeek branch")
	}
	if !both.Match(time.Date(2026, 11, 1, 12, 0, 0, 0, time.Local), true) {
		t.Fatal("1st must match the dayOfMonth branch")
	}
	if both.Match(time.Date(2026, 11, 3, 12, 0, 0, 0, time.Local), true) {
		t.Fatal("Tuesday the 3rd must not match")
	}
}

func TestCronMatchYearAndGroups(t *testing.T) {
	at2026 := time.Date(2026, 11, 9, 12, 0, 0, 0, time.Local)
	// 7 段年份字段按 now.Year() 匹配
	yearPattern := mustCron(t, "0 0 12 * * ? 2026")
	if !yearPattern.Match(at2026, true) {
		t.Fatal("2026 must match the year field")
	}
	if yearPattern.Match(time.Date(2027, 11, 9, 12, 0, 0, 0, time.Local), true) {
		t.Fatal("2027 must not match the year field 2026")
	}
	// 年份通配
	if !mustCron(t, "0 0 12 * * ? *").Match(at2026, true) {
		t.Fatal("wildcard year must match")
	}
	// | 分组 OR（不同时间点分别命中不同分组）
	groups := mustCron(t, "0 0 9 * * ? | 0 0 18 * * ?")
	if !groups.Match(time.Date(2026, 11, 9, 9, 0, 0, 0, time.Local), true) {
		t.Fatal("9:00 must match the first group")
	}
	if !groups.Match(time.Date(2026, 11, 9, 18, 0, 0, 0, time.Local), true) {
		t.Fatal("18:00 must match the second group")
	}
	if groups.Match(time.Date(2026, 11, 9, 12, 0, 0, 0, time.Local), true) {
		t.Fatal("12:00 must not match any group")
	}
	// 闰年边界：2028-02-29 12:00 匹配 2 月 29 日
	leap := mustCron(t, "0 0 12 29 2 ?")
	if !leap.Match(time.Date(2028, 2, 29, 12, 0, 0, 0, time.Local), true) {
		t.Fatal("leap day must match")
	}
	if leap.Match(time.Date(2027, 2, 28, 12, 0, 0, 0, time.Local), true) {
		t.Fatal("non leap day must not match")
	}
	// 跨月：月末 31 日
	if !mustCron(t, "0 0 12 31 * ?").Match(time.Date(2026, 12, 31, 12, 0, 0, 0, time.Local), true) {
		t.Fatal("month end must match")
	}
}

func TestNormalizeCronTimerMatchSecond(t *testing.T) {
	// matchSecond=true keeps the expression untouched
	cases := []string{"0 0/5 * * * ?", "*/5 * * * * ?", "0/5 * * * ?", "0 0 12 1 JAN ?"}
	for _, expression := range cases {
		normalized, err := NormalizeCron(expression, true)
		if err != nil {
			t.Fatal(err)
		}
		if normalized != expression {
			t.Fatalf("matchSecond=true must keep %q, got %q", expression, normalized)
		}
	}
	// matchSecond=false: the expression is registered unchanged, sysinfo honors
	// the config and ignores the seconds field (Java compatible taskList value).
	for _, expression := range []string{"*/5 * * * * ?", "0 0/5 * * * ?", "0/5 * * * ?"} {
		if normalized, err := NormalizeCron(expression, false); err != nil || normalized != expression {
			t.Fatalf("matchSecond=false must keep %q, got %q (%v)", expression, normalized, err)
		}
	}
	// | groups and 7 field year expressions are registered unchanged
	orExpr := "0 0 9 * * ? | 0 0 18 * * ?"
	if normalized, err := NormalizeCron(orExpr, false); err != nil || normalized != orExpr {
		t.Fatalf("or group expression must be kept: %q %v", normalized, err)
	}
	yearExpr := "0 0 9 9 11 ? 2026"
	if normalized, err := NormalizeCron(yearExpr, true); err != nil || normalized != yearExpr {
		t.Fatalf("7 field expression must be kept: %q %v", normalized, err)
	}
	// disabled / invalid
	if normalized, err := NormalizeCron("!0 0/5 * * * ?", false); err != nil || normalized != "" {
		t.Fatalf("disabled cron must normalize to empty: %q %v", normalized, err)
	}
	// 表达式合法性由 sysinfo（Hutool 超集）在注册时校验；这里只做启用/禁用判断，
	// 因此 L/负数等 Hutool 扩展语法必须原样透传，非法表达式也不会被误拦截。
	for _, expression := range []string{"0 0 9 L * ?", "0 0 9 * * ? -1", "bad cron"} {
		if normalized, err := NormalizeCron(expression, true); err != nil || normalized != expression {
			t.Fatalf("expression must be passed through for sysinfo validation: %q (%q %v)", expression, normalized, err)
		}
	}
}

func TestRegisterScriptCronsAndTrigger(t *testing.T) {
	instance := testApp(t)
	manager := NewManager(instance)
	executor := NewExecutor(manager)
	scripts := []map[string]any{
		{"id": "cron-1", "name": "cron", "context": "echo cron-run", "workspaceId": "DEFAULT",
			"autoExecCron": "*/1 * * * * ?", "defArgs": "[]"},
		{"id": "cron-2", "name": "no-cron", "context": "echo never", "workspaceId": "DEFAULT"},
		{"id": "cron-3", "name": "disabled", "context": "echo never", "workspaceId": "DEFAULT",
			"autoExecCron": "!0 0/5 * * * ?"},
	}
	for _, item := range scripts {
		if err := manager.Add(item); err != nil {
			t.Fatal(err)
		}
	}
	registry := newFakeCronRegistry()
	manager.RegisterScriptCrons(registry, executor, true, "en-US")
	if registry.size() != 1 {
		t.Fatalf("only one cron task expected, got %d", registry.size())
	}
	task, ok := registry.get(CronTaskID("cron-1"))
	if !ok {
		t.Fatal("cron task for cron-1 missing")
	}
	if task.expression != "*/1 * * * * ?" {
		t.Fatalf("unexpected registered expression: %s", task.expression)
	}
	// trigger the registered callback (the unified scheduler calls it)
	task.fn()
	deadline := time.Now().Add(10 * time.Second)
	var records []map[string]any
	for time.Now().Before(deadline) {
		list, err := manager.ListExecLogs()
		if err != nil {
			t.Fatal(err)
		}
		if len(list) > 0 {
			records = list
			break
		}
		time.Sleep(20 * time.Millisecond)
	}
	if len(records) == 0 {
		t.Fatal("cron callback did not create the execution record")
	}
	if intField(records[0], "triggerExecType") != 1 {
		t.Fatalf("unexpected triggerExecType: %v", records[0]["triggerExecType"])
	}
	executeID := stringField(records[0], "id")
	deadline = time.Now().Add(10 * time.Second)
	for time.Now().Before(deadline) {
		if _, err := os.Stat(manager.LogFile("cron-1", executeID)); err == nil {
			break
		}
		time.Sleep(20 * time.Millisecond)
	}
	if _, err := os.Stat(manager.LogFile("cron-1", executeID)); err != nil {
		t.Fatal("cron execution log file not created")
	}
	// Java cron task does not update lastRunUser
	stored, _, _ := manager.Get("cron-1")
	if _, ok := stored["lastRunUser"]; ok {
		t.Fatalf("cron must not update lastRunUser: %v", stored["lastRunUser"])
	}
}

func TestUpsertAndRemoveScriptCron(t *testing.T) {
	instance := testApp(t)
	manager := NewManager(instance)
	executor := NewExecutor(manager)
	item := map[string]any{"id": "s1", "name": "n", "context": "echo hi", "workspaceId": "DEFAULT",
		"autoExecCron": "0 0/5 * * * ?"}
	if err := manager.Add(item); err != nil {
		t.Fatal(err)
	}
	registry := newFakeCronRegistry()
	manager.UpsertScriptCron(registry, executor, "s1", true, "zh-CN")
	if task, ok := registry.get(CronTaskID("s1")); !ok || task.expression != "0 0/5 * * * ?" {
		t.Fatalf("unexpected registered task: %+v %v", task, ok)
	}
	// change the expression -> upsert updates in place
	if err := manager.UpdateFields("s1", map[string]any{"autoExecCron": "0 0/10 * * * ?"}); err != nil {
		t.Fatal(err)
	}
	manager.UpsertScriptCron(registry, executor, "s1", true, "zh-CN")
	if task, ok := registry.get(CronTaskID("s1")); !ok || task.expression != "0 0/10 * * * ?" {
		t.Fatalf("cron not updated: %+v %v", task, ok)
	}
	// clear the expression -> task removed
	if err := manager.UpdateFields("s1", map[string]any{"autoExecCron": ""}); err != nil {
		t.Fatal(err)
	}
	manager.UpsertScriptCron(registry, executor, "s1", true, "zh-CN")
	if _, ok := registry.get(CronTaskID("s1")); ok {
		t.Fatal("cleared cron must be removed")
	}
	// disabled "!" -> task removed
	if err := manager.UpdateFields("s1", map[string]any{"autoExecCron": "!0 0/5 * * * ?"}); err != nil {
		t.Fatal(err)
	}
	manager.UpsertScriptCron(registry, executor, "s1", true, "zh-CN")
	if _, ok := registry.get(CronTaskID("s1")); ok {
		t.Fatal("disabled cron must not be registered")
	}
	// register again then delete the script -> explicit remove
	if err := manager.UpdateFields("s1", map[string]any{"autoExecCron": "0 0/5 * * * ?"}); err != nil {
		t.Fatal(err)
	}
	manager.UpsertScriptCron(registry, executor, "s1", true, "zh-CN")
	if _, ok := registry.get(CronTaskID("s1")); !ok {
		t.Fatal("cron must be registered before removal")
	}
	manager.RemoveScriptCron(registry, "s1")
	if _, ok := registry.get(CronTaskID("s1")); ok {
		t.Fatal("cron task must be removed")
	}
}

func TestNormalizeCronPadsFiveField(t *testing.T) {
	// A 5 field minute based expression must be kept as-is for the unified
	// scheduler (sysinfo prepends the seconds field).
	normalized, err := NormalizeCron("0/5 * * * ?", false)
	if err != nil {
		t.Fatal(err)
	}
	if normalized != "0/5 * * * ?" {
		t.Fatalf("unexpected five field normalization: %q", normalized)
	}
}
