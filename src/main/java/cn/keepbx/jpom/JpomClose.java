package cn.keepbx.jpom;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.common.commander.AbstractCommander;
import cn.keepbx.jpom.model.ProjectInfoModel;
import cn.keepbx.jpom.util.ArgsUtil;

/**
 * 命令行关闭Jpom
 *
 * @author jiangzeyin
 * @date 2019/4/7
 */
public class JpomClose {

    public static void main(String[] args) throws Exception {
        String tag = ArgsUtil.getArgsValue(args, "jpom.applicationTag");
        if (StrUtil.isEmpty(tag)) {
            Console.error("请传入对应：jpom.applicationTag");
            return;
        }
        if (!AbstractCommander.getInstance().isRun(tag)) {
            Console.error("Jpom并没有运行");
            return;
        }
        ProjectInfoModel projectInfoModel = new ProjectInfoModel();
        projectInfoModel.setId(tag);
        String msg = AbstractCommander.getInstance().stop(projectInfoModel);
        Console.log(msg);
    }
}
