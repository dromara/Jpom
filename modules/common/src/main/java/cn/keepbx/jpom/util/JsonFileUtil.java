package cn.keepbx.jpom.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * json 文件读写工具
 *
 * @author jiangzeyin
 * @date 2017/5/15
 */
public class JsonFileUtil {

    /**
     * 读取json 文件，同步
     *
     * @param path 路径
     * @return JSON
     * @throws FileNotFoundException 文件异常
     */
    public static Object readJson(String path) throws FileNotFoundException {
        File file = new File(path);
        if (!file.exists()) {
            throw new FileNotFoundException("没有找到对应配置文件：" + path);
        }
        // 防止多线程操作文件异常
        synchronized (JsonFileUtil.class) {
            String json = FileUtil.readString(file, CharsetUtil.UTF_8);
            if (StrUtil.isEmpty(json)) {
                return JSONObject.parseObject("{}");
            }
            return JSON.parse(json);
        }
    }

    /**
     * 保存json 文件,同步
     *
     * @param path 路径
     * @param json 新的json内容
     */
    public static void saveJson(String path, JSON json) {
        // 防止多线程操作文件异常
        synchronized (JsonFileUtil.class) {
            // 输出格式化后的json 字符串
            String newsJson = JSON.toJSONString(json, true);
            FileUtil.writeString(newsJson, path, CharsetUtil.UTF_8);
        }
    }

    public static <T> JSONObject arrayToObjById(JSONArray array) {
        JSONObject jsonObject = new JSONObject();
        array.forEach(o -> {
            JSONObject jsonObject1 = (JSONObject) o;
            jsonObject.put(jsonObject1.getString("id"), jsonObject1);
        });
        return jsonObject;
    }
}
