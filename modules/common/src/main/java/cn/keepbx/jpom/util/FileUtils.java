package cn.keepbx.jpom.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.File;

/**
 * @author jiangzeyin
 * @date 2019/4/28
 */
public class FileUtils {

    public static String delStartPath(File file, String startPath, boolean inName) {
        String newWhitePath;
        if (inName) {
            newWhitePath = FileUtil.getAbsolutePath(file.getAbsolutePath());
        } else {
            newWhitePath = FileUtil.getAbsolutePath(file.getParentFile());
        }
        String itemAbsPath = FileUtil.getAbsolutePath(new File(startPath));
        String path = newWhitePath.substring(newWhitePath.indexOf(itemAbsPath) + itemAbsPath.length());
        path = FileUtil.normalize(path);
        if (path.startsWith(StrUtil.SLASH)) {
            path = path.substring(1);
        }
        return path;
    }

    public static JSONObject fileToJson(File file) {
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

    public static JSONArray parseInfo(File[] files, boolean time, String startPath) {
        int size = files.length;
        JSONArray arrayFile = new JSONArray(size);
        for (File file : files) {
            JSONObject jsonObject = FileUtils.fileToJson(file);
            //
            if (startPath != null) {
                String levelName = FileUtils.delStartPath(file, startPath, false);
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

}
