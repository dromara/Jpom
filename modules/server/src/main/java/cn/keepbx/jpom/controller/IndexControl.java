package cn.keepbx.jpom.controller;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.GlobalDefaultExceptionHandler;
import cn.keepbx.jpom.common.interceptor.NotLogin;
import cn.keepbx.jpom.model.Role;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.model.system.JpomManifest;
import cn.keepbx.jpom.service.node.NodeService;
import cn.keepbx.jpom.service.user.UserService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 首页
 *
 * @author Administrator
 */
@Controller
@RequestMapping(value = "/")
public class IndexControl extends BaseServerController {
    @Resource
    private UserService userService;

    @Resource
    private NodeService nodeService;

    @RequestMapping(value = "error", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    @NotLogin
    public String error(String id) {
        String msg = GlobalDefaultExceptionHandler.getErrorMsg(id);
        setAttribute("msg", msg);
        return "error";
    }

    /**
     * 加载首页
     *
     * @return page
     */
    @RequestMapping(value = {"index", "", "index.html", "/"}, method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String index() {
        if (userService.userListEmpty()) {
            getSession().invalidate();
            return "redirect:install.html";
        }

        // 版本号
        setAttribute("jpomManifest", JpomManifest.getInstance());
        return "index";
    }

    @RequestMapping(value = "menus_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String menusData() {
        UserModel user = getUser();
        NodeModel nodeModel = tryGetNode();
        // 菜单
        InputStream inputStream = ResourceUtil.getStream("classpath:/menus/index.json");
        String json = IoUtil.read(inputStream, CharsetUtil.CHARSET_UTF_8);
        JSONArray jsonArray = JSONArray.parseArray(json);
        List<Object> collect1 = jsonArray.stream().filter(o -> {
            JSONObject jsonObject = (JSONObject) o;
            if (!testMenus(jsonObject, user, nodeModel)) {
                return false;
            }
            JSONArray childs = jsonObject.getJSONArray("childs");
            if (childs != null) {
                List<Object> collect = childs.stream().filter(o1 -> {
                    JSONObject jsonObject1 = (JSONObject) o1;
                    return testMenus(jsonObject1, user, nodeModel);
                }).collect(Collectors.toList());
                jsonObject.put("childs", collect);
            }
            return true;
        }).collect(Collectors.toList());
        return JsonMessage.getString(200, "", collect1);
    }

    private boolean testMenus(JSONObject jsonObject, UserModel user, NodeModel nodeModel) {
        String role = jsonObject.getString("role");
        if (StrUtil.isNotEmpty(role)) {
            Role role1 = Role.valueOf(role);
            if (role1 == Role.System && !user.isSystemUser()) {
                return false;
            }
            if (role1 == Role.ServerManager && !user.isServerManager()) {
                return false;
            }
            if (role1 == Role.NodeManage) {
                if (nodeModel == null) {
                    return false;
                }
                if (!user.isManage(nodeModel.getId())) {
                    return false;
                }
            }
        }
        //
        String dynamic = jsonObject.getString("dynamic");
        if (StrUtil.isNotEmpty(dynamic)) {
            if ("showOutGiving".equals(dynamic)) {
                // 是否显示节点分发菜单
                List<NodeModel> list = nodeService.list();
                return list != null && !list.isEmpty();
            }
        }
        return true;
    }
}
