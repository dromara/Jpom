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
	"fmt"
	"io"
	"os"
	"path/filepath"
	"sort"
	"strconv"
	"strings"
	"sync"
	"sync/atomic"
	"time"

	"github.com/dromara/Jpom/go-agent/internal/app"
)

// 月份/星期别名（与 Hutool Month/Week 一致；星期 0=周日 ... 6=周六）。
var cronMonthNames = map[string]int{
	"JAN": 1, "FEB": 2, "MAR": 3, "APR": 4, "MAY": 5, "JUN": 6,
	"JUL": 7, "AUG": 8, "SEP": 9, "OCT": 10, "NOV": 11, "DEC": 12,
}

var cronWeekNames = map[string]int{
	"SUN": 0, "MON": 1, "TUE": 2, "WED": 3, "THU": 4, "FRI": 5, "SAT": 6,
}

// cronGroup 是一组 cron 字段（Hutool CronPattern 的单个匹配组）。
// 字段顺序：0 秒、1 分、2 时、3 日、4 月、5 周、6 年（仅 7 段表达式有年）。
type cronGroup struct {
	fields        [7][]int
	hasYear       bool
	domRestricted bool
	dowRestricted bool
}

// cronExpression 支持 Hutool CronPattern 的 | 分组 OR 与 5/6/7 段表达式：
//   - 5 段：[分 时 日 月 周]（秒固定为 0，Hutool 语义）
//   - 6 段：[秒 分 时 日 月 周]
//   - 7 段：[秒 分 时 日 月 周 年]（年 1970-2099）
//
// 周字段与 Hutool 一致：0=周日 ... 6=周六（支持 SUN/MON/.../SAT 别名）；
// "?" 与 "*" 等价；支持 L（按字段最大值处理，与 Hutool PartParser.parseAlias 一致）。
type cronExpression struct {
	pattern string
	groups  []cronGroup
}

// cronFieldBounds 描述 Hutool Part 的取值范围（与 Java cn.hutool.cron.pattern.Part 一致）。
var cronFieldBounds = [7][2]int{
	{0, 59},      // SECOND
	{0, 59},      // MINUTE
	{0, 23},      // HOUR
	{1, 31},      // DAY_OF_MONTH
	{1, 12},      // MONTH
	{0, 6},       // DAY_OF_WEEK（0=Sunday）
	{1970, 2099}, // YEAR
}

func parseCron(expression string) (*cronExpression, error) {
	expression = strings.TrimSpace(expression)
	if expression == "" {
		return nil, fmt.Errorf("cron expression must not be empty")
	}
	// Hutool CronPattern 用 | 表示多组表达式（OR）。
	groups := make([]cronGroup, 0, 2)
	for _, part := range strings.Split(expression, "|") {
		part = strings.TrimSpace(part)
		if part == "" {
			return nil, fmt.Errorf("invalid cron expression: %s", expression)
		}
		group, err := parseCronGroup(part)
		if err != nil {
			return nil, fmt.Errorf("invalid cron expression %q: %w", expression, err)
		}
		groups = append(groups, group)
	}
	return &cronExpression{pattern: expression, groups: groups}, nil
}

func parseCronGroup(expression string) (cronGroup, error) {
	fields := strings.Fields(expression)
	switch len(fields) {
	case 5:
		// 5 段没有秒字段，Hutool 会补 "0"。
		fields = append([]string{"0"}, fields...)
	case 6:
	case 7:
	default:
		return cronGroup{}, fmt.Errorf("cron expression must have 5-7 fields")
	}
	group := cronGroup{hasYear: len(fields) == 7}
	for index, field := range fields {
		bounds := cronFieldBounds[index]
		values, err := parseCronField(field, bounds[0], bounds[1], index)
		if err != nil {
			return cronGroup{}, err
		}
		group.fields[index] = values
	}
	group.domRestricted = !isWildcard(fields[3])
	group.dowRestricted = !isWildcard(fields[5])
	return group, nil
}

func isWildcard(field string) bool {
	return field == "*" || field == "?"
}

func parseCronField(field string, min, max, index int) ([]int, error) {
	field = strings.TrimSpace(field)
	if field == "" {
		return nil, fmt.Errorf("empty cron field")
	}
	if field == "*" || field == "?" {
		return rangeInts(min, max), nil
	}
	seen := map[int]struct{}{}
	add := func(value int) error {
		if value < min || value > max {
			return fmt.Errorf("cron field value out of range: %d", value)
		}
		seen[value] = struct{}{}
		return nil
	}
	for _, part := range strings.Split(field, ",") {
		part = strings.TrimSpace(part)
		if part == "" {
			continue
		}
		step := 1
		hasStep := false
		if base, stepValue, found := strings.Cut(part, "/"); found {
			parsed, err := strconv.Atoi(strings.TrimSpace(stepValue))
			if err != nil || parsed <= 0 {
				return nil, fmt.Errorf("invalid cron step: %s", part)
			}
			step = parsed
			hasStep = true
			part = strings.TrimSpace(base)
		}
		start, end := min, max
		if part != "*" && part != "?" {
			if low, high, found := strings.Cut(part, "-"); found {
				parsedLow, err := cronValue(low, index, min, max)
				if err != nil {
					return nil, err
				}
				parsedHigh, err := cronValue(high, index, min, max)
				if err != nil {
					return nil, err
				}
				start, end = parsedLow, parsedHigh
			} else {
				parsed, err := cronValue(part, index, min, max)
				if err != nil {
					return nil, err
				}
				start, end = parsed, parsed
				if hasStep {
					// Hutool/Quartz 语义：a/n 表示从 a 开始按步长到字段上限。
					end = max
				}
			}
		}
		if start > end {
			return nil, fmt.Errorf("invalid cron range: %s", part)
		}
		for value := start; value <= end; value += step {
			if err := add(value); err != nil {
				return nil, err
			}
		}
	}
	values := make([]int, 0, len(seen))
	for value := range seen {
		values = append(values, value)
	}
	sort.Ints(values)
	return values, nil
}

// cronValue 解析单个取值：数字、别名（月份 JAN-DEC、星期 SUN-SAT）、L（字段最大值）
// 以及 Hutool 支持的负数（相对字段最大值，如 -1 表示 max-1）。
func cronValue(raw string, index, min, max int) (int, error) {
	raw = strings.TrimSpace(raw)
	upper := strings.ToUpper(raw)
	if upper == "L" {
		return max, nil
	}
	if index == 4 {
		if value, ok := cronMonthNames[upper]; ok {
			return value, nil
		}
	}
	if index == 5 {
		if value, ok := cronWeekNames[upper]; ok {
			return value, nil
		}
	}
	value, err := strconv.Atoi(raw)
	if err != nil {
		return 0, fmt.Errorf("invalid cron value: %s", raw)
	}
	// Hutool parseNumber：负数加上字段最大值。
	if value < 0 {
		value += max
	}
	return value, nil
}

func rangeInts(min, max int) []int {
	values := make([]int, 0, max-min+1)
	for value := min; value <= max; value++ {
		values = append(values, value)
	}
	return values
}

func containsInt(values []int, target int) bool {
	for _, value := range values {
		if value == target {
			return true
		}
	}
	return false
}

// Match 判断给定时间是否命中表达式（| 分组命中任一组即触发）。
//
// matchSecond 对应 Java 的 jpom.system.timer-match-second：为 false 时 Hutool
// CronTimer 只在每分钟第 0 秒触发并忽略表达式的秒字段（PatternUtil.getFields
// 传入 -1），这里保持同样语义。
func (e *cronExpression) Match(now time.Time, matchSecond bool) bool {
	if !matchSecond && now.Second() != 0 {
		return false
	}
	for _, group := range e.groups {
		if group.match(now, matchSecond) {
			return true
		}
	}
	return false
}

func (g cronGroup) match(now time.Time, matchSecond bool) bool {
	if matchSecond && !containsInt(g.fields[0], now.Second()) {
		return false
	}
	if !containsInt(g.fields[1], now.Minute()) ||
		!containsInt(g.fields[2], now.Hour()) ||
		!containsInt(g.fields[4], int(now.Month())) {
		return false
	}
	if g.hasYear && !containsInt(g.fields[6], now.Year()) {
		return false
	}
	domMatch := containsInt(g.fields[3], now.Day())
	dowMatch := containsInt(g.fields[5], int(now.Weekday()))
	if g.domRestricted && g.dowRestricted {
		// Hutool PatternMatcher：日与周同时指定时为 OR。
		return domMatch || dowMatch
	}
	return domMatch && dowMatch
}

// cronTask 是一个定时任务及其执行统计（对应 hutool CronUtils.list 的字段）。
type cronTask struct {
	id       string
	pattern  string
	expr     *cronExpression
	fn       func()
	mu       sync.Mutex
	lastTime int64
	execute  int64
	failed   int64
	succeed  int64
	running  int32
}

// CronManager 是一个进程内的 cron 调度器（秒级 tick）。
type CronManager struct {
	app         *app.App
	matchSecond bool
	mu          sync.Mutex
	tasks       map[string]*cronTask
	order       []string
	once        sync.Once
}

var globalCron atomic.Pointer[CronManager]

// CronList 返回 /system/cache 的 taskList。
func CronList() []map[string]any {
	manager := globalCron.Load()
	if manager == nil {
		return []map[string]any{}
	}
	return manager.List()
}

// List 返回任务执行统计。
func (m *CronManager) List() []map[string]any {
	m.mu.Lock()
	defer m.mu.Unlock()
	result := make([]map[string]any, 0, len(m.order))
	for _, id := range m.order {
		task := m.tasks[id]
		task.mu.Lock()
		result = append(result, map[string]any{
			"taskId":          task.id,
			"cron":            task.pattern,
			"executeCount":    task.execute,
			"failedCount":     task.failed,
			"succeedCount":    task.succeed,
			"lastExecuteTime": strconv.FormatInt(task.lastTime, 10),
		})
		task.mu.Unlock()
	}
	return result
}

// Upsert 注册/更新一个 cron 任务（对应 CronUtils.upsert）。
func (m *CronManager) Upsert(id, expression string, fn func()) error {
	expr, err := parseCron(expression)
	if err != nil {
		return err
	}
	m.mu.Lock()
	defer m.mu.Unlock()
	task, ok := m.tasks[id]
	if !ok {
		task = &cronTask{id: id}
		m.tasks[id] = task
		m.order = append(m.order, id)
	}
	task.pattern = expression
	task.expr = expr
	task.fn = fn
	return nil
}

// Remove 移除一个 cron 任务（对应 CronUtils.remove(taskId)），
// 并发安全：持 m.mu 删除 map 与 order 中的记录。
func (m *CronManager) Remove(id string) {
	m.mu.Lock()
	defer m.mu.Unlock()
	if _, ok := m.tasks[id]; !ok {
		return
	}
	delete(m.tasks, id)
	for index, current := range m.order {
		if current == id {
			m.order = append(m.order[:index], m.order[index+1:]...)
			break
		}
	}
}

// Start 启动调度循环。
func (m *CronManager) Start() {
	m.once.Do(func() {
		go func() {
			ticker := time.NewTicker(time.Second)
			defer ticker.Stop()
			for now := range ticker.C {
				m.runDue(now)
			}
		}()
	})
}

func (m *CronManager) runDue(now time.Time) {
	// Hutool 在未开启匹配秒时按分钟边界触发（等价「只在 second==0 检查」）。
	if !m.matchSecond && now.Second() != 0 {
		return
	}
	m.mu.Lock()
	tasks := make([]*cronTask, 0, len(m.order))
	for _, id := range m.order {
		tasks = append(tasks, m.tasks[id])
	}
	m.mu.Unlock()
	for _, task := range tasks {
		if task.expr == nil || task.fn == nil || !task.expr.Match(now, m.matchSecond) {
			continue
		}
		if !atomic.CompareAndSwapInt32(&task.running, 0, 1) {
			continue
		}
		task.mu.Lock()
		task.execute++
		task.lastTime = now.UnixMilli()
		task.mu.Unlock()
		go func(current *cronTask) {
			defer atomic.StoreInt32(&current.running, 0)
			defer func() {
				if recovered := recover(); recovered != nil {
					current.mu.Lock()
					current.failed++
					current.mu.Unlock()
					m.app.Log().Error("cron task failed", "taskId", current.id, "panic", recovered)
				}
			}()
			current.fn()
			current.mu.Lock()
			current.succeed++
			current.mu.Unlock()
		}(task)
	}
}

// NewCronManager 创建一个 cron 调度器。
func NewCronManager(a *app.App) *CronManager {
	return &CronManager{
		app:         a,
		matchSecond: a.Config.Jpom.System.TimerMatchSecond,
		tasks:       map[string]*cronTask{},
	}
}

// SetGlobalCron 注册全局调度器（供 /system/cache 读取任务状态）。
func SetGlobalCron(manager *CronManager) { globalCron.Store(manager) }

// RegisterCronTask 允许其它功能域把自己的定时任务注册到统一调度器，
// 使 /system/cache 的 taskList 与 Java 端 CronUtils.list() 保持一致。
// 需在 sysinfo.Start 之后调用（registerProject 在 registerSystem 之后执行）。
func RegisterCronTask(id, expression string, fn func()) error {
	manager := globalCron.Load()
	if manager == nil {
		return fmt.Errorf("cron scheduler is not started")
	}
	return manager.Upsert(id, expression, fn)
}

// RemoveCronTask 移除其它功能域注册的定时任务（对应 Java CronUtils.remove）。
// 脚本保存为无 cron / 删除脚本时必须调用，避免 /system/cache.taskList 残留与旧任务继续触发。
func RemoveCronTask(id string) error {
	manager := globalCron.Load()
	if manager == nil {
		return fmt.Errorf("cron scheduler is not started")
	}
	manager.Remove(id)
	return nil
}

// parseDataSize 解析 Java DataSize 风格的配置值（如 50MB），非法时使用默认值。
func parseDataSize(value string, fallback int64) int64 {
	value = strings.TrimSpace(strings.ToUpper(value))
	if value == "" {
		return fallback
	}
	units := []struct {
		suffix string
		scale  int64
	}{
		{"TB", 1024 * 1024 * 1024 * 1024},
		{"GB", 1024 * 1024 * 1024},
		{"MB", 1024 * 1024},
		{"KB", 1024},
		{"B", 1},
	}
	scale := int64(1)
	for _, unit := range units {
		if strings.HasSuffix(value, unit.suffix) {
			scale = unit.scale
			value = strings.TrimSpace(strings.TrimSuffix(value, unit.suffix))
			break
		}
	}
	parsed, err := strconv.ParseFloat(value, 64)
	if err != nil || parsed < 0 {
		return fallback
	}
	return int64(parsed * float64(scale))
}

func copyFile(source, target string) error {
	input, err := os.Open(source)
	if err != nil {
		return err
	}
	defer func() { _ = input.Close() }()
	if err := os.MkdirAll(filepath.Dir(target), 0o755); err != nil {
		return err
	}
	output, err := os.Create(target)
	if err != nil {
		return err
	}
	defer func() { _ = output.Close() }()
	_, err = io.Copy(output, input)
	return err
}
