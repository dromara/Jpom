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
package io.jpom.script;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.model.data.DslYmlDto;
import io.jpom.system.AgentExtConfigBean;
import io.jpom.system.ConfigBean;
import io.jpom.util.CommandUtil;
import io.jpom.util.StringUtil;
import org.springframework.util.Assert;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 项目文件备份工具
 *
 * @author bwcx_jzy
 * @since 2022/5/10
 */
public class ProjectFileBackupUtil {

    /**
     * 整个项目的备份目录
     *
     * @param pathId 项目ID
     * @return file
     */
    public static File path(String pathId) {
        String dataPath = ConfigBean.getInstance().getDataPath();
        return FileUtil.file(dataPath, "project_file_backup", pathId);
    }

    /**
     * 获取项目的单次备份目录，备份ID
     *
     * @param pathId   项目ID
     * @param backupId 备份ID
     * @return file
     */
    public static File path(String pathId, String backupId) {
        File fileBackup = path(pathId);
        return FileUtil.file(fileBackup, backupId);
    }

    /**
     * 备份项目文件
     *
     * @param pathId      目录ID（项目ID）
     * @param projectPath 项目路径
     */
    public static String backup(String pathId, String projectPath) {
        int backupCount = AgentExtConfigBean.getInstance().getProjectFileBackupCount();
        if (backupCount <= 0) {
            // 未开启备份
            return null;
        }
        File file = FileUtil.file(projectPath);
        //
        if (!FileUtil.exist(file)) {
            return null;
        }
        String backupId = DateTime.now().toString(DatePattern.PURE_DATETIME_MS_FORMAT);
        File projectFileBackup = ProjectFileBackupUtil.path(pathId, backupId);
        Assert.state(!FileUtil.exist(projectFileBackup), "备份目录冲突：" + projectFileBackup.getName());
        FileUtil.copyContent(file, projectFileBackup, true);
        //
        return backupId;
    }

    /**
     * 检查备份保留个数
     *
     * @param backupPath 目录
     */
    private static void clearOldBackup(File backupPath, DslYmlDto dslYmlDto) {
        int backupCount = Optional.ofNullable(dslYmlDto)
            .map(DslYmlDto::getFile)
            .map(DslYmlDto.FileConfig::getBackupCount)
            .orElse(AgentExtConfigBean.getInstance().getProjectFileBackupCount());
        //
        if (!FileUtil.isDirectory(backupPath)) {
            return;
        }
        File[] files = backupPath.listFiles();
        if (files == null) {
            return;
        }
        List<File> collect = Arrays.stream(files)
            .filter(FileUtil::isDirectory)
            .sorted(Comparator.comparing(FileUtil::lastModifiedTime))
            .collect(Collectors.toList());
        // 截取
        int max = Math.max(collect.size() - backupCount, 0);
        if (max > 0) {
            collect = CollUtil.sub(collect, 0, max);
            // 删除
            collect.forEach(CommandUtil::systemFastDel);
        }
    }

    /**
     * 检查文件变动
     *
     * @param pathId      项目ID
     * @param projectPath 项目路径
     * @param backupId    要对比的备份ID
     */
    public static void checkDiff(String pathId, String projectPath, String backupId, DslYmlDto dslYmlDto) {
        if (StrUtil.isEmpty(backupId)) {
            // 备份ID 不存在
            return;
        }
        // 考虑到大文件对比，比较耗时。需要异步对比文件
        ThreadUtil.execute(() -> {
            File backupItemPath = ProjectFileBackupUtil.path(pathId, backupId);
            File backupPath = ProjectFileBackupUtil.path(pathId);
            // 获取文件列表
            Map<String, File> backupFiles = ProjectFileBackupUtil.listFiles(backupItemPath.getAbsolutePath());
            Map<String, File> nowFiles = ProjectFileBackupUtil.listFiles(projectPath);
            nowFiles.forEach((fileSha1, file) -> {
                // 当前目录存在的，但是备份目录也存在的相同文件则删除
                File backupFile = backupFiles.get(fileSha1);
                if (backupFile != null) {
                    CommandUtil.systemFastDel(backupFile);
                    backupFiles.remove(fileSha1);
                }
            });
            // 判断保存指定后缀
            String[] backupSuffix = Optional.ofNullable(dslYmlDto)
                .map(DslYmlDto::getFile)
                .map(DslYmlDto.FileConfig::getBackupSuffix)
                .orElse(AgentExtConfigBean.getInstance().getProjectFileBackupSuffix());
            if (ArrayUtil.isNotEmpty(backupSuffix)) {
                backupFiles.values()
                    .stream()
                    .filter(file -> {
                        String name = FileUtil.getName(file);
                        for (String reg : backupSuffix) {
                            if (ReUtil.isMatch(reg, name)) {
                                // 满足正则条件
                                return false;
                            }
                        }
                        return !StrUtil.endWithAny(name, backupSuffix);
                    })
                    .forEach(CommandUtil::systemFastDel);
            }
            // 删除空文件夹
            loopClean(backupItemPath);
            // 检查备份保留个数
            clearOldBackup(backupPath, dslYmlDto);
        });
    }

    private static void loopClean(File backupPath) {
        if (FileUtil.isFile(backupPath)) {
            return;
        }
        //
        Optional.ofNullable(backupPath.listFiles()).ifPresent(files1 -> {
            for (File file : files1) {
                ProjectFileBackupUtil.loopClean(file);
            }
        });
        // 检查目录是否为空
        if (FileUtil.isDirEmpty(backupPath)) {
            FileUtil.del(backupPath);
        }
    }

    /**
     * 获取文件列表信息
     *
     * @param path 路径
     * @return 文件列表信息
     */
    private static Map<String, File> listFiles(String path) {
        // 将所有的文件信息组装并签名
        List<File> files = FileUtil.loopFiles(path);
        List<JSONObject> collect = files.stream().map(file -> {
            //
            JSONObject item = new JSONObject();
            item.put("file", file);
            item.put("sha1", SecureUtil.sha1(file) + StrUtil.DASHED + StringUtil.delStartPath(file, path, true));
            return item;
        }).collect(Collectors.toList());
        return CollStreamUtil.toMap(collect,
            jsonObject12 -> jsonObject12.getString("sha1"),
            jsonObject1 -> (File) jsonObject1.get("file"));
    }
}
