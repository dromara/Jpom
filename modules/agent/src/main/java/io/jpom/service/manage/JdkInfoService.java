package io.jpom.service.manage;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.JavaInfo;
import cn.hutool.system.JavaRuntimeInfo;
import io.jpom.common.BaseOperService;
import io.jpom.model.data.JdkInfoModel;
import io.jpom.system.AgentConfigBean;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JdkInfoService extends BaseOperService<JdkInfoModel> {

    public JdkInfoService() {
        super(AgentConfigBean.JDK_CONF);
    }

    /**
     * 获取jdk列表
     *
     * @return JSONArray
     */
    public List<JdkInfoModel> list() {
        List<JdkInfoModel> list = super.list();
        JdkInfoModel defaultJdk = getDefaultJdk();
        if (list.size() <= 0) {
            if (null != defaultJdk) {
                addItem(defaultJdk);
                list.add(defaultJdk);
            }
        }
        return list;
    }

    /**
     * 使用中的jdk
     *
     * @return JdkInfoModel
     */
    private JdkInfoModel getDefaultJdk() {
        JavaRuntimeInfo info = new JavaRuntimeInfo();
        String homeDir = info.getHomeDir();
        String version = new JavaInfo().getVersion();
        if (StrUtil.isEmpty(homeDir) || StrUtil.isEmpty(version)) {
            return null;
        }
        String path = FileUtil.normalize(homeDir.replace("jre", ""));
        JdkInfoModel jdkInfoModel = new JdkInfoModel();
        jdkInfoModel.setId(version);
        jdkInfoModel.setVersion(version);
        jdkInfoModel.setPath(path);
        return jdkInfoModel;
    }

}
