/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.common.commander;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 命令操作执行结果
 *
 * @author bwcx_jzy
 * @since 2022/11/30
 */
public class CommandOpResult {

    /**
     * 是否成功
     */
    private boolean success;
    /**
     * 执行结果
     */
    private final List<String> msgs = new ArrayList<>();

    public static CommandOpResult of(boolean success) {
        return of(success, null);
    }

    public static CommandOpResult of(boolean success, String msg) {
        CommandOpResult commandOpResult = new CommandOpResult();
        commandOpResult.success = success;
        commandOpResult.appendMsg(msg);
        return commandOpResult;
    }

    public CommandOpResult appendMsg(String msg) {
        if (StrUtil.isEmpty(msg)) {
            return this;
        }
        msgs.add(msg);
        return this;
    }

    public CommandOpResult appendMsg(List<String> msgs) {
        for (String msg : msgs) {
            this.appendMsg(msg);
        }
        return this;
    }

    public CommandOpResult appendMsg(String... msgs) {
        for (String msg : msgs) {
            this.appendMsg(msg);
        }
        return this;
    }

    public List<String> getMsgs() {
        return msgs;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String msgStr() {
        return CollUtil.join(msgs, StrUtil.COMMA);
    }
}
