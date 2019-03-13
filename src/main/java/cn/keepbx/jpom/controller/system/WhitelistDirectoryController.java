package cn.keepbx.jpom.controller.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrSpliter;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.model.UserModel;
import cn.keepbx.jpom.service.system.SystemService;
import cn.keepbx.jpom.system.ConfigBean;
import cn.keepbx.jpom.system.ExtConfigBean;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;

/**
 * 白名单目录
 *
 * @author jiangzeyin
 * @date 2019/2/28
 */
@Controller
@RequestMapping(value = "/system")
public class WhitelistDirectoryController extends BaseController {
    @Resource
    private SystemService systemService;

    /**
     * 页面
     */
    @RequestMapping(value = "whitelistDirectory", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String whitelistDirectory() {
        JSONArray jsonArray = systemService.getWhitelistDirectory();
        setAttribute("project", systemService.convertToLine(jsonArray));
        //
        jsonArray = systemService.getCertificateDirectory();
        setAttribute("certificate", systemService.convertToLine(jsonArray));
        jsonArray = systemService.getNgxDirectory();
        setAttribute("nginx", systemService.convertToLine(jsonArray));
        return "system/whitelistDirectory";
    }

    /**
     * 保存接口
     *
     * @param project     项目
     * @param certificate 证书
     * @return json
     */
    @RequestMapping(value = "whitelistDirectory_submit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String whitelistDirectorySubmit(String project, String certificate, String nginx) {
        if (ConfigBean.getInstance().safeMode) {
            return JsonMessage.getString(401, "安全模式下不能修改白名单目录");
        }
        UserModel userName = getUser();
        if (!UserModel.SYSTEM_ADMIN.equals(userName.getParent())) {
            return JsonMessage.getString(401, "你没有权限修改白名单目录");
        }

        //
        List<String> certificateList = null;
        if (StrUtil.isNotEmpty(certificate)) {
            certificateList = StrSpliter.splitTrim(certificate, "\n", true);
            if (certificateList == null || certificateList.size() <= 0) {
                return JsonMessage.getString(401, "证书路径白名单不能为空");
            }
        }
        List<String> nList = null;
        if (StrUtil.isNotEmpty(nginx)) {
            nList = StrSpliter.splitTrim(nginx, "\n", true);
            if (nList == null || nList.size() <= 0) {
                return JsonMessage.getString(401, "nginx路径白名单不能为空");
            }
        }
        JsonMessage jsonMessage = save(project, certificateList, nList);
        return jsonMessage.toString();
    }

    public JsonMessage save(String project, List<String> certificate, List<String> nginx) {
        if (StrUtil.isEmpty(project)) {
            return new JsonMessage(401, "项目路径白名单不能为空");
        }
        List<String> list = StrSpliter.splitTrim(project, "\n", true);
        if (list == null || list.size() <= 0) {
            return new JsonMessage(401, "项目路径白名单不能为空");
        }
        return save(list, certificate, nginx);
    }

    public JsonMessage save(List<String> projects, List<String> certificate, List<String> nginx) {
        JSONArray projectArray;
        {
            projectArray = covertToArray(projects);
            if (projectArray == null) {
                return new JsonMessage(401, "项目路径白名单不能位于Jpom目录下");
            }
            if (projectArray.isEmpty()) {
                return new JsonMessage(401, "项目路径白名单不能为空");
            }
            String error = findStartsWith(projectArray, 0);
            if (error != null) {
                return new JsonMessage(401, "白名单目录中不能存在包含关系：" + error);
            }
        }
        JSONArray certificateArray = null;
        if (certificate != null && !certificate.isEmpty()) {
            certificateArray = covertToArray(certificate);
            if (certificateArray == null) {
                return new JsonMessage(401, "证书路径白名单不能位于Jpom目录下");
            }
            if (certificateArray.isEmpty()) {
                return new JsonMessage(401, "证书路径白名单不能为空");
            }
            String error = findStartsWith(certificateArray, 0);
            if (error != null) {
                return new JsonMessage(401, "证书目录中不能存在包含关系：" + error);
            }
        }
        JSONArray nginxArray = null;
        if (nginx != null && !nginx.isEmpty()) {
            nginxArray = covertToArray(nginx);
            if (nginxArray == null) {
                return new JsonMessage(401, "nginx路径白名单不能位于Jpom目录下");
            }
            if (nginxArray.isEmpty()) {
                return new JsonMessage(401, "nginx路径白名单不能为空");
            }
            String error = findStartsWith(nginxArray, 0);
            if (error != null) {
                return new JsonMessage(401, "nginx目录中不能存在包含关系：" + error);
            }
        }
        JSONObject jsonObject = systemService.getWhitelist();
        if (jsonObject == null) {
            jsonObject = new JSONObject();
        }
        jsonObject.put("project", projectArray);
        jsonObject.put("certificate", certificateArray);
        jsonObject.put("nginx", nginxArray);
        systemService.saveWhitelistDirectory(jsonObject);
        return new JsonMessage(200, "保存成功");
    }

    private JSONArray covertToArray(List<String> list) {
        JSONArray array = new JSONArray();
        for (String s : list) {
            String val = String.format("/%s/", s);
            val = val.replace("../", "");
            val = FileUtil.normalize(val);
            if (StrUtil.SLASH.equals(val)) {
                continue;
            }
            if (array.contains(val)) {
                continue;
            }
            // 判断是否保护jpom 路径
            if (val.startsWith(ConfigBean.getInstance().getPath())) {
                return null;
            }
            array.add(val);
        }
        return array;
    }


    private String findStartsWith(JSONArray jsonArray, int start) {
        if (!ExtConfigBean.getInstance().whitelistDirectoryCheckStartsWith) {
            return null;
        }
        String str = jsonArray.getString(start);
        int len = jsonArray.size();
        for (int i = 0; i < len; i++) {
            if (i == start) {
                continue;
            }
            String findStr = jsonArray.getString(i);
            if (findStr.startsWith(str)) {
                return str;
            }
        }
        if (start < len - 1) {
            return findStartsWith(jsonArray, start + 1);
        }
        return null;
    }
}
