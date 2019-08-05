package cn.keepbx.jpom.controller.openapi;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import cn.keepbx.jpom.common.ServerOpenApi;

/**
 * 节点管理
 *
 * @author bwcx_jzy
 * @date 2019/8/5
 */
@RestController
public class NodeInfoController {

    @RequestMapping(value = ServerOpenApi.UPDATE_NODE_INFO, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String update() {
        return "";
    }
}
