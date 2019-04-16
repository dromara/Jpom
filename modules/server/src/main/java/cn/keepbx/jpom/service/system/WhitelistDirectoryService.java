package cn.keepbx.jpom.service.system;

import cn.hutool.core.collection.CollUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.Whitelist;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author jiangzeyin
 * @date 2019/4/16
 */
@Service
public class WhitelistDirectoryService {

    public Whitelist getData(NodeModel model) {
        return NodeForward.requestData(model, NodeUrl.WhitelistDirectory_data, Whitelist.class);
    }

    /**
     * 获取项目路径白名单
     *
     * @return project
     */
    public List<String> getProjectDirectory(NodeModel model) {
        Whitelist whitelist = getData(model);
        if (whitelist == null) {
            return null;
        }
        return whitelist.getProject();
    }

    public List<String> getNgxDirectory(NodeModel model) {
        Whitelist whitelist = getData(model);
        if (whitelist == null) {
            return null;
        }
        return whitelist.getNginx();
    }

    public List<String> getCertificateDirectory(NodeModel model) {
        Whitelist whitelist = getData(model);
        if (whitelist == null) {
            return null;
        }
        return whitelist.getCertificate();
    }

    /**
     * 转换为字符串
     *
     * @param jsonArray jsonArray
     * @return str
     */
    public String convertToLine(List<String> jsonArray) {
        try {
            return CollUtil.join(jsonArray, "\r\n");
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return "";
    }
}
