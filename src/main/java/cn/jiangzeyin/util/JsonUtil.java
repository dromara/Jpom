package cn.jiangzeyin.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;

/**
 * Created by jiangzeyin on 2017/5/15.
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

    public static Object readJson(String path) throws IOException {
        String json = FileUtil.readToString(path);
        return JSON.parse(json);
    }

    public static void saveJson(String path, JSON json) throws IOException {
        String newsJson = JSON.toJSONString(json, true);
        FileUtil.writeFile(path, newsJson);
    }
}
