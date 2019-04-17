package cn.keepbx.jpom.common.forward;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.model.data.NodeModel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * @author jiangzeyin
 * @date 2019/4/16
 */
public class NodeForward {

    public static JsonMessage request(NodeModel nodeModel, HttpServletRequest request, NodeUrl nodeUrl) {
        Map params = ServletUtil.getParams(request);
        String url = StrUtil.format("http://{}{}", nodeModel.getUrl(), nodeUrl.getUrl());

        HttpRequest httpRequest = HttpUtil.createPost(url);
        //
        addUser(httpRequest);
        //
        String body = httpRequest
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
        //
        HttpRequest httpRequest = HttpUtil.createPost(url);
        if (name != null && value != null) {
            httpRequest.form(name, value, parameters);
        }
        //
        addUser(httpRequest);
        //
        String body = httpRequest
                .execute()
                .body();
        JsonMessage jsonMessage = JSON.parseObject(body, JsonMessage.class);
        Object data = jsonMessage.getData();
        if (jsonMessage.getCode() == 200 && null != data) {
            return JSONObject.parseObject(jsonMessage.getData().toString(), tClass);
        }
        System.out.println(jsonMessage.toString());
        return null;
    }

    public static JsonMessage requestMultipart(NodeModel nodeModel, MultipartHttpServletRequest request, NodeUrl nodeUrl) {
        String url = StrUtil.format("http://{}{}", nodeModel.getUrl(), nodeUrl.getUrl());
        HttpRequest httpRequest = HttpUtil.createPost(url);
        addUser(httpRequest);
        //
        Map params = ServletUtil.getParams(request);
        httpRequest.form(params);
        //
        Map<String, MultipartFile> fileMap = request.getFileMap();
        fileMap.forEach((s, multipartFile) -> {
            try {
                httpRequest.form(s, multipartFile.getBytes(), multipartFile.getOriginalFilename());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        String body = httpRequest
                .execute()
                .body();
        System.out.println(body);
        return JSON.parseObject(body, JsonMessage.class);
    }


    public static void requestDownload(NodeModel nodeModel, HttpServletRequest request, HttpServletResponse response, NodeUrl nodeUrl) {
        String url = StrUtil.format("http://{}{}", nodeModel.getUrl(), nodeUrl.getUrl());
        HttpRequest httpRequest = HttpUtil.createGet(url);
        addUser(httpRequest);
        //
        Map params = ServletUtil.getParams(request);
        httpRequest.form(params);
        //
        HttpResponse response1 = httpRequest
                .execute();
        String contentDisposition = response1.header("Content-Disposition");
        response.setHeader("Content-Disposition", contentDisposition);
        String contentType = response1.header("Content-Type");
        response.setContentType(contentType);
        ServletUtil.write(response, response1.bodyStream());
    }


    private static void addUser(HttpRequest httpRequest) {
        httpRequest.header("Jpom-Server-UserName", BaseController.getOptUserName());
    }

    public static String getSocketUrl(NodeModel nodeModel, NodeUrl nodeUrl) {
        return StrUtil.format("ws://{}{}", nodeModel.getUrl(), nodeUrl.getUrl());
    }

}
