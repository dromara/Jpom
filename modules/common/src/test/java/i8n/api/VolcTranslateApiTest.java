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
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.net.URLEncodeUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import cn.hutool.system.SystemUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

/**
 * <a href="https://www.volcengine.com/docs/4640/65067">https://www.volcengine.com/docs/4640/65067</a>
 *
 * @author bwcx_jzy
 * @since 2024/6/11
 */
public class VolcTranslateApiTest {

    private final String region;
    private final String service;
    private final String schema;
    private final String host;
    private final String path;
    private final String ak;
    private final String sk;


    private VolcTranslateApiTest(String region, String service, String schema, String host, String path, String ak, String sk) {
        this.region = region;
        this.service = service;
        this.host = host;
        this.schema = schema;
        this.path = path;
        this.ak = ak;
        this.sk = sk;
    }

    public VolcTranslateApiTest() {
        String volcSk = SystemUtil.get("JPOM_TRANSLATE_VOLC_SK", StrUtil.EMPTY);
        String volcAk = SystemUtil.get("JPOM_TRANSLATE_VOLC_AK", StrUtil.EMPTY);
        Assert.assertNotEquals("请配置火山翻译 SecretAccessKey[JPOM_TRANSLATE_VOLC_SK]", volcSk, StrUtil.EMPTY);
        Assert.assertNotEquals("请配置火山翻译 AccessKeyID[JPOM_TRANSLATE_VOLC_AK]", volcAk, StrUtil.EMPTY);

        this.region = "cn-north-1";
        this.service = "translate";
        this.host = "translate.volcengineapi.com";
        this.schema = "https";
        this.path = "/";
        this.ak = volcAk;
        this.sk = volcSk;
    }


    @Test
    public void test2() throws Exception {
        VolcTranslateApiTest translateApi = new VolcTranslateApiTest();
        JSONArray translateText = translateApi.translate("zh", "en", CollUtil.newArrayList("你好", "世界"));
        System.out.println(translateText);
    }

    public JSONArray translate(String source, String target, List<String> textList) throws Exception {


        String action = "TranslateText";
        String version = "2020-06-01";

        HashMap<String, String> queryMap = new HashMap<>(0);

        HashMap<String, Object> query2Map = new HashMap<>(3);
        query2Map.put("SourceLanguage", source);
        query2Map.put("TargetLanguage", target);
        query2Map.put("TextList", textList);

        String jsonStr = JSONObject.toJSONString(query2Map);
        String request = this.doRequest("POST", queryMap, jsonStr.getBytes(), action, version);
        JSONObject jsonObject = JSONObject.parse(request);
        Object error = jsonObject.getByPath("ResponseMetadata.Error");
        if (error != null) {
            throw new IllegalStateException("翻译异常：" + error);
        }
        return jsonObject.getJSONArray("TranslationList");
    }

    private String doRequest(String method, Map<String, String> queryList, byte[] body, String action, String version) throws Exception {
        if (body == null) {
            body = new byte[0];
        }
        String xContentSha256 = SecureUtil.sha256().digestHex(body);

        DateTime dateTime = DateTime.now().setTimeZone(TimeZone.getTimeZone("GMT"));
        String xDate = dateTime.toString("yyyyMMdd'T'HHmmss'Z'");

        String shortXDate = dateTime.toString(DatePattern.PURE_DATE_FORMAT);

        String contentType = "application/json";

        String signHeader = "host;x-date;x-content-sha256;content-type";


        SortedMap<String, String> realQueryList = new TreeMap<>(queryList);
        realQueryList.put("Action", action);
        realQueryList.put("Version", version);
        StringBuilder querySB = new StringBuilder();
        for (String key : realQueryList.keySet()) {
            querySB.append(signStringEncoder(key)).append("=").append(signStringEncoder(realQueryList.get(key))).append("&");
        }
        querySB.deleteCharAt(querySB.length() - 1);

        String canonicalStringBuilder = method + "\n" + path + "\n" + querySB + "\n" +
            "host:" + host + "\n" +
            "x-date:" + xDate + "\n" +
            "x-content-sha256:" + xContentSha256 + "\n" +
            "content-type:" + contentType + "\n" +
            "\n" +
            signHeader + "\n" +
            xContentSha256;

        //System.out.println(canonicalStringBuilder);

        String hashcanonicalString = SecureUtil.sha256().digestHex(canonicalStringBuilder.getBytes());
        String credentialScope = shortXDate + "/" + region + "/" + service + "/request";
        String signString = "HMAC-SHA256" + "\n" + xDate + "\n" + credentialScope + "\n" + hashcanonicalString;

        byte[] signKey = genSigningSecretKeyV4(sk, shortXDate, region, service);
        String signature = HexUtil.encodeHexStr(hmacSHA256(signKey, signString));

        Method method1 = Method.valueOf(method);
        HttpRequest request = HttpUtil.createRequest(method1, schema + "://" + host + path + "?" + querySB);


        request.header("Host", host);
        request.header("X-Date", xDate);
        request.header("X-Content-Sha256", xContentSha256);
        request.header("Content-Type", contentType);
        request.header("Authorization", "HMAC-SHA256" +
            " Credential=" + ak + "/" + credentialScope +
            ", SignedHeaders=" + signHeader +
            ", Signature=" + signature);
        if (!Objects.equals(method, "GET")) {
            request.body(body);
        }
        return request.thenFunction(HttpResponse::body);
    }

    private String signStringEncoder(String source) {
        return URLEncodeUtil.encodeQuery(source);
    }

    public static byte[] hmacSHA256(byte[] key, String content) throws Exception {
        return SecureUtil.hmacSha256(key).digest(content);
    }

    private byte[] genSigningSecretKeyV4(String secretKey, String date, String region, String service) throws Exception {
        byte[] kDate = hmacSHA256((secretKey).getBytes(), date);
        byte[] kRegion = hmacSHA256(kDate, region);
        byte[] kService = hmacSHA256(kRegion, service);
        return hmacSHA256(kService, "request");
    }
}
