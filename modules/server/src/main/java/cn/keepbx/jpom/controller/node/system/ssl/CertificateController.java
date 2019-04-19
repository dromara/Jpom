package cn.keepbx.jpom.controller.node.system.ssl;

import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.Role;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.common.interceptor.UrlPermission;
import cn.keepbx.jpom.model.data.UserOperateLogV1;
import cn.keepbx.jpom.service.system.WhitelistDirectoryService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;

/**
 * 证书管理
 *
 * @author Arno
 */
@Controller
@RequestMapping(value = "/node/system/certificate")
public class CertificateController extends BaseServerController {

    @Resource
    private WhitelistDirectoryService whitelistDirectoryService;

    @RequestMapping(value = "/list.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String certificate() {
        List<String> jsonArray = whitelistDirectoryService.getCertificateDirectory(getNode());
        setAttribute("certificate", jsonArray);
        return "node/system/certificate";
    }


    /**
     * 保存证书
     *
     * @return json
     */
    @RequestMapping(value = "/saveCertificate", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @UrlPermission(value = Role.NodeManage, optType = UserOperateLogV1.OptType.SaveCert)
    public String saveCertificate() {
        return NodeForward.requestMultipart(getNode(), getMultiRequest(), NodeUrl.System_Certificate_saveCertificate).toString();
    }


    /**
     * 证书列表
     */
    @RequestMapping(value = "/getCertList", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
//    @UrlPermission(Role.NodeManage)
    public String getCertList() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.System_Certificate_getCertList).toString();
    }

    /**
     * 删除证书
     *
     * @param id id
     * @return json
     */
    @RequestMapping(value = "/delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @UrlPermission(value = Role.System, optType = UserOperateLogV1.OptType.DelCert)
    public String delete(String id) {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.System_Certificate_delete).toString();
    }


    /**
     * 导出证书
     */
    @RequestMapping(value = "/export", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @UrlPermission(value = Role.NodeManage, optType = UserOperateLogV1.OptType.ExportCert)
    public void export(String id) {
        NodeForward.requestDownload(getNode(), getRequest(), getResponse(), NodeUrl.System_Certificate_export);
    }
}
