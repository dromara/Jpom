package io.jpom.monitor;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.model.data.MonitorModel;
import org.springframework.http.MediaType;

/**
 * 钉钉工具
 *
 * @author Arno
 */
public class WebHookUtil implements INotify {

    /**
     * 发送钉钉群自定义机器人消息
     *
     * @param notify  通知对象
     * @param title   描述标签
     * @param context 消息内容
     */
    @Override
    public void send(MonitorModel.Notify notify, String title, String context) {
        JSONObject text = new JSONObject();
        JSONObject param = new JSONObject();
        //消息内容
        text.put("content", title + "\n" + context);
        param.put("msgtype", "text");
        param.put("text", text);
        HttpRequest request = HttpUtil.
                createPost(notify.getValue()).
                contentType(MediaType.APPLICATION_JSON_UTF8_VALUE).
                body(param.toJSONString());
        request.execute();
    }
}
