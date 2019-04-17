package cn.keepbx.jpom.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.util.JsonFileUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * agent 端授权
 *
 * @author jiangzeyin
 * @date 2019/4/17
 */
@Configuration
public class AgentAuthorize {

    private static AgentAuthorize agentAuthorize;
    /**
     * 账号
     */
    @Value("${jpom.authorize.agentName}")
    private String agentName;
    /**
     * 密码
     */
    @Value("${jpom.authorize.agentPwd:}")
    private String agentPwd;

    private String authorize;

    /**
     * 单例
     *
     * @return this
     */
    public static AgentAuthorize getInstance() {
        if (agentAuthorize == null) {
            agentAuthorize = SpringUtil.getBean(AgentAuthorize.class);
            //
            agentAuthorize.checkPwd();
            // 生成密码授权字符串
            agentAuthorize.authorize = SecureUtil.sha1(agentAuthorize.agentName + "@" + agentAuthorize.agentPwd);
        }
        return agentAuthorize;
    }


    public boolean checkAuthorize(String authorize) {
        return StrUtil.equals(authorize, this.authorize);
    }

    /**
     * 检查是否配置密码
     */
    private void checkPwd() {
        String path = FileUtil.normalize(ConfigBean.getInstance().getDataPath() + "/" + AgentConfigBean.AUTHORIZE);

        if (StrUtil.isNotEmpty(agentPwd)) {
            // 有指定密码 清除旧密码信息
            FileUtil.del(path);
            return;
        }
        if (FileUtil.exist(path)) {
            // 读取旧密码
            try {
                String json = FileUtil.readString(path, CharsetUtil.CHARSET_UTF_8);
                AgentAuthorize oldAuthorize = JSONObject.parseObject(json, AgentAuthorize.class);
                if (StrUtil.isNotEmpty(oldAuthorize.agentPwd)) {
                    agentAuthorize.agentPwd = oldAuthorize.agentPwd;
                    DefaultSystemLog.LOG().info("已有授权密码：{},授权信息保存位置：{}", this.agentPwd, FileUtil.getAbsolutePath(path));
                    return;
                }

            } catch (Exception ignored) {
            }
        }
        this.agentPwd = RandomUtil.randomString(10);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("agentName", agentName);
        jsonObject.put("agentPwd", agentPwd);
        JsonFileUtil.saveJson(path, jsonObject);
        DefaultSystemLog.LOG().info("已经生成授权密码：{},授权信息保存位置：{}", this.agentPwd, FileUtil.getAbsolutePath(path));
    }
}
