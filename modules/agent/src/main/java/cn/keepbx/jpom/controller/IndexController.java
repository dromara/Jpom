package cn.keepbx.jpom.controller;

import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseAgentController;
import cn.keepbx.jpom.common.interceptor.NotAuthorize;
import cn.keepbx.jpom.model.system.JpomManifest;
import cn.keepbx.jpom.service.WhitelistDirectoryService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author jiangzeyin
 * @date 2019/4/17
 */
@RestController
public class IndexController extends BaseAgentController {
    @Resource
    private WhitelistDirectoryService whitelistDirectoryService;

    @RequestMapping(value = {"index", "", "index.html", "/"}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @NotAuthorize
    public String index() {
        return "Jpom-Agent";
    }

    @RequestMapping(value = "info", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String info() {
        int code;
        if (whitelistDirectoryService.isInstalled()) {
            code = 200;
        } else {
            code = 201;
        }
        return JsonMessage.getString(code, "", JpomManifest.getInstance());
    }
}
