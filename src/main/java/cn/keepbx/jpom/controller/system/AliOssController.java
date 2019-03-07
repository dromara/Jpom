package cn.keepbx.jpom.controller.system;

import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.model.UserModel;
import cn.keepbx.jpom.service.oss.OssManagerService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.FileNotFoundException;
import java.io.IOException;

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
    public String whitelistDirectory() throws IOException {
        try {
            setAttribute("item", ossManagerService.getConfig());
        } catch (FileNotFoundException ignored) {
        }
        setAttribute("manager", UserModel.SYSTEM_ADMIN.equals(getUser().getParent()));
        return "system/alioss";
    }

    @RequestMapping(value = "alioss_submit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String aliOssSubmit(String endpoint, String accessKeyId, String accessKeySecret, String bucketName, String keyPrefix) {
        if (!UserModel.SYSTEM_ADMIN.equals(getUser().getParent())) {
            return JsonMessage.getString(401, "你没有权限管理alioss");
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("endpoint", endpoint);
        jsonObject.put("accessKeyId", accessKeyId);
        jsonObject.put("accessKeySecret", accessKeySecret);
        jsonObject.put("bucketName", bucketName);
        jsonObject.put("keyPrefix", keyPrefix);
        ossManagerService.save(jsonObject);
        return JsonMessage.getString(200, "保存成功");
    }
}
