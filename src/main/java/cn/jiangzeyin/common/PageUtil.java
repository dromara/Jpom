package cn.jiangzeyin.common;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * Created by jiangzeyin on 2018/1/20.
 */
public class PageUtil {
    /**
     * 分页json格式
     *
     * @param code
     * @param msg
     * @param data
     * @return
     */
    public static String getPaginate(int code, String msg, JSONArray data) {
        JSONObject json = new JSONObject();
        json.put("code", code);
        json.put("msg", 0 < data.size() ? msg : "没有查询到数据!");
        json.put("count", data.size());
        json.put("data", data);
        return JSONObject.toJSONString(json);
    }
}
