/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common.commander;

import org.dromara.jpom.util.CommandUtil;

import java.io.File;

/**
 * 系统监控命令
 *
 * @author bwcx_jzy
 * @since 2019/4/16
 */
public abstract class AbstractSystemCommander implements SystemCommander {


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
