package cn.keepbx.jpom.service.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.common.BaseOperService;
import cn.keepbx.jpom.model.CertModel;
import cn.keepbx.jpom.system.ConfigBean;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public boolean addCert(CertModel certModel) {
        try {
            saveJson(ConfigBean.CERT, certModel.toJson());
            return true;
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * 获取证书列表
     *
     * @return 证书列表
     */
    @Override
    public List<CertModel> list() throws IOException {
        try {
            JSONObject jsonObject = getJsonObject(ConfigBean.CERT);
            if (jsonObject == null) {
                return null;
            }
            Set<Map.Entry<String, Object>> entries = jsonObject.entrySet();
            List<CertModel> list = new ArrayList<>();
            for (Map.Entry<String, Object> entry : entries) {
                JSONObject jsonObject1 = (JSONObject) entry.getValue();
                list.add(jsonObject1.toJavaObject(CertModel.class));
            }
            return list;
        } catch (FileNotFoundException e) {
            File file = FileUtil.touch(ConfigBean.getInstance().getDataPath(), ConfigBean.CERT);
            saveJson(file.getName(), new JSONObject());
            return null;
        } catch (IOException e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public CertModel getItem(String id) throws IOException {
        JSONObject jsonObject = getJsonObject(ConfigBean.CERT);
        if (jsonObject == null) {
            return null;
        }
        jsonObject = jsonObject.getJSONObject(id);
        if (jsonObject == null) {
            return null;
        }
        return jsonObject.toJavaObject(CertModel.class);
    }

    /**
     * 删除证书
     *
     * @param id id
     */
    public boolean delete(String id) {
        try {
            JSONObject jsonObject = getJsonObject(ConfigBean.CERT);
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
