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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.system.JpomRuntimeException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * json 文件读写工具
 *
 * @author bwcx_jzy
 * @since 2017/5/15
 */
public class JsonFileUtil {
    private static final ReentrantReadWriteLock FILE_LOCK = new ReentrantReadWriteLock();
    private final static ReentrantReadWriteLock.ReadLock READ_LOCK = FILE_LOCK.readLock();
    private final static ReentrantReadWriteLock.WriteLock WRITE_LOCK = FILE_LOCK.writeLock();

    /**
     * 读取json 文件，同步
     *
     * @param file 路径
     * @return JSON
     * @throws FileNotFoundException 文件异常
     */
    public static JSONObject readJson(File file) throws FileNotFoundException {
        if (!file.exists()) {
            throw new FileNotFoundException(I18nMessageUtil.get("i18n.no_config_file_found.9720") + file.getAbsolutePath());
        }
        READ_LOCK.lock();
        // 防止多线程操作文件异常
        try {
            String json = FileUtil.readString(file, CharsetUtil.CHARSET_UTF_8);
            if (StrUtil.isEmpty(json)) {
                return new JSONObject();
            }
            try {
                return JSONObject.parseObject(json);
            } catch (Exception e) {
                throw new JpomRuntimeException(I18nMessageUtil.get("i18n.data_file_content_error.e86f") + file.getAbsolutePath(), e);
            }
        } finally {
            READ_LOCK.unlock();
        }
    }

    /**
     * 读取json 文件，同步
     *
     * @param path 路径
     * @return JSON
     * @throws FileNotFoundException 文件异常
     */
    public static JSONObject readJson(String path) throws FileNotFoundException {
        File file = new File(path);
        return readJson(file);
    }

    /**
     * 保存json 文件,同步
     *
     * @param path 路径
     * @param json 新的json内容
     */
    public static void saveJson(String path, Object json) {
        WRITE_LOCK.lock();
        try {
            // 输出格式化后的json 字符串
            String newsJson = JSON.toJSONString(json);
            FileUtil.writeString(newsJson, path, CharsetUtil.UTF_8);
        } finally {
            WRITE_LOCK.unlock();
        }
    }

    /**
     * 保存json 文件,同步
     *
     * @param path 路径
     * @param json 新的json内容
     */
    public static void saveJson(File path, Object json) {
        saveJson(path.getAbsolutePath(), json);
    }

    public static <T> JSONObject arrayToObjById(JSONArray array) {
        JSONObject jsonObject = new JSONObject();
        array.forEach(o -> {
            JSONObject jsonObject1 = (JSONObject) o;
            jsonObject.put(jsonObject1.getString("id"), jsonObject1);
        });
        return jsonObject;
    }

    public static JSONArray formatToArray(JSONObject jsonObject) {
        if (jsonObject == null) {
            return new JSONArray();
        }
        Set<String> setKey = jsonObject.keySet();
        JSONArray jsonArray = new JSONArray();
        for (String key : setKey) {
            jsonArray.add(jsonObject.getJSONObject(key));
        }
        return jsonArray;
    }
}
