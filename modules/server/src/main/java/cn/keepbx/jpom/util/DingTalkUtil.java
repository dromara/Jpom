package cn.keepbx.jpom.util;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiRobotSendRequest;
import com.dingtalk.api.response.OapiRobotSendResponse;
import com.taobao.api.ApiException;

import java.util.Collections;

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
        DingTalkClient client = new DefaultDingTalkClient(serverUrl);
        OapiRobotSendRequest request = new OapiRobotSendRequest();
        request.setMsgtype("text");
        OapiRobotSendRequest.Text text = new OapiRobotSendRequest.Text();
        //消息内容
        text.setContent(content);
        request.setText(text);
        // 被@人的手机号
        if (StrUtil.isNotEmpty(phone)) {
            OapiRobotSendRequest.At at = new OapiRobotSendRequest.At();
            at.setAtMobiles(Collections.singletonList(phone));
            request.setAt(at);
        }
        try {
            OapiRobotSendResponse response = client.execute(request);
            if (response.isSuccess()) {
                DefaultSystemLog.LOG().info("发送钉钉消息成功");
            } else {
                DefaultSystemLog.ERROR().error("发送钉钉消息失败: " + response.getErrmsg());
            }
        } catch (ApiException e) {
            DefaultSystemLog.ERROR().error("发送钉钉消息失败", e);
        }
    }

}
