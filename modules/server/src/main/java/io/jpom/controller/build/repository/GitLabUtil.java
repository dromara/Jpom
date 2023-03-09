package io.jpom.controller.build.repository;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Page;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import lombok.Data;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GitLab 工具
 *
 * @author sam
 * @since 2023/3/9
 */
public class GitLabUtil {
    /**
     * GitLab 版本号信息，参考：https://docs.gitlab.com/ee/api/version.html
     */
    @Data
    private static class GitLabVersionInfo {

        /**
         * 版本号，如：8.13.0-pre
         */
        private String version;

        /**
         * 修订号，如：4e963fe
         */
        private String revision;

        /**
         * API 版本号，如：v4
         */
        private String apiVersion;
    }

    /**
     * GitLab 版本信息容器，key：GitLab 地址，value：GitLabVersionInfo
     */
    public static final Map<String, GitLabVersionInfo> gitlabVersionMap = new ConcurrentHashMap<>();

    /**
     * 获取 GitLab 版本
     *
     * @param gitlabAddress GitLab 地址
     * @param token         用户 token
     * @return 请求结果
     */
    public static HttpResponse getGitLabVersion(String gitlabAddress, String token, String apiVersion) {
        // 参考：https://docs.gitlab.com/ee/api/version.html
        return HttpUtil.createGet(StrUtil.format("{}/api/{}/version", gitlabAddress, apiVersion), true)
            .header("PRIVATE-TOKEN", token)
            .execute();
    }

    /**
     * 获取 GitLab 版本信息
     *
     * @param url   GitLab 地址
     * @param token 用户 token
     */
    public static GitLabVersionInfo getGitLabVersionInfo(String url, String token) {
        // 缓存中有的话，从缓存读取
        GitLabVersionInfo gitLabVersionInfo = gitlabVersionMap.get(url);
        if (gitLabVersionInfo != null) {
            return gitLabVersionInfo;
        }

        // 获取 GitLab 版本号信息
        GitLabVersionInfo glvi = null;
        String apiVersion = "v4";
        HttpResponse v4 = getGitLabVersion(url, token, apiVersion);
        if (v4 != null) {
            glvi = JSON.parseObject(v4.body(), GitLabVersionInfo.class);
        } else {
            apiVersion = "v3";
            HttpResponse v3 = getGitLabVersion(url, token, apiVersion);
            if (v3 != null) {
                glvi = JSON.parseObject(v3.body(), GitLabVersionInfo.class);
            }
        }

        Assert.state(glvi != null, "获取 GitLab 版本号失败，请检查 GitLab 地址和 token 是否正确");

        // 添加到缓存中
        glvi.setApiVersion(apiVersion);
        gitlabVersionMap.put(url, glvi);

        return glvi;
    }

    /**
     * 获取 GitLab API 版本号
     *
     * @param url   GitLab 地址
     * @param token 用户 token
     * @return GitLab API 版本号，如：v4
     */
    public static String getGitLabApiVersion(String url, String token) {
        return getGitLabVersionInfo(url, token).getApiVersion();
    }

    /**
     * 获取 GitLab 用户信息
     *
     * @param gitlabAddress GitLab 地址
     * @param token         用户 token
     * @return 请求结果
     */
    public static HttpResponse getGitLabUserInfo(String gitlabAddress, String token) {
        // 参考：https://docs.gitlab.com/ee/api/users.html
        return HttpUtil.createGet(
                StrUtil.format(
                    "{}/api/{}/user",
                    gitlabAddress,
                    getGitLabApiVersion(gitlabAddress, token)
                ), true
            )
            .form("access_token", token)
            .timeout(5000)
            .execute();
    }

    /**
     * 获取 GitLab 仓库信息
     *
     * @param gitlabAddress GitLab 地址
     * @param token         用户 token
     * @return 响应结果
     */
    public static Map<String, Object> getGitLabRepos(String gitlabAddress, String token, Page page, String condition) {
        // 参考：https://docs.gitlab.com/ee/api/projects.html
        HttpResponse reposResponse = HttpUtil.createGet(
                StrUtil.format(
                    "{}/api/{}/projects",
                    gitlabAddress,
                    getGitLabApiVersion(gitlabAddress, token)
                ), true
            )
            .form("private_token", token)
            .form("membership", true)
            // 当 simple=true 时，不返回项目的 visibility，无法判断是私有仓库、公开仓库还是内部仓库
//                .form("simple", true)
            .form("order_by", "updated_at")
            .form("page", page.getPageNumber())
            .form("per_page", page.getPageSize())
            .form("search", condition)
            .execute();

        String body = reposResponse.body();
        Assert.state(reposResponse.isOk(), "拉取仓库信息错误：" + body);

        String totalCountStr = reposResponse.header("X-Total");
        int totalCount = Convert.toInt(totalCountStr, 0);
        //String totalPage = reposResponse.header("total_page");

        Map<String, Object> map = new HashMap<>(2);
        map.put("body", body);
        map.put("total", totalCount);

        return map;
    }
}
