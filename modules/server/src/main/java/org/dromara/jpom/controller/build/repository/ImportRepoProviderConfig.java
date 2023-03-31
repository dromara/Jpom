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
package org.dromara.jpom.controller.build.repository;

import lombok.Data;

import java.util.Map;

/**
 * 仓库提供商配置
 *
 * @author WeiHongBin
 */
@Data
public class ImportRepoProviderConfig {
    private String baseUrl;
    /**
     * 鉴权方式 1:header 2:from 3:body
     */
    private Integer authType;
    /**
     * 鉴权key 例如：Authorization
     */
    private String authKey;
    /**
     * 鉴权值 例如：Bearer ${token}
     */
    private String authValue;
    /**
     * 扩展参数
     */
    private Map<String, String> extraParams;
    /**
     * 扩展参数类型 1:header 2:from 3:body
     */
    private Integer extraParamsType;
    /**
     * 获取用户信息的请求方式
     */
    private String currentUserMethod;
    /**
     * 获取用户信息的请求地址
     */
    private String currentUserUrl;
    /**
     * 获取用户名 path
     */
    private String userNamePath;
    /**
     * 获取仓库列表的请求方式
     */
    private String repoListMethod;
    /**
     * 获取仓库列表的请求地址
     */
    private String repoListUrl;
    /**
     * 获取仓库列表的请求参数
     */
    private Map<String, String> repoListParam;
    /**
     * 获取仓库列表数组 path
     */
    private String repoListPath;
    /**
     * 仓库信息 转换 path
     */
    private Map<String, String> repoConvertPath;
    /**
     * 获取仓库总数 X-Total
     */
    private String repoTotalHeader;

}
