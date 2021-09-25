/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 码之科技工作室
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
package io.jpom;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import com.sun.tools.attach.VirtualMachine;
import io.jpom.util.CommandUtil;
import io.jpom.util.JvmUtil;
import io.jpom.util.StringUtil;

import java.io.IOException;

/**
 * 命令行关闭Jpom
 *
 * @author jiangzeyin
 * @date 2019/4/7
 */
public class JpomClose {
    private static JpomClose jpomManager;

    public void main(String[] args) throws Exception {
        String tag = StringUtil.getArgsValue(args, "jpom.applicationTag");
        if (StrUtil.isEmpty(tag)) {
            return;
        }
        // 事件
        String event = StringUtil.getArgsValue(args, "event");
        if ("stop".equalsIgnoreCase(event)) {
            String status = JpomClose.getInstance().status(tag);
            if (!status.contains(StrUtil.COLON)) {
                Console.error("Jpom并没有运行");
            } else {
                String msg = JpomClose.getInstance().stop(tag);
                Console.log(msg);
            }
            System.exit(0);
        } else if ("status".equalsIgnoreCase(event)) {
            String status = JpomClose.getInstance().status(tag);
            Console.log(status);
            System.exit(0);
        }
    }

    /**
     * 单利模式
     *
     * @return JpomClose
     */
    public static JpomClose getInstance() {
        if (jpomManager != null) {
            return jpomManager;
        }
        if (SystemUtil.getOsInfo().isLinux()) {
            jpomManager = new Linux();
        } else {
            jpomManager = new Windows();
        }
        return jpomManager;
    }


    public String stop(String tag) throws IOException {
        VirtualMachine virtualMachine = JvmUtil.getVirtualMachine(tag);
        if (virtualMachine == null) {
            return null;
        }
        try {
            return virtualMachine.id();
        } finally {
            virtualMachine.detach();
        }
    }

    public String status(String tag) throws IOException {
        VirtualMachine virtualMachine = JvmUtil.getVirtualMachine(tag);
        if (virtualMachine == null) {
            return "Jpom并没有运行";
        }
        try {
            return "Jpom运行中:" + virtualMachine.id();
        } finally {
            virtualMachine.detach();
        }
    }


    private static class Windows extends JpomClose {

        @Override
        public String stop(String tag) throws IOException {
            String pid = super.stop(tag);
            if (pid == null) {
                return "stop";
            }
            String cmd = String.format("taskkill /F /PID %s", pid);
            return CommandUtil.execSystemCommand(cmd);
        }
    }

    private static class Linux extends JpomClose {

        @Override
        public String stop(String tag) throws IOException {
            String pid = super.stop(tag);
            if (pid == null) {
                return "stop";
            }
            String cmd = String.format("kill  %s", pid);
            return CommandUtil.execSystemCommand(cmd);
        }
    }
}
