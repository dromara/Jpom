package cn.keepbx.jpom.controller.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.model.CertModel;
import cn.keepbx.jpom.service.system.CertService;
import cn.keepbx.jpom.service.system.NgxService;
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
import java.util.List;

/**
 * nginx 管理
 *
 * @author Arno
 */
@Controller
@RequestMapping("/system")
public class NgxController extends BaseController {

    @Resource
    private SystemService systemService;
    @Resource
    private CertService certService;

    @Resource
    private NgxService ngxService;

    @RequestMapping(value = "nginx", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String ngx() {
        JSONArray ngxDirectory = systemService.getNgxDirectory();
        setAttribute("nginx", ngxDirectory);
        List<CertModel> certList = certService.list();
        setAttribute("cert", certList);
        return "system/nginx";
    }

    /**
     * 配置列表
     */
    @RequestMapping(value = "nginx/list", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String list() {
        JSONArray array = ngxService.list();
        return JsonMessage.getString(200, "", array);
    }

    @RequestMapping(value = "nginx/updateNgx", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String updateNgx(String name, String whitePath, String context) {
        if (StrUtil.isEmpty(name)) {
            return JsonMessage.getString(400, "请填写文件名");
        }
        if (StrUtil.isEmpty(whitePath)) {
            return JsonMessage.getString(400, "请选择文件路径");
        }
        if (!name.endsWith(".conf")) {
            return JsonMessage.getString(400, "文件后缀必须为\".conf\"");
        }
        if (StrUtil.isEmpty(context)) {
            return JsonMessage.getString(400, "请填写配置信息");
        }
        try {
            FileUtil.writeString(context, whitePath + "/" + name, CharsetUtil.UTF_8);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return JsonMessage.getString(400, "修改失败");
        }
        return JsonMessage.getString(200, "修改成功");
    }


    @RequestMapping(value = "nginx/uploadNgx", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String uploadNgx(String whitePath) {
        try {
            MultipartFileBuilder file = createMultipart().addFieldName("file")
                    .setSavePath(whitePath)
                    .setFileExt(".conf")
                    .setUseOriginalFilename(true);
            file.save();
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return JsonMessage.getString(400, "上传失败");
        }
        return JsonMessage.getString(200, "上传成功");
    }

    @RequestMapping(value = "nginx/addNgx", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String addNgx(String name, String whitePath, String context) {
        if (StrUtil.isEmpty(name)) {
            return JsonMessage.getString(400, "请填写文件名");
        }
        if (!name.endsWith(".conf")) {
            return JsonMessage.getString(400, "文件后缀必须为\".conf\"");
        }
        if (StrUtil.isEmpty(whitePath)) {
            return JsonMessage.getString(400, "请选择文件路径");
        }
        if (StrUtil.isEmpty(context)) {
            return JsonMessage.getString(400, "请填写配置信息");
        }
        try {
            FileUtil.writeString(context, whitePath + "/" + name, CharsetUtil.UTF_8);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return JsonMessage.getString(400, "创建失败");
        }
        return JsonMessage.getString(200, "创建成功");
    }

    @RequestMapping(value = "nginx/delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String delete(String path) {
        if (StrUtil.isEmpty(path)) {
            return JsonMessage.getString(400, "删除失败");
        }
        File file = FileUtil.file(path);
        boolean delete = file.delete();
        if (!delete) {
            return JsonMessage.getString(400, "删除失败");
        }
        return JsonMessage.getString(200, "删除成功");
    }

    @RequestMapping(value = "nginx/getConf", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String getConf(String path) {
        String string = FileUtil.readString(path, CharsetUtil.UTF_8);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("context", string);
        return JsonMessage.getString(200, "", jsonObject);
    }

}
