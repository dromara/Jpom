/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common.commander;

import java.io.File;

/**
 * @author bwcx_jzy
 * @since 23/12/29 029
 */
public interface SystemCommander {

    /**
     * 清空文件内容
     *
     * @param file 文件
     * @return 执行结果
     */
    String emptyLogFile(File file);


    /**
     * kill 进程
     *
     * @param pid  进程编号
     * @param file 指定文件夹执行
     * @return 结束进程命令
     */

    String kill(File file, int pid);
}
