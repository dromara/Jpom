package cn.keepbx.jpom.service;

import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.common.BaseDataService;
import cn.keepbx.jpom.model.data.Whitelist;
import cn.keepbx.jpom.system.AgentConfigBean;
import cn.keepbx.jpom.util.JsonFileUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

/**
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
//
//    public boolean checkProjectDirectory(String path) {
//
//        List<String> list = getProjectDirectory();
//        if (list == null) {
//            return false;
//        }
//        return list.contains(path);
//    }
//
//    public boolean checkNgxDirectory(String path) {
//        List<String> list = getNgxDirectory();
//        if (list == null) {
//            return false;
//        }
//        return list.contains(path);
//    }
//
//    public boolean checkCertificateDirectory(String path) {
//        List<String> list = getCertificateDirectory();
//        if (list == null) {
//            return false;
//        }
//        return list.contains(path);
//    }

//    private JSONArray getItemArray(String key) {
//        try {
//            JSONObject jsonObject = getWhitelist();
//            if (jsonObject == null) {
//                return null;
//            }
//            return jsonObject.getJSONArray(key);
//        } catch (Exception e) {
//            DefaultSystemLog.ERROR().error(e.getMessage(), e);
//        }
//        return null;
//    }



    public void saveWhitelistDirectory(Whitelist jsonObject) {
        String path = getDataFilePath(AgentConfigBean.WHITELIST_DIRECTORY);
        JsonFileUtil.saveJson(path, jsonObject.toJson());
    }
}
