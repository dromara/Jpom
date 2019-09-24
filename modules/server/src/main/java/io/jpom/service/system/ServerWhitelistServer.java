package io.jpom.service.system;

import cn.jiangzeyin.common.DefaultSystemLog;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseDataService;
import io.jpom.model.data.ServerWhitelist;
import io.jpom.system.ServerConfigBean;
import io.jpom.util.JsonFileUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author jiangzeyin
 * @date 2019/4/22
 */
@Service
public class ServerWhitelistServer extends BaseDataService {

    public ServerWhitelist getWhitelist() {
        try {
            JSONObject jsonObject = getJSONObject(ServerConfigBean.OUTGIVING_WHITELIST);
            if (jsonObject == null) {
                return null;
            }
            return jsonObject.toJavaObject(ServerWhitelist.class);
        } catch (Exception e) {
            DefaultSystemLog.getLog().error(e.getMessage(), e);
        }
        return null;
    }

    public void saveWhitelistDirectory(ServerWhitelist serverWhitelist) {
        String path = getDataFilePath(ServerConfigBean.OUTGIVING_WHITELIST);
        JsonFileUtil.saveJson(path, serverWhitelist.toJson());
    }


    public List<String> getOutGiving() {
        ServerWhitelist serverWhitelist = getWhitelist();
        if (serverWhitelist == null) {
            return null;
        }
        return serverWhitelist.getOutGiving();
    }
}
