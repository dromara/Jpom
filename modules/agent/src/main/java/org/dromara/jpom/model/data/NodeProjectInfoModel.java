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
package org.dromara.jpom.model.data;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.model.RunMode;
import org.dromara.jpom.system.JpomRuntimeException;
import org.springframework.util.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 项目配置信息实体
 *
 * @author bwcx_jzy
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NodeProjectInfoModel extends BaseWorkspaceModel {
    /**
     * 分组
     */
    private String group;
    private String mainClass;
    private String lib;
    /**
     * 授权目录
     */
    private String whitelistDirectory;
    private String log;

    /**
     * 日志目录
     */
    private String logPath;
    /**
     * jvm 参数
     */
    private String jvm;
    /**
     * java main 方法参数
     */
    private String args;
    /**
     * WebHooks
     */
    private String token;


    private RunMode runMode;
    /**
     * 节点分发项目，不允许在项目管理中编辑
     */
    private Boolean outGivingProject;
    /**
     * 实际运行的命令
     */
    private String runCommand;

    /**
     * -Djava.ext.dirs=lib -cp conf:run.jar
     * 填写【lib:conf】
     */
    private String javaExtDirsCp;

    /**
     * 项目自动启动
     */
    private Boolean autoStart;
    /**
     * dsl yml 内容
     *
     * @see DslYmlDto
     */
    private String dslContent;
    /**
     * dsl 环境变量
     */
    private String dslEnv;
    //  ---------------- 中转字段 start
    /**
     * 是否可以重新加载
     */
    private Boolean canReload;
    /**
     * DSL 流程信息统计
     */
    private List<JSONObject> dslProcessInfo;
    //  ---------------- 中转字段 end

    public String getJavaExtDirsCp() {
        return StrUtil.emptyToDefault(javaExtDirsCp, StrUtil.EMPTY);
    }

    public boolean isOutGivingProject() {
        return outGivingProject != null && outGivingProject;
    }

    public RunMode getRunMode() {
        if (runMode == null) {
            return RunMode.ClassPath;
        }
        return runMode;
    }


    public String getMainClass() {
        return StrUtil.emptyToDefault(mainClass, StrUtil.EMPTY);
    }

    public String getWhitelistDirectory() {
        if (StrUtil.isEmpty(whitelistDirectory)) {
            throw new JpomRuntimeException("恢复授权数据异常");
        }
        return whitelistDirectory;
    }

    public String allLib() {
        String directory = this.getWhitelistDirectory();
        return FileUtil.file(directory, this.getLib()).getAbsolutePath();
    }

    /**
     * 获取项目文件中的所有jar 文件
     *
     * @param nodeProjectInfoModel 项目
     * @return list
     */
    public static List<File> listJars(NodeProjectInfoModel nodeProjectInfoModel) {
        File fileLib = new File(nodeProjectInfoModel.allLib());
        File[] files = fileLib.listFiles();
        if (files == null) {
            return new ArrayList<>();
        }
        RunMode runMode = nodeProjectInfoModel.getRunMode();
        return Arrays.stream(files)
            .filter(File::isFile)
            .filter(file -> {
                if (runMode == RunMode.ClassPath || runMode == RunMode.Jar || runMode == RunMode.JavaExtDirsCp) {
                    return StrUtil.endWith(file.getName(), FileUtil.JAR_FILE_EXT, true);
                } else if (runMode == RunMode.JarWar) {
                    return StrUtil.endWith(file.getName(), "war", true);
                }
                return false;
            })
            .collect(Collectors.toList());
    }

    /**
     * 拼接java 执行的jar路径
     *
     * @param nodeProjectInfoModel 项目
     * @return classpath 或者 jar
     */
    public static String getClassPathLib(NodeProjectInfoModel nodeProjectInfoModel) {
        List<File> files = listJars(nodeProjectInfoModel);
        if (CollUtil.isEmpty(files)) {
            return "";
        }
        // 获取lib下面的所有jar包
        StringBuilder classPath = new StringBuilder();
        RunMode runMode = nodeProjectInfoModel.getRunMode();
        int len = files.size();
        if (runMode == RunMode.ClassPath) {
            classPath.append("-classpath ");
        } else if (runMode == RunMode.Jar || runMode == RunMode.JarWar) {
            classPath.append("-jar ");
            // 只取一个jar文件
            len = 1;
        } else if (runMode == RunMode.JavaExtDirsCp) {
            classPath.append("-Djava.ext.dirs=");
            String javaExtDirsCp = nodeProjectInfoModel.getJavaExtDirsCp();
            String[] split = StrUtil.splitToArray(javaExtDirsCp, StrUtil.COLON);
            if (ArrayUtil.isEmpty(split)) {
                classPath.append(". -cp ");
            } else {
                classPath.append(split[0]).append(" -cp ");
                if (split.length > 1) {
                    classPath.append(split[1]).append(FileUtil.PATH_SEPARATOR);
                }
            }
        } else {
            return StrUtil.EMPTY;
        }
        for (int i = 0; i < len; i++) {
            File file = files.get(i);
            classPath.append(file.getAbsolutePath());
            if (i != len - 1) {
                classPath.append(FileUtil.PATH_SEPARATOR);
            }
        }
        return classPath.toString();
    }

    public String getLogPath() {
        return StrUtil.emptyToDefault(this.logPath, StrUtil.EMPTY);
    }

    public String getLog() {
        if (StrUtil.isEmpty(this.getId())) {
            return StrUtil.EMPTY;
        }
        if (StrUtil.isNotEmpty(this.getLogPath())) {
            return FileUtil.normalize(String.format("%s/%s/%s.log", this.getLogPath(), this.getId(), this.getId()));
        }
        if (StrUtil.isEmpty(this.log)) {
            String log = new File(this.allLib()).getParent();
            this.log = FileUtil.normalize(String.format("%s/%s.log", log, this.getId()));
        }
        return StrUtil.emptyToDefault(this.log, StrUtil.EMPTY);
    }

    public String getAbsoluteLog() {
        String pathname = getLog();
        Assert.hasText(pathname, "log path error");
        File file = new File(pathname);
        // auto create dir
        FileUtil.mkParentDirs(file);
        return file.getAbsolutePath();
    }

    public File getLogBack() {
        String log1 = getLog();
        Assert.hasText(log1, "log path error");
        return new File(log1 + "_back");
    }

    /**
     * 默认
     *
     * @return url token
     */
    public String getToken() {
        // 兼容旧数据
        if ("no".equalsIgnoreCase(this.token)) {
            return "";
        }
        return StrUtil.emptyToDefault(token, StrUtil.EMPTY);
    }

    /**
     * 获取当前 dsl 配置
     *
     * @return DslYmlDto
     */
    public DslYmlDto dslConfig() {
        String dslContent = this.getDslContent();
        if (StrUtil.isEmpty(dslContent)) {
            return null;
        }
        return DslYmlDto.build(dslContent);
    }

    /**
     * 必须存在 dsl 配置
     *
     * @return DslYmlDto
     */
    public DslYmlDto mustDslConfig() {
        DslYmlDto dslYmlDto = this.dslConfig();
        Assert.notNull(dslYmlDto, "未配置 dsl 信息（项目信息错误）");
        return dslYmlDto;
    }

    /**
     * 获取 dsl 流程信息
     *
     * @param opt 操作
     * @return 结果
     */
    public DslYmlDto.BaseProcess tryDslProcess(String opt) {
        DslYmlDto build = dslConfig();
        return tryDslProcess(build, opt);
    }

    /**
     * 获取 dsl 流程信息
     *
     * @param opt 操作
     * @return 结果
     */
    public static DslYmlDto.BaseProcess tryDslProcess(DslYmlDto build, String opt) {
        return Optional.ofNullable(build)
            .map(DslYmlDto::getRun)
            .map(run -> (DslYmlDto.BaseProcess) ReflectUtil.getFieldValue(run, opt))
            .orElse(null);
    }

    /**
     * 获取 dsl 流程信息
     *
     * @param opt 操作
     * @return 结果
     */
    public DslYmlDto.BaseProcess getDslProcess(String opt) {
        DslYmlDto.BaseProcess baseProcess = this.tryDslProcess(opt);
        Assert.notNull(baseProcess, "DSL 未配置运行管理或者未配置 " + opt + " 流程");
        return baseProcess;
    }
}
