package cn.keepbx.jpom.common.commander.impl;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.common.commander.Commander;
import cn.keepbx.jpom.model.ProjectInfoModel;

import java.io.IOException;
import java.io.InputStream;

public class WindowsCommander extends Commander {

    // 启动
    @Override
    public String start(ProjectInfoModel projectInfoModel) throws Exception {
        String result;

        // 拼接命令
        String jvm = projectInfoModel.getJvm();
        String tag = projectInfoModel.getId();
        String lib = projectInfoModel.getLib();
        String mainClass = projectInfoModel.getMainClass();
        String args = projectInfoModel.getArgs();
        String javaHome = System.getProperty("java.home");
        String log = projectInfoModel.getLog();
        String command = String.format("java %s -Dapplication=%s -Djava.ext.dirs=%s;%s/jre/lib/ext %s %s >> %s", jvm, tag, lib, javaHome, mainClass, args, log);

        // 执行命令
        Process process = Runtime.getRuntime().exec(command);
        InputStream is;
        if (process.waitFor() == 0) {
            is = process.getInputStream();
        } else {
            is = process.getErrorStream();
        }

        // 读取io为字符串
        result = IoUtil.read(is, CharsetUtil.CHARSET_UTF_8);
        is.close();
        process.destroy();
        if (StrUtil.isEmpty(result)) {
            result = "没有返回任何执行结果";
        }

        return result;
    }

    // 停止
    @Override
    public String stop() {
        return null;
    }

    // 重启
    @Override
    public String restart() {
        String status = this.status();

        return null;
    }

    // 查看状态
    @Override
    public String status() {

        return null;
    }
}
