package cn.keepbx.jpom.controller.node.monitor;

import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.model.log.UserOperateLogV1;
import cn.keepbx.jpom.system.OperateType;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 内存查看
 *
 * @author Administrator
 */
@Controller
@RequestMapping(value = "/node/manage/")
public class InternalController extends BaseServerController {

    /**
     * 获取内存信息
     */
    @RequestMapping(value = "internal", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String getInternal(String tag) throws Exception {
        setAttribute("tag", tag);
        JSONObject data = NodeForward.requestData(getNode(), NodeUrl.Manage_internal_data, getRequest(), JSONObject.class);
        setAttribute("data", data);
        return "node/manage/internal";
    }

    /**
     * 查询监控线程列表
     */
    @RequestMapping(value = "threadInfos", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String threadInfos() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_internal_threadInfos).toString();
    }

    /**
     * 导出堆栈信息
     */
    @RequestMapping(value = "stack", method = RequestMethod.GET)
    @ResponseBody
    @OperateType(UserOperateLogV1.OptType.ExportStack)
    public void stack() {
        NodeForward.requestDownload(getNode(), getRequest(), getResponse(), NodeUrl.Manage_internal_stack);
    }

    /**
     * 导出内存信息
     */
    @RequestMapping(value = "ram", method = RequestMethod.GET)
    @ResponseBody
    @OperateType(UserOperateLogV1.OptType.ExportRam)
    public void ram() throws Exception {
        NodeForward.requestDownload(getNode(), getRequest(), getResponse(), NodeUrl.Manage_internal_ram);
    }
}
