import cn.hutool.core.lang.PatternPool;
import cn.hutool.core.util.ReUtil;

import java.nio.charset.Charset;
import java.util.SortedMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Created by jiangzeyin on 2019/3/1.
 */
public class TestString {
    public static void main(String[] args) {
//        System.out.println(CheckPassword.checkPassword("123aA!"));
//        DateTime dateTime = DateUtil.parseUTC("2019-04-04T10:11:21Z");
//        System.out.println(dateTime);
//        dateTime.setTimeZone(TimeZone.getDefault());
//        System.out.println(dateTime);
        Pattern pattern = Pattern.compile("(https://|http://)?([\\w-]+\\.)+[\\w-]+(:\\d+|/)+([\\w- ./?%&=]*)?");
        String url = "http://192.168.1.111:2122/node/index.html?nodeId=dyc";
        System.out.println(ReUtil.isMatch(pattern, url));
        System.out.println(ReUtil.isMatch(PatternPool.URL_HTTP, url));


//        System.out.println(FileUtil.file("/a", null, "", "ss"));

        System.out.println(Math.pow(1024, 2));

        System.out.println(Integer.MAX_VALUE);

        SortedMap<String, Charset> x = Charset.availableCharsets();
        x.values().forEach(new Consumer<Charset>() {
            @Override
            public void accept(Charset charset) {
                String name = charset.name();
                if ("utf-8".equalsIgnoreCase(name)) {
                    System.out.println(charset);
                }
            }
        });
        System.out.println(x);
    }
}
