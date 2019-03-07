package cn.keepbx.jpom.service.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.common.BaseDataService;
import cn.keepbx.jpom.model.CertModel;
import cn.keepbx.jpom.system.ConfigBean;
import cn.keepbx.jpom.system.init.CheckRunCommand;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * @author Arno
 */
@Service
public class CertService extends BaseDataService {

    private static final String FILENAME = ConfigBean.CERT;

    /**
     * 新增证书
     *
     * @param certModel 证书
     */
    public boolean addCert(CertModel certModel) {
        try {
            saveJson(FILENAME, certModel.toJson());
            return true;
        } catch (FileNotFoundException fileNotFoundException) {
            File file = new File(ConfigBean.getInstance().getDataPath(), ConfigBean.CERT);
            FileUtil.writeString("", file, CharsetUtil.UTF_8);
            return addCert(certModel);
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
    public JSONArray getCertList() {
        try {
            JSONObject jsonObject = getJsonObject(FILENAME);
            if (jsonObject == null) {
                return null;
            }
            Set<Map.Entry<String, Object>> entries = jsonObject.entrySet();
            JSONArray array = new JSONArray();
            for (Map.Entry<String, Object> entry : entries) {
                Object value = entry.getValue();
                array.add(value);
            }
            return array;
        } catch (IOException e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 删除证书
     *
     * @param id id
     */
    public boolean delete(String id) {
        try {
            deleteJson(FILENAME, id);
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
            updateJson(FILENAME, certModel.toJson());
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return false;
        }
        return true;
    }
}
