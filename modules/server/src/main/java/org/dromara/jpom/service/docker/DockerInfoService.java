/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.docker;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.sql.Condition;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.docker.DockerInfoModel;
import org.dromara.jpom.service.h2db.BaseWorkspaceService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * @author bwcx_jzy
 * @since 2022/1/26
 */
@Service
@Slf4j
public class DockerInfoService extends BaseWorkspaceService<DockerInfoModel> {


    public static final String DOCKER_CHECK_PLUGIN_NAME = "docker-cli:check";

    public static final String DOCKER_PLUGIN_NAME = "docker-cli";

    @Override
    protected void fillSelectResult(DockerInfoModel data) {
        //data.setRegistryPassword(null);
    }

    @Override
    protected void fillInsert(DockerInfoModel dockerInfoModel) {
        super.fillInsert(dockerInfoModel);
    }

    /**
     * 根据 tag 查询 容器
     *
     * @param workspaceId 工作空间
     * @param tag         tag
     * @return list
     */
    public List<DockerInfoModel> queryByTag(String workspaceId, String tag) {
        Condition workspaceIdCondition = new Condition("workspaceId", workspaceId);
        if (StrUtil.isEmpty(tag)) {
            return super.findByCondition(workspaceIdCondition);
        } else {
            Condition tagCondition = new Condition(" instr(tags,'" + StrUtil.wrap(tag, StrUtil.COLON) + "')", "");
            tagCondition.setPlaceHolder(false);
            tagCondition.setOperator("");
            return super.findByCondition(workspaceIdCondition, tagCondition);
        }
    }

    /**
     * 根据 tag 查询 容器
     *
     * @param workspaceId 工作空间
     * @param tag         tag
     * @return count
     */
    public int countByTag(String workspaceId, String tag) {
        String sql = StrUtil.format("SELECT * FROM {} where workspaceId=? and instr(tags,?)", super.getTableName());
        return (int) super.count(sql, workspaceId, StrUtil.wrap(tag, StrUtil.COLON));
    }

    /**
     * 根据 tag 查询 容器
     *
     * @param workspaceId 工作空间
     * @return count
     */
    public List<String> allTag(String workspaceId) {
        String sql = StrUtil.format("SELECT tags FROM {} where workspaceId=?", super.getTableName());
        List<Entity> query = super.query(sql, workspaceId);
        if (CollUtil.isEmpty(query)) {
            return new ArrayList<>();
        }
        return query.stream()
            .map(entity -> entity.getStr("tags"))
            .flatMap((Function<String, Stream<String>>) s -> StrUtil.splitTrim(s, StrUtil.COLON).stream())
            .filter(StrUtil::isNotEmpty)
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * 将节点信息同步到其他工作空间
     *
     * @param ids            多给节点ID
     * @param nowWorkspaceId 当前的工作空间ID
     * @param workspaceId    同步到哪个工作空间
     */
    public void syncToWorkspace(String ids, String nowWorkspaceId, String workspaceId) {
        StrUtil.splitTrim(ids, StrUtil.COMMA).forEach(id -> {
            DockerInfoModel data = super.getByKey(id, false, entity -> entity.set("workspaceId", nowWorkspaceId));
            Assert.notNull(data, I18nMessageUtil.get("i18n.no_docker_details.3343"));
            //
            DockerInfoModel where = new DockerInfoModel();
            where.setWorkspaceId(workspaceId);
            where.setMachineDockerId(data.getMachineDockerId());
            DockerInfoModel exits = super.queryByBean(where);
            Assert.isNull(exits, I18nMessageUtil.get("i18n.docker_already_exists_in_workspace.a0de"));
            // 不存在则添加节点
            data.setId(null);
            data.setWorkspaceId(workspaceId);
            data.setCreateTimeMillis(null);
            data.setModifyTimeMillis(null);
            data.setModifyUser(null);
            // 集群 不同步
            data.setSwarmId(null);
            data.setSwarmNodeId(null);
            this.insert(data);
        });
    }
}
