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

/**
 * 终端会话录制器契约
 * <p>
 * 变点：终端类型（SSH shell、后续 Docker exec 等）；调用方（SshHandler 等）只做接线，
 * 具体录制格式由实现类决定（当前为 asciicast v2）。开关关闭时返回 {@link #NOOP}。
 *
 * @author bwcx_jzy
 * @since 2026/11/26
 */
public interface TerminalRecorder extends AutoCloseable {

    /**
     * 空实现：未开启录制时使用，所有方法都是空操作
     */
    TerminalRecorder NOOP = new TerminalRecorder() {
        @Override
        public void input(byte[] data) {
        }

        @Override
        public void output(byte[] data) {
        }

        @Override
        public void resize(int cols, int rows) {
        }

        @Override
        public void marker(String label) {
        }

        @Override
        public String sessionId() {
            return null;
        }

        @Override
        public void close() {
        }
    };

    /**
     * 用户输入（浏览器 → 远端）
     *
     * @param data 原始字节
     */
    void input(byte[] data);

    /**
     * 远端输出（远端 → 浏览器）
     *
     * @param data 原始字节
     */
    void output(byte[] data);

    /**
     * 终端尺寸变化
     *
     * @param cols 列
     * @param rows 行
     */
    void resize(int cols, int rows);

    /**
     * 命令标记（命令索引）
     *
     * @param label 标记内容（JSON）
     */
    void marker(String label);

    /**
     * 会话 id，未录制返回 null
     *
     * @return 会话 id
     */
    String sessionId();

    /**
     * 关闭并落库（必须幂等，异常也要走到）
     */
    @Override
    void close();
}
