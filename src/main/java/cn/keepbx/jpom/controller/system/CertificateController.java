package cn.keepbx.jpom.controller.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.model.CertModel;
import cn.keepbx.jpom.service.system.CertService;
import cn.keepbx.jpom.service.system.SystemService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * 证书管理
 *
 * @author Arno
 */
@Controller
@RequestMapping(value = "/system")
public class CertificateController extends BaseController {

    @Resource
    private CertService certService;
    @Resource
    private SystemService systemService;

    @RequestMapping(value = "/certificate", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String certificate() {
        JSONArray jsonArray = systemService.getCertificateDirectory();
        setAttribute("certificate", jsonArray);
        return "/system/certificate";
    }


    /**
     * 新增证书
     */
    @RequestMapping(value = "/certificate/addCertificate", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String addCertificate() {
        try {
            CertModel certModel = getCertModel();
            boolean b = certService.addCert(certModel);
            if (!b) {
                return JsonMessage.getString(400, "上传失败");
            }
        } catch (Exception e) {
            return JsonMessage.getString(400, e.getMessage());
        }
        return JsonMessage.getString(200, "上传成功");
    }

    /**
     * 修改证书
     */
    @RequestMapping(value = "/certificate/updateCertificate", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String updateCertificate() {
        try {
            CertModel certModel = getCertModel();
            boolean b = certService.updateCert(certModel);
            if (!b) {
                return JsonMessage.getString(400, "修改失败");
            }
        } catch (Exception e) {
            return JsonMessage.getString(400, e.getMessage());
        }
        return JsonMessage.getString(200, "修改成功");
    }

    /**
     * 获取证书信息
     */
    private CertModel getCertModel() throws Exception {
        String id = getParameter("id");
        String path = getParameter("path");
        if (StrUtil.isEmpty(id)) {
            throw new RuntimeException("修改证书失败");
        }
        if (StrUtil.isEmpty(path)) {
            throw new RuntimeException("路径不能为空");
        }
        String temporary = path + "/" + id + "/";
        File file = FileUtil.file(temporary);
        if (!file.exists()) {
            boolean mkdirs = file.mkdirs();
            if (!mkdirs) {
                throw new RuntimeException("创建文件夹失败");
            }
        }
        MultipartFileBuilder cert = createMultipart().addFieldName("cert").setSavePath(temporary).setUseOriginalFilename(true);
        String certPath = cert.save();
        //解析证书
        JSONObject jsonObject = decodeCert(FileUtil.file(certPath));
        if (jsonObject == null) {
            throw new RuntimeException("解析证书失败");
        }
        MultipartFileBuilder key = createMultipart().addFieldName("key").setSavePath(temporary).setUseOriginalFilename(true);
        String keyPath = key.save();
        CertModel certModel = new CertModel();
        certModel.setId(id);
        certModel.setDomain(jsonObject.getString("domain"));
        certModel.setExpirationTime(jsonObject.getLongValue("expirationTime"));
        certModel.setCert(certPath);
        certModel.setKey(keyPath);
        return certModel;
    }

    /**
     * 解析证书失败
     *
     * @param file 证书文件
     */
    private JSONObject decodeCert(File file) {
        try {
            InputStream inStream = new FileInputStream(file);
            // 创建X509工厂类
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            // 创建证书对象
            X509Certificate oCert = (X509Certificate) cf.generateCertificate(inStream);
            inStream.close();
            //到期时间
            Date expirationTime = oCert.getNotAfter();
            //域名
            String name = oCert.getSubjectDN().getName();
            int i = name.indexOf("=");
            String domain = name.substring(i + 1);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("expirationTime", expirationTime.getTime());
            jsonObject.put("domain", domain);
            return jsonObject;
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return null;
    }


    /**
     * 证书列表
     */
    @RequestMapping(value = "/certificate/getCertList", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String getCertList() {
        JSONArray array = certService.getCertList();
        return JsonMessage.getString(200, "", array);
    }

    /**
     * 删除证书
     */
    @RequestMapping(value = "/certificate/delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String delete(String id) {
        if (StrUtil.isEmpty(id)) {
            return JsonMessage.getString(400, "删除失败");
        }
        boolean b = certService.delete(id);
        if (!b) {
            return JsonMessage.getString(400, "删除失败");
        }
        return JsonMessage.getString(200, "删除成功");
    }


}
