package cn.keepbx.jpom.controller.user;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpStatus;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.Role;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.common.interceptor.UrlPermission;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.service.node.NodeService;
import cn.keepbx.jpom.service.user.UserService;
import com.alibaba.fastjson.JSONArray;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 用户列表
 *
 * @author Administrator
 */
@Controller
@RequestMapping(value = "/user")
public class UserListController extends BaseServerController {
    private static final TimedCache<String, List<NodeModel>> TIMED_CACHE = new TimedCache<>(TimeUnit.MINUTES.toMillis(5));

    /**
     * 获取页面编辑的节点信息
     *
     * @param id id
     * @return list
     */
    static List<NodeModel> getNodeModel(String id) {
        List<NodeModel> nodeModels = TIMED_CACHE.get(id);
        TIMED_CACHE.remove(id);
        return nodeModels;
    }

    @Resource
    private UserService userService;

    @Resource
    private NodeService nodeService;

    /**
     * 展示用户列表
     */
    @RequestMapping(value = "list", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String projectInfo() {
        return "user/list";
    }

    @RequestMapping(value = "edit", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String edit(String id) {
        List<NodeModel> nodeModels = nodeService.list();
        Iterator<NodeModel> iterator = nodeModels.iterator();
        while (iterator.hasNext()) {
            NodeModel nodeModel = iterator.next();
            if (!nodeModel.isOpenStatus()) {
                iterator.remove();
                continue;
            }
            try {
                // 获取项目信息不需要状态
                JsonMessage jsonMessage = NodeForward.request(nodeModel, getRequest(), NodeUrl.Manage_GetProjectInfo, "notStatus", "true");
                if (jsonMessage.getCode() == HttpStatus.HTTP_OK) {
                    nodeModel.setProjects(NodeForward.toObj(jsonMessage, JSONArray.class));
                } else {
                    iterator.remove();
                }
            } catch (Exception e) {
                iterator.remove();
            }
        }

        String reqId = IdUtil.fastUUID();
        TIMED_CACHE.put(reqId, nodeModels);
        setAttribute("reqId", reqId);
        setAttribute("nodeModels", nodeModels);

        UserModel item = userService.getItem(id);
        setAttribute("userItem", item);
        return "user/edit";
    }

    /**
     * 查询所有用户
     */
    @RequestMapping(value = "getUserList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @UrlPermission(Role.ServerManager)
    public String getUserList() {
        UserModel userName = getUser();
        List<UserModel> userList = userService.list();
        if (userList != null) {
            Iterator<UserModel> userModelIterator = userList.iterator();
            // 不显示自己的信息
            while (userModelIterator.hasNext()) {
                UserModel item = userModelIterator.next();
                if (item.getId().equals(userName.getId())) {
                    userModelIterator.remove();
                    break;
                }
            }
        }
        return JsonMessage.getString(200, "", userList);
    }
}
