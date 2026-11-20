/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.build;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.enums.BuildBuildNodeState;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 远程构建任务的事件会话：把插件端推来的 stage/log/result/exit 帧收敛成
 * 「阶段完成 / 终态 / 产物信息」三种可等待的状态，供 {@link RemoteBuildRunner} 串行推进。
 * <p>
 * 帧的到达顺序即阶段推进顺序（单 WebSocket 单线程消费），因此用一把锁 + wait/notify
 * 即可，不需要额外的状态机。
 *
 * @author jpom
 * @since 2.11.15
 */
class RemoteBuildSession {

    /**
     * 各阶段退出码：key=阶段名
     */
    private final Map<String, Integer> stageCodes = new HashMap<>(4);
    private final Map<String, String> stageMessages = new HashMap<>(4);
    private final Object monitor = new Object();
    private JSONObject result;
    private JSONObject env;
    private String pullCommitId;
    private String pullBranch;
    private boolean exited;
    private int exitCode;
    private String state = BuildBuildNodeState.RUNNING;
    private String exitMessage;
    private long lastSeq;

    /**
     * 消费一帧
     *
     * @param json 帧
     * @return 是否被处理（用于日志行之外的副作用判断）
     */
    boolean accept(JSONObject json) {
        long seq = json.getLongValue("seq");
        synchronized (monitor) {
            if (seq > 0 && seq <= lastSeq) {
                // 补发/重连导致的重复帧
                return true;
            }
            if (seq > 0) {
                lastSeq = seq;
            }
            String type = json.getString("type");
            if (type == null) {
                return false;
            }
            switch (type) {
                case "stage": {
                    String stage = json.getString("stage");
                    String event = json.getString("event");
                    if ("end".equals(event) && StrUtil.isNotEmpty(stage)) {
                        stageCodes.put(stage, json.getIntValue("code"));
                        if ("pull".equals(stage)) {
                            // 节点可选回传实际使用的分支与 commit（用于差异构建的基线记录）
                            this.pullCommitId = json.getString("commitId");
                            this.pullBranch = json.getString("branch");
                        }
                        String message = json.getString("message");
                        if (StrUtil.isNotEmpty(message)) {
                            stageMessages.put(stage, message);
                        }
                        monitor.notifyAll();
                    }
                    return false;
                }
                case "env":
                    this.env = json.getJSONObject("data");
                    return true;
                case "result":
                    this.result = json;
                    return true;
                case "exit":
                    this.exited = true;
                    this.exitCode = json.getIntValue("code");
                    this.state = StrUtil.emptyToDefault(json.getString("state"), BuildBuildNodeState.FAILED);
                    this.exitMessage = json.getString("message");
                    monitor.notifyAll();
                    return true;
                case "ping":
                    return true;
                default:
                    return false;
            }
        }
    }

    /**
     * 等待某个阶段结束
     *
     * @param stage     阶段名（pull/executeCommand/packageFile）
     * @param timeoutMs 超时
     * @return 阶段退出码
     */
    int awaitStage(String stage, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        synchronized (monitor) {
            while (!stageCodes.containsKey(stage)) {
                if (exited) {
                    // 任务提前结束（失败/取消），用终态退出码兜底
                    return exitCode;
                }
                long wait = deadline - System.currentTimeMillis();
                if (wait <= 0) {
                    throw new IllegalStateException(I18nMessageUtil.get("i18n.build_node_wait_stage_timeout.6f2a") + stage);
                }
                try {
                    monitor.wait(Math.min(wait, TimeUnit.SECONDS.toMillis(5)));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }
            return stageCodes.get(stage);
        }
    }

    String stageMessage(String stage) {
        synchronized (monitor) {
            return stageMessages.get(stage);
        }
    }

    /**
     * 等待任务终态
     *
     * @param timeoutMs 超时
     */
    void awaitExit(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        synchronized (monitor) {
            while (!exited) {
                long wait = deadline - System.currentTimeMillis();
                if (wait <= 0) {
                    throw new IllegalStateException(I18nMessageUtil.get("i18n.build_node_wait_exit_timeout.2b91"));
                }
                try {
                    monitor.wait(Math.min(wait, TimeUnit.SECONDS.toMillis(5)));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }
        }
    }

    boolean isExited() {
        synchronized (monitor) {
            return exited;
        }
    }

    void markLost(String message) {
        synchronized (monitor) {
            if (!exited) {
                exited = true;
                exitCode = -1;
                state = BuildBuildNodeState.LOST;
                exitMessage = message;
                monitor.notifyAll();
            }
        }
    }

    int getExitCode() {
        synchronized (monitor) {
            return exitCode;
        }
    }

    String getState() {
        synchronized (monitor) {
            return state;
        }
    }

    String getExitMessage() {
        synchronized (monitor) {
            return exitMessage;
        }
    }

    JSONObject getResult() {
        synchronized (monitor) {
            return result;
        }
    }

    JSONObject getEnv() {
        synchronized (monitor) {
            return env;
        }
    }

    String getPullCommitId() {
        synchronized (monitor) {
            return pullCommitId;
        }
    }

    String getPullBranch() {
        synchronized (monitor) {
            return pullBranch;
        }
    }

    long getLastSeq() {
        synchronized (monitor) {
            return lastSeq;
        }
    }
}
