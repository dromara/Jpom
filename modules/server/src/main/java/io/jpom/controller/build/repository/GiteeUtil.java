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

import cn.hutool.core.convert.Convert;
import cn.hutool.db.Page;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * Gitee 工具
 *
 * @author sam
 * @since 2023/3/9
 */
public class GiteeUtil {

    /**
     * Gitee API 前缀
     */
    private static final String GITEE_API_PREFIX = "https://gitee.com/api";

    /**
     * Gitee API 版本号
     */
    private static final String API_VERSION = "v5";

    /**
     * Gitee API 地址前缀
     */
    private static final String GITEE_API_URL_PREFIX = GITEE_API_PREFIX + "/" + API_VERSION;

    /**
     * 用户授权码
     */
    private static final String ACCESS_TOKEN = "access_token";

    /**
     * 排序方式: 创建时间(created)，更新时间(updated)，最后推送时间(pushed)，仓库所属与名称(full_name)。默认: full_name
     */
    private static final String SORT = "sort";

    /**
     * 当前的页码
     */
    private static final String PAGE = "page";

    /**
     * 每页的数量，最大为 100
     */
    private static final String PER_PAGE = "per_page";

    /**
     * 获取 Gitee 用户名
     *
     * @param token 用户授权码
     * @return Gitee 用户名
     */
    public static String getGiteeUsername(String token) {
        // 参考：https://gitee.com/api/v5/swagger#/getV5User
        HttpResponse userResponse = HttpUtil.createGet(GITEE_API_URL_PREFIX + "/user", true)
            .form(ACCESS_TOKEN, token)
            .execute();
        Assert.state(userResponse.isOk(), "令牌不正确：" + userResponse.body());
        JSONObject userBody = JSONObject.parseObject(userResponse.body());
        return userBody.getString("login");
    }

    /**
     * 获取 Gitee 用户仓库信息
     *
     * @param token 用户授权码
     * @param page  分页参数
     * @return
     */
    public static Map<String, Object> getGiteeRepos(String token, Page page, String condition) {
        // 参考：https://gitee.com/api/v5/swagger#/getV5UserRepos
        HttpResponse reposResponse = HttpUtil.createGet(GITEE_API_URL_PREFIX + "/user/repos", true)
            .form(ACCESS_TOKEN, token)
            .form(SORT, "pushed")
            .form(PAGE, page.getPageNumber())
            .form(PER_PAGE, page.getPageSize())
            // 搜索关键字
            .form("q", condition)
            .execute();
        String body = reposResponse.body();
        Assert.state(reposResponse.isOk(), "获取仓库信息错误：" + body);

        // 所有仓库总数，包括公开的和私有的
        String totalCountStr = reposResponse.header("total_count");
        int totalCount = Convert.toInt(totalCountStr, 0);
        //String totalPage = reposResponse.header("total_page");

        Map<String, Object> map = new HashMap<>(2);
        map.put("jsonArray", JSONArray.parseArray(body));
        // 仓库总数
        map.put("totalCount", totalCount);
        return map;
    }
}
