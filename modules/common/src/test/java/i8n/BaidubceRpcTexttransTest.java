package i8n;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
import cn.hutool.system.SystemUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;

/**
 * @author bwcx_jzy1
 * @since 2024/6/12
 */
public class BaidubceRpcTexttransTest {

    @Test
    public void doTranslate() {
        String token = this.getToken();
        UrlBuilder urlBuilder = UrlBuilder.of("https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions");
        urlBuilder.addQuery("access_token", token);

        HttpRequest httpRequest = HttpUtil.createPost(urlBuilder.build());
        httpRequest.header(Header.CONTENT_TYPE, ContentType.JSON.getValue());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model", "moonshot-v1-8k");
        jsonObject.put("temperature", 0.3);
        JSONObject message = new JSONObject();
        message.put("role", "user");
        //
        InputStream inputStream = ResourceUtil.getStream("baidubce_translate.txt");
        String string = IoUtil.readUtf8(inputStream);
        HashMap<Object, Object> map = new HashMap<>();
        JSONArray jsonArray = new JSONArray();
        jsonArray.add("测试变量");
        jsonArray.add("连接关闭 {} {}");
        jsonArray.add("清除临时文件失败,请手动清理：");
        map.put("REQUEST_STR", jsonArray.toString());
        string = StrUtil.format(string, map);
        System.out.println(string);
        message.put("content", string);
        jsonObject.put("messages", CollUtil.newArrayList(message));
        //
        httpRequest.body(jsonObject.toString());
        String result = httpRequest.thenFunction(httpResponse -> {
            String body = httpResponse.body();
            JSONObject jsonObject1 = JSONObject.parseObject(body);
            return jsonObject1.getString("result");
        });
        System.out.println(result);
    }

    private String getToken() {
        File file = new File("");
        String absolutePath = FileUtil.getAbsolutePath(file);
        File tokenCache = FileUtil.file(absolutePath, ".baidubce.token");
        if (tokenCache.exists()) {
            JSONObject cacheData = JSONObject.parseObject(FileUtil.readUtf8String(tokenCache));
            int expiresIn = cacheData.getIntValue("expires_in");
            if (SystemClock.now() / 1000L < expiresIn) {
                System.out.println("token 缓存有效，直接使用");
                return cacheData.getString("access_token");
            }
        }
        JSONObject cacheData = this.doTokenByApi(tokenCache);
        return cacheData.getString("access_token");
    }

    /**
     * https://cloud.baidu.com/doc/WENXINWORKSHOP/s/7lpch74jm
     *
     * @return
     */
    private JSONObject doTokenByApi(File file) {
        String bceCi = SystemUtil.get("JPOM_TRANSLATE_BAIDUBCE_CI", StrUtil.EMPTY);
        String bceCs = SystemUtil.get("JPOM_TRANSLATE_BAIDUBCE_CS", StrUtil.EMPTY);
        Assert.assertNotEquals("请配置百度千帆大模型 client_id[JPOM_TRANSLATE_BAIDUBCE_CI]", bceCi, StrUtil.EMPTY);
        Assert.assertNotEquals("请配置百度千帆大模型 client_secret[JPOM_TRANSLATE_BAIDUBCE_CS]", bceCs, StrUtil.EMPTY);

        HttpRequest httpRequest = HttpUtil.createPost("https://aip.baidubce.com/oauth/2.0/token");
        httpRequest.form("grant_type", "client_credentials")
            .form("client_id", "WdRlwS6iSEDJrUtHrzt2ZUot")
            .form("client_secret", "NO0pJQ8ynMYHklYstQtrLF2YzXV6jM4S");
        httpRequest.header(Header.CONTENT_TYPE, ContentType.JSON.getValue());
        httpRequest.header(Header.ACCEPT, ContentType.JSON.getValue());
        JSONObject json = httpRequest.thenFunction(httpResponse -> {
            int status = httpResponse.getStatus();
            Assert.assertEquals("token 生成异常", HttpStatus.HTTP_OK, status);
            String body = httpResponse.body();
            return JSONObject.parse(body);
        });
        String token = json.getString("access_token");
        int expiresIn = json.getIntValue("expires_in");
        System.out.println("获取最新的 token");
        JSONObject cacheData = new JSONObject();
        cacheData.put("access_token", token);
        cacheData.put("expires_in", expiresIn + SystemClock.now() / 1000L);
        //
        FileUtil.writeUtf8String(cacheData.toString(), file);
        return cacheData;
    }

    @Test
    public void doToken() {
        System.out.println(this.getToken());
    }
}
