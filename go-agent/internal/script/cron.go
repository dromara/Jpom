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
	"strconv"
	"strings"
	"time"
)

// This file keeps the script autoExecCron integration. The actual scheduling
// loop lives in the platform domain (sysinfo.CronManager, exposed through
// sysinfo.RegisterCronTask) so /system/cache.taskList sees every agent cron task
// exactly like the Java CronUtils registry. Here we only provide:
//   - a Hutool CronPattern compatible parser (validation + semantics tests),
//   - the expression normalization required by jpom.system.timer-match-second,
//   - the Java CronUtils task id ("script:"+scriptId),
//   - the execution trigger (NodeScriptServer.CronTask).

// cronField describes one field of a Hutool CronPattern.
type cronField struct {
	name     string
	min, max int
	aliases  map[string]int
}

var cronFields = []cronField{
	{name: "second", min: 0, max: 59},
	{name: "minute", min: 0, max: 59},
	{name: "hour", min: 0, max: 23},
	{name: "dayOfMonth", min: 1, max: 31},
	{name: "month", min: 1, max: 12, aliases: map[string]int{
		"jan": 1, "feb": 2, "mar": 3, "apr": 4, "may": 5, "jun": 6,
		"jul": 7, "aug": 8, "sep": 9, "oct": 10, "nov": 11, "dec": 12,
	}},
	// Hutool dayOfWeek: 0=Sunday, 1=Monday ... 6=Saturday.
	{name: "dayOfWeek", min: 0, max: 6, aliases: map[string]int{
		"sun": 0, "mon": 1, "tue": 2, "wed": 3, "thu": 4, "fri": 5, "sat": 6,
	}},
	{name: "year", min: 1970, max: 2099},
}

// cronPart matches one field of one cron group. A nil any field matches
// everything (PartParser AlwaysTrueMatcher for "*" and "?").
type cronPart struct {
	any    bool
	values map[int]struct{}
}

func (p cronPart) matches(value int) bool {
	if p.any {
		return true
	}
	_, ok := p.values[value]
	return ok
}

// cronGroup is one whitespace separated expression (the "|" separated groups are
// OR-ed), always normalized to 7 fields.
type cronGroup struct {
	parts [7]cronPart
}

// CronPattern is a parsed Hutool CronPattern compatible expression.
type CronPattern struct {
	source string
	groups []cronGroup
}

// ParseCron parses a Hutool CronPattern expression (5, 6 or 7 whitespace
// separated fields, optionally "|" separated groups). A 5 field expression is
// minute based (second defaults to 0), a 6 field expression starts with seconds.
//
// This mirrors cn.hutool.cron.pattern.parser.PatternParser so the autoExecCron
// values stored by the Java agent are validated/interpreted identically.
func ParseCron(expression string) (*CronPattern, error) {
	trimmed := strings.TrimSpace(expression)
	if trimmed == "" {
		return nil, fmt.Errorf("cron 表达式格式不正确")
	}
	pattern := &CronPattern{source: trimmed}
	for _, groupText := range strings.Split(trimmed, "|") {
		group := cronGroup{}
		fields := strings.Fields(groupText)
		if len(fields) < 5 || len(fields) > 7 {
			return nil, fmt.Errorf("cron 表达式格式不正确: %s", expression)
		}
		if len(fields) == 5 {
			fields = append([]string{"0"}, fields...)
		}
		if len(fields) == 6 {
			fields = append(fields, "*")
		}
		for index := 0; index < 7; index++ {
			part, err := parseCronPart(cronFields[index], fields[index])
			if err != nil {
				return nil, fmt.Errorf("cron 表达式格式不正确: %s (%v)", expression, err)
			}
			group.parts[index] = part
		}
		pattern.groups = append(pattern.groups, group)
	}
	return pattern, nil
}

// parseCronPart parses one field: "*", "?", "a", "a-b", "a/n", "*/n", "a-b/n"
// and comma separated lists of those.
func parseCronPart(field cronField, text string) (cronPart, error) {
	text = strings.TrimSpace(text)
	if text == "" {
		return cronPart{}, fmt.Errorf("%s 字段为空", field.name)
	}
	if text == "*" || text == "?" {
		return cronPart{any: true}, nil
	}
	values := map[int]struct{}{}
	for _, item := range strings.Split(text, ",") {
		item = strings.TrimSpace(item)
		if item == "" {
			return cronPart{}, fmt.Errorf("%s 字段存在空项", field.name)
		}
		base := item
		step := 1
		if index := strings.Index(item, "/"); index >= 0 {
			base = item[:index]
			stepText := item[index+1:]
			parsed, err := strconv.Atoi(stepText)
			if err != nil || parsed <= 0 {
				return cronPart{}, fmt.Errorf("%s 步长不正确: %s", field.name, item)
			}
			step = parsed
		}
		var start, end int
		switch {
		case base == "*" || base == "?":
			start, end = field.min, field.max
		case strings.Contains(base, "-"):
			pieces := strings.SplitN(base, "-", 2)
			parsedStart, err := parseCronValue(field, pieces[0])
			if err != nil {
				return cronPart{}, err
			}
			parsedEnd, err := parseCronValue(field, pieces[1])
			if err != nil {
				return cronPart{}, err
			}
			start, end = parsedStart, parsedEnd
		default:
			parsed, err := parseCronValue(field, base)
			if err != nil {
				return cronPart{}, err
			}
			start, end = parsed, parsed
			if step > 1 {
				end = field.max
			}
		}
		if start > end {
			return cronPart{}, fmt.Errorf("%s 范围不正确: %s", field.name, item)
		}
		for value := start; value <= end; value += step {
			values[value] = struct{}{}
		}
	}
	return cronPart{values: values}, nil
}

func parseCronValue(field cronField, text string) (int, error) {
	text = strings.TrimSpace(text)
	if field.aliases != nil {
		if value, ok := field.aliases[strings.ToLower(text)]; ok {
			return value, nil
		}
	}
	value, err := strconv.Atoi(text)
	if err != nil {
		return 0, fmt.Errorf("%s 值不正确: %s", field.name, text)
	}
	if value < field.min || value > field.max {
		return 0, fmt.Errorf("%s 值超出范围: %d", field.name, value)
	}
	return value, nil
}

// Match reports whether the pattern matches now. When matchSecond is false the
// seconds field is ignored (Hutool CronPattern#match with isMatchSecond=false).
func (p *CronPattern) Match(now time.Time, matchSecond bool) bool {
	if p == nil {
		return false
	}
	values := [7]int{
		now.Second(), now.Minute(), now.Hour(), now.Day(), int(now.Month()),
		int(now.Weekday()), now.Year(),
	}
	for _, group := range p.groups {
		if group.matches(values, matchSecond) {
			return true
		}
	}
	return false
}

func (g cronGroup) matches(values [7]int, matchSecond bool) bool {
	if matchSecond && !g.parts[0].matches(values[0]) {
		return false
	}
	if !g.parts[1].matches(values[1]) || !g.parts[2].matches(values[2]) {
		return false
	}
	if !g.parts[4].matches(values[4]) || !g.parts[6].matches(values[6]) {
		return false
	}
	dayOfMonth := g.parts[3]
	dayOfWeek := g.parts[5]
	switch {
	case dayOfMonth.any && dayOfWeek.any:
		return true
	case dayOfMonth.any:
		return dayOfWeek.matches(values[5])
	case dayOfWeek.any:
		return dayOfMonth.matches(values[3])
	default:
		// Quartz 语义：两者都配置时取或
		return dayOfMonth.matches(values[3]) || dayOfWeek.matches(values[5])
	}
}

// CronTaskID is the Java CronUtils task id of a script ("script:"+id).
func CronTaskID(scriptID string) string { return "script:" + scriptID }

// CronRegistry is the unified agent cron scheduler
// (sysinfo.CronManager / sysinfo.RegisterCronTask).
type CronRegistry interface {
	// Register creates/updates a cron task.
	Register(id, expression string, fn func()) error
	// Remove deletes a cron task (CronUtils.remove).
	Remove(id string) error
}

// NormalizeCron validates the expression for the unified scheduler and returns
// the expression to register.
//
// jpom.system.timer-match-second is honored by sysinfo.CronManager (it ignores
// the seconds field and only ticks at minute boundaries when disabled, exactly
// like Hutool), so the expression is kept as-is and taskList shows the original
// value like the Java agent. A leading "!" disables the schedule.
func NormalizeCron(expression string, matchSecond bool) (string, error) {
	trimmed := strings.TrimSpace(expression)
	if trimmed == "" || strings.HasPrefix(trimmed, "!") {
		return "", nil
	}
	if strings.Contains(trimmed, "|") {
		return "", fmt.Errorf("暂不支持 | 分组的 cron 表达式: %s", trimmed)
	}
	if _, err := ParseCron(trimmed); err != nil {
		return "", err
	}
	return trimmed, nil
}

// RegisterScriptCrons registers every script with an autoExecCron expression at
// agent startup (Java NodeScriptServer implements ICron#queryStartingList).
func (m *Manager) RegisterScriptCrons(registry CronRegistry, executor *Executor, matchSecond bool, lang string) {
	list, err := m.List()
	if err != nil {
		m.app.Log().Error("load scripts for cron registration failed", "error", err)
		return
	}
	for _, item := range list {
		m.UpsertScriptCron(registry, executor, stringField(item, "id"), matchSecond, lang)
	}
}

// UpsertScriptCron re-registers one script after save.json; an empty/disabled
// cron removes the task (Java NodeScriptServer#checkCron).
func (m *Manager) UpsertScriptCron(registry CronRegistry, executor *Executor, scriptID string, matchSecond bool, lang string) {
	if strings.TrimSpace(scriptID) == "" {
		return
	}
	item, exists, err := m.Get(scriptID)
	if err != nil {
		m.app.Log().Error("load script for cron registration failed", "id", scriptID, "error", err)
		return
	}
	taskID := CronTaskID(scriptID)
	if !exists {
		m.RemoveScriptCron(registry, scriptID)
		return
	}
	expression, err := NormalizeCron(stringField(item, "autoExecCron"), matchSecond)
	if err != nil {
		m.app.Log().Warn("invalid script autoExecCron", "id", scriptID, "error", err)
		m.RemoveScriptCron(registry, scriptID)
		return
	}
	if expression == "" {
		m.RemoveScriptCron(registry, scriptID)
		return
	}
	if err := registry.Register(taskID, expression, func() { m.RunCronScript(executor, scriptID, lang) }); err != nil {
		m.app.Log().Error("register script cron failed", "id", scriptID, "cron", expression, "error", err)
	}
}

// RemoveScriptCron removes the cron task of one script (delete.json).
func (m *Manager) RemoveScriptCron(registry CronRegistry, scriptID string) {
	if strings.TrimSpace(scriptID) == "" {
		return
	}
	if err := registry.Remove(CronTaskID(scriptID)); err != nil {
		m.app.Log().Debug("remove script cron failed", "id", scriptID, "error", err)
	}
}

// RunCronScript triggers one automatic execution, mirroring
// NodeScriptServer.CronTask: it re-reads the script (so edits apply), creates the
// execution record with triggerExecType=1 and runs with the default arguments.
// The Java cron task intentionally does not update lastRunUser.
func (m *Manager) RunCronScript(executor *Executor, scriptID, lang string) {
	item, exists, err := m.Get(scriptID)
	if err != nil || !exists {
		return
	}
	executeID := NewExecutionID()
	record := map[string]any{
		"id":               executeID,
		"createTimeMillis": time.Now().UnixMilli(),
		"scriptId":         scriptID,
		"scriptName":       stringField(item, "name"),
		"workspaceId":      workspaceIDOf(item),
		"triggerExecType":  1,
	}
	if err := m.AddExecLog(record); err != nil {
		m.app.Log().Error("add script cron exec log failed", "id", scriptID, "error", err)
		return
	}
	if err := executor.Start(item, executeID, stringField(item, "defArgs"), nil, lang); err != nil {
		m.app.Log().Error("start cron script failed", "id", scriptID, "error", err)
	}
}

func workspaceIDOf(item map[string]any) string {
	workspaceID := stringField(item, "workspaceId")
	if workspaceID == "" {
		return "DEFAULT"
	}
	return workspaceID
}
