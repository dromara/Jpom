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
        String internal = commandService.getInternal(tag);
        internal = internal.replaceAll("\n", "<br/>");
        internal = internal.replaceAll(" ", "&nbsp;&nbsp;");
        JSONObject object = new JSONObject();
        object.put("ram", internal);
        setAttribute("internal", object);
        return "/manage/internal";
    }

}
