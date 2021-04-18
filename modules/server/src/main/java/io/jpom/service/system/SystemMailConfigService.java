package io.jpom.service.system;

import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseDataService;
import io.jpom.model.data.MailAccountModel;
import io.jpom.system.ServerConfigBean;
import io.jpom.util.JsonFileUtil;
import org.springframework.stereotype.Service;

/**
 * 监控管理Service
 *
 * @author Arno
 */
@Service
public class SystemMailConfigService extends BaseDataService {

    /**
     * 获取配置
     *
     * @return config
     */
    public MailAccountModel getConfig() {
        JSONObject config = getJSONObject(ServerConfigBean.MAIL_CONFIG);
        if (config == null) {
            return null;
        }
        return config.toJavaObject(MailAccountModel.class);
    }

    /**
     * 保存配置
     *
     * @param mailAccountModel config
     */
    public void save(MailAccountModel mailAccountModel) {
        String path = getDataFilePath(ServerConfigBean.MAIL_CONFIG);
        JsonFileUtil.saveJson(path, mailAccountModel.toJson());
    }
}
