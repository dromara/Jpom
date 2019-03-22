package cn.keepbx.jpom.controller.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.model.CertModel;
import cn.keepbx.jpom.service.system.CertService;
import cn.keepbx.jpom.service.system.WhitelistDirectoryService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.File;
import java.util.List;

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
    private WhitelistDirectoryService whitelistDirectoryService;

    @RequestMapping(value = "/certificate", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String certificate() {
        JSONArray jsonArray = whitelistDirectoryService.getCertificateDirectory();
        setAttribute("certificate", jsonArray);
        return "system/certificate";
    }


    /**
     * 新增证书
     */
    @RequestMapping(value = "/certificate/addCertificate", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String addCertificate() {
        try {
            CertModel certModel = getCertModel();
            certService.addItem(certModel);
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
        String name = getParameter("name");
        if (StrUtil.isEmpty(id)) {
            throw new RuntimeException("证书id异常");
        }
        if (Validator.isChinese(id)) {
            throw new RuntimeException("证书id不能使用中文");
        }
        if (StrUtil.isEmpty(name)) {
            throw new RuntimeException("请填写证书名称");
        }
        if (!whitelistDirectoryService.checkCertificateDirectory(path)) {
            throw new RuntimeException("请选择正确的项目路径,或者还没有配置白名单");
        }
        String temporary = path + "/" + id + "/";
        File file = FileUtil.file(temporary);
        if (!file.exists()) {
            boolean mkdirs = file.mkdirs();
            if (!mkdirs) {
                throw new RuntimeException("创建" + path + "目录失败,请手动创建");
            }
        }
        MultipartFileBuilder cert = createMultipart().addFieldName("cert").setSavePath(temporary).setUseOriginalFilename(true);
        String certPath = cert.save();
        //解析证书
        JSONObject jsonObject = CertModel.decodeCert(certPath);
        if (jsonObject == null) {
            throw new RuntimeException("解析证书失败");
        }
        MultipartFileBuilder key = createMultipart().addFieldName("key").setSavePath(temporary).setUseOriginalFilename(true);
        String keyPath = key.save();
        CertModel certModel = new CertModel();
        certModel.setId(id);
        certModel.setWhitePath(path);
        certModel.setCert(certPath);
        certModel.setKey(keyPath);
        certModel.setName(name);
        certModel.setDomain(jsonObject.getString("domain"));
        certModel.setExpirationTime(jsonObject.getLongValue("expirationTime"));
        certModel.setEffectiveTime(jsonObject.getLongValue("effectiveTime"));
        return certModel;
    }


    /**
     * 证书列表
     */
    @RequestMapping(value = "/certificate/getCertList", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String getCertList() {
        List<CertModel> array = certService.list();
        return JsonMessage.getString(200, "", array);
    }

    /**
     * 删除证书
     *
     * @param id id
     * @return json
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
