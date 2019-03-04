package cn.keepbx.jpom.service.system;

import cn.hutool.core.collection.CollUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.common.BaseDataService;
import cn.keepbx.jpom.system.ConfigBean;
import cn.keepbx.jpom.system.init.CheckRunCommand;
import cn.keepbx.jpom.util.JsonUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;

/**
 * @author jiangzeyin
 * @date 2019/2/28
 */
@Service
public class SystemService extends BaseDataService {

    public JSONObject getWhitelist() {
        try {
            JSONObject jsonObject = getJsonObject(ConfigBean.WHITELIST_DIRECTORY);
            if (jsonObject == null) {
                return null;
            }
            return jsonObject;
        } catch (FileNotFoundException fileNotFoundException) {
            CheckRunCommand.repairData();
            return getWhitelist();
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 获取项目路径白名单
     *
     * @return project
     */
    public JSONArray getWhitelistDirectory() {
        try {
            JSONObject jsonObject = getWhitelist();
            if (jsonObject == null) {
                return null;
            }
            return jsonObject.getJSONArray("project");
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return null;
    }

    public JSONArray getCertificateDirectory() {
        try {
            JSONObject jsonObject = getWhitelist();
            if (jsonObject == null) {
                return null;
            }
            return jsonObject.getJSONArray("certificate");
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 转换为字符串
     *
     * @param jsonArray jsonArray
     * @return str
     */
    public String convertToLine(JSONArray jsonArray) {
        try {
            return CollUtil.join(jsonArray, "\r\n");
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return "";
    }

    public void saveWhitelistDirectory(JSONObject jsonObject) {
        String path = getDataFilePath(ConfigBean.WHITELIST_DIRECTORY);
        JsonUtil.saveJson(path, jsonObject);
    }
}
