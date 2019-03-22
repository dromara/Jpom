package cn.keepbx.jpom.service.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.common.BaseOperService;
import cn.keepbx.jpom.model.CertModel;
import cn.keepbx.jpom.system.ConfigBean;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Arno
 */
@Service
public class CertService extends BaseOperService<CertModel> {


    /**
     * 新增证书
     *
     * @param certModel 证书
     */
    @Override
    public void addItem(CertModel certModel) {
        saveJson(ConfigBean.CERT, certModel.toJson());
    }

    /**
     * 获取证书列表
     *
     * @return 证书列表
     */
    @Override
    public List<CertModel> list() {
        JSONObject jsonObject = getJSONObject(ConfigBean.CERT);
        if (jsonObject == null) {
            return null;
        }
        JSONArray jsonArray = formatToArray(jsonObject);
        return jsonArray.toJavaList(CertModel.class);
    }

    @Override
    public CertModel getItem(String id) throws IOException {
        return getJsonObjectById(ConfigBean.CERT, id, CertModel.class);
    }

    /**
     * 删除证书
     *
     * @param id id
     */
    public boolean delete(String id) {
        try {
            JSONObject jsonObject = getJSONObject(ConfigBean.CERT);
            if (jsonObject == null) {
                return false;
            }
            JSONObject cert = jsonObject.getJSONObject(id);
            if (cert == null) {
                return true;
            }
            String keyPath = cert.getString("key");
            deleteJson(ConfigBean.CERT, id);
            if (StrUtil.isNotEmpty(keyPath)) {
                File parentFile = FileUtil.file(keyPath).getParentFile();
                return FileUtil.del(parentFile);
            }
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    /**
     * 修改证书
     *
     * @param certModel 证书
     */
    public boolean updateCert(CertModel certModel) {
        try {
            updateJson(ConfigBean.CERT, certModel.toJson());
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return false;
        }
        return true;
    }
}
