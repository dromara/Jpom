package cn.jiangzeyin.controller.manage;

import cn.hutool.core.io.FileUtil;
import cn.hutool.system.RuntimeInfo;
import cn.jiangzeyin.controller.BaseController;
import cn.jiangzeyin.service.manage.CommandService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;

/**
 * 内存查看
 */
@Controller
@RequestMapping(value = "/manage/")
public class InternalController extends BaseController {

    @Resource
    private CommandService commandService;

    @RequestMapping(value = "internal", method = RequestMethod.GET)
    public String getInternal(String tag) {
        JSONObject internal = commandService.getInternal(tag);


        RuntimeInfo runtimeInfo = new RuntimeInfo();
        JSONObject object = new JSONObject();
        object.put("total", FileUtil.readableFileSize(runtimeInfo.getMaxMemory()));
        object.put("use", FileUtil.readableFileSize(runtimeInfo.getTotalMemory() - runtimeInfo.getFreeMemory()));
        object.put("allot", FileUtil.readableFileSize(runtimeInfo.getTotalMemory()));
        object.put("free", FileUtil.readableFileSize(runtimeInfo.getFreeMemory()));
        object.put("useAble", FileUtil.readableFileSize(runtimeInfo.getUsableMemory()));
        setAttribute("internal", object);
        return "/manage/internal";
    }

}
