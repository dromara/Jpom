package cn.keepbx.jpom.controller.user.role;

import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.model.BaseModel;
import cn.keepbx.jpom.service.BaseDynamicService;
import cn.keepbx.permission.DynamicData;
import cn.keepbx.plugin.ClassFeature;
import com.alibaba.fastjson.JSONArray;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * @author bwcx_jzy
 * @date 2019/8/15
 */
@Controller
@RequestMapping(value = "/user/role")
public class UserRoleDynamicController extends BaseServerController {

    @RequestMapping(value = "dynamicData.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String list() {
        Map<ClassFeature, DynamicData> dynamicDataMap = DynamicData.getDynamicDataMap();
        setAttribute("dynamicDataMap", dynamicDataMap);
        return "user/role/dynamicData";
    }

    @RequestMapping(value = "getDynamic.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String getDynamic(String id, String dynamic) {
        ClassFeature classFeature = ClassFeature.valueOf(dynamic);
        Map<ClassFeature, DynamicData> dynamicDataMap = DynamicData.getDynamicDataMap();
        DynamicData dynamicData = dynamicDataMap.get(classFeature);
        if (dynamicData == null) {
            return JsonMessage.getString(404, "没有配置对应动态数据");
        }
        Class<? extends BaseDynamicService> baseOperService = dynamicData.getBaseOperService();
        BaseDynamicService<BaseModel> bean = SpringUtil.getBean(baseOperService);
        JSONArray jsonArray = bean.listDynamic();
        if (jsonArray == null) {
            return JsonMessage.getString(405, "没有动态数据");
        }
        return JsonMessage.getString(200, "", jsonArray);
    }
}
