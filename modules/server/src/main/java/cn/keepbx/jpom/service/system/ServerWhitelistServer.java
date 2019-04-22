package cn.keepbx.jpom.service.system;

import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.common.BaseDataService;
import cn.keepbx.jpom.model.data.ServerWhitelist;
import cn.keepbx.jpom.system.ServerConfigBean;
import cn.keepbx.jpom.util.JsonFileUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

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
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return null;
    }

    public void saveWhitelistDirectory(ServerWhitelist serverWhitelist) {
        String path = getDataFilePath(ServerConfigBean.OUTGIVING_WHITELIST);
        JsonFileUtil.saveJson(path, serverWhitelist.toJson());
    }

}
