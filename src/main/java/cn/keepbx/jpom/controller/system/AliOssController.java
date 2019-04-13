package cn.keepbx.jpom.controller.system;

import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.common.Role;
import cn.keepbx.jpom.common.interceptor.UrlPermission;
import cn.keepbx.jpom.service.oss.OssManagerService;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.oss.OSSClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * 阿里云oss 配置
 *
 * @author jiangzeyin
 * @date 2019/3/5
 */
@Controller
@RequestMapping(value = "/system")
public class AliOssController extends BaseController {
    @Resource
    private OssManagerService ossManagerService;

    /**
     * 页面
     */
    @RequestMapping(value = "alioss", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String whitelistDirectory() {
        setAttribute("item", ossManagerService.getConfig());
        return "system/alioss";
    }

    /**
     * 保存
     *
     * @param endpoint        接入点
     * @param accessKeyId     id
     * @param accessKeySecret secret
     * @param bucketName      空间名称
     * @param keyPrefix       前缀
     * @return json
     */
    @RequestMapping(value = "alioss_submit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @UrlPermission(Role.System)
    public String aliOssSubmit(String endpoint, String accessKeyId, String accessKeySecret, String bucketName, String keyPrefix) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("endpoint", endpoint);
        jsonObject.put("accessKeyId", accessKeyId);
        jsonObject.put("accessKeySecret", accessKeySecret);
        jsonObject.put("bucketName", bucketName);
        jsonObject.put("keyPrefix", keyPrefix);
        try {
            OSSClient ossClient = ossManagerService.getOSSClient(jsonObject);
            ossClient.listObjects(bucketName);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("oss 失败", e);
            return JsonMessage.getString(400, "oss连接失败,请检查配置信息:" + e.getMessage());
        }
        ossManagerService.save(jsonObject);
        return JsonMessage.getString(200, "保存成功");
    }
}
