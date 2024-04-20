package org.dromara.jpom.func.user.controller;

import cn.hutool.extra.servlet.ServletUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import org.dromara.jpom.func.user.dto.UserNotificationDto;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.service.system.SystemParametersServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * @author bwcx_jzy1
 * @since 2024/4/20
 */
@RestController
@RequestMapping(value = "/user/notification")
@Feature(cls = ClassFeature.USER)
@SystemPermission
public class UserNotificationController {

    public static final String KEY = "SYSTEM-USER-NOTIFICATION";

    private final SystemParametersServer systemParametersServer;

    public UserNotificationController(SystemParametersServer systemParametersServer) {
        this.systemParametersServer = systemParametersServer;
    }

    /**
     * 获取通知
     *
     * @return json
     */
    @GetMapping(value = "get", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<UserNotificationDto> getNotification() {
        UserNotificationDto notificationDto = systemParametersServer.getConfigDefNewInstance(KEY, UserNotificationDto.class);
        return JsonMessage.success("", notificationDto);
    }


    /**
     * 保存通知
     *
     * @return json
     */
    @PostMapping(value = "save", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<UserNotificationDto> saveNotification(HttpServletRequest request) {
        UserNotificationDto userNotification = ServletUtil.toBean(request, UserNotificationDto.class, true);
        Assert.notNull(userNotification, "请配置用户通知");
        userNotification.verify();
        systemParametersServer.upsert(KEY, userNotification, "");
        return JsonMessage.success("保存成功");
    }
}
