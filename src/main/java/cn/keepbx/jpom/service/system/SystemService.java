package cn.keepbx.jpom.service.system;

import cn.hutool.core.collection.CollUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.common.BaseDataService;
import cn.keepbx.jpom.system.ConfigBean;
import cn.keepbx.jpom.system.init.CheckRunCommand;
import cn.keepbx.jpom.util.JsonUtil;
import com.alibaba.fastjson.JSONArray;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;

/**
 * @author jiangzeyin
 * @date 2019/2/28
 */
@Service
public class SystemService extends BaseDataService {

    public JSONArray getWhitelistDirectory() {
        try {
            return getJSONArray(ConfigBean.WHITELIST_DIRECTORY);
        } catch (FileNotFoundException fileNotFoundException) {
            CheckRunCommand.repairData();
            return getWhitelistDirectory();
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return null;
    }

    public String getWhitelistDirectoryLine() {
        try {
            JSONArray jsonArray = getWhitelistDirectory();
            return CollUtil.join(jsonArray, "\r\n");
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return "";
    }

    public void saveWhitelistDirectory(JSONArray jsonArray) {
        String path = getDataFilePath(ConfigBean.WHITELIST_DIRECTORY);
        JsonUtil.saveJson(path, jsonArray);
    }
}
