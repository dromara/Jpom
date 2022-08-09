/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
