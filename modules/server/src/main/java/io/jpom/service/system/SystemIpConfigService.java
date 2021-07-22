package io.jpom.service.system;

import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseDataService;
import io.jpom.model.data.SystemIpConfigModel;
import io.jpom.system.ServerConfigBean;
import io.jpom.util.JsonFileUtil;
import org.springframework.stereotype.Service;

/**
 * 系统ip 白名单
 *
 * @author bwcx_jzy
 */
@Service
public class SystemIpConfigService extends BaseDataService {

    /**
     * 获取配置
     *
     * @return config
     */
    public SystemIpConfigModel getConfig() {
        JSONObject config = getJSONObject(ServerConfigBean.IP_CONFIG);
        if (config == null) {
            return null;
        }
        return config.toJavaObject(SystemIpConfigModel.class);
    }

    /**
     * 保存配置
     *
     * @param configModel config
     */
    public void save(SystemIpConfigModel configModel) {
        String path = getDataFilePath(ServerConfigBean.IP_CONFIG);
        JsonFileUtil.saveJson(path, configModel.toJson());
    }

    public String filePath() {
        return getDataFilePath(ServerConfigBean.IP_CONFIG);
    }
}
