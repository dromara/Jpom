package cn.jiangzeyin.util;

import org.springframework.util.Assert;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字符串工具类
 *
 * @author jiangzeyin
 * @date 2016-8-10
 */
public final class StringUtil {
    private final static String CONFIG_LOCATION_DELIMITERS = ",; \t\n";

    /**
     * GENERAL_PUNCTUATION 判断中文的“号
     * <p>
     * CJK_SYMBOLS_AND_PUNCTUATION 判断中文的。号
     * <p>
     * HALFWIDTH_AND_FULLWIDTH_FORMS 判断中文的，号
     *
     * @param c
     * @return
     * @author jiangzeyin
     * @date 2016-8-23
     */
    public static final boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
                ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
                ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
                ub == Character.UnicodeBlock.GENERAL_PUNCTUATION ||
                ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION ||
                ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
            return true;
        }
        return false;
    }

    /**
     * 判断是否包含中文
     *
     * @param strName
     * @return
     * @author jiangzeyin
     * @date 2016-8-23
     */
    public static final boolean isChinese(String strName) {
        char[] ch = strName.toCharArray();
        for (int i = 0; i < ch.length; i++) {
            char c = ch[i];
            if (isChinese(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 验证是否是数字
     *
     * @return
     */
    public static int isInteger(String str) {
        int res = 1;
        if (str == null || "".equals(str)) {
            return 1;
        }
        if (Pattern.matches("^[0-9]*$", str)) {
            res = Integer.parseInt(str);
        }
        return res;
    }

    /**
     * 字符串验证
     *
     * @param data      要验证的字符串
     * @param maxLength 字符串的最大长度
     * @param minLength 字符串最小长度
     * @return
     */
    public static boolean notEmptyIsValidity(String data, int maxLength, int minLength) {
        if (data == null)
            return false;
        data = data.replaceAll("　", "");
        data = data.trim();
        data = compileHtml(data);
        if ("".equals(data) || data.length() == 0)
            return false;
        if (maxLength == 0 || minLength == 0)
            return true;
        if (data.length() > maxLength || data.length() < minLength)
            return false;
        return true;
    }

    /**
     * 用于路径格式化操作
     *
     * @param str
     * @return str
     */
    public static String cleanPath(String str) {
        if (str != null && !"".equals(str)) {
            if (str.contains("\\")) {
                str = str.replace("\\", "/");
            }
            if (str.contains("//")) {
                str.replace("//", "/");
            }
        }
        return str;
    }

    /**
     * 判断是是否为空 可以判断长度
     *
     * @param data
     * @param minLength 顺序可以交换
     * @param maxLength
     * @return 是空或者 满足条件 返回true
     * @author jiangzeyin
     * @date 2016-8-10
     */
    public static boolean IsEmpty(String data, int minLength, int maxLength) {
        // 对象 空
        if (data == null)
            return true;
        data = data.trim();
        if (data.isEmpty())
            return true;
        // 处理空格
        data = data.replaceAll("　".intern(), "".intern());
        if (data.isEmpty())
            return true;
        // html 标签
        data = compileHtml(data);
        if (data.isEmpty())
            return true;
        if (maxLength <= 0 || minLength <= 0)
            return false;
        // 校正 顺序
        if (minLength > maxLength) {
            minLength = minLength ^ maxLength;
            maxLength = minLength ^ maxLength;
            minLength = minLength ^ maxLength;
        }
        if (data.length() > maxLength || data.length() < minLength)
            return true;
        return false;
    }

    /**
     * 验证字符串非空
     *
     * @param data
     * @return true为非空 false为空
     */
    public static boolean IsEmpty(String data) {
        return IsEmpty(data, -1, -1);
    }

    /**
     * 过滤<, >,\n 字符的方法。
     *
     * @param input 需要过滤的字符
     * @return 完成过滤以后的字符串
     */
    public static String filterHTML(String input) {
        if (input == null) {
            return null;
        }
        if (input.length() == 0) {
            return input;
        }
        input = input.trim();
        // input = input.replaceAll("　", "");
        // input = input.replaceAll("&", "&amp;");
        input = input.replaceAll("<", "&lt;");
        input = input.replaceAll(">", "&gt;");
        // input = input.replaceAll(" ", "&nbsp;");
        input = input.replaceAll("'", "&#39;");
        // input = input.replaceAll("\"", "&quot;");
        //input = input.replaceAll("\n", "<br>");

        // String s = "<:&lt;,>:&gt;, :&nbsp;,':&#39;,\":&quot;,\n:<br>";
        // StringBuffer sb = new StringBuffer(input);
        // String[] t = StringToArray(s, ",");
        // for (String string : t) {
        // String[] temp = string.split(":");
        // int i = sb.indexOf(temp[0]);
        // while (i > -1) {
        // int oldLen = temp[0].length();
        // int newLen = temp[1].length();
        // sb.delete(i, i + oldLen);
        // sb.insert(i, temp[1]);
        // i = sb.indexOf(temp[0], i + newLen);
        // }
        // }
        // return sb.toString();
        return input;
    }

    public static String filterHTML2(String input) {
        if (input == null) {
            return null;
        }
        if (input.length() == 0) {
            return input;
        }
        input = input.trim();
        input = input.replaceAll("&", "&amp;");
        return input;
    }

    /**
     * 编译html
     *
     * @param input
     * @return
     */
    public static String compileHtml(String input) {
        if (input == null) {
            return null;
        }
        if (input.length() == 0) {
            return input;
        }
        input = input.replaceAll("&amp;", "&");
        input = input.replaceAll("&lt;", "<");
        input = input.replaceAll("&gt;", ">");
        input = input.replaceAll("&nbsp;", " ");
        input = input.replaceAll("&#39;", "'");
        input = input.replaceAll("&quot;", "\"");
        return input.replaceAll("<br>", "\n");
    }

    public static String compileStr(String input) {
        if (input == null) {
            return null;
        }
        if (input.length() == 0) {
            return input;
        }
        input = input.replaceAll("\n", "<br>");
        input = input.replaceAll("\t", "  ");
        input = input.replaceAll("\\\"", "\"");
        return input;
    }

    public static String compileHtml2(String input) {
        if (input == null) {
            return null;
        }
        if (input.length() == 0) {
            return input;
        }
        // input = input.replaceAll("&amp;", "&");
        input = input.replaceAll("&lt;", "<");
        input = input.replaceAll("&gt;", ">");
        input = input.replaceAll("&nbsp;", " ");
        // input = input.replaceAll("&#39;", "'");
        input = input.replaceAll("&quot;", "\"");
        return input.replaceAll("<br>", "\n");
    }

    /**
     * 判断sql 类型
     *
     * @param str
     * @return
     */
    public static String sql_inj(String str) {
        String inj_str = "':and:exec:insert:select:delete:update:count:*:%:chr:mid:master:truncate:char:declare:;:or:-:+:,";
        String inj_stra[] = inj_str.split(":");
        for (int i = 0; i < inj_stra.length; i++) {
            try {
                str = str.replaceAll(inj_stra[i], "?");
            } catch (Exception e) {
                // TODO: handle exception
            }

        }
        return str;
    }

    /**
     * 模仿搜索加%
     *
     * @param str
     * @return
     */
    public static String sql_likeTo(String str) {
        StringBuffer sBuffer = new StringBuffer(str);
        for (int i = 0; i < str.length(); i++) {
            sBuffer.insert(i * 2, "%");// ("%", i);
        }
        sBuffer.insert(sBuffer.length(), "%");
        // System.out.println(sBuffer.toString());
        return sBuffer.toString();
    }

    /**
     * @param sequence
     * @param interval
     * @return
     * @author jiangzeyin
     * @date 2016-12-2
     */
    public static String RemoveStringInterval(CharSequence sequence, String interval) {
        String string = sequence.toString().trim();
        if (string.startsWith(interval)) {
            string = string.substring(1);
        }
        if (string.endsWith(interval)) {
            string = string.substring(0, string.length() - 1);
        }
        return string;
    }

    public static String RemoveStringInterval(CharSequence sequence) {
        return RemoveStringInterval(sequence, ",");
    }

    public static String[] StringToArray(String str) {
        return StringToArray(str, CONFIG_LOCATION_DELIMITERS);
    }

    public static String[] StringToArray(String str, String separator) {
        if ((str == null) || (separator == null)) {
            return null;
        }

        int i = 0;
        StringTokenizer st = new StringTokenizer(str, separator);
        String[] array = new String[st.countTokens()];
        while (st.hasMoreTokens()) {
            array[(i++)] = st.nextToken();
        }
        return array;
    }

    public static ArrayList<String> StringToArrayList(String str, String separator) {
        ArrayList<String> arr = new ArrayList<String>();
        if ((str == null) || (separator == null)) {
            return arr;
        }
        StringTokenizer st = new StringTokenizer(str, separator);
        while (st.hasMoreTokens()) {
            arr.add(st.nextToken());
        }
        return arr;
    }

    public static int[] StringToIntArray(String str, String separator) {
        if ((str == null) || (separator == null)) {
            return null;
        }

        int i = 0;
        StringTokenizer st = new StringTokenizer(str, separator);
        int[] array = new int[st.countTokens()];
        while (st.hasMoreTokens()) {
            array[(i++)] = parseInt(st.nextToken());
        }
        return array;
    }

    public static int parseInt(String num) {
        return parseInt(num, 0);
    }

    public static int parseInt(Object num) {
        if (num == null)
            return 0;
        return parseInt(num.toString(), 0);
    }

    public static int parseInt(Object obj, int default_) {
        return parseInt(convertNULL(obj), default_);
    }

    public static int parseInt(String num, int default_) {
        if ((num == null) || (num.length() == 0))
            return default_;

        try {
            return Integer.parseInt(num);
        } catch (NumberFormatException e) {
        }
        return default_;
    }

    public static long parseLong(String num) {
        if (num == null)
            return 0L;
        try {
            return Long.parseLong(num);
        } catch (NumberFormatException e) {
        }
        return 0L;
    }

    public static float parseFloat(String num) {
        if (num == null)
            return 0.0F;
        try {
            return Float.parseFloat(num);
        } catch (NumberFormatException e) {
        }
        return 0.0F;
    }

    public static double parseDouble(String num) {
        if (num == null)
            return 0.0D;
        try {
            return Double.parseDouble(num);
        } catch (NumberFormatException e) {
        }
        return 0.0D;
    }

    /**
     * 编码字符串
     *
     * @param str
     * @return
     */
    public static String getUTF8(String str) {
        if (IsEmpty(str))
            return "";
        try {
            return new String(str.getBytes("ISO-8859-1"), "utf-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    public static String getEncoding(String str) {
        String encode = "GB2312";
        try {
            if (str.equals(new String(str.getBytes(encode), encode))) {
                String s = encode;
                return s;
            }
        } catch (Exception exception) {
        }
        encode = "ISO-8859-1";
        try {
            if (str.equals(new String(str.getBytes(encode), encode))) {
                String s1 = encode;
                return s1;
            }
        } catch (Exception exception1) {
        }
        encode = "UTF-8";
        try {
            if (str.equals(new String(str.getBytes(encode), encode))) {
                String s2 = encode;
                return s2;
            }
        } catch (Exception exception2) {
        }
        encode = "GBK";
        try {
            if (str.equals(new String(str.getBytes(encode), encode))) {
                String s3 = encode;
                return s3;
            }
        } catch (Exception exception3) {
        }
        return "";
    }

    public static String convertUTF8(String input) throws UnsupportedEncodingException {
        if (input == null)
            return "";
        return new String(input.getBytes(getEncoding(input)), "UTF-8");
    }

    public static String convertNULL(String input) {
        if (input == null)
            return "";
        return input.trim().intern();
    }

    public static String convertNULL(Object input) {
        if (input == null)
            return "";
        return convertNULL(input.toString());
    }

    /**
     * 替换Stringbuffer内容
     *
     * @param sb
     * @param oldStr
     * @param newStr
     * @return
     */
    public static StringBuffer replaceAll(StringBuffer sb, String oldStr, String newStr) {
        int i = sb.indexOf(oldStr);
        int oldLen = oldStr.length();
        int newLen = newStr.length();
        while (i > -1) {
            sb.delete(i, i + oldLen);
            sb.insert(i, newStr.intern());
            i = sb.indexOf(oldStr.intern(), i + newLen);
        }
        return sb;
    }

    /**
     * 判断是否包含汉字
     *
     * @param str
     * @return
     */
    public static boolean isCharacters(String str) {
        char[] chars = str.toCharArray();
        boolean isGB2312 = false;
        for (int i = 0; i < chars.length; i++) {
            byte[] bytes = ("" + chars[i]).getBytes();
            if (bytes.length == 2) {
                int[] ints = new int[2];
                ints[0] = bytes[0] & 0xff;
                ints[1] = bytes[1] & 0xff;
                if (ints[0] >= 0x81 && ints[0] <= 0xFE && ints[1] >= 0x40 && ints[1] <= 0xFE) {
                    isGB2312 = true;
                    break;
                }
            }
        }
        return isGB2312;
    }

    /**
     * 小写大写互转
     *
     * @param str
     * @return
     */
    public static String exChange(String str) {
        StringBuffer sb = new StringBuffer();
        if (str != null) {
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                if ((int) c >= 48 && (int) c <= 48 + 9) {
                    sb.append(c);
                    continue;
                }
                if (Character.isUpperCase(c)) {
                    sb.append(Character.toLowerCase(c));
                } else if (Character.isLowerCase(c)) {
                    sb.append(Character.toUpperCase(c));
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    // public static boolean checkLastLine(String src, String key) {
    // int c;
    // String line; // 读取新文件
    // RandomAccessFile rf = null;
    // boolean isEnd = false;
    // try {
    // rf = new RandomAccessFile(src, "r");
    // long len = rf.length();
    // long start = rf.getFilePointer();
    // long nextend = start + len - 1;
    // rf.seek(start + len - 1);
    // while (nextend > start) {
    // c = rf.read();
    // line = rf.readLine();
    // // 判断读到的最后一行
    // if ((c == '\n' || c == '\r') && line != null
    // && !line.equals("")) {
    // if (line.equals(key)) {
    // isEnd = true;
    // }
    // break;
    // }
    // nextend--;
    // rf.seek(nextend);
    // }
    // } catch (FileNotFoundException e) {
    // e.printStackTrace();
    // } catch (IOException e) {
    // e.printStackTrace();
    // } finally {
    // try {
    // rf.close();
    // } catch (IOException e) {
    // e.printStackTrace();
    // }
    // }
    // return isEnd;
    // }


    public static String captureName(String inString) {
        if (StringUtil.IsEmpty(inString))
            return "";
        if (inString.length() > 1)
            return inString.substring(0, 1).toUpperCase() + inString.substring(1);
        return inString.toUpperCase();

    }

    /**
     * 转换异常信息
     *
     * @param e
     * @return
     * @author jiangzeyin
     * @date 2016-8-9
     */
    public static String fromException(Throwable e) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return System.lineSeparator() + sw.toString() + System.lineSeparator();
        } catch (Exception e2) {

        }
        return "";
    }

    /***
     * 把中文替换为指定字符<br>
     * 注意:一次只匹配一个中文字符
     * @param source
     * @param replacement
     * @return
     */
    public static String replaceChinese(String source, String replacement) {
        if (IsEmpty(source))
            return "";
        if (IsEmpty(replacement))
            throw new IllegalArgumentException("not null");
        String reg = "[\u4e00-\u9fa5]";
        Pattern pat = Pattern.compile(reg);
        Matcher mat = pat.matcher(source);
        return mat.replaceAll(replacement);
    }

    public static String maskPhone(String mobile) {
        String newPhone = "";
        for (int i = 0; i < mobile.length(); i++) {
            if (i == mobile.length() - 11) {
                newPhone += mobile.charAt(i);
            } else if (i == mobile.length() - 10) {
                newPhone += mobile.charAt(i);
            } else if (i == mobile.length() - 9) {
                newPhone += mobile.charAt(i);
            } else if (i == mobile.length() - 4) {
                newPhone += mobile.charAt(i);
            } else if (i == mobile.length() - 3) {
                newPhone += mobile.charAt(i);
            } else if (i == mobile.length() - 2) {
                newPhone += mobile.charAt(i);
            } else if (i == mobile.length() - 1) {
                newPhone += mobile.charAt(i);
            } else {
                newPhone += "*";
            }
        }
        return newPhone;
    }

    public static String arrayToString(String[] strings) {
        Assert.notNull(strings);
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < strings.length; i++) {
            if (i != 0)
                stringBuffer.append(",");
            stringBuffer.append(strings[i]);
        }
        return stringBuffer.toString();
    }

    /**
     * 获取当前时间,默认格式："yyyy-MM-dd HH:mm:ss"
     * @return
     */
    public static String currTime() {
        return currTime("yyyy-MM-dd HH:mm:ss");
    }
    public static String currTime(String format) {

        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(new Date());
    }

    /**
     * 获取字符串Md5值
     * @param str
     * @return
     */
    public static String getMd5(String str) {

        byte[] md5Byte = new byte[0];
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md5Byte = md.digest(str.getBytes("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        StringBuffer hexStr = new StringBuffer();
        int num;
        for (int i = 0; i < md5Byte.length; i++) {
            num = md5Byte[i];
            if (num < 0) {
                num += 256;
            }
            if (num < 16) {
                hexStr.append("0");
            }
            hexStr.append(Integer.toHexString(num));
        }

        return hexStr.toString().toUpperCase();
    }
}