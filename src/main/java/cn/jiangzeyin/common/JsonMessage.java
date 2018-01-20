package cn.jiangzeyin.common;
/**
 * Created by jiangzeyin on 2017/2/6.
 */

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.Serializable;

/**
 * @author jiangzeyin
 * @create 2017 02 06 18:42
 */
public class JsonMessage implements Serializable {
    public static final String CODE = "code";
    public static final String MSG = "msg";
    public static final String DATA = "data";

    private int code;
    private String msg;
    private Object data;

    public JsonMessage(int code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public JsonMessage(int code, String msg) {
        this(code, msg, null);
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    /**
     * @return
     * @author jiangzeyin
     * @date 2016-8-8
     */
    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return JSONObject.toJSONString(this);// new JSONObject(this).toString();
    }


    /**
     * @param code
     * @param msg
     * @return
     * @author jiangzeyin
     * @date 2016-9-6
     */
    public static String getString(int code, String msg) {
        return new JsonMessage(code, msg).toString();
    }

    /**
     * @param code
     * @param msg
     * @param data
     * @return
     * @author jiangzeyin
     * @date 2016-9-6
     */
    public static String getString(int code, String msg, Object data) {
        return new JsonMessage(code, msg, data).toString();
    }

    /**
     * 分页json格式
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
