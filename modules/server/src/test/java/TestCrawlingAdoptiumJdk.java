/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2022/8/9
 */
public class TestCrawlingAdoptiumJdk {

    @Test
    public void test() throws Exception {
        HttpRequest httpRequest = HttpUtil.createGet("https://mirrors.tuna.tsinghua.edu.cn/Adoptium/");
        String html = httpRequest.thenFunction(HttpResponse::body);
        //使用正则获取所有 url
        List<String> urls = ReUtil.findAll("<td class=\"link\">(.*?)</td>", html, 1);
        List<Integer> versions = urls.stream()
            .map(s -> {
                List<String> all = ReUtil.findAllGroup0("title=\"\\d+\"", s);
                return CollUtil.getFirst(all);
            })
            .filter(StrUtil::isNotEmpty)
            .map(ReUtil::getFirstNumber)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());


        StringBuilder builder = StrUtil.builder();
        builder.append("case \"${JAVA_VERSION}_${ARCH}\" in").append(StrUtil.LF);
        StringBuilder tipBuilder = StrUtil.builder();
        for (Integer version : versions) {
            Map<String, String> jdkItem = jdkItem(version);
            Set<Map.Entry<String, String>> entries = jdkItem.entrySet();
            //

            for (Map.Entry<String, String> entry : entries) {
                builder.append(StrUtil.format("{}_{})", version, entry.getKey())).append(StrUtil.LF);
                builder.append(StrUtil.format("\tdownload_url=\"{}\"", entry.getValue())).append(StrUtil.LF);
                builder.append("\t;;").append(StrUtil.LF);
            }
            tipBuilder.append(version).append("(").append(CollUtil.join(jdkItem.keySet(), StrUtil.COMMA)).append(")").append(StrUtil.SPACE);
        }
        builder.append("*)\n" + "\techo \"目前只支持 ").append(tipBuilder).append("\"\n").append("\texit 1\n").append("\t;;\n").append("esac");
        System.out.println(builder);
//        for (String title : urls) {
//            //打印标题
//            Console.log(title);
//        }
    }

    private Map<String, String> jdkItem(int version) {
        String versionUrl = StrUtil.format("https://mirrors.tuna.tsinghua.edu.cn/Adoptium/{}/jdk/", version);
        HttpRequest httpRequest = HttpUtil.createGet(versionUrl);
        String html = httpRequest.thenFunction(HttpResponse::body);

        List<String> urls = ReUtil.findAll("<td class=\"link\">(.*?)</td>", html, 1);
        return urls.stream()
            .map(s -> {
                List<String> all = ReUtil.findAll("title=\"(.*?)\"", s, 1);
                return CollUtil.getFirst(all);
            })
            .filter(StrUtil::isNotEmpty)
            .map(s -> {
                String typeUrl = StrUtil.format("https://mirrors.tuna.tsinghua.edu.cn/Adoptium/{}/jdk/{}/linux/", version, s);
                HttpRequest httpRequest1 = HttpUtil.createGet(typeUrl);
                String html1 = httpRequest1.thenFunction(HttpResponse::body);
                List<String> urls1 = ReUtil.findAll("<td class=\"link\">(.*?)</td>", html1, 1);
                List<String> durl = urls1.stream()
                    .map(s1 -> {
                        List<String> all = ReUtil.findAll("title=\"OpenJDK(.*?).tar.gz\"", s1, 0);
                        all = all.stream()
                            .filter(Objects::nonNull)
                            .map(s2 -> {
                                List<String> all1 = ReUtil.findAll("title=\"(.*?)\"", s2, 1);
                                return CollUtil.getFirst(all1);
                            })
                            .collect(Collectors.toList());
                        return CollUtil.getFirst(all);
                    })
                    .filter(StrUtil::isNotEmpty)
                    .collect(Collectors.toList());

                String first = CollUtil.getFirst(durl);
                if (StrUtil.isEmpty(first)) {
                    return null;
                }
                return new Tuple(s, StrUtil.format("{}{}", typeUrl, first));
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(o -> o.get(0), objects -> objects.get(1)));
    }
}
