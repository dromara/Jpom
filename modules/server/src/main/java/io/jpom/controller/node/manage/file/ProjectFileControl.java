package io.jpom.controller.node.manage.file;

import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.node.manage.ProjectInfoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * 文件管理
 *
 * @author Administrator
 */
@Controller
@RequestMapping(value = "/node/manage/file/")
@Feature(cls = ClassFeature.PROJECT)
public class ProjectFileControl extends BaseServerController {
    @Resource
    private ProjectInfoService projectInfoService;

    @Value("${fileFormat}")
    private String fileFormat;
//    /**
//     * 文件管理页面
//     *
//     * @param id 项目id
//     * @return page
//     */
//    @RequestMapping(value = "list.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @Feature(method = MethodFeature.FILE)
//    public String fileManage(String id) {
//        setAttribute("id", id);
//        JSONObject projectInfo = projectInfoService.getItem(getNode(), id);
//        String lib = projectInfo.getString("lib");
//        String whitelistDirectory = projectInfo.getString("whitelistDirectory");
//        lib = FileUtil.getAbsolutePath(FileUtil.file(whitelistDirectory, lib));
//        setAttribute("absLib", lib);
//        return "node/manage/filemanage";
//    }

    /**
     * 列出目录下的文件
     *
     * @return json
     */
    @RequestMapping(value = "getFileList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    // @ProjectPermission()
    @Feature(method = MethodFeature.FILE)
    public String getFileList() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_File_GetFileList).toString();
    }


    /**
     * 上传文件
     *
     * @return json
     */
    @RequestMapping(value = "upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @OptLog(UserOperateLogV1.OptType.UploadProjectFile)
    @Feature(method = MethodFeature.UPLOAD)
    public String upload() {
        return NodeForward.requestMultipart(getNode(), getMultiRequest(), NodeUrl.Manage_File_Upload).toString();
    }

    /**
     * 下载文件
     */
    @RequestMapping(value = "download", method = RequestMethod.GET)
    @ResponseBody
    @OptLog(UserOperateLogV1.OptType.DownloadProjectFile)
    @Feature(method = MethodFeature.DOWNLOAD)
    public void download() {
        NodeForward.requestDownload(getNode(), getRequest(), getResponse(), NodeUrl.Manage_File_Download);
    }

    /**
     * 删除文件
     *
     * @return json
     */
    @RequestMapping(value = "deleteFile", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.DEL_FILE)
    @OptLog(UserOperateLogV1.OptType.DelProjectFile)
    public String deleteFile() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_File_DeleteFile).toString();
    }


    /**
     * 更新配置文件
     *
     * @return json
     */
    @RequestMapping(value = "updateConfigFile", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.UPDATE_CONFIG_FILE)
    public String updateConfigFile() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_File_UpdateConfigFile).toString();
    }

    /**
     * 删除文件
     *
     * @return json
     */
    @RequestMapping(value = "readFile", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.READ_FILE)
    public String readFile() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_File_ReadFile).toString();
    }

    /**
     * 获取可编辑文件格式
     *
     * @return json
     */
    @RequestMapping(value = "geFileFormat", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.GET_FILE_FOMAT)
    public String geFileFormat() {
        String[] file = fileFormat.split("\\|");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("fileFormat",file);
        return JsonMessage.getString(200,"获取成功",jsonObject);
    }
}
