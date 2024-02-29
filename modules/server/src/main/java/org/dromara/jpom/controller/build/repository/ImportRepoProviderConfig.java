/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
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
