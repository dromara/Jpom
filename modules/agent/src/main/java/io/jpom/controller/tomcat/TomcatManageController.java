/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.controller.tomcat;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseAgentController;
import io.jpom.common.commander.AbstractTomcatCommander;
import io.jpom.model.data.TomcatInfoModel;
import io.jpom.service.manage.TomcatEditService;
import io.jpom.service.manage.TomcatManageService;
import io.jpom.socket.AgentFileTailWatcher;
import io.jpom.util.LayuiTreeUtil;
import io.jpom.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author lf
 */
@RestController
@RequestMapping(value = "/tomcat/")
@Slf4j
public class TomcatManageController extends BaseAgentController {

    @Resource
    private TomcatEditService tomcatEditService;
    @Resource
    private TomcatManageService tomcatManageService;

    /**
     * 列出所有的tomcat项目列表
     *
     * @param id 项目id
     * @return Tomcat下的项目列表
     */
    @RequestMapping(value = "getTomcatProjectList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
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
    @RequestMapping(value = "getTomcatStatus", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String getStatus(String id) {
        return JsonMessage.getString(200, "查询成功", tomcatManageService.getTomcatStatus(id));
    }


    /**
     * 启动tomcat
     *
     * @param id tomcat id
     * @return 操作结果
     */
    @RequestMapping(value = "start", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String start(String id) {
        // 查询tomcat信息
        TomcatInfoModel tomcatInfoModel = tomcatEditService.getItem(id);

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
    @RequestMapping(value = "stop", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String stop(String id) {
        // 查询tomcat信息
        TomcatInfoModel tomcatInfoModel = tomcatEditService.getItem(id);

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
    @RequestMapping(value = "restart", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String restart(String id) {
        // 查询tomcat信息
        TomcatInfoModel tomcatInfoModel = tomcatEditService.getItem(id);

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
    @RequestMapping(value = "tomcatProjectManage", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String tomcatProjectManage(String id, String path,
                                      @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "操作项不对") String op) {

        TomcatOp tomcatOp = TomcatOp.valueOf(op);
        return tomcatManageService.tomcatProjectManage(id, path, tomcatOp).toString();
    }

    /**
     * 获取项目文件列表
     *
     * @param id   tomcat id
     * @param path 项目路径
     * @return 文件列表
     */
    @RequestMapping(value = "getFileList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String getFileList(String id, String path) {
        // 查询项目路径
        TomcatInfoModel tomcatInfoModel = tomcatEditService.getItem(id);
        if (tomcatInfoModel == null) {
            return JsonMessage.getString(500, "查询失败：项目不存在");
        }
        if (StrUtil.isEmptyOrUndefined(path)) {
            return JsonMessage.getString(500, "path value error");
        }
        String appBasePath = tomcatInfoModel.getAppBase();
        File fileDir = FileUtil.file(appBasePath, FileUtil.normalize(path));
        if (!fileDir.exists()) {
            return JsonMessage.getString(500, "目录不存在");
        }
        File[] filesAll = fileDir.listFiles();
        if (filesAll == null) {
            return JsonMessage.getString(500, "目录是空");
        }

        // JSONArray arrayFile = FileUtils.parseInfo(filesAll, false, appBasePath);
        JSONArray arrayFile = new JSONArray();
        JSONArray arrayDir = new JSONArray();
        for (File file : filesAll) {
            JSONObject jsonObject = new JSONObject();
            String parentPath = StringUtil.delStartPath(file, appBasePath, false);
            jsonObject.put("parentPath", parentPath);
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
    @RequestMapping(value = "upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String upload(String id, String path) {
        TomcatInfoModel tomcatInfoModel = tomcatEditService.getItem(id);

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
    @RequestMapping(value = "uploadWar", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String uploadWar(String id) {
        TomcatInfoModel tomcatInfoModel = tomcatEditService.getItem(id);

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
     * @param path     tomcat路径
     * @return 操作结果
     */
    @RequestMapping(value = "deleteFile", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String deleteFile(String id, String path, String filename) {
        TomcatInfoModel tomcatInfoModel = tomcatEditService.getItem(id);
        if (tomcatInfoModel == null) {
            return JsonMessage.getString(500, "tomcat不存在");
        }
        File file;
        if ("_tomcat_log".equals(path)) {
            //删除日志文件
            file = FileUtil.file(tomcatInfoModel.getPath(), "logs", filename);
            // 判断修改时间
            long modified = file.lastModified();
            if (System.currentTimeMillis() - modified < TimeUnit.DAYS.toMillis(1)) {
                return JsonMessage.getString(405, "不能删除当天的日志");
            }
            // 判断最后修改日期
            AgentFileTailWatcher.offlineFile(file);
        } else {
            file = FileUtil.file(tomcatInfoModel.getAppBase(), path, filename);
        }
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
     * @param path     tomcat路径
     * @return 操作结果
     */
    @RequestMapping(value = "download", method = RequestMethod.GET)
    public String download(String id, String path, String filename) {
        filename = FileUtil.normalize(filename);
        path = FileUtil.normalize(path);
        try {
            TomcatInfoModel tomcatInfoModel = tomcatEditService.getItem(id);
            File file;
            //下载日志文件
            if ("_tomcat_log".equals(path)) {
                file = FileUtil.file(tomcatInfoModel.getPath(), "logs", filename);
            } else {
                file = FileUtil.file(tomcatInfoModel.getAppBase(), path, filename);
            }
            if (file.isDirectory()) {
                return "暂不支持下载文件夹";
            }
            ServletUtil.write(getResponse(), file);
        } catch (Exception e) {
            log.error("下载文件异常", e);
        }
        return "下载失败。请刷新页面后重试";
    }

    /**
     * 获取tomcat 日志列表
     *
     * @param id tomcat id
     * @return json
     */
    @RequestMapping(value = "logList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String logList(String id) {
        TomcatInfoModel item = tomcatEditService.getItem(id);
        if (item == null) {
            return JsonMessage.getString(300, "没有对应数据");
        }
        JSONArray jsonArray = LayuiTreeUtil.getTreeData(FileUtil.file(item.pathAndCheck(), "logs").getAbsolutePath());
        return JsonMessage.getString(200, "", jsonArray);
    }
}
