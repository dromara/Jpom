package cn.keepbx.jpom.controller.manage;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorConfig;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import cn.keepbx.jpom.common.BaseAgentController;
import cn.keepbx.jpom.common.commander.AbstractProjectCommander;
import cn.keepbx.jpom.model.data.ProjectInfoModel;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * 项目文件管理
 *
 * @author jiangzeyin
 * @date 2019/4/17
 */
@RestController
@RequestMapping(value = "/manage/")
public class ProjectStatusController extends BaseAgentController {

    /**
     * 获取项目的进程id
     *
     * @param id 项目id
     * @return json
     */
    @RequestMapping(value = "getProjectStatus", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getProjectStatus(@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "项目id 不正确")) String id) {
        ProjectInfoModel projectInfoModel = tryGetProjectInfoModel();
        if (projectInfoModel == null) {
            return JsonMessage.getString(HttpStatus.NOT_FOUND.value(), "项目id不存在");
        }
        int pid = 0;
        try {
            pid = AbstractProjectCommander.getInstance().getPid(id);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("获取项目pid 失败", e);
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("pId", pid);
        return JsonMessage.getString(200, "", jsonObject);
    }

    /**
     * 获取项目的运行端口
     *
     * @param ids ids
     * @return obj
     */
    @RequestMapping(value = "getProjectPort", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getProjectPort(String ids) {
        if (StrUtil.isEmpty(ids)) {
            return JsonMessage.getString(400, "");
        }
        JSONArray jsonArray = JSONArray.parseArray(ids);
        JSONObject jsonObject = new JSONObject();
        JSONObject itemObj;
        for (Object object : jsonArray) {
            String item = object.toString();
            int pid;
            try {
                pid = AbstractProjectCommander.getInstance().getPid(item);
            } catch (Exception e) {
                DefaultSystemLog.ERROR().error("获取端口错误", e);
                continue;
            }
            if (pid <= 0) {
                continue;
            }
            itemObj = new JSONObject();
            String port = AbstractProjectCommander.getInstance().getMainPort(pid);
            itemObj.put("port", port);
            itemObj.put("pid", pid);
            jsonObject.put(item, itemObj);
        }
        return JsonMessage.getString(200, "", jsonObject);
    }

    @RequestMapping(value = "restart", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String restart(@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "项目id 不正确")) String id) {
        ProjectInfoModel item = projectInfoService.getItem(id);
        if (item == null) {
            return JsonMessage.getString(405, "没有找到对应的项目");
        }
        String result;
        try {
            result = AbstractProjectCommander.getInstance().restart(item);
            boolean status = AbstractProjectCommander.getInstance().isRun(item.getId());
            if (status) {
                return JsonMessage.getString(200, result);
            }
            return JsonMessage.getString(201, "重启失败：" + result);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("获取项目pid 失败", e);
            result = "error:" + e.getMessage();
            return JsonMessage.getString(500, "重启异常：" + result);
        }
    }
}
