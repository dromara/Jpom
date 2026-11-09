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

	"jpom.io/go-agent/internal/app"
)

// cronExpression 是 Quartz 风格的 6/7 段 cron 表达式（秒 分 时 日 月 周 [年]）。
type cronExpression struct {
	fields        [6][]int
	domRestricted bool
	dowRestricted bool
}

var cronMonthNames = map[string]int{
	"JAN": 1, "FEB": 2, "MAR": 3, "APR": 4, "MAY": 5, "JUN": 6,
	"JUL": 7, "AUG": 8, "SEP": 9, "OCT": 10, "NOV": 11, "DEC": 12,
}

var cronWeekNames = map[string]int{
	"SUN": 0, "MON": 1, "TUE": 2, "WED": 3, "THU": 4, "FRI": 5, "SAT": 6,
}

func parseCron(expression string) (*cronExpression, error) {
	fields := strings.Fields(strings.TrimSpace(expression))
	if len(fields) == 5 {
		fields = append([]string{"0"}, fields...)
	}
	if len(fields) < 6 {
		return nil, fmt.Errorf("cron 表达式错误: %s", expression)
	}
	fields = fields[:6]
	bounds := [6][2]int{{0, 59}, {0, 59}, {0, 23}, {1, 31}, {1, 12}, {0, 7}}
	result := &cronExpression{}
	for index, field := range fields {
		values, err := parseCronField(field, bounds[index][0], bounds[index][1], index)
		if err != nil {
			return nil, fmt.Errorf("cron 表达式错误 %q: %w", expression, err)
		}
		result.fields[index] = values
	}
	result.domRestricted = !isWildcard(fields[3])
	result.dowRestricted = !isWildcard(fields[5])
	return result, nil
}

func isWildcard(field string) bool {
	return field == "*" || field == "?"
}

func parseCronField(field string, min, max, index int) ([]int, error) {
	field = strings.TrimSpace(field)
	if field == "" {
		return nil, fmt.Errorf("空字段")
	}
	if field == "*" || field == "?" {
		return rangeInts(min, max), nil
	}
	seen := map[int]struct{}{}
	add := func(value int) error {
		if value < min || value > max {
			return fmt.Errorf("取值越界: %d", value)
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
				return nil, fmt.Errorf("步长错误: %s", part)
			}
			step = parsed
			hasStep = true
			part = strings.TrimSpace(base)
		}
		start, end := min, max
		if part != "*" && part != "?" {
			if low, high, found := strings.Cut(part, "-"); found {
				parsedLow, err := cronValue(low, index)
				if err != nil {
					return nil, err
				}
				parsedHigh, err := cronValue(high, index)
				if err != nil {
					return nil, err
				}
				start, end = parsedLow, parsedHigh
			} else {
				parsed, err := cronValue(part, index)
				if err != nil {
					return nil, err
				}
				start, end = parsed, parsed
				if hasStep {
					// Quartz 语义：a/n 表示从 a 开始按步长一直到字段上限。
					end = max
				}
			}
		}
		if start > end {
			return nil, fmt.Errorf("范围错误: %s", part)
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

func cronValue(raw string, index int) (int, error) {
	raw = strings.TrimSpace(strings.ToUpper(raw))
	if index == 4 {
		if value, ok := cronMonthNames[raw]; ok {
			return value, nil
		}
	}
	if index == 5 {
		if value, ok := cronWeekNames[raw]; ok {
			// Quartz 中 1=周日 ... 7=周六，这里统一转成 Go 的 0=周日。
			return (value + 1) % 7, nil
		}
	}
	value, err := strconv.Atoi(raw)
	if err != nil {
		return 0, fmt.Errorf("非法取值: %s", raw)
	}
	if index == 5 && value == 7 {
		value = 0
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

// Match 判断给定时间是否命中表达式。
//
// matchSecond 对应 Java 的 jpom.system.timer-match-second：为 false 时 Hutool
// CronTimer 只在每分钟第 0 秒触发并忽略表达式的秒字段（PatternUtil.getFields
// 传入 -1），这里保持同样语义。
func (e *cronExpression) Match(now time.Time, matchSecond bool) bool {
	if matchSecond {
		if !containsInt(e.fields[0], now.Second()) {
			return false
		}
	} else if now.Second() != 0 {
		// 未开启秒级匹配时只在分钟边界（第 0 秒）触发。
		return false
	}
	if !containsInt(e.fields[1], now.Minute()) ||
		!containsInt(e.fields[2], now.Hour()) ||
		!containsInt(e.fields[4], int(now.Month())) {
		return false
	}
	domMatch := containsInt(e.fields[3], now.Day())
	dowMatch := containsInt(e.fields[5], int(now.Weekday()))
	if e.domRestricted && e.dowRestricted {
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
					m.app.Log().Error("定时任务异常", "taskId", current.id, "panic", recovered)
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
		return fmt.Errorf("cron 调度器尚未启动")
	}
	return manager.Upsert(id, expression, fn)
}

// RemoveCronTask 移除其它功能域注册的定时任务（对应 Java CronUtils.remove）。
// 脚本保存为无 cron / 删除脚本时必须调用，避免 /system/cache.taskList 残留与旧任务继续触发。
func RemoveCronTask(id string) error {
	manager := globalCron.Load()
	if manager == nil {
		return fmt.Errorf("cron 调度器尚未启动")
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
