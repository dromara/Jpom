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
package io.jpom.model.data;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import io.jpom.build.BuildUtil;
import io.jpom.model.BaseEnum;
import io.jpom.model.BaseWorkspaceModel;
import io.jpom.model.enums.GitProtocolEnum;
import io.jpom.service.h2db.TableName;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Hotstrip
 * 仓库地址实体类
 */
@TableName(value = "REPOSITORY", name = "仓库信息")
public class RepositoryModel extends BaseWorkspaceModel {
    /**
     * 名称
     */
    private String name;
    /**
     * 仓库地址
     */
    private String gitUrl;
    /**
     * 仓库类型{0: GIT, 1: SVN}
     */
    private Integer repoType;
    /**
     * 拉取代码的协议{0: http, 1: ssh}
     *
     * @see GitProtocolEnum
     */
    private Integer protocol;
    /**
     * 登录用户
     */
    private String userName;
    /**
     * 登录密码
     */
    private String password;
    /**
     * SSH RSA 公钥
     */
    @Deprecated
    private String rsaPub;
    /**
     * SSH RSA 私钥
     */
    private String rsaPrv;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGitUrl() {
        return gitUrl;
    }

    public void setGitUrl(String gitUrl) {
        this.gitUrl = gitUrl;
    }

    public Integer getRepoType() {
        return repoType;
    }

    public void setRepoType(Integer repoType) {
        this.repoType = repoType;
    }

    /**
     * 返回协议类型，如果为 null 会尝试识别 http
     *
     * @return 枚举的值（1/0）
     * @see GitProtocolEnum
     */
    public Integer getProtocol() {
        if (protocol != null) {
            return protocol;
        }
        String gitUrl = this.getGitUrl();
        if (StrUtil.isEmpty(gitUrl)) {
            return null;
        }
        if (HttpUtil.isHttps(gitUrl) || HttpUtil.isHttp(gitUrl)) {
            return GitProtocolEnum.HTTP.getCode();
        }
        return null;
    }

    public void setProtocol(Integer protocol) {
        this.protocol = protocol;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Deprecated
    public String getRsaPub() {
        return rsaPub;
    }

    @Deprecated
    public void setRsaPub(String rsaPub) {
        this.rsaPub = rsaPub;
    }

    public String getRsaPrv() {
        return rsaPrv;
    }

    public void setRsaPrv(String rsaPrv) {
        this.rsaPrv = rsaPrv;
    }

    /**
     * 转换为 map
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>(10);
        map.put("url", this.getGitUrl());
        map.put("protocol", this.getProtocol());
        map.put("username", this.getUserName());
        map.put("password", this.getPassword());
        map.put("rsaFile", BuildUtil.getRepositoryRsaFile(this));
        return map;
    }

    /**
     * 仓库类型
     */
    public enum RepoType implements BaseEnum {
        /**
         * git
         */
        Git(0, "Git"),
        Svn(1, "Svn"),
        ;
        private final int code;
        private final String desc;

        RepoType(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        @Override
        public int getCode() {
            return code;
        }

        @Override
        public String getDesc() {
            return desc;
        }
    }
}
