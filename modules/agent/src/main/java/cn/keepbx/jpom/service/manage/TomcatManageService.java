package cn.keepbx.jpom.service.manage;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseOperService;
import cn.keepbx.jpom.model.data.TomcatInfoModel;
import cn.keepbx.jpom.system.AgentConfigBean;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TomcatManageService extends BaseOperService<TomcatInfoModel> {

    /**
     * 查询tomcat列表
     * @return Tomcat列表
     */
    @Override
    public List<TomcatInfoModel> list() {
        JSONObject jsonObject = getJSONObject(AgentConfigBean.TOMCAT);
        JSONArray jsonArray = formatToArray(jsonObject);
        return jsonArray.toJavaList(TomcatInfoModel.class);
    }

    /**
     * 查询Tomcat信息
     * @param id 数据id
     * @return Tomcat信息
     */
    @Override
    public TomcatInfoModel getItem(String id) {
        return getJsonObjectById(AgentConfigBean.TOMCAT, id, TomcatInfoModel.class);
    }

    /**
     * 根据tomcat名称查询tomcat信息
     * @param name tomcat的名称
     * @return tomcat信息
     */
    public TomcatInfoModel getItemByName(String name) {
        JSONObject allTomcat = getJSONObject(AgentConfigBean.TOMCAT);

        if (allTomcat == null) {
            return null;
        }

        JSONObject tomcat = null;
        for (String key : allTomcat.keySet()) {
            JSONObject object = allTomcat.getJSONObject(key);
            if (name.equals(object.getString("name"))) {
                tomcat = object;
                break;
            }
        }

        return JSONObject.toJavaObject(tomcat, TomcatInfoModel.class);
    }

    /**
     * 添加Tomcat
     * @param tomcatInfoModel tomcat信息
     */
    @Override
    public void addItem(TomcatInfoModel tomcatInfoModel) {
        saveJson(AgentConfigBean.TOMCAT, tomcatInfoModel.toJson());
    }

    /**
     * 删除tomcat信息
     * @param id 数据id
     */
    @Override
    public void deleteItem(String id) {
        deleteJson(AgentConfigBean.TOMCAT, id);
    }

    /**
     * 修改tomcat信息
     * @param tomcatInfoModel tomcat信息
     * @return 更新结果
     */
    @Override
    public boolean updateItem(TomcatInfoModel tomcatInfoModel) {
        updateJson(AgentConfigBean.TOMCAT, tomcatInfoModel.toJson());
        return true;
    }

    /**
     * 查询tomcat状态
     * @param id tomcat的id
     * @return tomcat状态0表示未运行，1表示运行中
     */
    public int getTomcatStatus(String id) {
        int result = 0;
        TomcatInfoModel tomcatInfoModel = getItem(id);
        String url = String.format("http://127.0.0.1:%d/", tomcatInfoModel.getPort());
        HttpRequest httpRequest = new HttpRequest(url);
        httpRequest.setConnectionTimeout(3000); // 设置超时时间为3秒
        try {
            HttpResponse httpResponse = httpRequest.execute();

            result = 1;
        } catch (Exception ignored) {}

        return result;
    }

    /**
     * 查询tomcat的项目列表
     * @param id tomcat的id
     * @return tomcat的项目列表
     */
    public JSONArray getTomcatProjectList(String id) {
        TomcatInfoModel tomcatInfoModel = getItem(id);
        String body = tomcatCmd(tomcatInfoModel, "text/list");

        String[] result = body.split("\r\n");

        JSONArray jsonArray = new JSONArray();

        for (int i = 1;  i < result.length; i++) {
            String str = result[i];
            JSONObject jsonObject = new JSONObject();
            String[] strs = str.split(":");
            if (strs[0].endsWith("jpomAgent")) {
                continue;
            }

            jsonObject.put("path", strs[0].equals("/") ? "/ROOT" : strs[0]);
            jsonObject.put("status", strs[1]);
            jsonObject.put("session", strs[2]);

            jsonArray.add(jsonObject);
        }

        return jsonArray;
    }

    /**
     * 访问tomcat Url
     * @param tomcatInfoModel tomcat信息
     * @param cmd 命令
     * @return 访问结果
     */
    private String tomcatCmd(TomcatInfoModel tomcatInfoModel, String cmd) {
        String url = String.format("http://127.0.0.1:%d/jpomAgent/%s", tomcatInfoModel.getPort(), cmd);
        HttpRequest httpRequest = new HttpRequest(url);
        httpRequest.setConnectionTimeout(3000); // 设置超时时间为3秒
        String body = "";

        try {
             HttpResponse httpResponse = httpRequest.execute();
             if (httpResponse.isOk()) {
                 body = httpResponse.body();
             }
        } catch (Exception ignored) {}

        return body;
    }

    /**
     * tomcat项目管理
     * @param id tomcat id
     * @param path 项目路径
     * @param op 执行的操作 start=>启动项目 stop=>停止项目 relaod=>重启项目
     * @return 操作结果
     */
    public JsonMessage tomcatProjectManage(String id, String path, String op) {
        TomcatInfoModel tomcatInfoModel = getItem(id);
        String result = tomcatCmd(tomcatInfoModel, String.format("text/%s?path=%s", op, path));

        if (result.startsWith("OK")) {
            return new JsonMessage(200, "操作成功");
        } else {
            return new JsonMessage(500, "操作失败");
        }
    }
}
