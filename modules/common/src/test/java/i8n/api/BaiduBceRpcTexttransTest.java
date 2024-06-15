/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package i8n.api;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
import cn.hutool.system.SystemUtil;
import com.alibaba.fastjson2.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author bwcx_jzy1
 * @since 2024/6/12
 */
public class BaiduBceRpcTexttransTest {

    @Test
    public void testTranslate() {
        ArrayList<String> strings = CollUtil.newArrayList("请输入正确的验证码", "请传入 body 参数", "开始准备项目重启：{} {}");
        JSONObject jsonObject = this.doTranslate(strings);
        System.out.println(jsonObject);
    }

    private boolean checkHasI18nKey(JSONObject jsonObject) {
        Set<String> keyed = jsonObject.keySet();
        for (String s : keyed) {
            if (StrUtil.startWith(s, "i18n.")) {
                // 提前失败 或者翻译失败
                //System.err.println("翻译失败或者提取失败," + s + "=" + jsonObject.get(s));
                return true;
            }
        }
        return false;
    }

    public JSONObject doTranslate(Collection<String> words) {
        while (true) {
            JSONObject jsonObject = this.doTranslate2(words);
            if (checkHasI18nKey(jsonObject)) {
                System.err.println("翻译失败或者提取失败,自动重试," + jsonObject);
            } else {
                return jsonObject;
            }
        }
    }


    private JSONObject doTranslate2(Collection<String> words) {
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
        //
        JSONObject from = new JSONObject();
        for (String value : words) {
            String key;
            do {
                key = StrUtil.format("i18n.{}", RandomUtil.randomStringUpper(6));
            } while (from.containsKey(key));
            from.put(key, value);
        }
        string = StrUtil.format(string, MapUtil.of("REQUEST_STR", from.toString()));
        //System.out.println(string);
        message.put("content", string);
        jsonObject.put("messages", CollUtil.newArrayList(message));
        //
        httpRequest.body(jsonObject.toString());
        String result = httpRequest.thenFunction(httpResponse -> {
            String body = httpResponse.body();
            JSONObject jsonObject1 = JSONObject.parseObject(body);
            if (jsonObject1.getIntValue("error_code") != 0) {
                Assert.fail(jsonObject1.getString("error_msg"));
            }
            return jsonObject1.getString("result");
        });
        String patternString = "(?s)```json\\s*([^`]*?)\\s*```";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(result);
        //
        JSONObject jsonObject1 = null;
        while (matcher.find()) {
            //System.out.println(result);
            String jsonContent = matcher.group(1);
            jsonObject1 = JSONObject.parseObject(jsonContent);
            if (!this.checkHasI18nKey(jsonObject1)) {
                return jsonObject1;
            }
        }
        Assert.assertNotNull("翻译失败或者提取失败", jsonObject1);
        return jsonObject1;
    }

    private String getToken() {
        File file = new File("");
        String absolutePath = FileUtil.getAbsolutePath(file);
        File tokenCache = FileUtil.file(absolutePath, ".baidubce.token");
        if (tokenCache.exists()) {
            JSONObject cacheData = JSONObject.parseObject(FileUtil.readUtf8String(tokenCache));
            int expiresIn = cacheData.getIntValue("expires_in");
            if (SystemClock.now() / 1000L < expiresIn) {
                //System.out.println("token 缓存有效，直接使用");
                return cacheData.getString("access_token");
            }
        }
        JSONObject cacheData = this.doTokenByApi(tokenCache);
        return cacheData.getString("access_token");
    }

    /**
     * <a href="https://cloud.baidu.com/doc/WENXINWORKSHOP/s/7lpch74jm">https://cloud.baidu.com/doc/WENXINWORKSHOP/s/7lpch74jm</a>
     *
     * @return token
     */
    private JSONObject doTokenByApi(File file) {
        String bceCi = SystemUtil.get("JPOM_TRANSLATE_BAIDUBCE_CI", StrUtil.EMPTY);
        String bceCs = SystemUtil.get("JPOM_TRANSLATE_BAIDUBCE_CS", StrUtil.EMPTY);
        Assert.assertNotEquals("请配置百度千帆大模型 client_id[JPOM_TRANSLATE_BAIDUBCE_CI]", bceCi, StrUtil.EMPTY);
        Assert.assertNotEquals("请配置百度千帆大模型 client_secret[JPOM_TRANSLATE_BAIDUBCE_CS]", bceCs, StrUtil.EMPTY);

        HttpRequest httpRequest = HttpUtil.createPost("https://aip.baidubce.com/oauth/2.0/token");
        httpRequest.form("grant_type", "client_credentials")
            .form("client_id", bceCi)
            .form("client_secret", bceCs);
        httpRequest.header(Header.CONTENT_TYPE, ContentType.JSON.getValue());
        httpRequest.header(Header.ACCEPT, ContentType.JSON.getValue());
        JSONObject json = httpRequest.thenFunction(httpResponse -> {
            int status = httpResponse.getStatus();
            String body = httpResponse.body();
            Assert.assertEquals("token 生成异常," + body, HttpStatus.HTTP_OK, status);
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
