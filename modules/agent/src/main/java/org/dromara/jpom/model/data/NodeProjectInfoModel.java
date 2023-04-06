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
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.model.BaseJsonModel;
import org.dromara.jpom.model.RunMode;
import org.dromara.jpom.system.JpomRuntimeException;
import org.springframework.util.Assert;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 项目配置信息实体
 *
 * @author jiangzeyin
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
     * 白名单目录
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

    private List<JavaCopyItem> javaCopyItemList;
    /**
     * WebHooks
     */
    private String token;


    private RunMode runMode;
    /**
     * 节点分发项目，不允许在项目管理中编辑
     */
    private boolean outGivingProject;
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


    public List<JavaCopyItem> getJavaCopyItemList() {
        return javaCopyItemList;
    }

    public void setJavaCopyItemList(List<JavaCopyItem> javaCopyItemList) {
        this.javaCopyItemList = javaCopyItemList;
    }

    public String getJavaExtDirsCp() {
        return StrUtil.emptyToDefault(javaExtDirsCp, StrUtil.EMPTY);
    }

    public boolean isOutGivingProject() {
        return outGivingProject;
    }

    public void setOutGivingProject(boolean outGivingProject) {
        this.outGivingProject = outGivingProject;
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
            throw new JpomRuntimeException("恢复白名单数据异常");
        }
        return whitelistDirectory;
    }

    public void setWhitelistDirectory(String whitelistDirectory) {
        this.whitelistDirectory = whitelistDirectory;
    }


    public String allLib() {
        String directory = this.getWhitelistDirectory();
        directory = AgentWhitelist.convertRealPath(directory);
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
        return Arrays.stream(files).filter(File::isFile).filter(file -> {
            if (runMode == RunMode.ClassPath || runMode == RunMode.Jar || runMode == RunMode.JavaExtDirsCp) {
                return StrUtil.endWith(file.getName(), FileUtil.JAR_FILE_EXT, true);
            } else if (runMode == RunMode.JarWar) {
                return StrUtil.endWith(file.getName(), "war", true);
            }
            return false;
        }).collect(Collectors.toList());
        //		List<File> files1 = new ArrayList<>();
        //		if (files != null) {
        //			for (File file : files) {
        //				if (!file.isFile()) {
        //					continue;
        //				}
        //
        //				if (runMode == RunMode.ClassPath || runMode == RunMode.Jar || runMode == RunMode.JavaExtDirsCp) {
        //					if (!StrUtil.endWith(file.getName(), FileUtil.JAR_FILE_EXT, true)) {
        //						continue;
        //					}
        //				} else if (runMode == RunMode.JarWar) {
        //					if (!StrUtil.endWith(file.getName(), "war", true)) {
        //						continue;
        //					}
        //				}
        //				files1.add(file);
        //			}
        //		}
        //		return files1;
    }

    /**
     * 拼接java 执行的jar路径
     *
     * @param nodeProjectInfoModel 项目
     * @return classpath 或者 jar
     */
    public static String getClassPathLib(NodeProjectInfoModel nodeProjectInfoModel) {
        List<File> files = listJars(nodeProjectInfoModel);
        if (files.size() <= 0) {
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

    /**
     * 副本的控制台日志文件
     *
     * @param javaCopyItem 副本信息
     * @return file
     */
    public File getLog(JavaCopyItem javaCopyItem) {
        String log1 = getLog();
        Assert.hasText(log1, "log path error");
        File file = FileUtil.file(log1);
        return FileUtil.file(file.getParentFile(), getId() + "_" + javaCopyItem.getId() + ".log");
    }

    public String getAbsoluteLog(JavaCopyItem javaCopyItem) {
        String pathname = getLog();
        Assert.hasText(pathname, "log path error");
        File file = javaCopyItem == null ? new File(pathname) : getLog(javaCopyItem);
        // auto create dir
        FileUtil.mkParentDirs(file);
        return file.getAbsolutePath();
    }

    public File getLogBack() {
        String log1 = getLog();
        Assert.hasText(log1, "log path error");
        return new File(log1 + "_back");
    }

    public File getLogBack(JavaCopyItem javaCopyItem) {
        return new File(getLog(javaCopyItem) + "_back");
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

    public JavaCopyItem findCopyItem(String copyId) {
        if (StrUtil.isEmpty(copyId)) {
            return null;
        }
        List<JavaCopyItem> javaCopyItemList = getJavaCopyItemList();
        if (CollUtil.isEmpty(javaCopyItemList)) {
            return null;
        }
        Optional<JavaCopyItem> first = javaCopyItemList.stream().filter(javaCopyItem -> StrUtil.equals(javaCopyItem.getId(), copyId)).findFirst();
        return first.orElse(null);
    }

    public boolean removeCopyItem(String copyId) {

        if (StrUtil.isEmpty(copyId) || CollUtil.isEmpty(javaCopyItemList)) {
            return true;
        }
        int size = javaCopyItemList.size();
        List<JavaCopyItem> collect = javaCopyItemList.stream().filter(javaCopyItem -> !StrUtil.equals(javaCopyItem.getId(), copyId)).collect(Collectors.toList());
        if (size - 1 == collect.size()) {
            this.javaCopyItemList = collect;
            return true;
        } else {
            return false;
        }
    }

    public DslYmlDto dslConfig() {
        String dslContent = this.getDslContent();
        if (StrUtil.isEmpty(dslContent)) {
            return null;
        }
        return DslYmlDto.build(dslContent);
    }

    /**
     * 获取 dsl 流程信息
     *
     * @param opt 操作
     * @return 结果
     */
    public DslYmlDto.BaseProcess tryDslProcess(String opt) {
        DslYmlDto build = dslConfig();
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
        DslYmlDto build = dslConfig();
        Assert.notNull(build, "yml 还未配置");
        return build.runProcess(opt);
    }

    @Data
    public static class JavaCopyItem extends BaseJsonModel {
        /**
         * 父级项目id
         */
        @Deprecated
        private String parendId;
        /**
         * 名称
         */
        private String name;
        private String parentId;
        /**
         * id
         */
        private String id;

        /**
         * jvm 参数
         */
        private String jvm;
        /**
         * java main 方法参数
         */
        private String args;

        private String modifyTime;

        /**
         * 日志
         */
        private String log;

        /**
         * 日志备份
         */
        private String logBack;

        public String getTagId() {
            return getTagId(this.getParentId(), id);
        }

        /**
         * 创建进程标记
         *
         * @param id     父级项目ID
         * @param copyId 副本ID
         * @return 运行ID
         */
        public static String getTagId(String id, String copyId) {
            if (StrUtil.isEmpty(copyId)) {
                return id;
            }
            return StrUtil.format("{}:{}", id, copyId);
        }

        @Deprecated
        public String getParendId() {
            return parendId;
        }

        @Deprecated
        public void setParendId(String parentId) {
            this.parendId = parentId;
            this.parentId = parentId;
        }

        public String getParentId() {
            return StrUtil.emptyToDefault(this.parentId, this.parendId);
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            JavaCopyItem that = (JavaCopyItem) o;
            return Objects.equals(parendId, that.parendId) &&
                Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(parendId, id, jvm, args, modifyTime);
        }
    }
}
