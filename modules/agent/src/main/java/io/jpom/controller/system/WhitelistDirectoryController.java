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
package io.jpom.controller.system;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.RegexPool;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.common.BaseJpomController;
import io.jpom.model.data.AgentWhitelist;
import io.jpom.service.WhitelistDirectoryService;
import io.jpom.system.AgentExtConfigBean;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author jiangzeyin
 * @since 2019/4/16
 */
@RestController
@RequestMapping(value = "/system")
public class WhitelistDirectoryController extends BaseJpomController {

    private final WhitelistDirectoryService whitelistDirectoryService;

    public WhitelistDirectoryController(WhitelistDirectoryService whitelistDirectoryService) {
        this.whitelistDirectoryService = whitelistDirectoryService;
    }

    @RequestMapping(value = "whitelistDirectory_data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String whiteListDirectoryData() {
        AgentWhitelist agentWhitelist = whitelistDirectoryService.getWhitelist();
        return JsonMessage.getString(200, "", agentWhitelist);
    }


    @PostMapping(value = "whitelistDirectory_submit", produces = MediaType.APPLICATION_JSON_VALUE)
    public String whitelistDirectorySubmit(String project, String certificate, String nginx, String nginxPath, String allowEditSuffix, String allowRemoteDownloadHost) {
        List<String> list = AgentWhitelist.parseToList(project, true, "项目路径白名单不能为空");
        //
        List<String> certificateList = AgentWhitelist.parseToList(certificate, "证书路径白名单不能为空");
        List<String> nList = AgentWhitelist.parseToList(nginx, "nginx路径白名单不能为空");
        List<String> allowEditSuffixList = AgentWhitelist.parseToList(allowEditSuffix, "允许编辑的文件后缀不能为空");
        List<String> allowRemoteDownloadHostList = AgentWhitelist.parseToList(allowRemoteDownloadHost, "允许远程下载的 host 不能配置为空");
        return save(list, certificateList, nList, nginxPath, allowEditSuffixList, allowRemoteDownloadHostList).toString();
    }
//
//	private JsonMessage<String> save(String project, List<String> certificate, List<String> nginx, List<String> allowEditSuffixList) {
//
//		return save(list, certificate, nginx);
//	}


    private JsonMessage<String> save(List<String> projects,
                                     List<String> certificate,
                                     List<String> nginx,
                                     String nginxPath,
                                     List<String> allowEditSuffixList,
                                     List<String> allowRemoteDownloadHostList) {
        List<String> projectArray;
        {
            projectArray = AgentWhitelist.covertToArray(projects, "项目路径白名单不能位于Jpom目录下");
            String error = findStartsWith(projectArray, 0);
            Assert.isNull(error, "白名单目录中不能存在包含关系：" + error);
        }
        List<String> certificateArray = null;
        if (certificate != null && !certificate.isEmpty()) {
            certificateArray = AgentWhitelist.covertToArray(certificate, "证书路径白名单不能位于Jpom目录下");

            String error = findStartsWith(certificateArray, 0);
            Assert.isNull(error, "证书目录中不能存在包含关系：" + error);

        }
        List<String> nginxArray = null;
        if (nginx != null && !nginx.isEmpty()) {
            nginxArray = AgentWhitelist.covertToArray(nginx, "nginx路径白名单不能位于Jpom目录下");
            String error = findStartsWith(nginxArray, 0);
            Assert.isNull(error, "nginx目录中不能存在包含关系：" + error);
        }
        //
        if (CollUtil.isNotEmpty(allowEditSuffixList)) {
            for (String s : allowEditSuffixList) {
                List<String> split = StrUtil.split(s, StrUtil.AT);
                if (split.size() > 1) {
                    String last = CollUtil.getLast(split);
                    try {
                        CharsetUtil.charset(last);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("配置的字符编码格式不合法：" + s);
                    }
                }
            }
        }
        if (CollUtil.isNotEmpty(allowRemoteDownloadHostList)) {
            for (String s : allowRemoteDownloadHostList) {
                Assert.state(ReUtil.isMatch(RegexPool.URL_HTTP, s), "配置的远程地址不规范,请重新填写：" + s);
            }
        }

        AgentWhitelist agentWhitelist = whitelistDirectoryService.getWhitelist();
        agentWhitelist.setNginxPath(nginxPath);
        agentWhitelist.setProject(projectArray);
        agentWhitelist.setCertificate(certificateArray);
        agentWhitelist.setNginx(nginxArray);
        agentWhitelist.setAllowEditSuffix(allowEditSuffixList);
        agentWhitelist.setAllowRemoteDownloadHost(allowRemoteDownloadHostList == null ? null : CollUtil.newHashSet(allowRemoteDownloadHostList));
        whitelistDirectoryService.saveWhitelistDirectory(agentWhitelist);
        return new JsonMessage<>(200, "保存成功");
    }

    /**
     * 检查白名单包含关系
     *
     * @param jsonArray 要检查的对象
     * @param start     检查的坐标
     * @return null 正常
     */
    private String findStartsWith(List<String> jsonArray, int start) {
        if (jsonArray == null || !AgentExtConfigBean.getInstance().whitelistDirectoryCheckStartsWith) {
            return null;
        }
        String str = jsonArray.get(start);
        int len = jsonArray.size();
        for (int i = 0; i < len; i++) {
            if (i == start) {
                continue;
            }
            String findStr = jsonArray.get(i);
            if (findStr.startsWith(str)) {
                return str;
            }
        }
        if (start < len - 1) {
            return findStartsWith(jsonArray, start + 1);
        }
        return null;
    }
}
