package io.jpom.controller.manage;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorConfig;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseAgentController;
import io.jpom.common.commander.AbstractProjectCommander;
import io.jpom.model.data.ProjectInfoModel;
import io.jpom.service.manage.ConsoleService;
import io.jpom.socket.ConsoleCommandOp;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 项目文件管理
 *
 * @author jiangzeyin
 * @date 2019/4/17
 */
@RestController
@RequestMapping(value = "/manage/")
public class ProjectStatusController extends BaseAgentController {

    private final ConsoleService consoleService;

    public ProjectStatusController(ConsoleService consoleService) {
        this.consoleService = consoleService;
    }


    /**
     * 获取项目的进程id
     *
     * @param id 项目id
     * @return json
     */
    @RequestMapping(value = "getProjectStatus", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getProjectStatus(@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "项目id 不正确")) String id, String getCopy) {
        ProjectInfoModel projectInfoModel = tryGetProjectInfoModel();
        if (projectInfoModel == null) {
            return JsonMessage.getString(HttpStatus.NOT_FOUND.value(), "项目id不存在");
        }
        int pid = 0;
        try {
            pid = AbstractProjectCommander.getInstance().getPid(id);
        } catch (Exception e) {
            DefaultSystemLog.getLog().error("获取项目pid 失败", e);
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("pId", pid);
        //
        if (StrUtil.isNotEmpty(getCopy)) {
            List<ProjectInfoModel.JavaCopyItem> javaCopyItemList = projectInfoModel.getJavaCopyItemList();
            JSONArray copys = new JSONArray();
            if (javaCopyItemList != null) {
                for (ProjectInfoModel.JavaCopyItem javaCopyItem : javaCopyItemList) {
                    JSONObject jsonObject1 = new JSONObject();
                    jsonObject1.put("copyId", javaCopyItem.getId());
                    jsonObject1.put("status", javaCopyItem.tryGetStatus());
                    copys.add(jsonObject1);
                }
            }
            jsonObject.put("copys", copys);
        }
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
                DefaultSystemLog.getLog().error("获取端口错误", e);
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


    /**
     * 获取项目的运行端口
     *
     * @param id      项目id
     * @param copyIds 副本 ids ["aa","ss"]
     * @return obj
     */
    @RequestMapping(value = "getProjectCopyPort", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getProjectPort(String id, String copyIds) {
        if (StrUtil.isEmpty(copyIds) || StrUtil.isEmpty(id)) {
            return JsonMessage.getString(400, "");
        }
        ProjectInfoModel projectInfoModel = getProjectInfoModel();

        JSONArray jsonArray = JSONArray.parseArray(copyIds);
        JSONObject jsonObject = new JSONObject();
        JSONObject itemObj;
        for (Object object : jsonArray) {
            String item = object.toString();
            ProjectInfoModel.JavaCopyItem copyItem = projectInfoModel.findCopyItem(item);
            int pid;
            try {
                pid = AbstractProjectCommander.getInstance().getPid(copyItem.getTagId());
                if (pid <= 0) {
                    continue;
                }
            } catch (Exception e) {
                DefaultSystemLog.getLog().error("获取端口错误", e);
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
    public String restart(@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "项目id 不正确")) String id, String copyId) {
        ProjectInfoModel item = projectInfoService.getItem(id);
        if (item == null) {
            return JsonMessage.getString(405, "没有找到对应的项目");
        }
        ProjectInfoModel.JavaCopyItem copyItem = item.findCopyItem(copyId);
        String tagId = copyItem == null ? item.getId() : copyItem.getTagId();
        String result;
        try {
            result = consoleService.execCommand(ConsoleCommandOp.restart, item, copyItem);
            boolean status = AbstractProjectCommander.getInstance().isRun(tagId);
            if (status) {
                return JsonMessage.getString(200, result);
            }
            return JsonMessage.getString(201, "重启失败：" + result);
        } catch (Exception e) {
            DefaultSystemLog.getLog().error("获取项目pid 失败", e);
            result = "error:" + e.getMessage();
            return JsonMessage.getString(500, "重启异常：" + result);
        }
    }
}
