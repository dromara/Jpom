package cn.jiangzeyin.service;

import cn.jiangzeyin.common.spring.SpringUtil;
import cn.jiangzeyin.util.JsonUtil;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class BaseService {

    /**
     * 获取数据的路径，如果没有这个路径，则创建一个
     *
     * @return
     */
    public File getDataPath() {
        String path = SpringUtil.getEnvironment().getProperty("data.conf");
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    /**
     * 获取数据文件的路径，如果文件不存在，则创建一个
     *
     * @param filename 文件名
     * @return
     * @throws IOException
     */
    public String getDataFilePath(String filename) throws IOException {
        File file = new File(getDataPath(), filename);

        if (!file.exists()) {
            throw new FileNotFoundException(file.getPath() + " 文件不存在！");
        }

        return file.getPath();
    }

    /**
     * 保存json对象
     *
     * @param filename 文件名
     * @param json     json数据
     * @throws IOException
     */
    public void saveJson(String filename, JSONObject json) throws Exception {
        String key = json.getString("id");
        // 读取文件，如果存在记录，则抛出异常
        JSONObject allData = getJsonObject(filename);
        JSONObject data = allData.getJSONObject(key);

        // 判断是否存在数据
        if (null != data && 0 < data.keySet().size()) {
            throw new Exception("项目名称已存在！");
        } else {
            allData.put(key, json);
            JsonUtil.saveJson(getDataFilePath(filename), allData);
        }
    }

    /**
     * 修改json对象
     *
     * @param filename 文件名
     * @param json     json数据
     */
    public void updateJson(String filename, JSONObject json) throws Exception {
        String key = json.getString("id");
        // 读取文件，如果不存在记录，则抛出异常
        JSONObject allData = getJsonObject(filename);
        JSONObject data = allData.getJSONObject(key);

        // 判断是否存在数据
        if (null == data || 0 == data.keySet().size()) {
            throw new Exception("项目名称不存在！");
        } else {
            allData.put(key, json);
            JsonUtil.saveJson(getDataFilePath(filename), allData);
        }
    }

    /**
     * 删除json对象
     *
     * @param filename
     * @param key
     * @throws Exception
     */
    public void deleteJson(String filename, String key) throws Exception {
        // 读取文件，如果存在记录，则抛出异常
        JSONObject allData = getJsonObject(filename);
        JSONObject data = allData.getJSONObject(key);

        // 判断是否存在数据
        if (JsonUtil.jsonIsEmpty(data)) {
            throw new Exception("项目名称存不在！");
        } else {
            allData.remove(key);
            JsonUtil.saveJson(getDataFilePath(filename), allData);
        }
    }

    /**
     * 根据主键读取json对象
     *
     * @param filename 文件名
     * @param key      主键
     * @return
     * @throws IOException
     */
    public JSONObject getJsonObject(String filename, String key) throws IOException {
        JSONObject json_data = getJsonObject(filename);

        return json_data.getJSONObject(key);
    }

    /**
     * 读取整个json文件
     *
     * @param filename 文件名
     * @return
     * @throws IOException
     */
    public JSONObject getJsonObject(String filename) throws IOException {
        JSONObject json_data = (JSONObject) JsonUtil.readJson(getDataFilePath(filename));

        return json_data;
    }
}
