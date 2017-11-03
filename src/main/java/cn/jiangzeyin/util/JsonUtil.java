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
     * @param obj
     * @return
     */
    public static boolean jsonIsEmpty(Object obj) {

        boolean flag = false;

        if (null == obj) {
            flag = true;
        }

        if (obj instanceof JSONObject) {
            JSONObject jsonobj = (JSONObject)obj;
            if (0 == jsonobj.keySet().size()) {
                flag = true;
            }
        }

        if (obj instanceof JSONArray) {
            JSONArray jsonarr = (JSONArray)obj;
            if(0 == jsonarr.size()) {
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
        String newsJson = formatJson(json.toJSONString());
        FileUtil.writeFile(path, newsJson);
    }

    /**
     * 格式化
     *
     * @param jsonStr
     * @return
     * @author lizhgb
     * @Date 2015-10-14 下午1:17:35
     */
    public static String formatJson(String jsonStr) {
        if (null == jsonStr || "".equals(jsonStr))
            return "";
        StringBuilder sb = new StringBuilder();
        char last = '\0';
        char current = '\0';
        int indent = 0;
        for (int i = 0; i < jsonStr.length(); i++) {
            last = current;
            current = jsonStr.charAt(i);
            switch (current) {
                case '{':
                case '[':
                    sb.append(current);
                    sb.append('\n');
                    indent++;
                    addIndentBlank(sb, indent);
                    break;
                case '}':
                case ']':
                    sb.append('\n');
                    indent--;
                    addIndentBlank(sb, indent);
                    sb.append(current);
                    break;
                case ',':
                    sb.append(current);
                    if (last != '\\') {
                        sb.append('\n');
                        addIndentBlank(sb, indent);
                    }
                    break;
                default:
                    sb.append(current);
            }
        }
        return sb.toString();
    }

    /**
     * 添加space
     *
     * @param sb
     * @param indent
     * @author lizhgb
     * @Date 2015-10-14 上午10:38:04
     */
    private static void addIndentBlank(StringBuilder sb, int indent) {
        for (int i = 0; i < indent; i++) {
            sb.append("  ");
        }
    }
}
