package cn.keepbx.jpom.controller.node.manage;

import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.common.interceptor.ProjectPermission;
import cn.keepbx.jpom.model.data.UserOperateLogV1;
import cn.keepbx.jpom.system.OperateType;
import com.alibaba.fastjson.JSONArray;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 构建
 *
 * @author jiangzeyin
 * @date 2018/9/29
 */
@Controller
@RequestMapping(value = "/node/manage/")
public class OssBuildController extends BaseServerController {

    @RequestMapping(value = "build", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String build(String id) {
        JSONArray array = NodeForward.requestData(getNode(), NodeUrl.Manage_build_data, JSONArray.class, "id", id);
        if (array != null) {
            setAttribute("array", array);
            setAttribute("id", id);
        }
        return "node/manage/build";
    }

    /**
     * 构建下载
     *
     * @param id  项目id
     * @param key key
     * @return url
     */
    @RequestMapping(value = "build_download", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    @ProjectPermission(optType = UserOperateLogV1.OptType.BuildDownload)
    public String buildDownload(String id, String key) {
        String url = NodeForward.requestData(getNode(), NodeUrl.Manage_build_download, String.class,
                "id", id,
                "key", key);
        if (url == null) {
            return "redirect:error";
        }
        return "redirect:" + url;
    }

    /**
     * 构建安装
     *
     * @return json
     */
    @RequestMapping(value = "build_install", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @ProjectPermission(optType = UserOperateLogV1.OptType.BuildInstall)
    public String buildInstall() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_build_install).toString();
    }
}
