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

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.File;

/**
 * layui
 *
 * @author bwcx_jzy
 * @since 2019/7/21
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
