package cn.jiangzeyin.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.jiangzeyin.controller.BaseController;
import cn.jiangzeyin.util.JsonUtil;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @author jiangzeyin
 */
public abstract class BaseService {

    /**
     * 获取数据的路径，如果没有这个路径，则创建一个
     *
     * @return file
     */
    private File getDataPath() throws IOException {
        String path = SpringUtil.getEnvironment().getProperty("boot-online.data");
        File file = new File(path);
        if (!file.exists() && !file.mkdirs()) {
            throw new IOException(path);
        }
        return file;
    }

    public File getTempPath() throws IOException {
        File file = getDataPath();
        String userName = BaseController.getUserName();
        if (StrUtil.isEmpty(userName)) {
            throw new RuntimeException("没有登录");
        }
        file = new File(file.getPath() + "/temp/", userName);
        FileUtil.mkdir(file);
        return file;
    }

    /**
     * 获取数据文件的路径，如果文件不存在，则创建一个
     *
     * @param filename 文件名
     * @return
     * @throws IOException
     */
    private String getDataFilePath(String filename) throws IOException {
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
    protected void saveJson(String filename, JSONObject json) throws Exception {
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
    protected void updateJson(String filename, JSONObject json) throws Exception {
        String key = json.getString("id");
        // 读取文件，如果不存在记录，则抛出异常
        JSONObject allData = getJsonObject(filename);
        JSONObject data = allData.getJSONObject(key);

        // 判断是否存在数据
        if (null == data || 0 == data.keySet().size()) {
            throw new Exception("数据不存在");
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
    protected void deleteJson(String filename, String key) throws Exception {
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
    protected JSONObject getJsonObject(String filename, String key) throws IOException {
        JSONObject jsonData = getJsonObject(filename);
        return jsonData.getJSONObject(key);
    }

    /**
     * 读取整个json文件
     *
     * @param filename 文件名
     * @return
     * @throws IOException
     */
    public JSONObject getJsonObject(String filename) throws IOException {
        return (JSONObject) JsonUtil.readJson(getDataFilePath(filename));
    }
}
