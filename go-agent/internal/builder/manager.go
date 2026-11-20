//
// Copyright (c) 2019 Of Him Code Technology Studio
// Jpom is licensed under Mulan PSL v2.
// You can use this software according to the terms and conditions of the Mulan PSL v2.
// You may obtain a copy of Mulan PSL v2 at:
// 			http://license.coscl.org.cn/MulanPSL2
// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
// See the Mulan PSL v2 for more details.
//

package builder

import (
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/dromara/Jpom/go-agent/internal/app"
	"github.com/dromara/Jpom/go-agent/internal/proc"
)

// 业务码映射（PROTOCOL §2）：ErrBusy → 409，ErrUnsupported → 501。
var (
	// ErrBusy 并发已满，Server 排队重试。
	ErrBusy = errors.New("build node is busy")
	// ErrUnsupported 该 job 不被本节点支持（如 svn 仓库）。
	ErrUnsupported = errors.New("build job is not supported by this agent")
	// ErrJobNotFound job 不存在（状态查询/产物下载）。
	ErrJobNotFound = errors.New("build job not found")
)

// DefaultTimeoutSeconds 是 job 未下发 timeoutSec 时的兜底超时（2 小时）。
const DefaultTimeoutSeconds = 7200

// jobRecord 是 data/build/jobs/<jobId>.json 的结构（重启恢复用）。
type jobRecord struct {
	JobID         string `json:"jobId"`
	BuildModelID  string `json:"buildModelId"`
	BuildNumberID int    `json:"buildNumberId"`
	State         string `json:"state"`
	Stage         string `json:"stage"`
	Pid           int    `json:"pid"`
	Pgid          int    `json:"pgid"`
	ExitCode      int    `json:"exitCode"`
	Message       string `json:"message"`
	StartedAt     string `json:"startedAt"`
	FinishedAt    string `json:"finishedAt"`
	LogFile       string `json:"logFile"`
	SourceDir     string `json:"sourceDir"`
	ZipPath       string `json:"zipPath"`
	ResultReady   bool   `json:"resultReady"`
	ResultSize    int64  `json:"resultSize"`
	ResultSha1    string `json:"resultSha1"`
	LastSeq       int    `json:"lastSeq"`
}

// Manager 是插件端构建执行器：并发闸门、job 注册表、状态持久化、重启恢复、事件补发。
type Manager struct {
	app *app.App

	dataDir string // <data>/build
	jobsDir string // <data>/build/jobs
	tmpDir  string // <data>/tmp（脚本临时文件）
	charset string // jpom.system.console-charset
	lang    string

	maxConcurrency int
	keepLogDays    int

	mu      sync.Mutex
	jobs    map[string]*Job
	records map[string]jobRecord
	running int
}

// NewManager 创建执行器并完成重启恢复（running → interrupted）与过期清理。
func NewManager(a *app.App) *Manager {
	charset := ""
	lang := ""
	maxConcurrency := 1
	keepLogDays := 7
	if a != nil && a.Config != nil {
		charset = a.Config.Jpom.System.ConsoleCharset
		lang = a.Config.Jpom.System.Lang
		maxConcurrency = a.Config.BuildMaxConcurrency()
		keepLogDays = a.Config.BuildKeepLogDays()
	}
	if strings.TrimSpace(charset) == "" {
		charset = proc.DefaultSystemCharset()
	}
	dataDir := filepath.Join(a.DataDir, "build")
	manager := &Manager{
		app:            a,
		dataDir:        dataDir,
		jobsDir:        filepath.Join(dataDir, "jobs"),
		tmpDir:         a.TmpDir,
		charset:        charset,
		lang:           lang,
		maxConcurrency: maxConcurrency,
		keepLogDays:    keepLogDays,
		jobs:           map[string]*Job{},
		records:        map[string]jobRecord{},
	}
	_ = os.MkdirAll(manager.jobsDir, 0o755)
	manager.recover()
	go manager.cleanupExpired()
	return manager
}

// MaxConcurrency 返回并发上限（构建列表/诊断用）。
func (m *Manager) MaxConcurrency() int { return m.maxConcurrency }

// Start 受理一个 job：并发已满返回 ErrBusy，仓库类型不支持返回 ErrUnsupported。
func (m *Manager) Start(spec *Spec) (*Job, error) {
	if spec == nil {
		return nil, errors.New("build job is required")
	}
	if strings.TrimSpace(spec.JobID) == "" {
		return nil, errors.New("jobId is required")
	}
	repoType := strings.ToLower(strings.TrimSpace(spec.Repo.Type))
	if repoType != "" && repoType != "git" {
		return nil, fmt.Errorf("%w: repo type %s", ErrUnsupported, spec.Repo.Type)
	}
	m.mu.Lock()
	if existing, ok := m.jobs[spec.JobID]; ok && !existing.terminal() {
		// 重复投递（Server 重试）按已受理返回，避免产生两个并发 job。
		m.mu.Unlock()
		return existing, nil
	}
	if m.running >= m.maxConcurrency {
		m.mu.Unlock()
		return nil, ErrBusy
	}
	job, err := newJob(m, spec)
	if err != nil {
		m.mu.Unlock()
		return nil, err
	}
	m.jobs[spec.JobID] = job
	m.running++
	m.mu.Unlock()
	job.persist()
	go job.run()
	return job, nil
}

// Status 返回 job 状态；内存中没有时回查持久化记录（重启后报 lost）。
func (m *Manager) Status(jobID string) (*Status, error) {
	jobID = strings.TrimSpace(jobID)
	if jobID == "" {
		return nil, ErrJobNotFound
	}
	m.mu.Lock()
	job := m.jobs[jobID]
	record, hasRecord := m.records[jobID]
	m.mu.Unlock()
	if job != nil {
		return job.status(), nil
	}
	if hasRecord {
		return recordStatus(record), nil
	}
	loaded, err := readRecord(m.jobFile(jobID))
	if err != nil {
		return nil, ErrJobNotFound
	}
	return recordStatus(loaded), nil
}

// recordStatus 把持久化记录转成对外状态（interrupted 一律报 lost）。
func recordStatus(record jobRecord) *Status {
	state := record.State
	if state == StateInterrupted {
		state = StateLost
	}
	return &Status{
		JobID:          record.JobID,
		State:          state,
		Stage:          record.Stage,
		ExitCode:       record.ExitCode,
		Message:        record.Message,
		SourceDirExist: dirExists(record.SourceDir),
		ResultReady:    record.ResultReady,
		ResultSize:     record.ResultSize,
		ResultSha1:     record.ResultSha1,
		LastSeq:        record.LastSeq,
		LogFile:        record.LogFile,
	}
}

// Cancel 取消 job（幂等：未知 job 视为成功，由 handler 决定返回码）。
func (m *Manager) Cancel(jobID string) bool {
	m.mu.Lock()
	job := m.jobs[strings.TrimSpace(jobID)]
	m.mu.Unlock()
	if job == nil {
		return false
	}
	return job.Cancel()
}

// Subscribe 订阅 job 事件流；未知 job 返回 ErrJobNotFound（WS 需回 lost 帧）。
func (m *Manager) Subscribe(jobID string, fromSeq int) ([]Event, <-chan Event, func(), error) {
	m.mu.Lock()
	job := m.jobs[strings.TrimSpace(jobID)]
	m.mu.Unlock()
	if job == nil {
		return nil, nil, nil, ErrJobNotFound
	}
	backlog, channel, cancel := job.log.Subscribe(fromSeq)
	return backlog, channel, cancel, nil
}

// ZipPath 返回已就绪的产物 zip 路径（/build/download 使用）。
func (m *Manager) ZipPath(jobID string) (string, bool) {
	jobID = strings.TrimSpace(jobID)
	m.mu.Lock()
	job := m.jobs[jobID]
	record, hasRecord := m.records[jobID]
	m.mu.Unlock()
	path := ""
	if job != nil {
		job.mu.Lock()
		if job.resultReady {
			path = job.zipPath
		}
		job.mu.Unlock()
	} else if hasRecord && record.ResultReady {
		path = record.ZipPath
	}
	if path == "" {
		return "", false
	}
	info, err := os.Stat(path)
	if err != nil || !info.Mode().IsRegular() {
		return "", false
	}
	return path, true
}

// ZipPathByBuild 按构建 id + 构建号定位产物 zip（jobId 丢失时的兜底路径）。
func (m *Manager) ZipPathByBuild(buildModelID string, buildNumberID int) (string, bool) {
	buildModelID = strings.TrimSpace(buildModelID)
	if buildModelID == "" || buildNumberID <= 0 {
		return "", false
	}
	path := m.zipFile(buildModelID, buildNumberID)
	info, err := os.Stat(path)
	if err != nil || !info.Mode().IsRegular() || info.Size() == 0 {
		return "", false
	}
	return path, true
}

// SourceExists 批量探测节点上的源码目录是否存在（修正 Server 列表的 sourceDirExist）。
func (m *Manager) SourceExists(buildModelIDs []string) map[string]bool {
	result := make(map[string]bool, len(buildModelIDs))
	for _, id := range buildModelIDs {
		id = strings.TrimSpace(id)
		if id == "" {
			continue
		}
		result[id] = dirExists(m.sourceDir(id))
	}
	return result
}

// release 释放并发名额（job 进入终态时调用）。
func (m *Manager) release(job *Job) {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.running > 0 {
		m.running--
	}
	record := job.recordSnapshot()
	m.records[job.JobID()] = record
}

// writeRecord 原子写 job 状态文件（tmp + rename）。
func (m *Manager) writeRecord(record jobRecord) {
	path := m.jobFile(record.JobID)
	content, err := json.Marshal(record)
	if err != nil {
		return
	}
	if err := os.MkdirAll(filepath.Dir(path), 0o755); err != nil {
		return
	}
	temp := path + ".tmp"
	if err := os.WriteFile(temp, content, 0o644); err != nil {
		return
	}
	_ = os.Rename(temp, path)
}

// recover 扫描 jobs 目录：running → interrupted（对外报 lost），并载入历史记录。
func (m *Manager) recover() {
	entries, err := os.ReadDir(m.jobsDir)
	if err != nil {
		return
	}
	for _, entry := range entries {
		if entry.IsDir() || !strings.HasSuffix(entry.Name(), ".json") {
			continue
		}
		record, readErr := readRecord(filepath.Join(m.jobsDir, entry.Name()))
		if readErr != nil {
			continue
		}
		if record.State == StateRunning {
			record.State = StateInterrupted
			record.FinishedAt = time.Now().Format(time.RFC3339)
			m.writeRecord(record)
		}
		m.records[record.JobID] = record
	}
}

// cleanupExpired 按 build.keep-log-days 清理过期日志/历史产物与 job 记录（启动时执行一次）。
func (m *Manager) cleanupExpired() {
	if m.keepLogDays <= 0 {
		return
	}
	cutoff := time.Now().AddDate(0, 0, -m.keepLogDays)
	buildDirs, err := os.ReadDir(m.dataDir)
	if err != nil {
		return
	}
	for _, entry := range buildDirs {
		if !entry.IsDir() || entry.Name() == "jobs" {
			continue
		}
		base := filepath.Join(m.dataDir, entry.Name())
		for _, sub := range []string{"log", "history"} {
			_ = removeOlderThan(filepath.Join(base, sub), cutoff)
		}
	}
	_ = removeOlderThan(m.jobsDir, cutoff)
}

// removeOlderThan 删除目录下修改时间早于 cutoff 的文件（目录本身随空目录一起清理）。
func removeOlderThan(dir string, cutoff time.Time) error {
	entries, err := os.ReadDir(dir)
	if err != nil {
		return err
	}
	for _, entry := range entries {
		path := filepath.Join(dir, entry.Name())
		info, infoErr := entry.Info()
		if infoErr != nil {
			continue
		}
		if info.ModTime().After(cutoff) {
			continue
		}
		_ = os.RemoveAll(path)
	}
	return nil
}

// buildDir 返回某个构建模型的节点数据目录。
func (m *Manager) buildDir(buildModelID string) string {
	return filepath.Join(m.dataDir, safeSegment(buildModelID, "unknown"))
}

// sourceDir 返回源码目录（缓存复用）。
func (m *Manager) sourceDir(buildModelID string) string {
	return filepath.Join(m.buildDir(buildModelID), "source")
}

// logFile 返回 agent 侧日志副本路径：log/#<buildNumberId>.log。
func (m *Manager) logFile(buildModelID string, buildNumberID int) string {
	return filepath.Join(m.buildDir(buildModelID), "log", "#"+strconv.Itoa(buildNumberID)+".log")
}

// zipFile 返回产物 zip 路径：history/#<buildNumberId>/zip。
func (m *Manager) zipFile(buildModelID string, buildNumberID int) string {
	return filepath.Join(m.buildDir(buildModelID), "history", "#"+strconv.Itoa(buildNumberID), "zip")
}

// jobFile 返回 job 状态文件路径。
func (m *Manager) jobFile(jobID string) string {
	return filepath.Join(m.jobsDir, safeSegment(jobID, "job")+".json")
}

// safeSegment 把外部 id 变成安全目录名（拒绝穿越）。
func safeSegment(value, fallback string) string {
	value = strings.TrimSpace(value)
	value = strings.ReplaceAll(value, "\\", "_")
	value = strings.ReplaceAll(value, "/", "_")
	if value == "" || value == "." || value == ".." {
		return fallback
	}
	return value
}

// readRecord 读取一个 job 状态文件。
func readRecord(path string) (jobRecord, error) {
	record := jobRecord{}
	content, err := os.ReadFile(path)
	if err != nil {
		return record, err
	}
	if err := json.Unmarshal(content, &record); err != nil {
		return record, err
	}
	return record, nil
}
