package cn.keepbx.monitor;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.keepbx.jpom.model.data.MonitorModel;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;

/**
 * 钉钉工具
 *
 * @author Arno
 */
public class DingTalkUtil implements INotify {

    /**
     * 发送钉钉群自定义机器人消息
     *
     * @param notify  webhook地址
     * @param title   被@人的手机号
     * @param context 消息内容
     */
    @Override
    public void send(MonitorModel.Notify notify, String title, String context) {
        JSONObject text = new JSONObject();
        JSONObject param = new JSONObject();
        //消息内容
        text.put("content", title + "\n" + context);
//        // 被@人的手机号
//        if (StrUtil.isNotEmpty(phone)) {
//            JSONArray atMobiles = new JSONArray();
//            atMobiles.add(phone);
//            JSONObject at = new JSONObject();
//            at.put("atMobiles", atMobiles);
//            param.put("at", at);
//        }
        param.put("msgtype", "text");
        param.put("text", text);
        HttpRequest request = HttpUtil.createPost(notify.getValue()).contentType(MediaType.APPLICATION_JSON_UTF8_VALUE).body(param.toJSONString());
        HttpResponse execute = request.execute();
        String body = execute.body();
        System.out.println(body);
    }
}
