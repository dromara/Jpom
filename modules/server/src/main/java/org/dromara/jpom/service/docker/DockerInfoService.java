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
package org.dromara.jpom.service.docker;

import cn.hutool.core.util.StrUtil;
import cn.hutool.db.sql.Condition;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.model.docker.DockerInfoModel;
import org.dromara.jpom.service.h2db.BaseWorkspaceService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;


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
     * 将节点信息同步到其他工作空间
     *
     * @param ids            多给节点ID
     * @param nowWorkspaceId 当前的工作空间ID
     * @param workspaceId    同步到哪个工作空间
     */
    public void syncToWorkspace(String ids, String nowWorkspaceId, String workspaceId) {
        StrUtil.splitTrim(ids, StrUtil.COMMA).forEach(id -> {
            DockerInfoModel data = super.getByKey(id, false, entity -> entity.set("workspaceId", nowWorkspaceId));
            Assert.notNull(data, "没有对应到docker信息");
            //
            DockerInfoModel where = new DockerInfoModel();
            where.setWorkspaceId(workspaceId);
            where.setMachineDockerId(data.getMachineDockerId());
            DockerInfoModel exits = super.queryByBean(where);
            Assert.isNull(exits, "对应工作空间已经存在对应的 docker 啦");
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
