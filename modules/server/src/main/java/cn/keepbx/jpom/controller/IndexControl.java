package cn.keepbx.jpom.controller;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.GlobalDefaultExceptionHandler;
import cn.keepbx.jpom.common.interceptor.NotLogin;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.system.JpomManifest;
import cn.keepbx.jpom.service.node.NodeService;
import cn.keepbx.jpom.service.user.UserService;
import com.alibaba.fastjson.JSONArray;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        if (msg == null) {
            msg = "页面已经失效";
        }
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
        // 是否显示节点分发菜单
        List<NodeModel> list = nodeService.list();
        Map<String, Boolean> dynamic = new HashMap<>(10);
        dynamic.put("showOutGiving", list != null && list.size() > 1);
        // 动态
        setAttribute("dynamicMenus", dynamic);
        // 菜单
        InputStream inputStream = ResourceUtil.getStream("classpath:/menus/index.json");
        String json = IoUtil.read(inputStream, CharsetUtil.CHARSET_UTF_8);
        JSONArray jsonArray = JSONArray.parseArray(json);
        setAttribute("menus", jsonArray);
        return "index";
    }


}
