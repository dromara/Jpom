package io.jpom.system;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.script.ScriptUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 外部配置文件
 *
 * @author jiangzeyin
 * @date 2019/3/04
 */
@Configuration
public class ServerExtConfigBean {

    /**
     * 系统最多能创建多少用户
     */
    @Value("${user.maxCount:10}")
    public int userMaxCount;
    /**
     * 用户连续登录失败次数，超过此数将自动不再被允许登录，零是不限制
     */
    @Value("${user.alwaysLoginError:5}")
    public int userAlwaysLoginError;

    /**
     * 当ip连续登录失败，锁定对应IP时长，单位毫秒
     */
    @Value("${user.ipErrorLockTime:60*60*5*1000}")
    private String ipErrorLockTime;
    private long ipErrorLockTimeValue = -1;
    /**
     * 日志记录最大条数
     */
    @Value("${db.logStorageCount:100000}")
    private int h2DbLogStorageCount;

    /**
     * 服务端api token,长度要求大于等于6位，字母数字符号组合
     */
    @Value("${jpom.authorize.token:}")
    private String authorizeToken;


    public String getAuthorizeToken() {
        return authorizeToken;
    }

    public long getIpErrorLockTime() {
        if (this.ipErrorLockTimeValue == -1) {
            String str = StrUtil.emptyToDefault(this.ipErrorLockTime, "60*60*5*1000");
            this.ipErrorLockTimeValue = Convert.toLong(ScriptUtil.eval(str), TimeUnit.HOURS.toMillis(5));
        }
        return this.ipErrorLockTimeValue;
    }

    public int getH2DbLogStorageCount() {
        return h2DbLogStorageCount;
    }

    /**
     * 单例
     *
     * @return this
     */
    public static ServerExtConfigBean getInstance() {
        return SpringUtil.getBean(ServerExtConfigBean.class);
    }
}
