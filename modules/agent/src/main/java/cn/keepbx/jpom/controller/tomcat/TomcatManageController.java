package cn.keepbx.jpom.controller.tomcat;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import cn.keepbx.jpom.common.BaseAgentController;
import cn.keepbx.jpom.common.commander.AbstractTomcatCommander;
import cn.keepbx.jpom.model.data.ProjectInfoModel;
import cn.keepbx.jpom.model.data.TomcatInfoModel;
import cn.keepbx.jpom.service.manage.TomcatManageService;
import cn.keepbx.jpom.socket.ConsoleCommandOp;
import cn.keepbx.jpom.system.AgentConfigBean;
import cn.keepbx.jpom.util.FileUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author lf
 */
@RestController
@RequestMapping(value = "/tomcat/")
public class TomcatManageController extends BaseAgentController {

    @Resource
    private TomcatManageService tomcatManageService;

    /**
     * 列出所有的tomcat项目列表
     *
     * @return Tomcat下的项目列表
     */
    @RequestMapping(value = "getTomcatProjectList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getTomcatProjectList(String id) {
        JSONArray array = tomcatManageService.getTomcatProjectList(id);
        return JsonMessage.getString(200, "查询成功", array);
    }

    /**
     * 查询tomcat状态
     *
     * @param id tomcat的id
     * @return tomcat运行状态
     */
    @RequestMapping(value = "getTomcatStatus", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getStatus(String id) {
        return JsonMessage.getString(200, "查询成功", tomcatManageService.getTomcatStatus(id));
    }


    /**
     * 列出所有的tomcat
     *
     * @return Tomcat列表
     */
    @RequestMapping(value = "list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String list() {
        // 查询tomcat列表
        List<TomcatInfoModel> tomcatInfoModels = tomcatManageService.list();
        return JsonMessage.getString(200, "查询成功", tomcatInfoModels);
    }

    /**
     * 根据Id查询Tomcat信息
     *
     * @param id Tomcat的主键
     * @return 操作结果
     */
    @RequestMapping(value = "getItem", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getItem(String id) {
        // 查询tomcat列表
        return JsonMessage.getString(200, "查询成功", tomcatManageService.getItem(id));
    }


    /**
     * 添加Tomcat
     *
     * @param tomcatInfoModel Tomcat信息
     * @return 操作结果
     */
    @RequestMapping(value = "add", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String add(TomcatInfoModel tomcatInfoModel) {
        // 根据Tomcat名称查询tomcat是否已经存在
        String name = tomcatInfoModel.getName();
        TomcatInfoModel tomcatInfoModelTemp = tomcatManageService.getItemByName(name);
        if (tomcatInfoModelTemp != null) {
            return JsonMessage.getString(401, "名称已经存在，请使用其他名称！");
        }
        String error = this.doInitTomcat(tomcatInfoModel);
        if (error != null) {
            return error;
        }

        tomcatInfoModel.setId(SecureUtil.md5(DateUtil.now()));
        tomcatInfoModel.setCreator(getUserName());

        // 设置tomcat路径，去除多余的符号
        tomcatInfoModel.setPath(FileUtil.normalize(tomcatInfoModel.getPath()));
        tomcatManageService.addItem(tomcatInfoModel);
        return JsonMessage.getString(200, "保存成功");
    }


    /**
     * 修改Tomcat信息
     *
     * @param tomcatInfoModel Tomcat信息
     * @return 操作结果
     */
    @RequestMapping(value = "update", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String update(TomcatInfoModel tomcatInfoModel) {
        // 根据Tomcat名称查询tomcat是否已经存在
        String name = tomcatInfoModel.getName();
        TomcatInfoModel tomcatInfoModelTemp = tomcatManageService.getItemByName(name);
        if (tomcatInfoModelTemp != null && !tomcatInfoModelTemp.getId().equals(tomcatInfoModel.getId())) {
            return JsonMessage.getString(401, "名称已经存在，请使用其他名称！");
        }
        String error = this.doInitTomcat(tomcatInfoModel);
        if (error != null) {
            return error;
        }

        tomcatInfoModel.setModifyUser(getUserName());
        // 设置tomcat路径，去除多余的符号
        tomcatInfoModel.setPath(FileUtil.normalize(tomcatInfoModel.getPath()));
        tomcatManageService.updateItem(tomcatInfoModel);
        return JsonMessage.getString(200, "修改成功");

    }

    private String doInitTomcat(TomcatInfoModel tomcatInfoModel) {
        String tomcatPath = tomcatInfoModel.getPath();
        // 判断Tomcat路径是否正确
        if (!TomcatInfoModel.isTomcatRoot(tomcatPath)) {
            return JsonMessage.getString(405, String.format("没有在路径：%s 下检测到Tomcat", tomcatPath));
        }

        if (StrUtil.isEmpty(tomcatInfoModel.getAppBase())) {
            tomcatInfoModel.setAppBase(FileUtil.normalize(tomcatInfoModel.getPath()).concat(File.separator).concat("webapps"));
        } else {
            String path = FileUtil.normalize(tomcatInfoModel.getAppBase());
            if (FileUtil.isAbsolutePath(path)) {
                // appBase如：/project/、D:/project/
                tomcatInfoModel.setAppBase(path);
            } else {
                // appBase填写的是对相路径如：project/dir
                tomcatInfoModel.setAppBase(FileUtil.normalize(tomcatInfoModel.getPath()).concat(FileUtil.normalize(path)));
            }
        }
        InputStream inputStream = ResourceUtil.getStream("classpath:/bin/jpomAgent.zip");
        if (inputStream == null) {
            return JsonMessage.getString(500, "jpomAgent.zip不存在");
        }
        // 解压代理工具到tomcat的appBase目录下
        ZipUtil.unzip(inputStream, new File(tomcatInfoModel.getAppBase()), CharsetUtil.CHARSET_UTF_8);
        return null;
    }

    /**
     * 删除tomcat
     *
     * @param id tomcat id
     * @return 操作结果
     */
    @RequestMapping(value = "delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String delete(String id) {
        tomcatManageService.deleteItem(id);
        return JsonMessage.getString(200, "删除成功");
    }

    /**
     * 启动tomcat
     *
     * @param id tomcat id
     * @return 操作结果
     */
    @RequestMapping(value = "start", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String start(String id) {
        // 查询tomcat信息
        TomcatInfoModel tomcatInfoModel = tomcatManageService.getItem(id);

        String result = AbstractTomcatCommander.getInstance().execCmd(tomcatInfoModel, "start");
        String msg = "启动成功";
        if ("stopped".equals(result)) {
            msg = "启动失败";
        }
        return JsonMessage.getString(200, msg, result);
    }


    /**
     * 删除tomcat
     *
     * @param id tomcat id
     * @return 操作结果
     */
    @RequestMapping(value = "stop", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String stop(String id) {
        // 查询tomcat信息
        TomcatInfoModel tomcatInfoModel = tomcatManageService.getItem(id);

        String result = AbstractTomcatCommander.getInstance().execCmd(tomcatInfoModel, "stop");
        String msg = "停止成功";
        if ("started".equals(result)) {
            msg = "停止失败";
        }
        return JsonMessage.getString(200, msg, result);
    }

    /**
     * 重启tomcat
     *
     * @param id tomcat id
     * @return 操作结果
     */
    @RequestMapping(value = "restart", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String restart(String id) {
        // 查询tomcat信息
        TomcatInfoModel tomcatInfoModel = tomcatManageService.getItem(id);

        String stopResult = AbstractTomcatCommander.getInstance().execCmd(tomcatInfoModel, "stop");
        String startResult = AbstractTomcatCommander.getInstance().execCmd(tomcatInfoModel, "start");
        return JsonMessage.getString(200, StrUtil.format("重启成功 {}  {}", stopResult, startResult));
    }

    /**
     * tomcat项目管理
     *
     * @param id   tomcat id
     * @param path 项目路径
     * @param op   执行的操作
     * @return 操作结果
     */
    @RequestMapping(value = "tomcatProjectManage", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String tomcatProjectManage(String id, String path, String op) {
        return tomcatManageService.tomcatProjectManage(id, path, op).toString();
    }

    /**
     * 获取项目文件列表
     *
     * @param id   tomcat id
     * @param path 项目路径
     * @return 文件列表
     */
    @RequestMapping(value = "getFileList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getFileList(String id, String path, String except) {
        // 查询项目路径
        TomcatInfoModel tomcatInfoModel = tomcatManageService.getItem(id);
        if (tomcatInfoModel == null) {
            return JsonMessage.getString(500, "查询失败：项目不存在");
        }

        String appBasePath = tomcatInfoModel.getAppBase();
        File fileDir;
        if (!StrUtil.isEmptyOrUndefined(path)) {
            fileDir = FileUtil.file(appBasePath, FileUtil.normalize(path));
        } else {
            fileDir = new File(path);
        }
        if (!fileDir.exists()) {
            return JsonMessage.getString(500, "目录不存在");
        }
        File[] filesAll = fileDir.listFiles();
        if (filesAll == null) {
            return JsonMessage.getString(500, "目录是空");
        }

//        JSONArray arrayFile = FileUtils.parseInfo(filesAll, false, appBasePath);
        JSONArray arrayFile = new JSONArray();
        JSONArray arrayDir = new JSONArray();
        for (File file : filesAll) {
            JSONObject jsonObject = new JSONObject();

            jsonObject.put("parentPath", FileUtil.normalize(file.getParent()).replace(tomcatInfoModel.getAppBase(), ""));
            jsonObject.put("filename", file.getName());
            long mTime = file.lastModified();
            jsonObject.put("modifyTimeLong", mTime);
            jsonObject.put("modifyTime", DateUtil.date(mTime).toString());

            if (file.isDirectory()) {
                jsonObject.put("isDirectory", true);
                long sizeFile = FileUtil.size(file);
                jsonObject.put("fileSize", FileUtil.readableFileSize(sizeFile));
                arrayDir.add(jsonObject);
            } else {
                jsonObject.put("fileSize", FileUtil.readableFileSize(file.length()));
                arrayFile.add(jsonObject);
            }
        }

        JSONArray resultArray = new JSONArray();
        resultArray.addAll(arrayDir);
        resultArray.addAll(arrayFile);
        return JsonMessage.getString(200, "查询成功", resultArray);
    }


    /**
     * 上传文件
     *
     * @param id   tomcat id
     * @param path 文件路径
     * @return 操作结果
     */
    @RequestMapping(value = "upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String upload(String id, String path) {
        TomcatInfoModel tomcatInfoModel = tomcatManageService.getItem(id);

        MultipartFileBuilder multipartFileBuilder = createMultipart()
                .addFieldName("file");

        File dir = new File(tomcatInfoModel.getAppBase().concat(FileUtil.normalize(path)));

        multipartFileBuilder.setSavePath(dir.getAbsolutePath())
                .setUseOriginalFilename(true);
        // 保存
        try {
            multipartFileBuilder.save();
        } catch (IOException e) {
            return JsonMessage.getString(500, "上传异常");
        }

        return JsonMessage.getString(200, "上传成功");
    }

    /**
     * 上传war文件
     *
     * @param id tomcat id
     * @return 操作结果
     */
    @RequestMapping(value = "uploadWar", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String uploadWar(String id) {
        TomcatInfoModel tomcatInfoModel = tomcatManageService.getItem(id);

        MultipartFileBuilder multipartFileBuilder = createMultipart()
                .addFieldName("file");

        File dir = new File(tomcatInfoModel.getAppBase());

        multipartFileBuilder.setSavePath(dir.getAbsolutePath())
                .setUseOriginalFilename(true);
        // 保存
        try {
            multipartFileBuilder.save();
        } catch (IOException e) {
            return JsonMessage.getString(500, "上传异常");
        }

        return JsonMessage.getString(200, "上传成功");
    }


    /**
     * 删除文件
     *
     * @param id       tomcat id
     * @param filename 文件名
     * @return 操作结果
     */
    @RequestMapping(value = "deleteFile", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String deleteFile(String id, String path, String filename) {
        TomcatInfoModel tomcatInfoModel = tomcatManageService.getItem(id);
        if (tomcatInfoModel == null) {
            return JsonMessage.getString(500, "tomcat不存在");
        }

        path = FileUtil.normalize(path);
        filename = FileUtil.normalize(filename);

        File file = new File(tomcatInfoModel.getAppBase().concat(path).concat(File.separator).concat(filename));
        if (file.exists()) {
            if (file.delete()) {
                return JsonMessage.getString(200, "删除成功");
            } else {
                return JsonMessage.getString(500, "删除失败");
            }
        } else {
            return JsonMessage.getString(404, "文件不存在");
        }
    }

    /**
     * 下载文件
     *
     * @param id       tomcat id
     * @param filename 文件名
     * @return 操作结果
     */
    @RequestMapping(value = "download", method = RequestMethod.GET)
    public String download(String id, String path, String filename) {
        filename = FileUtil.normalize(filename);
        path = FileUtil.normalize(path);
        try {
            TomcatInfoModel tomcatInfoModel = tomcatManageService.getItem(id);
            File file = new File(tomcatInfoModel.getAppBase().concat(path).concat(File.separator).concat(filename));
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
