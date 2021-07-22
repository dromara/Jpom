package io.jpom.controller.user.log;

import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorConfig;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.dblog.DbUserOperateLogService;
import io.jpom.service.user.UserService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * 用户操作日志
 *
 * @author jiangzeyin
 * @date 2019/4/19
 */
@Controller
@RequestMapping(value = "/user/log")
@Feature(cls = ClassFeature.USER)
public class UserOptLogController extends BaseServerController {
    @Resource
    private UserService userService;
    @Resource
    private DbUserOperateLogService dbUserOperateLogService;

//    /**
//     * 展示用户列表
//     *
//     * @return page
//     */
//    @RequestMapping(value = "list.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @Feature(method = MethodFeature.LOG)
//    public String projectInfo() {
//        // 所有节点
//        List<NodeModel> nodeModels = nodeService.list();
//        setAttribute("nodeArray", nodeModels);
//        // 用户
//        List<UserModel> userModels = userService.list();
//        setAttribute("userArray", userModels);
//        return "user/log/list";
//    }


    /**
     * 展示用户列表
     *
     * @param limit 大小
     * @param page  page
     * @return json
     */
    @RequestMapping(value = "list_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.LOG)
    public String listData(
            @ValidatorConfig(value = {
                    @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "limit error")
            }, defaultVal = "10") int limit,
            @ValidatorConfig(value = {
                    @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "page error")
            }, defaultVal = "1") int page) {

        Page pageObj = new Page(page, limit);
        Entity entity = Entity.create();
        this.doPage(pageObj, entity, "optTime");

        String selectNode = getParameter("selectNode");
        if (StrUtil.isNotEmpty(selectNode)) {
            entity.set("nodeId".toUpperCase(), selectNode);
        }

        String selectUser = getParameter("selectUser");
        if (StrUtil.isNotEmpty(selectUser)) {
            entity.set("userId".toUpperCase(), selectUser);
        }

        PageResult<UserOperateLogV1> pageResult = dbUserOperateLogService.listPage(entity, pageObj);
        JSONObject jsonObject = JsonMessage.toJson(200, "获取成功", pageResult);
        jsonObject.put("total", pageResult.getTotal());
        return jsonObject.toString();
    }
}
