package cn.keepbx.jpom.common.commander.impl;

import cn.keepbx.jpom.common.commander.Commander;
import cn.keepbx.jpom.model.ProjectInfoModel;

public class LinuxCommander extends Commander {

    // 启动
    @Override
    public String start(ProjectInfoModel projectInfoModel) {
        return null;
    }

    // 停止
    @Override
    public String stop() {
        return null;
    }

    // 重启
    @Override
    public String restart() {
        return null;
    }

    // 查询状态
    @Override
    public String status() {
        return null;
    }
}
