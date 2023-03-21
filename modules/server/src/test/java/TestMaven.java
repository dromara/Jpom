import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ReUtil;
import cn.hutool.http.HttpUtil;

import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2023/3/21
 */
public class TestMaven {

    public static void main(String[] args) {
        String html = HttpUtil.get("https://mirrors.tuna.tsinghua.edu.cn/apache/maven/maven-3/");
        //使用正则获取所有可用版本
        List<String> titles = ReUtil.findAll("<a\\s+href=\"3.*?/\">(.*?)</a>", html, 1);
        for (String title : titles) {
            //打印标题
            Console.log(title);
        }
    }
}
