package io.jpom.func.user.controller;

import io.jpom.common.BaseServerController;
import io.jpom.common.JsonMessage;
import io.jpom.func.user.model.UserLoginLogModel;
import io.jpom.func.user.server.UserLoginLogServer;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.SystemPermission;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import top.jpom.model.PageResultDto;

import javax.servlet.http.HttpServletRequest;

/**
 * @author bwcx_jzy
 * @since 2023/3/9
 */
@RestController
@RequestMapping(value = "/user/login-log")
@Feature(cls = ClassFeature.USER_LOGIN_LOG)
@SystemPermission
public class UserLoginLogController extends BaseServerController {

    private final UserLoginLogServer userLoginLogServer;

    public UserLoginLogController(UserLoginLogServer userLoginLogServer) {
        this.userLoginLogServer = userLoginLogServer;
    }

    /**
     * 登录日志列表
     *
     * @return json
     */
    @RequestMapping(value = "list-data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<PageResultDto<UserLoginLogModel>> listData(HttpServletRequest request) {
        PageResultDto<UserLoginLogModel> pageResult = userLoginLogServer.listPage(request);
        return JsonMessage.success("", pageResult);
    }
}
