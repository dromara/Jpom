import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;

import java.io.File;

/**
 * Created by jiangzeyin on 2019/3/7.
 */
public class TestCert {
    public static void main(String[] args) {
        HttpRequest request = HttpUtil.createPost("https://myssl.com/api/v1/tools/cert_decode");
        request.form("certfile", new File("D:\\SystemDocument\\Desktop\\web_hulianwangjia\\full_chain.pem"));
        request.form("type", "upload");
        HttpResponse response = request.execute();
        System.out.println(response.body());
    }
}
