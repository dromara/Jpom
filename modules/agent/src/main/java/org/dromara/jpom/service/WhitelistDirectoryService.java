/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.AgentConst;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.data.AgentWhitelist;
import org.dromara.jpom.util.JsonFileUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 授权服务
 *
 * @author bwcx_jzy
 * @since 2019/2/28
 */
@Service
@Slf4j
public class WhitelistDirectoryService extends BaseOperService<AgentWhitelist> {

    public WhitelistDirectoryService() {
        super(AgentConst.WHITELIST_DIRECTORY);
    }

    /**
     * 获取授权信息配置、如何没有配置或者配置错误将返回新对象
     *
     * @return AgentWhitelist
     */
    public AgentWhitelist getWhitelist() {
        try {
            JSONObject jsonObject = getJSONObject();
            if (jsonObject == null) {
                return new AgentWhitelist();
            }
            return jsonObject.toJavaObject(AgentWhitelist.class);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return new AgentWhitelist();
    }

    /**
     * 单项添加授权
     *
     * @param item 授权
     */
    public void addProjectWhiteList(String item) {
        ArrayList<String> list = CollUtil.newArrayList(item);
        List<String> checkOk = AgentWhitelist.covertToArray(list, I18nMessageUtil.get("i18n.project_path_auth_not_under_jpom.0e18"));

        AgentWhitelist agentWhitelist = getWhitelist();
        List<String> project = agentWhitelist.getProject();
        project = ObjectUtil.defaultIfNull(project, new ArrayList<>());
        project = CollUtil.addAll(project, checkOk)
            .stream()
            .distinct()
            .collect(Collectors.toList());
        agentWhitelist.setProject(project);
        saveWhitelistDirectory(agentWhitelist);
    }

    public boolean checkProjectDirectory(String path) {
        AgentWhitelist agentWhitelist = getWhitelist();
        List<String> list = agentWhitelist.getProject();
        return AgentWhitelist.checkPath(list, path);
    }


    /**
     * 保存授权
     *
     * @param jsonObject 实体
     */
    public void saveWhitelistDirectory(AgentWhitelist jsonObject) {
        String path = getDataFilePath(AgentConst.WHITELIST_DIRECTORY);
        JsonFileUtil.saveJson(path, jsonObject.toJson());
    }
}
