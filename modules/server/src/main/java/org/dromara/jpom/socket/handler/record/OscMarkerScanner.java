/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.socket.handler.record;

import cn.hutool.core.util.StrUtil;

/**
 * OSC 133 / OSC 633（shell integration 语义 prompt）增量扫描器
 * <p>
 * 从终端输出字节流里识别命令边界标记，得到「命令原文 + 退出码」，不需要猜测按键：
 * <pre>
 * ESC ] 133 ; A ST            prompt 开始
 * ESC ] 133 ; B ST            prompt 结束
 * ESC ] 133 ; C ST            命令开始执行
 * ESC ] 133 ; D ; &lt;exit&gt; ST   命令结束 + 退出码
 * ESC ] 633 ; E ; &lt;cmd&gt; ST    命令行原文（VS Code 扩展）
 * </pre>
 * 说明：目标机 shell 未启用 integration 时不会产生任何事件，调用方需自行降级处理；
 * 本类不得抛出异常影响终端转发（所有解析异常内部吞掉）。
 *
 * @author bwcx_jzy
 * @since 2026/11/26
 */
public class OscMarkerScanner {

    /**
     * 命令事件回调
     */
    public interface Listener {
        /**
         * 一条命令结束
         *
         * @param command  命令原文（可能为空）
         * @param exitCode 退出码（可能为空）
         */
        void onCommand(String command, Integer exitCode);
    }

    private static final String OSC_START = "\u001b]";
    private static final String ST = "\u001b\\";
    private static final int MAX_BUFFER = 8192;

    private final StringBuilder buffer = new StringBuilder();
    private String pendingCommand;
    private volatile boolean seen;

    /**
     * 是否已捕获到任何 OSC 133/633 标记
     *
     * @return true 已捕获（说明目标机 shell 启用了 integration）
     */
    public boolean isSeen() {
        return seen;
    }

    /**
     * 增量扫描一段终端输出
     *
     * @param data     输出文本
     * @param listener 回调
     */
    public void scan(String data, Listener listener) {
        if (StrUtil.isEmpty(data)) {
            return;
        }
        try {
            buffer.append(data);
            int from = 0;
            while (true) {
                int start = buffer.indexOf(OSC_START, from);
                if (start < 0) {
                    // 没有起始符号，保留尾部可能的半截序列
                    this.keepTail(buffer.length());
                    return;
                }
                int payloadStart = start + OSC_START.length();
                int bel = buffer.indexOf("\u0007", payloadStart);
                int esc = buffer.indexOf(ST, payloadStart);
                int end;
                int skip;
                if (bel >= 0 && (esc < 0 || bel < esc)) {
                    end = bel;
                    skip = bel + 1;
                } else if (esc >= 0) {
                    end = esc;
                    skip = esc + ST.length();
                } else {
                    // 序列还未接收完整，保留等待下次
                    this.keepFrom(start);
                    return;
                }
                String payload = buffer.substring(payloadStart, end);
                this.handle(payload, listener);
                from = skip;
            }
        } catch (Exception e) {
            // 解析异常绝不外抛，避免影响终端会话
            buffer.setLength(0);
        }
    }

    private void keepTail(int length) {
        if (length <= 0) {
            return;
        }
        int start = buffer.lastIndexOf(OSC_START);
        if (start >= 0) {
            this.keepFrom(start);
            return;
        }
        // 没有任何起始符：只保留末尾可能是半截 "ESC ]" 的部分（逐字节喂入时必须保留 ESC）
        int keep = Math.min(OSC_START.length(), buffer.length());
        if (buffer.length() > keep) {
            buffer.delete(0, buffer.length() - keep);
        }
        if (buffer.length() > MAX_BUFFER) {
            buffer.setLength(0);
        }
    }

    private void keepFrom(int start) {
        if (start <= 0) {
            if (buffer.length() > MAX_BUFFER) {
                buffer.setLength(0);
            }
            return;
        }
        buffer.delete(0, start);
        if (buffer.length() > MAX_BUFFER) {
            buffer.setLength(0);
        }
    }

    private void handle(String payload, Listener listener) {
        if (StrUtil.isEmpty(payload)) {
            return;
        }
        if (StrUtil.startWith(payload, "133;")) {
            this.seen = true;
            String body = payload.substring(4);
            if (StrUtil.startWith(body, "D")) {
                String exitText = StrUtil.subAfter(body, ";", false);
                this.emit(listener, this.parseInt(exitText));
            }
            return;
        }
        if (StrUtil.startWith(payload, "633;")) {
            this.seen = true;
            String body = payload.substring(4);
            if (StrUtil.startWith(body, "E;")) {
                // fc/history 取到的命令行前面带制表符，统一 trim
                String command = StrUtil.trim(unescape(body.substring(2)));
                if (this.accept(command)) {
                    this.pendingCommand = command;
                }
            } else if (StrUtil.startWith(body, "D")) {
                String exitText = StrUtil.subAfter(body, ";", false);
                this.emit(listener, this.parseInt(exitText));
            }
        }
    }

    private void emit(Listener listener, Integer exitCode) {
        String command = StrUtil.trim(pendingCommand);
        pendingCommand = null;
        // 没有命令原文的标记（如被过滤的注入脚本、空行回车）不产生命令索引，避免列表出现空白行
        if (listener != null && StrUtil.isNotEmpty(command)) {
            listener.onCommand(command, exitCode);
        }
    }

    /**
     * 过滤注入脚本自身产生的伪命令
     *
     * @param command 命令
     * @return true 采集
     */
    private boolean accept(String command) {
        if (StrUtil.isEmpty(command)) {
            return false;
        }
        return !StrUtil.containsAny(command, "__jpom_mark", "PROMPT_COMMAND", "precmd_functions");
    }

    private Integer parseInt(String text) {
        String value = StrUtil.trimToEmpty(text);
        if (StrUtil.isEmpty(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 还原 633;E 的转义：\\ 反斜杠、\xHH 单字节
     *
     * @param text 原始
     * @return 文本
     */
    private static String unescape(String text) {
        if (StrUtil.isEmpty(text) || !StrUtil.contains(text, '\\')) {
            return text;
        }
        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != '\\' || i + 1 >= text.length()) {
                result.append(c);
                continue;
            }
            char next = text.charAt(i + 1);
            if (next == '\\') {
                result.append('\\');
                i++;
            } else if ((next == 'x' || next == 'X') && i + 3 < text.length()) {
                String hex = text.substring(i + 2, i + 4);
                try {
                    result.append((char) Integer.parseInt(hex, 16));
                    i += 3;
                } catch (NumberFormatException e) {
                    result.append(c);
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
