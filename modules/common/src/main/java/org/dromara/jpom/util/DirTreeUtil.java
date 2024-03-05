/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.util;

import cn.hutool.core.comparator.CompareUtil;
import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson2.JSONObject;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 目录树
 *
 * @author bwcx_jzy
 * @since 2019/7/21
 */
public class DirTreeUtil {

    /**
     * 获取树的json
     *
     * @param path 文件名
     * @return jsonArray
     */
    public static List<JSONObject> getTreeData(String path) {
        File file = FileUtil.file(path);
        return readTree(file, path);
    }

    private static List<JSONObject> readTree(File file, String logFile) {
        File[] files = file.listFiles();
        if (files == null) {
            return null;
        }
        return Arrays.stream(files)
            .sorted((o1, o2) -> CompareUtil.compare(o2.lastModified(), o1.lastModified()))
            .map(file1 -> {
                JSONObject jsonObject = new JSONObject();
                String path = StringUtil.delStartPath(file1, logFile, true);
                jsonObject.put("title", file1.getName());
                jsonObject.put("path", path);
                if (file1.isDirectory()) {
                    List<JSONObject> children = readTree(file1, logFile);
                    jsonObject.put("children", children);
                }
                return jsonObject;
            })
            .collect(Collectors.toList());
    }
}
