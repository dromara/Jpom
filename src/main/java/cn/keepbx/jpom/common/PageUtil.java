//package cn.keepbx.jpom.common;
//
//import com.alibaba.fastjson.JSONArray;
//import com.alibaba.fastjson.JSONObject;
//
///**
// * 分页工具
// *
// * @author jiangzeyin
// * @date 2018/1/20
// */
//public class PageUtil {
//    /**
//     * 分页json格式
//     *
//     * @param code 状态码
//     * @param msg  响应消息
//     * @param data 返回的数组信息
//     * @return json字符串
//     */
//    public static String getPaginate(int code, String msg, JSONArray data) {
//        JSONObject json = new JSONObject();
//        json.put("code", code);
//        json.put("msg", 0 < data.size() ? msg : "没有查询到数据!");
//        json.put("count", data.size());
//        json.put("data", data);
//        return JSONObject.toJSONString(json);
//    }
//}
