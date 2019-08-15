package cn.keepbx.jpom.controller.user.role;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.model.data.RoleModel;
import cn.keepbx.jpom.service.user.RoleService;
import cn.keepbx.permission.CacheControllerFeature;
import cn.keepbx.plugin.ClassFeature;
import cn.keepbx.plugin.MethodFeature;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户权限基本管理
 *
 * @author bwcx_jzy
 * @date 2019/8/15
 */
@Controller
@RequestMapping(value = "/user/role")
public class UserRoleListController extends BaseServerController {

    @Resource
    private RoleService roleService;

    @RequestMapping(value = "list.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String list() {
        return "user/role/list";
    }

    @RequestMapping(value = "edit.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String edit(String id) {
        if (StrUtil.isNotEmpty(id)) {
            RoleModel item = roleService.getItem(id);
            setAttribute("item", item);
        }
        return "user/role/edit";
    }


    /**
     * 查询所有用户
     */
    @RequestMapping(value = "list_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String listData() {
        return JsonMessage.getString(200, "", roleService.list());
    }

    @RequestMapping(value = "getFeature.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String getFeature(String id) {
        //
        RoleModel item = roleService.getItem(id);

        Map<ClassFeature, Set<MethodFeature>> featureMap = CacheControllerFeature.getFeatureMap();
        Set<Map.Entry<ClassFeature, Set<MethodFeature>>> entries = featureMap.entrySet();
        JSONArray jsonArray = new JSONArray();
        entries.forEach(classFeatureSetEntry -> {
            ClassFeature classFeature = classFeatureSetEntry.getKey();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("title", classFeature.getName());
            jsonObject.put("id", classFeature.name());
            Set<MethodFeature> value = classFeatureSetEntry.getValue();
            JSONArray children = new JSONArray();
            value.forEach(methodFeature -> {
                JSONObject cJson = new JSONObject();
                cJson.put("title", methodFeature.getName());
                cJson.put("id", classFeature.name() + "_" + methodFeature.name());
                //
                if (item != null) {
                    List<MethodFeature> methodFeature1 = item.getMethodFeature(classFeature);
                    if (methodFeature1 != null && methodFeature1.contains(methodFeature)) {
                        cJson.put("checked", true);
                    }
                }
                children.add(cJson);
            });
            jsonObject.put("children", children);
            jsonObject.put("spread", true);
            jsonArray.add(jsonObject);
        });
        return JsonMessage.getString(200, "", jsonArray);
    }

    @RequestMapping(value = "save.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String save(String id,
                       @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "请输入角色名称") String name,
                       @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "请输入选择权限") String feature) {
        JSONArray jsonArray = JSONArray.parseArray(feature);
        RoleModel item = roleService.getItem(id);
        if (item == null) {
            item = new RoleModel();
            item.setId(IdUtil.fastSimpleUUID());
        }
        item.setName(name);
        List<RoleModel.RoleFeature> roleFeatures = new ArrayList<>();
        jsonArray.forEach(o -> {
            JSONObject jsonObject = (JSONObject) o;
            JSONArray children = jsonObject.getJSONArray("children");
            if (children == null || children.isEmpty()) {
                return;
            }
            String id1 = jsonObject.getString("id");
            ClassFeature classFeature = ClassFeature.valueOf(id1);
            RoleModel.RoleFeature roleFeature = new RoleModel.RoleFeature();
            roleFeature.setFeature(classFeature);
            roleFeatures.add(roleFeature);
            //
            List<MethodFeature> methodFeatures = new ArrayList<>();
            children.forEach(o1 -> {
                JSONObject childrenItem = (JSONObject) o1;
                String id11 = childrenItem.getString("id");
                String[] split = StrUtil.split(id11, StrUtil.UNDERLINE);
                MethodFeature methodFeature = MethodFeature.valueOf(split[1]);
                methodFeatures.add(methodFeature);
            });
            roleFeature.setMethodFeatures(methodFeatures);
        });
        item.setFeatures(roleFeatures);
        //
        if (StrUtil.isNotEmpty(id)) {
            roleService.updateItem(item);
        } else {
            roleService.addItem(item);
        }
        return JsonMessage.getString(200, "操作成功");
    }
}
