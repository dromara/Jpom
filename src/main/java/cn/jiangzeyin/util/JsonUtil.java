package cn.jiangzeyin.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * @author jiangzeyin
 * date 2017/5/15
 */
public class JsonUtil {

    /**
     * 判断json对象是否为空
     *
     * @param obj
     * @return
     */
    public static boolean jsonIsEmpty(Object obj) {

        boolean flag = false;

        if (null == obj) {
            flag = true;
        }

        if (obj instanceof JSONObject) {
            JSONObject jsonobj = (JSONObject) obj;
            if (0 == jsonobj.keySet().size()) {
                flag = true;
            }
        }

        if (obj instanceof JSONArray) {
            JSONArray jsonarr = (JSONArray) obj;
            if (0 == jsonarr.size()) {
                flag = true;
            }
        }

        return flag;
    }

    public static Object readJson(String path) {
        String json = cn.hutool.core.io.FileUtil.readString(path, "UTF-8");
        return JSON.parse(json);
    }

    public static void saveJson(String path, JSON json) {
        String newsJson = JSON.toJSONString(json, true);
        cn.hutool.core.io.FileUtil.writeString(newsJson, path, "UTF-8");
    }
}
