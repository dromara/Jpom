package cn.keepbx.jpom.controller.user.role;

import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.interceptor.OptLog;
import cn.keepbx.jpom.model.data.RoleModel;
import cn.keepbx.jpom.model.log.UserOperateLogV1;
import cn.keepbx.jpom.service.user.RoleService;
import cn.keepbx.permission.BaseDynamicService;
import cn.keepbx.permission.DynamicData;
import cn.keepbx.plugin.ClassFeature;
import cn.keepbx.plugin.Feature;
import cn.keepbx.plugin.MethodFeature;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author bwcx_jzy
 * @date 2019/8/15
 */
@Controller
@RequestMapping(value = "/user/role")
@Feature(cls = ClassFeature.USER_ROLE)
public class UserRoleDynamicController extends BaseServerController {

    @Resource
    private RoleService roleService;

    @RequestMapping(value = "dynamicData.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public String list() {
        Map<ClassFeature, DynamicData> dynamicDataMap = DynamicData.getDynamicDataMap();
        setAttribute("dynamicDataMap", dynamicDataMap);
        return "user/role/dynamicData";
    }

    @RequestMapping(value = "getDynamic.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.EDIT)
    public String getDynamic(String id, String dynamic) {
        ClassFeature classFeature = ClassFeature.valueOf(dynamic);
        JSONArray jsonArray = roleService.listDynamic(id, classFeature, null);
        return JsonMessage.getString(200, "", jsonArray);
    }

    @RequestMapping(value = "saveDynamic.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.EDIT)
    @OptLog(value = UserOperateLogV1.OptType.EditRole)
    public String saveDynamic(String id, String dynamic) {
        RoleModel item = roleService.getItem(id);
        if (item == null) {
            return JsonMessage.getString(404, "角色信息错误");
        }
        //
        JSONObject jsonObject = JSONObject.parseObject(dynamic);
        Map<ClassFeature, List<String>> dynamicData1 = new HashMap<>(jsonObject.keySet().size());
        //
        Set<Map.Entry<String, Object>> entries = jsonObject.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            ClassFeature classFeature = ClassFeature.valueOf(entry.getKey());
            Map<ClassFeature, DynamicData> dynamicDataMap = DynamicData.getDynamicDataMap();
            DynamicData dynamicData = dynamicDataMap.get(classFeature);
            if (dynamicData == null) {
                return JsonMessage.getString(404, entry.getKey() + "没有配置对应动态数据");
            }
            Class<? extends BaseDynamicService> baseOperService = dynamicData.getBaseOperService();
            BaseDynamicService bean = SpringUtil.getBean(baseOperService);
            JSONArray value = (JSONArray) entry.getValue();
            List<String> list = bean.parserValue(dynamicData1, classFeature, value);
            dynamicData1.put(classFeature, list);
        }
        item.setDynamicData(dynamicData1);
        roleService.updateItem(item);
        return JsonMessage.getString(200, "保存成功");
    }
}
