package cn.keepbx.jpom.controller.manage;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrSpliter;
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
import java.util.List;

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
        String[] split = internal.split("\n");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            String s = split[i].replaceAll(" ", "&nbsp;&nbsp;");
            result.append(s).append("<br/>");
        }
        JSONObject jsonObject = formatTop(internal);
        JSONObject object = new JSONObject();
        object.put("ram", result.toString());
        object.put("tag", tag);
        setAttribute("item", jsonObject);
        setAttribute("internal", object);
        return "manage/internal";
    }


    private JSONObject formatTop(String top) {
        List<String> list = StrSpliter.splitTrim(top, "\n", true);
        if (list.size() < 5) {
            return null;
        }
        String topName = list.get(list.size() - 2);
        List<String> nameList = StrSpliter.splitTrim(topName, " ", true);
        String ram = list.get(list.size() - 1);
        List<String> ramList = StrSpliter.splitTrim(ram, " ", true);
        JSONObject item = new JSONObject();
        for (int i = 0; i < nameList.size(); i++) {
            String name = nameList.get(i);
            String value = ramList.get(i);
            if (i == 0) {
                item.put("pid", value);
                continue;
            }
            name = name.replaceAll("%", "").replace("+", "");
            if ("VIRT".equalsIgnoreCase(name) || "RES".equalsIgnoreCase(name) || "SHR".equalsIgnoreCase(name)) {
                value = Convert.toLong(value) / 1024 + "mb";
            }
            if ("�".equals(name)) {
                name = "S";
            }
            if ("S".equals(name)) {
                if ("S".equals(value)) {
                    value = "睡眠";
                } else if ("R".equals(value)) {
                    value = "运行";
                } else if ("T".equals(value)) {
                    value = "跟踪/停止";
                } else if ("Z".equals(value)) {
                    value = "僵尸进程 ";
                } else if ("D".equals(value)) {
                    value = "不可中断的睡眠状态 ";
                }
            }
            if ("CPU".equalsIgnoreCase(name) || "MEM".equalsIgnoreCase(name)) {
                value += "%";
            }
            item.put(name, value);
        }
        return item;
    }

    /**
     * 导出堆栈信息
     */
    @RequestMapping(value = "stack", method = RequestMethod.GET)
    @ResponseBody
    public String stack(String tag) throws Exception {
        if (AbstractCommander.OS_INFO.isLinux()) {
            ProjectInfoModel projectInfoModel = projectInfoService.getItem(tag);
            String pid = commandService.execCommand(CommandService.CommandOp.pid, projectInfoModel, null).trim();
            pid = pid.replace("\n", "");
            String fileName = ConfigBean.getInstance().getTempPathName() + "/" + tag + "_java_cpu.txt";
            fileName = FileUtil.normalize(fileName);
            String commandPath = ConfigBean.getInstance().getCpuCommandPath();
            String command = String.format("%s %s %s %s", commandPath, pid, 300, fileName);
            AbstractCommander.getInstance().execCommand(command);
            downLoad(getResponse(), fileName);
        }
        return JsonMessage.getString(200, "");
    }

    /**
     * 导出内存信息
     */
    @RequestMapping(value = "ram", method = RequestMethod.GET)
    @ResponseBody
    public String ram(String tag) throws Exception {
        if (AbstractCommander.OS_INFO.isLinux()) {
            ProjectInfoModel projectInfoModel = projectInfoService.getItem(tag);
            String pid = commandService.execCommand(CommandService.CommandOp.pid, projectInfoModel, null).trim();
            String fileName = ConfigBean.getInstance().getTempPathName() + "/" + tag + "_java_ram.txt";
            fileName = FileUtil.normalize(fileName);
            String commandPath = ConfigBean.getInstance().getRamCommandPath();
            String command = String.format("%s %s %s", commandPath, pid, fileName);
            AbstractCommander.getInstance().execCommand(command);
            downLoad(getResponse(), fileName);
        }
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
