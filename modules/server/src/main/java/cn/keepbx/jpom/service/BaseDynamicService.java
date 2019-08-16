package cn.keepbx.jpom.service;

import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.service.user.RoleService;
import cn.keepbx.permission.DynamicData;
import cn.keepbx.plugin.ClassFeature;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.*;

/**
 * @author bwcx_jzy
 * @date 2019/8/15
 */
public interface BaseDynamicService {

    /**
     * 查询动态数据的array
     *
     * @param dataId 上级数据id
     * @return array
     */
    JSONArray listToArray(String dataId);

    /**
     * 查询功能下面的所有动态数据
     *
     * @param classFeature 功能
     * @param roleId       角色id
     * @param dataId       上级数据id
     * @return tree array
     */
    default JSONArray listDynamic(ClassFeature classFeature, String roleId, String dataId) {
        JSONArray listToArray = listToArray(dataId);
        if (listToArray == null || listToArray.isEmpty()) {
            return null;
        }
        JSONArray jsonArray = new JSONArray();
        listToArray.forEach(obj -> {
            JSONObject jsonObject = new JSONObject();
            JSONObject data = (JSONObject) obj;
            String name = data.getString("name");
            String id = data.getString("id");
            jsonObject.put("title", name);
            jsonObject.put("id", id);
            RoleService bean = SpringUtil.getBean(RoleService.class);
            boolean doChildren = this.doChildren(classFeature, roleId, id, jsonObject);
            if (!doChildren) {
                List<String> checkList = bean.listDynamicData(roleId, classFeature);
                if (checkList != null && checkList.contains(id)) {
                    jsonObject.put("checked", true);
                }
            }
            jsonArray.add(jsonObject);
        });
        return jsonArray;
    }

    /**
     * 处理子级数据
     *
     * @param classFeature 功能
     * @param roleId       角色id
     * @param dataId       数据id
     * @param jsonObject   parent
     * @return 是否包含子级
     */
    default boolean doChildren(ClassFeature classFeature, String roleId, String dataId, JSONObject jsonObject) {
        Set<ClassFeature> children = DynamicData.getChildren(classFeature);
        if (children == null) {
            return false;
        }
        JSONArray childrens = new JSONArray();
        children.forEach(classFeature1 -> {
            RoleService bean = SpringUtil.getBean(RoleService.class);
            JSONArray jsonArray1 = bean.listDynamic(roleId, classFeature1, dataId);
            if (jsonArray1 == null || jsonArray1.isEmpty()) {
                return;
            }
            JSONObject jsonObject1 = new JSONObject();
            jsonObject1.put("children", jsonArray1);
            jsonObject1.put("title", classFeature1.getName());
            jsonObject1.put("id", classFeature1.name());
            childrens.add(jsonObject1);
        });
        if (!childrens.isEmpty()) {
            jsonObject.put("children", childrens);
        }
        return true;
    }

    /**
     * 接收前端的值
     *
     * @param classFeatureListMap map
     * @param classFeature        功能
     * @param jsonArray           array
     * @return list
     */
    default List<String> parserValue(Map<ClassFeature, List<String>> classFeatureListMap, ClassFeature classFeature, JSONArray jsonArray) {
        if (jsonArray == null) {
            return null;
        }
        List<String> list = new ArrayList<>();
        jsonArray.forEach(o -> {
            JSONObject jsonObject = (JSONObject) o;
            JSONArray children = jsonObject.getJSONArray("children");
            if (children != null && !children.isEmpty()) {
                parserChildren(classFeature, classFeatureListMap, children);
            }
            list.add(jsonObject.getString("id"));
        });
        return list;
    }

    /**
     * 转换子级
     *
     * @param classFeature        功能
     * @param classFeatureListMap map
     * @param jsonArray           array
     */
    default void parserChildren(ClassFeature classFeature, Map<ClassFeature, List<String>> classFeatureListMap, JSONArray jsonArray) {
        Set<ClassFeature> children = DynamicData.getChildren(classFeature);
        if (children == null) {
            return;
        }
        Map<ClassFeature, JSONArray> jsonArrayMap = this.convertArray(jsonArray);
        for (ClassFeature child : children) {
            JSONArray jsonArray1 = jsonArrayMap.get(child);
            List<String> list = parserValue(classFeatureListMap, child, jsonArray1);
            classFeatureListMap.put(child, list);
        }
    }

    /**
     * 将二级数据转换为map
     *
     * @param jsonArray array
     * @return map
     */
    default Map<ClassFeature, JSONArray> convertArray(JSONArray jsonArray) {
        Map<ClassFeature, JSONArray> newMap = new HashMap<>();
        jsonArray.forEach(o -> {
            JSONObject jsonObject = (JSONObject) o;
            String id = jsonObject.getString("id");
            ClassFeature classFeature = ClassFeature.valueOf(id);
            newMap.put(classFeature, jsonObject.getJSONArray("children"));
        });
        return newMap;
    }
}
