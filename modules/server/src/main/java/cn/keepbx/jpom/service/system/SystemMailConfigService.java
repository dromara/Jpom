package cn.keepbx.jpom.service.system;

import cn.keepbx.jpom.common.BaseDataService;
import cn.keepbx.jpom.model.data.MailAccountModel;
import cn.keepbx.jpom.system.ServerConfigBean;
import cn.keepbx.util.JsonFileUtil;
import com.alibaba.fastjson.JSONObject;
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
     * 报错配置
     *
     * @param mailAccountModel config
     */
    public void save(MailAccountModel mailAccountModel) {
        String path = getDataFilePath(ServerConfigBean.MAIL_CONFIG);
        JsonFileUtil.saveJson(path, mailAccountModel.toJson());
    }
}
