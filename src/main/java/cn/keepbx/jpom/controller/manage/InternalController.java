package cn.keepbx.jpom.controller.manage;

import cn.hutool.core.io.FileUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.common.commander.AbstractCommander;
import cn.keepbx.jpom.model.ProjectInfoModel;
import cn.keepbx.jpom.service.manage.CommandService;
import cn.keepbx.jpom.service.manage.ProjectInfoService;
import cn.keepbx.jpom.system.ConfigBean;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * 内存查看
 *
 * @author Administrator
 */
@Controller
@RequestMapping(value = "/manage/")
public class InternalController extends BaseController {

    @Resource
    private CommandService commandService;

    @Resource
    private ProjectInfoService projectInfoService;

    /**
     * 获取内存信息
     */
    @RequestMapping(value = "internal", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String getInternal(String tag) throws Exception {
        ProjectInfoModel projectInfoModel = projectInfoService.getItem(tag);
        String pid = commandService.execCommand(CommandService.CommandOp.pid, projectInfoModel, null);
        String command = "top -b -n 1 -p " + pid;
        String internal = AbstractCommander.getInstance().execCommand(command);
        internal = internal.replaceAll("\n", "<br/>");
        internal = internal.replaceAll(" ", "&nbsp;&nbsp;");
        JSONObject object = new JSONObject();
        object.put("ram", internal);
        object.put("tag", tag);
        setAttribute("internal", object);
        return "manage/internal";
    }

    /**
     * 导出堆栈信息
     */
    @RequestMapping(value = "stack", method = RequestMethod.GET)
    @ResponseBody
    public String stack(String tag) throws Exception {
        ProjectInfoModel projectInfoModel = projectInfoService.getItem(tag);
        String pid = commandService.execCommand(CommandService.CommandOp.pid, projectInfoModel, null).trim();
        pid = pid.replace("\n", "");
        String fileName = ConfigBean.getInstance().getTempPathName() + "/" + tag + "_java_cpu.txt";
        fileName = FileUtil.normalize(fileName);
        String commandPath = ConfigBean.getInstance().getCpuCommandPath();
        String command = String.format("%s %s %s %s", commandPath, pid, 300, fileName);
        AbstractCommander.getInstance().execCommand(command);
        downLoad(getResponse(), fileName);
        return JsonMessage.getString(200, "");
    }

    /**
     * 导出内存信息
     */
    @RequestMapping(value = "ram", method = RequestMethod.GET)
    @ResponseBody
    public String ram(String tag) throws Exception {
        ProjectInfoModel projectInfoModel = projectInfoService.getItem(tag);
        String pid = commandService.execCommand(CommandService.CommandOp.pid, projectInfoModel, null).trim();
        String fileName = ConfigBean.getInstance().getTempPathName() + "/" + tag + "_java_ram.txt";
        fileName = FileUtil.normalize(fileName);
        String commandPath = ConfigBean.getInstance().getRamCommandPath();
        String command = String.format("%s %s %s", commandPath, pid, fileName);
        AbstractCommander.getInstance().execCommand(command);
        downLoad(getResponse(), fileName);
        return JsonMessage.getString(200, "");
    }

    /**
     * 下载文件
     *
     * @param response response
     * @param fileName 文件名字
     */
    private void downLoad(HttpServletResponse response, String fileName) {
        //获取项目根路径
        File file = new File(fileName);
        ServletUtil.write(response, file);
        FileUtil.del(file);
    }

}
