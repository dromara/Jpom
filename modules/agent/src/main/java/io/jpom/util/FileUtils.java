package io.jpom.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.File;

/**
 * 文件工具
 *
 * @author jiangzeyin
 * @date 2019/4/28
 */
public class FileUtils {

    private static JSONObject fileToJson(File file) {
        JSONObject jsonObject = new JSONObject(6);
        if (file.isDirectory()) {
            jsonObject.put("isDirectory", true);
            long sizeFile = FileUtil.size(file);
            jsonObject.put("fileSize", FileUtil.readableFileSize(sizeFile));
        } else {
            jsonObject.put("fileSize", FileUtil.readableFileSize(file.length()));
        }
        jsonObject.put("filename", file.getName());
        long mTime = file.lastModified();
        jsonObject.put("modifyTimeLong", mTime);
        jsonObject.put("modifyTime", DateUtil.date(mTime).toString());
        return jsonObject;
    }

    /**
     * 对文件信息解析排序
     *
     * @param files     文件数组
     * @param time      是否安装时间排序
     * @param startPath 开始路径
     * @return 排序后的json
     */
    public static JSONArray parseInfo(File[] files, boolean time, String startPath) {
        int size = files.length;
        JSONArray arrayFile = new JSONArray(size);
        for (File file : files) {
            JSONObject jsonObject = FileUtils.fileToJson(file);
            //
            if (startPath != null) {
                String levelName = StringUtil.delStartPath(file, startPath, false);
                jsonObject.put("levelName", levelName);
            }
            //
            arrayFile.add(jsonObject);
        }
        arrayFile.sort((o1, o2) -> {
            JSONObject jsonObject1 = (JSONObject) o1;
            JSONObject jsonObject2 = (JSONObject) o2;
            if (time) {
                return jsonObject2.getLong("modifyTimeLong").compareTo(jsonObject1.getLong("modifyTimeLong"));
            }
            return jsonObject1.getString("filename").compareTo(jsonObject2.getString("filename"));
        });
        final int[] i = {0};
        arrayFile.forEach(o -> {
            JSONObject jsonObject = (JSONObject) o;
            jsonObject.put("index", ++i[0]);
        });
        return arrayFile;
    }

    /**
     * 判断路径是否满足jdk 条件
     *
     * @param path 路径
     * @return 判断存在java文件
     */
    public static boolean isJdkPath(String path) {
        String fileName = getJdkJavaPath(path, false);
        File newPath = new File(fileName);
        return newPath.exists() && newPath.isFile();
    }

    /**
     * 获取java 文件路径
     *
     * @param path path
     * @param w    是否使用javaw
     * @return 完整路径
     */
    public static String getJdkJavaPath(String path, boolean w) {
        String fileName;
        if (SystemUtil.getOsInfo().isWindows()) {
            fileName = w ? "javaw.exe" : "java.exe";
        } else {
            fileName = w ? "javaw" : "java";
        }
        File newPath = FileUtil.file(path, "bin", fileName);
        return FileUtil.getAbsolutePath(newPath);
    }

    /**
     * 获取jdk 版本
     *
     * @param path jdk 路径
     * @return 获取成功返回版本号
     */
    public static String getJdkVersion(String path) {
        String newPath = getJdkJavaPath(path, false);
        if (path.contains(StrUtil.SPACE)) {
            newPath = String.format("\"%s\"", newPath);
        }
        String command = CommandUtil.execSystemCommand(newPath + "  -version");
        String[] split = StrUtil.splitToArray(command, StrUtil.LF);
        if (split == null || split.length <= 0) {
            return null;
        }
        String[] strings = StrUtil.splitToArray(split[0], "\"");
        if (strings == null || strings.length <= 1) {
            return null;
        }
        return strings[1];
    }


    public static String getJarSeparator() {
        return SystemUtil.getOsInfo().isWindows() ? ";" : ":";
    }
}
