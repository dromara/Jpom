package io.jpom.controller.openapi.build;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.common.ServerOpenApi;
import io.jpom.common.interceptor.NotLogin;
import io.jpom.model.data.BuildModel;
import io.jpom.model.data.UserModel;
import io.jpom.service.build.BuildService;
import io.jpom.service.user.UserService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

/**
 * @author bwcx_jzy
 * @date 2019/9/4
 */
@RestController
@NotLogin
public class BuildTriggerApiController {

    @Resource
    private BuildService buildService;

    @Resource
    private UserService userService;

    @RequestMapping(value = ServerOpenApi.BUILD_TRIGGER_BUILD, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String install(@PathVariable String id, @PathVariable String token) {
        BuildModel item = buildService.getItem(id);
        if (item == null) {
            return JsonMessage.getString(404, "没有对应数据");
        }
        List<UserModel> list = userService.list(false);
        Optional<UserModel> first = list.stream().filter(UserModel::isSystemUser).findFirst();
        if (!first.isPresent()) {
            return JsonMessage.getString(404, "没有对应数据");
        }
        UserModel userModel = first.get();
        if (!StrUtil.equals(token, item.getTriggerToken())) {
            return JsonMessage.getString(404, "触发token错误");
        }
        return buildService.start(userModel, id);
    }
}
