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
package io.jpom.util;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.util.AntPathMatcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文件工具
 *
 * @author jiangzeyin
 * @since 2019/4/28
 */
public class FileUtils {

    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

    private static JSONObject fileToJson(File file) {
        JSONObject jsonObject = new JSONObject(6);
        long sizeFile = FileUtil.size(file);
        if (file.isDirectory()) {
            jsonObject.put("isDirectory", true);
            jsonObject.put("fileSize", FileUtil.readableFileSize(sizeFile));
        } else {
            jsonObject.put("fileSize", FileUtil.readableFileSize(sizeFile));
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
    public static List<JSONObject> parseInfo(File[] files, boolean time, String startPath) {
        return parseInfo(CollUtil.newArrayList(files), time, startPath);
    }

    /**
     * 对文件信息解析排序
     *
     * @param files     文件数组
     * @param time      是否安装时间排序
     * @param startPath 开始路径
     * @return 排序后的json
     */
    public static List<JSONObject> parseInfo(Collection<File> files, boolean time, String startPath) {
        if (files == null) {
            return new ArrayList<>();
        }
        return files.stream().map(file -> {
            JSONObject jsonObject = FileUtils.fileToJson(file);
            //
            if (startPath != null) {
                String levelName = StringUtil.delStartPath(file, startPath, false);
                jsonObject.put("levelName", levelName);
            }
            return jsonObject;
        }).sorted((jsonObject1, jsonObject2) -> {
            if (time) {
                return jsonObject2.getLong("modifyTimeLong").compareTo(jsonObject1.getLong("modifyTimeLong"));
            }
            return jsonObject1.getString("filename").compareTo(jsonObject2.getString("filename"));
        }).collect(Collectors.toList());
//        final int[] i = {0};
//        arrayFile.forEach(o -> {
//            JSONObject jsonObject = (JSONObject) o;
//            jsonObject.put("index", ++i[0]);
//        });
//        return arrayFile;
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

    /**
     * 读取 日志文件
     *
     * @param logFile 日志文件
     * @param line    开始行数
     * @return data
     */
    public static JSONObject readLogFile(File logFile, int line) {
        JSONObject data = new JSONObject();
        // 读取文件
        //int linesInt = Convert.toInt(line, 1);
        LimitQueue<String> lines = new LimitQueue<>(1000);
        final int[] readCount = {0};
        FileUtil.readLines(logFile, CharsetUtil.CHARSET_UTF_8, (LineHandler) line1 -> {
            readCount[0]++;
            if (readCount[0] < line) {
                return;
            }
            lines.add(line1);
        });
        // 下次应该获取的行数
        data.put("line", readCount[0] + 1);
        data.put("getLine", line);
        data.put("dataLines", lines);
        return data;
    }

    /**
     * 读取环境变量文件
     *
     * @param envFile 文件
     * @return map
     */
    public static Map<String, String> readEnvFile(File envFile) {
        HashMap<String, String> map = MapUtil.newHashMap(10);
        if (FileUtil.isFile(envFile)) {
            List<String> list = FileUtil.readLines(envFile, CharsetUtil.CHARSET_UTF_8);
            List<Tuple> collect = list.stream()
                .map(StrUtil::trim)
                .filter(s -> !StrUtil.isEmpty(s) && !StrUtil.startWith(s, "#"))
                .map(s -> {
                    List<String> list1 = StrUtil.splitTrim(s, "=");
                    if (CollUtil.size(list1) != 2) {
                        return null;
                    }
                    return new Tuple(list1.get(0), list1.get(1));
                }).filter(Objects::nonNull).collect(Collectors.toList());
            Map<String, String> envMap = CollStreamUtil.toMap(collect, objects -> objects.get(0), objects -> objects.get(1));
            // java.lang.UnsupportedOperationException
            map.putAll(envMap);
        }
        return map;
    }


    public static List<String> antPathMatcher(File rootFile, String match) {
        String matchStr = FileUtil.normalize(StrUtil.SLASH + match);
        List<String> paths = new ArrayList<>();
        //
        FileUtil.walkFiles(rootFile.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                return this.test(file);
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes exc) throws IOException {
                return this.test(dir);
            }

            private FileVisitResult test(Path path) {
                String subPath = FileUtil.subPath(FileUtil.getAbsolutePath(rootFile), path.toFile());
                subPath = FileUtil.normalize(StrUtil.SLASH + subPath);
                if (ANT_PATH_MATCHER.match(matchStr, subPath)) {
                    paths.add(subPath);
                    //return FileVisitResult.TERMINATE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return paths;
    }
}
