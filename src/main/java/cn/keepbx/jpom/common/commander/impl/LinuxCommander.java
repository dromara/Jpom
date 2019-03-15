package cn.keepbx.jpom.common.commander.impl;

import cn.hutool.core.util.CharsetUtil;
import cn.keepbx.jpom.common.commander.Commander;
import cn.keepbx.jpom.model.ProjectInfoModel;

public class LinuxCommander extends Commander {

    public LinuxCommander() {
        charset = CharsetUtil.CHARSET_UTF_8;
    }

    // 启动
    @Override
    public String start(ProjectInfoModel projectInfoModel) {
        return null;
    }

    // 停止
    @Override
    public String stop(String tag) {
        return null;
    }

    // 重启
    @Override
    public String restart(ProjectInfoModel projectInfoModel) {
        return null;
    }
}
