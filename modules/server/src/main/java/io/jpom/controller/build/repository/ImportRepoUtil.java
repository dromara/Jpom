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
package io.jpom.controller.build.repository;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.PatternPool;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Page;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.setting.yaml.YamlUtil;
import io.jpom.system.ExtConfigBean;
import lombok.Lombok;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author WeiHongBin
 */
@Slf4j
@UtilityClass
public class ImportRepoUtil {

    private static final String IMPORT_REPO_PROVIDER_DIR = "/import-repo-provider/";

    @SneakyThrows
    public Map<String, Map<String, Object>> getProviderList() {
        Resource[] configResources = ExtConfigBean.getConfigResources("import-repo-provider/*.yml");
        return Arrays.stream(configResources)
                .map(resource -> {
                    String filename = resource.getFilename();
                    String mainName = FileUtil.mainName(filename);

                    try (InputStream inputStream = resource.getInputStream()) {
                        ImportRepoProviderConfig providerConfig = YamlUtil.load(inputStream, ImportRepoProviderConfig.class);
                        Map<String, Object> map = new HashMap<>();
                        map.put("name", mainName);
                        map.put("baseUrl", providerConfig.getBaseUrl());
                        // 是否支持查询
                        map.put("query", providerConfig.getRepoListParam().values().stream().anyMatch(s -> s.contains("${query}")));
                        return map;
                    } catch (Exception e) {
                        throw Lombok.sneakyThrow(e);
                    }
                })
                .collect(Collectors.toMap(map -> (String) map.get("name"), map -> map));
    }

    @SneakyThrows
    public ImportRepoProviderConfig getProviderConfig(String platform) {
        try (InputStream inputStream = ExtConfigBean.getConfigResourceInputStream(IMPORT_REPO_PROVIDER_DIR + platform + ".yml")) {
            return YamlUtil.load(inputStream, ImportRepoProviderConfig.class);
        }
    }

    private void setCommonParams(String platform, HttpRequest request, String token) {
        ImportRepoProviderConfig provider = getProviderConfig(platform);
        String callToken = provider.getAuthValue().replace("${token}", token);
        if (provider.getAuthType() == 1) {
            request.header(provider.getAuthKey(), callToken);
        } else if (provider.getAuthType() == 2) {
            request.form(provider.getAuthKey(), callToken);
        } else if (provider.getAuthType() == 3) {
            request.body(JSONUtil.createObj().set(provider.getAuthKey(), callToken).toString());
        }

        Map<String, String> extraParams = provider.getExtraParams();
        if (CollUtil.isNotEmpty(extraParams)) {
            if (provider.getExtraParamsType() == 1) {
                extraParams.forEach(request::header);
            } else if (provider.getExtraParamsType() == 2) {
                extraParams.forEach(request::form);
            } else if (provider.getExtraParamsType() == 3) {
                request.body(JSONUtil.toJsonStr(extraParams));
            }
        }
    }

    public JSONObject getRepoList(String platform, String query, Page page, String token, String username, String baseUrl) {
        baseUrl = StrUtil.blankToDefault(baseUrl, getProviderConfig(platform).getBaseUrl());
        ImportRepoProviderConfig provider = getProviderConfig(platform);
        HttpRequest request = HttpUtil.createRequest(Method.valueOf(provider.getRepoListMethod()), baseUrl + provider.getRepoListUrl());
        setCommonParams(platform, request, token);
        query = StrUtil.blankToDefault(query, "");
        String finalQuery = query;
        provider.getRepoListParam().forEach((k, v) -> {
            if ("${query}".equals(v)) {
                v = v.replace("${query}", finalQuery);
            }
            if ("${page}".equals(v)) {
                v = v.replace("${page}", String.valueOf(page.getPageNumber()));
            }
            if ("${pageSize}".equals(v)) {
                v = v.replace("${pageSize}", String.valueOf(page.getPageSize()));
            }
            request.form(k, v);
        });
        String body;
        int total;
        request.getUrl();
        log.debug(String.format("url: %s headers: %s form: %s", request.getUrl(), request.headers(), request.form()));
        try (HttpResponse execute = request.execute()) {
            body = execute.body();
            int status = execute.getStatus();
            Map<String, List<String>> headers = execute.headers();
            String totalHeader = execute.header(provider.getRepoTotalHeader());
            int totalCount = page.getPageSize() * page.getPageNumber();
            if ("Link".equals(provider.getRepoTotalHeader()) && StrUtil.isNotBlank(totalHeader)) {
                // github 特殊处理
                Pattern pattern = PatternPool.get("page=(\\d+)&per_page=(\\d+)>; rel=\"last\"");
                Matcher matcher = pattern.matcher(totalHeader);
                if (matcher.find()) {
                    int linkPage = Integer.parseInt(matcher.group(2));
                    int linkPerPage = Integer.parseInt(matcher.group(1));
                    total = linkPage * linkPerPage;
                } else {
                    total = totalCount;
                }
            } else {
                total = StrUtil.isNotBlank(totalHeader) ? Integer.parseInt(totalHeader) : totalCount;
            }
            log.debug(String.format("status: %s body: %s headers: %s", status, body, headers));
            Assert.state(execute.isOk(), String.format("请求失败: status: %s body: %s headers: %s", status, body, headers));
        }
        JSONArray jsonArray = JSONUtil.parse(body).getByPath(provider.getRepoListPath(), JSONArray.class);
        List<JSONObject> data = jsonArray.stream().map(o -> {
            JSONObject obj = (JSONObject) o;
            JSONObject entries = new JSONObject();
            provider.getRepoConvertPath().forEach((k, v) -> {
                if (StrUtil.startWith(v, "$ ")) {
                    String[] expression = v.split(" ");
                    String value = obj.getStr(expression[1]);
                    // 对比方式
                    String compare = expression[2];
                    // 对比值
                    String compareValue = expression[3];
                    switch (compare) {
                        case "==":
                            entries.set(k, value.equals(compareValue));
                            break;
                        case "!=":
                            entries.set(k, !value.equals(compareValue));
                            break;
                        default:
                            throw new IllegalStateException("表达式目前仅支持 == 和 != 比较");
                    }
                } else {
                    entries.set(k, obj.get(v));
                }
            });
            entries.set("username", username);
            return entries;
        }).collect(Collectors.toList());
        return JSONUtil.createObj().set("data", data).set("total", total);
    }

    public String getCurrentUserName(String platform, String token, String baseUrl) {
        baseUrl = StrUtil.blankToDefault(baseUrl, getProviderConfig(platform).getBaseUrl());
        Assert.state(StrUtil.isNotBlank(baseUrl), String.format("请填写 %s 的 地址", platform));
        ImportRepoProviderConfig provider = getProviderConfig(platform);
        HttpRequest request = HttpUtil.createRequest(Method.valueOf(provider.getCurrentUserMethod()), baseUrl + provider.getCurrentUserUrl());
        setCommonParams(platform, request, token);
        String body;
        log.debug(String.format("url: %s headers: %s form: %s", request.getUrl(), request.headers(), request.form()));
        try (HttpResponse execute = request.execute()) {
            body = execute.body();
            int status = execute.getStatus();
            Map<String, List<String>> headers = execute.headers();
            log.debug(String.format("status: %s body: %s headers: %s", status, body, headers));
            Assert.state(execute.isOk(), String.format("请求失败: status: %s body: %s headers: %s", status, body, headers));
        }
        return JSONUtil.parse(body).getByPath(provider.getUserNamePath(), String.class);
    }

}
