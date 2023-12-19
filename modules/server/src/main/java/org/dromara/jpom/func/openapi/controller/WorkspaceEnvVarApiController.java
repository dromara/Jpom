package org.dromara.jpom.func.openapi.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseJpomController;
import org.dromara.jpom.common.ServerOpenApi;
import org.dromara.jpom.common.interceptor.NotLogin;
import org.dromara.jpom.model.data.WorkspaceEnvVarModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.system.WorkspaceEnvVarService;
import org.dromara.jpom.service.user.TriggerTokenLogServer;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author bwcx_jzy
 * @since 23/12/19 019
 */
@RestController
@NotLogin
@Slf4j
public class WorkspaceEnvVarApiController extends BaseJpomController {

    private final WorkspaceEnvVarService workspaceEnvVarService;
    private final TriggerTokenLogServer triggerTokenLogServer;

    public WorkspaceEnvVarApiController(WorkspaceEnvVarService workspaceEnvVarService,
                                        TriggerTokenLogServer triggerTokenLogServer) {
        this.workspaceEnvVarService = workspaceEnvVarService;
        this.triggerTokenLogServer = triggerTokenLogServer;
    }

    /**
     * 参数获取并验证变量
     *
     * @param id       变量id
     * @param token    token
     * @param response 响应
     * @return data
     */
    private WorkspaceEnvVarModel get(String id, String token, HttpServletResponse response) {
        WorkspaceEnvVarModel item = workspaceEnvVarService.getByKey(id);
        if (item == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            ServletUtil.write(response, "没有对应数据", MediaType.TEXT_PLAIN_VALUE);
            return null;
        }
        if (!StrUtil.equals(token, item.getTriggerToken())) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            ServletUtil.write(response, "触发token错误,或者已经失效", MediaType.TEXT_PLAIN_VALUE);
            return null;
        }
        //
        UserModel userModel = triggerTokenLogServer.getUserByToken(token, workspaceEnvVarService.typeName());
        if (userModel == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            ServletUtil.write(response, "触发token错误,或者已经失效:-1", MediaType.TEXT_PLAIN_VALUE);
            return null;
        }
        Integer privacy = item.getPrivacy();
        if (privacy == null || privacy != 0) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            ServletUtil.write(response, "非明文变量不能查看", MediaType.TEXT_PLAIN_VALUE);
            return null;
        }
        return item;
    }


    /**
     * 获取变量值
     *
     * @param id    变量ID
     * @param token 变量的token
     */
    @GetMapping(value = ServerOpenApi.SERVER_ENV_VAR_TRIGGER_URL, produces = MediaType.TEXT_PLAIN_VALUE)
    public void trigger(@PathVariable String id, @PathVariable String token, HttpServletResponse response) {
        WorkspaceEnvVarModel item = this.get(id, token, response);
        if (item != null) {
            ServletUtil.write(response, item.getValue(), MediaType.TEXT_PLAIN_VALUE);
        }
    }

    /**
     * 修改变量值
     *
     * @param id    变量ID
     * @param token 变量的token
     */
    @PostMapping(value = ServerOpenApi.SERVER_ENV_VAR_TRIGGER_URL, produces = MediaType.TEXT_PLAIN_VALUE)
    public void trigger(@PathVariable String id, @PathVariable String token, String value, HttpServletResponse response, HttpServletRequest request) {
        this.update(id, token, value, response);
    }

    /**
     * 修改变量值
     *
     * @param id    变量ID
     * @param token 变量的token
     */
    @PutMapping(value = ServerOpenApi.SERVER_ENV_VAR_TRIGGER_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    public void triggerPut(@PathVariable String id, @PathVariable String token, HttpServletResponse response, HttpServletRequest request) {
        String value = ServletUtil.getBody(request);
        this.update(id, token, value, response);
    }

    /**
     * 修改变量操作
     *
     * @param id       变量id
     * @param token    变量token
     * @param value    变量值
     * @param response 响应
     */
    private void update(String id, String token, String value, HttpServletResponse response) {
        if (StrUtil.isEmpty(value)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ServletUtil.write(response, "修改的值为空", MediaType.TEXT_PLAIN_VALUE);
            return;
        }
        WorkspaceEnvVarModel item = this.get(id, token, response);
        if (item != null) {
            WorkspaceEnvVarModel update = new WorkspaceEnvVarModel();
            update.setId(item.getId());
            update.setValue(value);
            workspaceEnvVarService.updateById(update);
            ServletUtil.write(response, "success", MediaType.TEXT_PLAIN_VALUE);
        }
    }
}
