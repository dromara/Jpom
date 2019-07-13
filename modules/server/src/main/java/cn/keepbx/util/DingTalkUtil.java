package cn.keepbx.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;

/**
 * 钉钉工具
 *
 * @author Arno
 */
public class DingTalkUtil {

    /**
     * 发送钉钉群自定义机器人消息
     *
     * @param serverUrl webhook地址
     * @param phone     被@人的手机号
     * @param content   消息内容
     */
    public static void sendMsg(String serverUrl, String phone, String content) {
        JSONObject text = new JSONObject();
        JSONObject param = new JSONObject();
        //消息内容
        text.put("content", content);
        // 被@人的手机号
        if (StrUtil.isNotEmpty(phone)) {
            JSONArray atMobiles = new JSONArray();
            atMobiles.add(phone);
            JSONObject at = new JSONObject();
            at.put("atMobiles", atMobiles);
            param.put("at", at);
        }
        param.put("msgtype", "text");
        param.put("text", text);
        HttpRequest request = HttpUtil.createPost(serverUrl).contentType(MediaType.APPLICATION_JSON_UTF8_VALUE).body(param.toJSONString());
        HttpResponse execute = request.execute();
        String body = execute.body();
        System.out.println(body);
    }

}
