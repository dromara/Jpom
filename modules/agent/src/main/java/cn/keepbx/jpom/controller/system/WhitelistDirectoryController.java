package cn.keepbx.jpom.controller.system;

import cn.hutool.core.text.StrSpliter;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseJpomController;
import cn.keepbx.jpom.model.data.Whitelist;
import cn.keepbx.jpom.service.WhitelistDirectoryService;
import cn.keepbx.jpom.system.AgentExtConfigBean;
import cn.keepbx.jpom.system.ExtConfigBean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jiangzeyin
 * @date 2019/4/16
 */
@RestController
@RequestMapping(value = "/system")
public class WhitelistDirectoryController extends BaseJpomController {

    @Resource
    private WhitelistDirectoryService whitelistDirectoryService;

    @RequestMapping(value = "whitelistDirectory_data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String whitelistdirectoryData() {

        Whitelist whitelist = whitelistDirectoryService.getWhitelist();
        return JsonMessage.getString(200, "", whitelist);
    }

    @RequestMapping(value = "whitelistDirectory_submit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String whitelistDirectorySubmit(String project, String certificate, String nginx) {
        //
        List<String> certificateList = null;
        if (StrUtil.isNotEmpty(certificate)) {
            certificateList = StrSpliter.splitTrim(certificate, StrUtil.LF, true);
            if (certificateList == null || certificateList.size() <= 0) {
                return JsonMessage.getString(401, "证书路径白名单不能为空");
            }
        }
        List<String> nList = null;
        if (StrUtil.isNotEmpty(nginx)) {
            nList = StrSpliter.splitTrim(nginx, StrUtil.LF, true);
            if (nList == null || nList.size() <= 0) {
                return JsonMessage.getString(401, "nginx路径白名单不能为空");
            }
        }
        return save(project, certificateList, nList).toString();
    }

    private JsonMessage save(String project, List<String> certificate, List<String> nginx) {
        if (StrUtil.isEmpty(project)) {
            return new JsonMessage(401, "项目路径白名单不能为空");
        }
        List<String> list = StrSpliter.splitTrim(project, StrUtil.LF, true);
        if (list == null || list.size() <= 0) {
            return new JsonMessage(401, "项目路径白名单不能为空");
        }
        return save(list, certificate, nginx);
    }


    private JsonMessage save(List<String> projects, List<String> certificate, List<String> nginx) {
        List<String> projectArray;
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
        List<String> certificateArray = null;
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
        List<String> nginxArray = null;
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
        Whitelist whitelist = whitelistDirectoryService.getWhitelist();
        if (whitelist == null) {
            whitelist = new Whitelist();
        }
        whitelist.setProject(projectArray);
        whitelist.setCertificate(certificateArray);
        whitelist.setNginx(nginxArray);
        whitelistDirectoryService.saveWhitelistDirectory(whitelist);
        return new JsonMessage(200, "保存成功");
    }

    private List<String> covertToArray(List<String> list) {
        List<String> array = new ArrayList<>();
        for (String s : list) {
            String val = String.format("/%s/", s);
            val = pathSafe(val);
            if (StrUtil.SLASH.equals(val)) {
                continue;
            }
            if (array.contains(val)) {
                continue;
            }
            // 判断是否保护jpom 路径
            if (val.startsWith(ExtConfigBean.getInstance().getPath())) {
                return null;
            }
            array.add(val);
        }
        return array;
    }

    /**
     * 检查白名单包含关系
     *
     * @param jsonArray 要检查的对象
     * @param start     检查的坐标
     * @return null 正常
     */
    private String findStartsWith(List<String> jsonArray, int start) {
        if (!AgentExtConfigBean.getInstance().whitelistDirectoryCheckStartsWith) {
            return null;
        }
        String str = jsonArray.get(start);
        int len = jsonArray.size();
        for (int i = 0; i < len; i++) {
            if (i == start) {
                continue;
            }
            String findStr = jsonArray.get(i);
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
