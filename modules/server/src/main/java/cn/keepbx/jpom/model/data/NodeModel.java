package cn.keepbx.jpom.model.data;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.model.BaseModel;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * 节点实体
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public class NodeModel extends BaseModel {

    private String url;
    private String loginName;
    private String loginPwd;
    /**
     * 节点协议
     */
    private String protocol = "http";

    private String authorize;
    /**
     * 项目信息
     */
    private JSONArray projects;
    /**
     * 开启状态，如果关闭状态就暂停使用节点
     */
    private boolean openStatus;

    /**
     * 返回按照项目分组 排列的数组
     *
     * @return array
     */
    public JSONArray getGroupProjects() {
        JSONArray array = getProjects();
        if (array == null) {
            return null;
        }
        JSONArray newArray = new JSONArray();
        Map<String, JSONObject> map = new HashMap<>(array.size());
        array.forEach(o -> {
            JSONObject pItem = (JSONObject) o;
            String group = pItem.getString("group");
            JSONObject jsonObject = map.computeIfAbsent(group, s -> {
                JSONObject jsonObject1 = new JSONObject();
                jsonObject1.put("group", s);
                jsonObject1.put("projects", new JSONArray());
                return jsonObject1;
            });
            JSONArray jsonArray = jsonObject.getJSONArray("projects");
            jsonArray.add(pItem);
        });
        newArray.addAll(map.values());
        return newArray;
    }

    public String getAuthorize(boolean get) {
        if (authorize == null) {
            authorize = SecureUtil.sha1(loginName + "@" + loginPwd);
        }
        return authorize;
    }

    public String getRealUrl(NodeUrl nodeUrl) {
        return StrUtil.format("{}://{}{}", getProtocol(), getUrl(), nodeUrl.getUrl());
    }
}
