package io.jpom.system.init;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import io.jpom.system.JpomRuntimeException;
import io.jpom.system.ServerExtConfigBean;
import io.jpom.util.CheckPassword;

/**
 * 验证token 合法性
 *
 * @author bwcx_jzy
 * @date 2019/8/5
 */
@PreLoadClass
public class CheckAuthorizeToken {

    @PreLoadMethod
    private static void check() {
        String authorizeToken = ServerExtConfigBean.getInstance().getAuthorizeToken();
        if (StrUtil.isEmpty(authorizeToken)) {
            return;
        }
        if (authorizeToken.length() < 6) {
            DefaultSystemLog.getLog().error("", new JpomRuntimeException("配置的授权token长度小于六位不生效"));
            System.exit(-1);
        }
        int password = CheckPassword.checkPassword(authorizeToken);
        if (password != 2) {
            DefaultSystemLog.getLog().error("", new JpomRuntimeException("配置的授权token 需要包含数字，字母，符号的组合"));
            System.exit(-1);
        }
    }
}
