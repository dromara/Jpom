package org.dromara.jpom.func.assets.controller;

import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.func.assets.model.ScriptLibraryModel;
import org.dromara.jpom.func.assets.server.ScriptLibraryServer;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * @author bwcx_jzy
 * @since 2024/6/16
 */
@RestController
@RequestMapping(value = "/system/assets/script-library")
@Feature(cls = ClassFeature.SYSTEM_ASSETS_GLOBAL_SCRIPT)
@Slf4j
public class ScriptLibraryNoPermissionController {

    private final ScriptLibraryServer scriptLibraryServer;

    public ScriptLibraryNoPermissionController(ScriptLibraryServer scriptLibraryServer) {
        this.scriptLibraryServer = scriptLibraryServer;
    }

    @PostMapping(value = "list-data-no-permission", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<ScriptLibraryModel>> listJson(HttpServletRequest request) {
        PageResultDto<ScriptLibraryModel> pageResultDto = scriptLibraryServer.listPage(request);
        return JsonMessage.success("", pageResultDto);
    }
}
