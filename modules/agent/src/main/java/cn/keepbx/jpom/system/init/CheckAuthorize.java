package cn.keepbx.jpom.system.init;

import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import cn.keepbx.jpom.system.AgentAuthorize;

/**
 * 检查授权信息
 *
 * @author jiangzeyin
 * @date 2019/4/17
 */
@PreLoadClass
public class CheckAuthorize {
    @PreLoadMethod
    private static void startAutoBackLog() {
        AgentAuthorize.getInstance();
    }
}
