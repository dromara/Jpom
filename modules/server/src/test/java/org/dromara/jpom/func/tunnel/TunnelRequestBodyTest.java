/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.tunnel;

import org.dromara.jpom.transport.DataContentType;
import org.dromara.jpom.transport.INodeInfo;
import org.dromara.jpom.transport.IUrlItem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.net.Proxy;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 反向隧道请求体编码回归测试。
 * <p>
 * 事故背景（2.11.14.2 候选验证发现）：{@code TunnelRequestBody} 早期实现用
 * {@code name=value} 直接拼接表单体，参数值里出现 {@code &}（shell 的 {@code &&}、
 * URL、HTML 属性）时会被插件端解析成多个参数，插件端只能拿到第一个 {@code &} 之前的内容，
 * 表现为「在开启反向隧道的节点上编辑项目文件，保存后文件被截断」。
 * <p>
 * 测试口径：请求体必须能被标准 {@code application/x-www-form-urlencoded} 解析器原样还原；
 * 同时用「未编码拼接」的旧实现做反证，证明该断言能拦住回归。
 *
 * @author jpom
 * @since 2026/11/19
 */
public class TunnelRequestBodyTest {

    private static final String TRICKY = "#!/bin/sh\n"
            + "if [ -f \"$PID_FILE\" ] && kill -0 \"$(cat \"$PID_FILE\")\" 2>/dev/null; then\n"
            + "  echo \"a=1&b=2 + c%20 中文行\"\n"
            + "fi\n";

    @Test
    public void testFormValueRoundTrip() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("nodeId", "machine-1");
        data.put("fileText", TRICKY);
        data.put("other", "plain");

        TunnelRequestBody body = TunnelRequestBody.build(node(), urlItem(), data);
        Assertions.assertEquals("application/x-www-form-urlencoded", body.getContentType());
        Map<String, String> parsed = parse(new String(body.getBody(), StandardCharsets.UTF_8));
        Assertions.assertEquals(TRICKY, parsed.get("fileText"), "含 & = + % 与中文的文件内容必须原样还原");
        Assertions.assertEquals("machine-1", parsed.get("nodeId"));
        Assertions.assertEquals("plain", parsed.get("other"));
        Assertions.assertEquals(3, parsed.size(), "值里的 & 不应被解析成额外参数");
    }

    @Test
    public void testArrayValueRoundTrip() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ids", new String[]{"a&b", "c=d"});
        TunnelRequestBody body = TunnelRequestBody.build(node(), urlItem(), data);
        String plain = new String(body.getBody(), StandardCharsets.UTF_8);
        Assertions.assertTrue(plain.contains("ids=a%26b"), "数组值必须编码后逐项追加：" + plain);
        Assertions.assertTrue(plain.contains("ids=c%3Dd"), "数组值必须编码后逐项追加：" + plain);
        Assertions.assertTrue(plain.contains("&ids="), "数组值应以重复 key 形式出现：" + plain);
    }

    /**
     * 反证：未编码拼接的旧实现必须还原失败，证明上面的断言不是「怎么改都能过」。
     */
    @Test
    public void testUnencodedImplementationWouldBreak() {
        StringBuilder naive = new StringBuilder();
        naive.append("fileText=").append(TRICKY);
        Map<String, String> parsed = parse(naive.toString());
        Assertions.assertNotEquals(TRICKY, parsed.get("fileText"), "旧实现（不编码）必须被本测试拦截");
        Assertions.assertTrue(parsed.size() > 1, "旧实现会把值里的 & 拆成额外参数");
    }

    private static Map<String, String> parse(String body) {
        Map<String, String> map = new HashMap<>();
        for (String pair : body.split("&")) {
            int idx = pair.indexOf('=');
            if (idx < 0) {
                continue;
            }
            try {
                map.put(URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8.name()),
                        URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8.name()));
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }
        return map;
    }

    private static INodeInfo node() {
        return new INodeInfo() {
            @Override
            public String id() {
                return "machine-1";
            }

            @Override
            public String name() {
                return "machine-1";
            }

            @Override
            public String url() {
                return "127.0.0.1:1";
            }

            @Override
            public String scheme() {
                return "http";
            }

            @Override
            public String authorize() {
                return "x";
            }

            @Override
            public Proxy proxy() {
                return null;
            }

            @Override
            public Integer timeout() {
                return 5;
            }

            @Override
            public Integer transportEncryption() {
                return 0;
            }
        };
    }

    private static IUrlItem urlItem() {
        Map<String, String> headers = new HashMap<>();
        return new IUrlItem() {
            @Override
            public String path() {
                return "/manage/file/update_config_file";
            }

            @Override
            public Integer timeout() {
                return 5;
            }

            @Override
            public String workspaceId() {
                return "default";
            }

            @Override
            public DataContentType contentType() {
                return DataContentType.FORM_URLENCODED;
            }

            @Override
            public Map<String, String> header() {
                return headers;
            }
        };
    }
}
