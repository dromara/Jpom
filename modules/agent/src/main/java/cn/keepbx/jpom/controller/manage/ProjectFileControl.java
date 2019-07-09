package cn.keepbx.jpom.controller.manage;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import cn.keepbx.jpom.common.BaseAgentController;
import cn.keepbx.jpom.model.data.ProjectInfoModel;
import cn.keepbx.jpom.service.manage.ConsoleService;
import cn.keepbx.jpom.socket.ConsoleCommandOp;
import cn.keepbx.jpom.system.AgentConfigBean;
import cn.keepbx.jpom.util.CompressionFileUtil;
import cn.keepbx.jpom.util.FileUtils;
import cn.keepbx.jpom.util.StringUtil;
import com.alibaba.fastjson.JSONArray;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.File;
import java.util.List;

/**
 * 项目文件管理
 *
 * @author jiangzeyin
 * @date 2019/4/17
 */
@RestController
@RequestMapping(value = "/manage/file/")
public class ProjectFileControl extends BaseAgentController {
    @Resource
    private ConsoleService consoleService;

    @RequestMapping(value = "getFileList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getFileList(String id, String path) {
        // 查询项目路径
        ProjectInfoModel pim = projectInfoService.getItem(id);
        if (pim == null) {
            return JsonMessage.getString(500, "查询失败：项目不存在");
        }
        String lib = pim.getLib();
        File fileDir;
        if (!StrUtil.isEmptyOrUndefined(path)) {
            fileDir = FileUtil.file(lib, path);
        } else {
            fileDir = new File(lib);
        }
        if (!fileDir.exists()) {
            return JsonMessage.getString(500, "目录不存在");
        }
        File[] filesAll = fileDir.listFiles();
        if (filesAll == null) {
            return JsonMessage.getString(500, "目录是空");
        }
        JSONArray arrayFile = FileUtils.parseInfo(filesAll, false, lib);
        return JsonMessage.getString(200, "查询成功", arrayFile);
    }


    @RequestMapping(value = "upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String upload() throws Exception {
        ProjectInfoModel pim = getProjectInfoModel();
        MultipartFileBuilder multipartFileBuilder = createMultipart()
                .addFieldName("file");
        // 压缩文件
        String type = getParameter("type");
        // 是否清空
        String clearType = getParameter("clearType");
        String levelName = getParameter("levelName");
        File lib;
        if (StrUtil.isEmpty(levelName)) {
            lib = new File(pim.getLib());
        } else {
            lib = FileUtil.file(pim.getLib(), levelName);
        }

        if ("unzip".equals(type)) {
            multipartFileBuilder.setFileExt(StringUtil.PACKAGE_EXT);
            multipartFileBuilder.setSavePath(AgentConfigBean.getInstance().getTempPathName());
            String path = multipartFileBuilder.save();
            // 判断是否需要清空
            if ("clear".equalsIgnoreCase(clearType)) {
                if (!FileUtil.clean(lib)) {
                    return JsonMessage.getString(500, "清除旧lib失败");
                }
            }
            // 解压
            File file = new File(path);
            try {
                List<String> names = CompressionFileUtil.unCompress(file.getAbsolutePath(), lib.getAbsolutePath());
                if (names == null || names.isEmpty()) {
                    return JsonMessage.getString(500, "没有解压出任何文件");
                }
            } finally {
                if (!file.delete()) {
                    DefaultSystemLog.LOG().info("删除失败：" + file.getPath());
                }
            }
        } else {
            multipartFileBuilder.setSavePath(FileUtil.getAbsolutePath(lib))
                    .setUseOriginalFilename(true);
            // 保存
            multipartFileBuilder.save();
        }
        // 修改使用状态
        pim.setUseLibDesc("upload");
        projectInfoService.updateItem(pim);
        //
        String after = getParameter("after");
        if ("restart".equalsIgnoreCase(after)) {
            String result = consoleService.execCommand(ConsoleCommandOp.restart, pim);
            return JsonMessage.getString(200, "上传成功并重启：" + result);
        }

        return JsonMessage.getString(200, "上传成功");
    }


    @RequestMapping(value = "deleteFile", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String deleteFile(String filename, String type, String levelName) {
        ProjectInfoModel pim = getProjectInfoModel();
        if ("clear".equalsIgnoreCase(type)) {
            // 清空文件
            File file = new File(pim.getLib());
            if (FileUtil.clean(file)) {
                return JsonMessage.getString(200, "清除成功");
            }
            if (pim.isStatus(true)) {
                return JsonMessage.getString(501, "文件被占用，请先停止项目");
            }
            return JsonMessage.getString(500, "删除失败：" + file.getAbsolutePath());
        } else {
            // 删除文件
            String fileName = pathSafe(filename);
            if (StrUtil.isEmpty(fileName)) {
                return JsonMessage.getString(405, "非法操作");
            }
            File file;
            if (StrUtil.isEmpty(levelName)) {
                file = FileUtil.file(pim.getLib(), fileName);
            } else {
                file = FileUtil.file(pim.getLib(), levelName, fileName);
            }
            if (file.exists()) {
                if (FileUtil.del(file)) {
                    return JsonMessage.getString(200, "删除成功");
                }
            } else {
                return JsonMessage.getString(404, "文件不存在");
            }
            return JsonMessage.getString(500, "删除失败");
        }
    }

    @RequestMapping(value = "download", method = RequestMethod.GET)
    public String download(String id, String filename, String levelName) {
        String safeFileName = pathSafe(filename);
        if (StrUtil.isEmpty(safeFileName)) {
            return JsonMessage.getString(405, "非法操作");
        }
        try {
            ProjectInfoModel pim = projectInfoService.getItem(id);
            File file;
            if (StrUtil.isEmpty(levelName)) {
                file = FileUtil.file(pim.getLib(), filename);
            } else {
                file = FileUtil.file(pim.getLib(), levelName, filename);
            }
            if (file.isDirectory()) {
                return "暂不支持下载文件夹";
            }
            ServletUtil.write(getResponse(), file);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("下载文件异常", e);
        }
        return "下载失败。请刷新页面后重试";
    }

}
