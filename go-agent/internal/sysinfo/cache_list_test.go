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

import "testing"

// TestScriptTaskListMatchesJavaSemantics 校验 /system/cache.taskList 对齐 Java CronUtils.list：
// 只包含首次触发过的脚本任务，且排除内部缓存任务。
func TestScriptTaskListMatchesJavaSemantics(t *testing.T) {
	previous := globalCron.Load()
	t.Cleanup(func() { globalCron.Store(previous) })
	manager := &CronManager{tasks: map[string]*cronTask{}}
	SetGlobalCron(manager)
	if err := manager.Upsert("script-cache-task", "0 0/10 * * * ?", func() {}); err != nil {
		t.Fatal(err)
	}
	if err := manager.Upsert("auto_back_log", "0 0/10 * * * ?", func() {}); err != nil {
		t.Fatal(err)
	}
	// 未触发前不出现（Java TASK_STAT 首次 onStart 才创建）。
	if list := scriptTaskList(); len(list) != 0 {
		t.Fatalf("taskList before execution = %#v, want empty", list)
	}
	// 模拟脚本任务首次触发；内部任务即使执行也不能输出。
	manager.tasks["script-cache-task"].execute = 1
	manager.tasks["auto_back_log"].execute = 1
	list := scriptTaskList()
	if len(list) != 1 {
		t.Fatalf("taskList after execution = %#v, want only script task", list)
	}
	if id, _ := list[0]["taskId"].(string); id != "script-cache-task" {
		t.Fatalf("taskList taskId = %q, want script-cache-task", id)
	}
}
