package cn.jiangzeyin.controller.manage;

import cn.hutool.core.util.RandomUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.BaseController;
import cn.jiangzeyin.model.ProjectInfoModel;
import cn.jiangzeyin.service.manage.CommandService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

/**
 * 内存查看
 */
@Controller
@RequestMapping(value = "/manage/")
public class InternalController extends BaseController {

    @Resource
    private CommandService commandService;

    /**
     * 获取内存信息
     */
    @RequestMapping(value = "internal")
    public String getInternal(String tag) {
        ProjectInfoModel projectInfoModel = new ProjectInfoModel();
        projectInfoModel.setTag(tag);
        String pid = commandService.execCommand(CommandService.CommandOp.pid, projectInfoModel, null);
        String command = "top -b -n 1 -p " + pid;
        String internal = commandService.execCommand(command);
        internal = internal.replaceAll("\n", "<br/>");
        internal = internal.replaceAll(" ", "&nbsp;&nbsp;");
        JSONObject object = new JSONObject();
        object.put("ram", internal);
        object.put("tag", tag);
        setAttribute("internal", object);
        return "/manage/internal";
    }

    /**
     * 导出堆栈信息
     */
    @RequestMapping(value = "stack")
    @ResponseBody
    public String stack(String tag) {
        ProjectInfoModel projectInfoModel = new ProjectInfoModel();
        projectInfoModel.setTag(tag);
        String pid = commandService.execCommand(CommandService.CommandOp.pid, projectInfoModel, null).trim();
        pid = pid.replace("\n", "");
        String fileName = "java_cpu" + RandomUtil.randomNumbers(5) + ".txt";
        String command = String.format("%s %s %s %s", "/boot-line/command/java_cpu.sh", pid, 0, fileName);
        commandService.execCommand(command);
        downLoad(getResponse(), fileName, "java_cpu.txt");
        return JsonMessage.getString(200, "");
    }

    /**
     * 导出内存信息
     */
    @RequestMapping(value = "ram")
    @ResponseBody
    public String ram(String tag) {
        ProjectInfoModel projectInfoModel = new ProjectInfoModel();
        projectInfoModel.setTag(tag);
        String pid = commandService.execCommand(CommandService.CommandOp.pid, projectInfoModel, null).trim();
        String fileName = "java_ram" + RandomUtil.randomNumbers(5) + ".txt";
        String command = String.format("%s %s %s", "/boot-line/command/java_ram.sh", pid, fileName);
        commandService.execCommand(command);
        downLoad(getResponse(), fileName, "java_ram.txt");
        return JsonMessage.getString(200, "");
    }

    /**
     * 下载文件
     *
     * @param response response
     * @param fileName 文件名字
     * @param name     文件下载名字
     */
    private void downLoad(HttpServletResponse response, String fileName, String name) {
        //获取项目根路径
        String realPath = System.getProperty("user.dir");
        String path = realPath + "/" + fileName;
        File file = new File(path);
        try {
            String filename = file.getName();
            // 以流的形式下载文件。
            InputStream fis = new BufferedInputStream(new FileInputStream(path));
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            fis.close();
            // 清空response
            response.reset();
            // 设置response的Header
            //设置文件名
            response.addHeader("Content-Disposition", "attachment;filename=" + name);
            //设置文件打下
            response.addHeader("Content-Length", "" + file.length());
            OutputStream toClient = new BufferedOutputStream(response.getOutputStream());
            response.setContentType("application/octet-stream");
            toClient.write(buffer);
            toClient.flush();
            toClient.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            file.delete();
        }
    }

}
