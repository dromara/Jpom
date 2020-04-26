package io.jpom.util;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.File;

/**
 * layui
 *
 * @author bwcx_jzy
 * @date 2019/7/21
 */
public class LayuiTreeUtil {

    /**
     * 获取树的json
     *
     * @param path 文件名
     * @return jsonArray
     */
    public static JSONArray getTreeData(String path) {
        File file = FileUtil.file(path);
        return readTree(file, path);
    }

    private static JSONArray readTree(File file, String logFile) {
        File[] files = file.listFiles();
        if (files == null) {
            return null;
        }
        JSONArray jsonArray = new JSONArray();
        for (File file1 : files) {
            JSONObject jsonObject = new JSONObject();
            String path = StringUtil.delStartPath(file1, logFile, true);
            jsonObject.put("title", file1.getName());
            jsonObject.put("path", path);
            if (file1.isDirectory()) {
                JSONArray children = readTree(file1, logFile);
                jsonObject.put("children", children);
                //
                jsonObject.put("spread", true);
            }
            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }
}
