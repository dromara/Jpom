package cn.keepbx.jpom.controller.user;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.model.log.UserOperateLogV1;
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
import java.sql.SQLException;
import java.util.List;

/**
 * 用户操作日志
 *
 * @author jiangzeyin
 * @date 2019/4/19
 */
@Controller
@RequestMapping(value = "/user/log")
public class UserOptLogController extends BaseServerController {
    @Resource
    private NodeService nodeService;
    @Resource
    private UserService userService;

    /**
     * 展示用户列表
     */
    @RequestMapping(value = "list.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String projectInfo() {
        // 所有节点
        List<NodeModel> nodeModels = nodeService.list();
        setAttribute("nodeArray", nodeModels);
        // 用户
        List<UserModel> userModels = userService.list();
        setAttribute("userArray", userModels);
        return "user/log/list";
    }


    /**
     * 展示用户列表
     */
    @RequestMapping(value = "list_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String listData(String time) throws SQLException {
        int limit = getParameterInt("limit", 10);
        int page1 = getParameterInt("page", 1);
        Page page = new Page(page1, limit);
        Entity entity = Entity.create(UserOperateLogV1.TABLE_NAME);
        page.addOrder(new Order("optTime".toUpperCase(), Direction.DESC));
        // 时间
        if (StrUtil.isNotEmpty(time)) {
            String[] val = StrUtil.split(time, "~");
            if (val.length == 2) {
                DateTime startDateTime = DateUtil.parse(val[0], DatePattern.NORM_DATETIME_FORMAT);
                entity.set("optTime".toUpperCase(), ">= " + startDateTime.getTime());

                DateTime endDateTime = DateUtil.parse(val[1], DatePattern.NORM_DATETIME_FORMAT);
                if (startDateTime.equals(endDateTime)) {
                    endDateTime = DateUtil.endOfDay(endDateTime);
                }
                entity.set("optTime ".toUpperCase(), "<= " + endDateTime.getTime());
            }
        }
        String selectNode = getParameter("selectNode");
        if (StrUtil.isNotEmpty(selectNode)) {
            entity.set("nodeId".toUpperCase(), selectNode);
        }

        String selectUser = getParameter("selectUser");
        if (StrUtil.isNotEmpty(selectUser)) {
            entity.set("userId".toUpperCase(), selectUser);
        }

        PageResult<Entity> pageResult = Db.use().page(entity, page);
        CopyOptions copyOptions = new CopyOptions();
        copyOptions.setIgnoreError(true);
        copyOptions.setIgnoreCase(true);
        JSONArray jsonArray = new JSONArray();
        pageResult.forEach(entity1 -> {
            UserOperateLogV1 v1 = BeanUtil.mapToBean(entity1, UserOperateLogV1.class, copyOptions);
            jsonArray.add(v1);
        });
        JSONObject jsonObject = JsonMessage.toJson(200, "获取成功", jsonArray);
        jsonObject.put("total", pageResult.getTotal());
        return jsonObject.toString();
    }
}
