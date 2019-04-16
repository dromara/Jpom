package cn.keepbx.jpom.common;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.HttpUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.model.data.NodeModel;
import com.alibaba.fastjson.JSON;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author jiangzeyin
 * @date 2019/4/16
 */
public class NodeForward {

    public static JsonMessage request(NodeModel nodeModel, HttpServletRequest request, NodeUrl nodeUrl) {
        Map params = ServletUtil.getParams(request);
        String url = StrUtil.format("{}{}", nodeModel.getUrl(), nodeUrl.getUrl());
        String body = HttpUtil.createPost(url)
                .form(params)
                .execute()
                .body();
        return JSON.parseObject(body, JsonMessage.class);
    }
}
