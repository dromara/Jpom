package cn.jiangzeyin.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
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
        } else if (obj instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) obj;
            flag = jsonObject.isEmpty();
        } else if (obj instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) obj;
            flag = jsonArray.isEmpty();
        }
        return flag;
    }

    public static Object readJson(String path) {
        synchronized (JsonUtil.class) {
            String json = FileUtil.readString(path, "UTF-8");
            if (StrUtil.isEmpty(json)) {
                return JSONObject.parseObject("{}");
            }
            return JSON.parse(json);
        }
    }

    public static void saveJson(String path, JSON json) {
        synchronized (JsonUtil.class) {
            String newsJson = JSON.toJSONString(json, true);
            FileUtil.writeString(newsJson, path, "UTF-8");
        }
    }
}
