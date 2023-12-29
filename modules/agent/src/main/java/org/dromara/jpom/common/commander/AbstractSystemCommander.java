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
package org.dromara.jpom.common.commander;

import cn.hutool.system.SystemUtil;
import org.dromara.jpom.common.commander.impl.LinuxSystemCommander;
import org.dromara.jpom.common.commander.impl.MacOsSystemCommander;
import org.dromara.jpom.common.commander.impl.WindowsSystemCommander;
import org.dromara.jpom.system.JpomRuntimeException;
import org.dromara.jpom.util.CommandUtil;

import java.io.File;

/**
 * 系统监控命令
 *
 * @author bwcx_jzy
 * @since 2019/4/16
 */
public abstract class AbstractSystemCommander {

    private static AbstractSystemCommander abstractSystemCommander = null;

    public static AbstractSystemCommander getInstance() {
        if (abstractSystemCommander != null) {
            return abstractSystemCommander;
        }
        if (SystemUtil.getOsInfo().isLinux()) {
            // Linux系统
            abstractSystemCommander = new LinuxSystemCommander();
        } else if (SystemUtil.getOsInfo().isWindows()) {
            // Windows系统
            abstractSystemCommander = new WindowsSystemCommander();
        } else if (SystemUtil.getOsInfo().isMac()) {
            abstractSystemCommander = new MacOsSystemCommander();
        } else {
            throw new JpomRuntimeException("不支持的：" + SystemUtil.getOsInfo().getName());
        }
        return abstractSystemCommander;
    }

    /**
     * 清空文件内容
     *
     * @param file 文件
     * @return 执行结果
     */
    public abstract String emptyLogFile(File file);

//    /**
//     * 查询服务状态
//     *
//     * @param serviceName 服务名称
//     * @return true 运行中
//     */
//    public abstract boolean getServiceStatus(String serviceName);
//
//    /**
//     * 启动服务
//     *
//     * @param serviceName 服务名称
//     * @return 结果
//     */
//    public abstract String startService(String serviceName);
//
//    /**
//     * 关闭服务
//     *
//     * @param serviceName 服务名称
//     * @return 结果
//     */
//    public abstract String stopService(String serviceName);

    /**
     * 构建kill 命令
     *
     * @param pid 进程编号
     * @return 结束进程命令
     */
    public abstract String buildKill(int pid);

    /**
     * kill
     *
     * @param pid 进程编号
     */
    public String kill(File file, int pid) {
        String kill = buildKill(pid);
        return CommandUtil.execSystemCommand(kill, file);
    }
}
