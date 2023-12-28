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
package org.dromara.jpom.controller.system;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import org.dromara.jpom.common.BaseJpomController;
import org.dromara.jpom.model.data.AgentWhitelist;
import org.dromara.jpom.service.WhitelistDirectoryService;
import org.dromara.jpom.system.AgentConfig;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2019/4/16
 */
@RestController
@RequestMapping(value = "/system")
public class WhitelistDirectoryController extends BaseJpomController {

    private final WhitelistDirectoryService whitelistDirectoryService;
    private final AgentConfig agentConfig;

    public WhitelistDirectoryController(WhitelistDirectoryService whitelistDirectoryService,
                                        AgentConfig agentConfig) {
        this.whitelistDirectoryService = whitelistDirectoryService;
        this.agentConfig = agentConfig;
    }

    @RequestMapping(value = "whitelistDirectory_data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<AgentWhitelist> whiteListDirectoryData() {
        AgentWhitelist agentWhitelist = whitelistDirectoryService.getWhitelist();
        return JsonMessage.success("", agentWhitelist);
    }


    @PostMapping(value = "whitelistDirectory_submit", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<String> whitelistDirectorySubmit(String project,


                                                         String allowEditSuffix) {
        List<String> list = AgentWhitelist.parseToList(project, true, "项目路径授权不能为空");
        //
        List<String> allowEditSuffixList = AgentWhitelist.parseToList(allowEditSuffix, "允许编辑的文件后缀不能为空");
        return save(list, allowEditSuffixList);
    }


    private JsonMessage<String> save(List<String> projects,

                                     List<String> allowEditSuffixList) {
        List<String> projectArray;
        {
            projectArray = AgentWhitelist.covertToArray(projects, "项目路径授权不能位于Jpom目录下");
            String error = findStartsWith(projectArray, 0);
            Assert.isNull(error, "授权目录中不能存在包含关系：" + error);
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

        AgentWhitelist agentWhitelist = whitelistDirectoryService.getWhitelist();

        agentWhitelist.setProject(projectArray);
        agentWhitelist.setAllowEditSuffix(allowEditSuffixList);
        whitelistDirectoryService.saveWhitelistDirectory(agentWhitelist);
        return new JsonMessage<>(200, "保存成功");
    }

    /**
     * 检查授权包含关系
     *
     * @param jsonArray 要检查的对象
     * @param start     检查的坐标
     * @return null 正常
     */
    private String findStartsWith(List<String> jsonArray, int start) {
        if (jsonArray == null || !agentConfig.getWhitelist().isCheckStartsWith()) {
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
