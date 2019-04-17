package cn.keepbx.jpom.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.common.BaseDataService;
import cn.keepbx.jpom.model.data.Whitelist;
import cn.keepbx.jpom.system.AgentConfigBean;
import cn.keepbx.jpom.util.JsonFileUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

/**
 * 白名单服务
 *
 * @author jiangzeyin
 * @date 2019/2/28
 */
@Service
public class WhitelistDirectoryService extends BaseDataService {

    public Whitelist getWhitelist() {
        try {
            JSONObject jsonObject = getJSONObject(AgentConfigBean.WHITELIST_DIRECTORY);
            if (jsonObject == null) {
                return null;
            }
            return jsonObject.toJavaObject(Whitelist.class);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return null;
    }

    public boolean isInstalled() {
        Whitelist whitelist = getWhitelist();
        if (whitelist == null) {
            return false;
        }
        List<String> project = whitelist.getProject();
        return project != null && !project.isEmpty();
    }

    private List<String> getNgxDirectory() {
        Whitelist whitelist = getWhitelist();
        if (whitelist == null) {
            return null;
        }
        return whitelist.getNginx();
    }

    public boolean checkProjectDirectory(String path) {
        Whitelist whitelist = getWhitelist();
        if (whitelist == null) {
            return false;
        }
        List<String> list = whitelist.getProject();
        if (list == null) {
            return false;
        }
        return list.contains(path);
    }

    public boolean checkNgxDirectory(String path) {
        List<String> list = getNgxDirectory();
        if (list == null) {
            return false;
        }
        return checkPath(list, path);
    }

    private boolean checkPath(List<String> list, String path) {
        if (StrUtil.isEmpty(path)) {
            return false;
        }
        File file1, file2 = FileUtil.file(path);
        for (String item : list) {
            file1 = FileUtil.file(item);
            if (FileUtil.pathEquals(file1, file2)) {
                return true;
            }
        }
        return false;
    }

    private List<String> getCertificateDirectory() {
        Whitelist whitelist = getWhitelist();
        if (whitelist == null) {
            return null;
        }
        return whitelist.getCertificate();
    }

    public boolean checkCertificateDirectory(String path) {
        List<String> list = getCertificateDirectory();
        if (list == null) {
            return false;
        }
        return checkPath(list, path);
    }

    public void saveWhitelistDirectory(Whitelist jsonObject) {
        String path = getDataFilePath(AgentConfigBean.WHITELIST_DIRECTORY);
        JsonFileUtil.saveJson(path, jsonObject.toJson());
    }
}
