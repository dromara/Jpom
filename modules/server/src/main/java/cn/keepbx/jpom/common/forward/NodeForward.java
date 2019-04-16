package cn.keepbx.jpom.common.forward;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.model.data.NodeModel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author jiangzeyin
 * @date 2019/4/16
 */
public class NodeForward {

    public static JsonMessage request(NodeModel nodeModel, HttpServletRequest request, NodeUrl nodeUrl) {
        Map params = ServletUtil.getParams(request);
        String url = StrUtil.format("http://{}{}", nodeModel.getUrl(), nodeUrl.getUrl());
        String body = HttpUtil.createPost(url)
                .form(params)
                .execute()
                .body();
        return JSON.parseObject(body, JsonMessage.class);
    }


    public static <T> T requestData(NodeModel nodeModel, NodeUrl nodeUrl, Class<T> tClass) {
        return requestData(nodeModel, nodeUrl, tClass, null, null);
    }


    public static <T> T requestData(NodeModel nodeModel, NodeUrl nodeUrl, Class<T> tClass, String name, Object value, Object... parameters) {
        String url = StrUtil.format("http://{}{}", nodeModel.getUrl(), nodeUrl.getUrl());

        HttpRequest httpRequest = HttpUtil.createPost(url);
        if (name != null && value != null) {
            httpRequest.form(name, value, parameters);
        }
        String body = httpRequest
                .execute()
                .body();
        JsonMessage jsonMessage = JSON.parseObject(body, JsonMessage.class);
        if (jsonMessage.getCode() == 200) {
            if (jsonMessage.getData() == null) {
                return null;
            }
            return JSONObject.parseObject(jsonMessage.getData().toString(), tClass);
        }
        System.out.println(jsonMessage.toString());
        return null;
    }


    public static String getSocketUrl(NodeModel nodeModel, NodeUrl nodeUrl) {
        return StrUtil.format("ws://{}{}", nodeModel.getUrl(), nodeUrl.getUrl());
    }

}
