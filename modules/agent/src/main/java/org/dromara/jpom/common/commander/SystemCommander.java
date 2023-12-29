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
