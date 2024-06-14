/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.system;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import org.dromara.jpom.common.BaseJpomController;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.data.AgentWhitelist;
import org.dromara.jpom.service.WhitelistDirectoryService;
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

    public WhitelistDirectoryController(WhitelistDirectoryService whitelistDirectoryService) {
        this.whitelistDirectoryService = whitelistDirectoryService;
    }

    @RequestMapping(value = "whitelistDirectory_data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<AgentWhitelist> whiteListDirectoryData() {
        AgentWhitelist agentWhitelist = whitelistDirectoryService.getWhitelist();
        return JsonMessage.success("", agentWhitelist);
    }


    @PostMapping(value = "whitelistDirectory_submit", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<String> whitelistDirectorySubmit(String project,


                                                         String allowEditSuffix) {
        List<String> list = AgentWhitelist.parseToList(project, true, I18nMessageUtil.get("i18n.project_path_auth_required.9e58"));
        //
        List<String> allowEditSuffixList = AgentWhitelist.parseToList(allowEditSuffix, I18nMessageUtil.get("i18n.suffix_cannot_be_empty.ec72"));
        return save(list, allowEditSuffixList);
    }


    private JsonMessage<String> save(List<String> projects,

                                     List<String> allowEditSuffixList) {
        List<String> projectArray;
        {
            projectArray = AgentWhitelist.covertToArray(projects, I18nMessageUtil.get("i18n.project_path_auth_not_under_jpom.0e18"));
            String error = findStartsWith(projectArray);
            Assert.isNull(error, I18nMessageUtil.get("i18n.auth_directory_cannot_contain_hierarchy.d6ca") + error);
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
                        throw new IllegalArgumentException(I18nMessageUtil.get("i18n.illegal_character_encoding_format.af7a") + s);
                    }
                }
            }
        }

        AgentWhitelist agentWhitelist = whitelistDirectoryService.getWhitelist();

        agentWhitelist.setProject(projectArray);
        agentWhitelist.setAllowEditSuffix(allowEditSuffixList);
        whitelistDirectoryService.saveWhitelistDirectory(agentWhitelist);
        return new JsonMessage<>(200, I18nMessageUtil.get("i18n.save_succeeded.3b10"));
    }

    /**
     * 检查授权包含关系
     *
     * @param jsonArray 要检查的对象
     * @return null 正常
     */
    private String findStartsWith(List<String> jsonArray) {
        return AgentWhitelist.findStartsWith(jsonArray);
    }
}
