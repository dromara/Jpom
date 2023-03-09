package io.jpom.func.user.model;

import io.jpom.model.BaseUserModifyDbModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import top.jpom.h2db.TableName;

/**
 * @author bwcx_jzy
 * @since 2023/3/9
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "USER_LOGIN_LOG", name = "用户登录日志")
@Data
@NoArgsConstructor
public class UserLoginLogModel extends BaseUserModifyDbModel {

    /**
     * 操作ip
     */
    private String ip;

    /**
     * 用户名称
     */
    private String username;

    /**
     * 浏览器标识
     */
    private String userAgent;

    /**
     * 是否使用 mfa
     */
    private Boolean useMfa;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 错误原因
     * <p>
     * 0 正常登录
     * 1 密码错误
     * 2 被锁定
     * 3 续期
     * 4 账号被禁用
     * 5 登录成功，但是需要 mfa 验证
     */
    private Integer operateCode;
}
