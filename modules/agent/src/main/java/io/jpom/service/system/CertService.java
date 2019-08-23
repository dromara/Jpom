package io.jpom.service.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import io.jpom.common.BaseOperService;
import io.jpom.model.data.CertModel;
import io.jpom.system.AgentConfigBean;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * @author Arno
 */
@Service
public class CertService extends BaseOperService<CertModel> {

    public CertService() {
        super(AgentConfigBean.CERT);
    }


    /**
     * 删除证书
     *
     * @param id id
     */
    @Override
    public void deleteItem(String id) {
        CertModel certModel = getItem(id);
        if (certModel == null) {
            return;
        }
        String keyPath = certModel.getCert();
        super.deleteItem(id);
        if (StrUtil.isNotEmpty(keyPath)) {
            // 删除证书文件
            File parentFile = FileUtil.file(keyPath).getParentFile();
            FileUtil.del(parentFile);
        }
    }
}
