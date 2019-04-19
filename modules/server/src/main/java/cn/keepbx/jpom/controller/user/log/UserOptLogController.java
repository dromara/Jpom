package cn.keepbx.jpom.controller.user.log;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.model.data.UserOperateLogV1;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.sql.SQLException;

/**
 * 用户操作日志
 *
 * @author jiangzeyin
 * @date 2019/4/19
 */
@Controller
@RequestMapping(value = "/user/log")
public class UserOptLogController extends BaseServerController {

    /**
     * 展示用户列表
     */
    @RequestMapping(value = "list.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String projectInfo() {
        return "user/log/list";
    }


    /**
     * 展示用户列表
     */
    @RequestMapping(value = "list_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String listData() throws SQLException {
        int limit = getParameterInt("limit", 10);
        int page1 = getParameterInt("page", 1);
        Page page = new Page(page1, limit);
        Entity entity = Entity.create(UserOperateLogV1.TABLE_NAME);
        page.addOrder(new Order("optTime", Direction.DESC));
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
