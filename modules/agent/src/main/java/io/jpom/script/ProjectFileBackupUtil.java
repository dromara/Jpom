package io.jpom.script;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.system.AgentExtConfigBean;
import io.jpom.system.ConfigBean;
import io.jpom.util.CommandUtil;
import io.jpom.util.StringUtil;
import org.springframework.util.Assert;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
        String backupId = DateTime.now().toString(DatePattern.PURE_DATETIME_MS_FORMAT);
        File projectFileBackup = ProjectFileBackupUtil.path(pathId, backupId);
        Assert.state(!FileUtil.exist(projectFileBackup), "备份目录冲突：" + projectFileBackup.getName());
        FileUtil.copyContent(FileUtil.file(projectPath), projectFileBackup, true);
        //
        return backupId;
    }

    /**
     * 检查备份保留个数
     *
     * @param backupPath 目录
     */
    private static void clearOldBackup(File backupPath) {
        int backupCount = AgentExtConfigBean.getInstance().getProjectFileBackupCount();
        File[] files = backupPath.listFiles();
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
    public static void checkDiff(String pathId, String projectPath, String backupId) {
        if (StrUtil.isEmpty(backupId)) {
            // 备份ID 不存在
            return;
        }
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
            }
        });
        // 删除空文件夹
        loopClean(backupItemPath);
        // 检查备份保留个数
        clearOldBackup(backupPath);
    }

    private static void loopClean(File backupPath) {
        if (FileUtil.isFile(backupPath)) {
            return;
        }
        // 检查目录是否为空
        if (FileUtil.isDirEmpty(backupPath)) {
            FileUtil.del(backupPath);
            return;
        }
        File[] files = backupPath.listFiles();
        for (File file : files) {
            ProjectFileBackupUtil.loopClean(file);
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
