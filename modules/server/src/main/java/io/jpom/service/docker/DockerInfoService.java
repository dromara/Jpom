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
package io.jpom.service.docker;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.db.Entity;
import cn.hutool.db.sql.Condition;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.Const;
import io.jpom.cron.CronUtils;
import io.jpom.cron.IAsyncLoad;
import io.jpom.model.data.UserModel;
import io.jpom.model.docker.DockerInfoModel;
import io.jpom.model.docker.DockerSwarmInfoMode;
import io.jpom.plugin.IPlugin;
import io.jpom.plugin.PluginFactory;
import io.jpom.service.h2db.BaseWorkspaceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @author bwcx_jzy
 * @since 2022/1/26
 */
@Service
@Slf4j
public class DockerInfoService extends BaseWorkspaceService<DockerInfoModel> implements IAsyncLoad, Task {

    private static final String CRON_ID = "docker-monitor";

    public static final String DOCKER_CHECK_PLUGIN_NAME = "docker-cli:check";

    public static final String DOCKER_PLUGIN_NAME = "docker-cli";

    private final DockerSwarmInfoService dockerSwarmInfoService;

    public DockerInfoService(DockerSwarmInfoService dockerSwarmInfoService) {
        this.dockerSwarmInfoService = dockerSwarmInfoService;
    }

    @Override
    public void startLoad() {
        CronUtils.add(CRON_ID, "0 0/1 * * * ?", () -> DockerInfoService.this);
        //
        localLocalDocker();
    }

    private void localLocalDocker() {
        try {
            IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
            String dockerHost = (String) plugin.execute("testLocal", new HashMap<>(1));
            Entity entity = Entity.create();
            entity.set("host", dockerHost);
            entity.set("workspaceId", Const.WORKSPACE_DEFAULT_ID);
            boolean exists = this.exists(entity);
            if (exists) {
                return;
            }
            DockerInfoModel.DockerInfoModelBuilder builder = DockerInfoModel.builder();
            builder.host(dockerHost).name("localhost").status(1);
            DockerInfoModel dockerInfoModel = builder.build();
            dockerInfoModel.setWorkspaceId(Const.WORKSPACE_DEFAULT_ID);
            dockerInfoModel.setModifyUser(UserModel.SYSTEM_ADMIN);
            this.insert(dockerInfoModel);
            Console.log("Automatically add local docker host: {}", dockerHost);
        } catch (Throwable e) {
            Console.error("There is no docker service local {}", e.getMessage());
        }
    }

    @Override
    public void execute() {
        List<DockerInfoModel> list = this.list();
        if (CollUtil.isEmpty(list)) {
            return;
        }
        this.checkList(list);
    }

    private void checkList(List<DockerInfoModel> monitorModels) {
        monitorModels.forEach(monitorModel -> ThreadUtil.execute(() -> DockerInfoService.this.updateMonitor(monitorModel)));
    }

    /**
     * 监控 容器
     *
     * @param dockerInfoModel docker
     */
    public boolean updateMonitor(DockerInfoModel dockerInfoModel) {
        try {
            IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
            Map<String, Object> parameter = dockerInfoModel.toParameter();
            parameter.put("timeout", dockerInfoModel.getHeartbeatTimeout());
            //
            JSONObject info = plugin.execute("info", parameter, JSONObject.class);
            //
            DockerInfoModel update = new DockerInfoModel();
            update.setId(dockerInfoModel.getId());
            update.setStatus(1);
            update.setLastHeartbeatTime(SystemClock.now());
            //
            update.setDockerVersion(info.getString("serverVersion"));
            JSONObject swarm = info.getJSONObject("swarm");
            if (swarm != null) {
                String nodeId = swarm.getString("nodeID");
                update.setSwarmNodeId(nodeId);
                if (StrUtil.isEmpty(nodeId)) {
                    // 集群退出
                    update.setSwarmId(StrUtil.EMPTY);
                }
            } else {
                update.setSwarmNodeId(StrUtil.EMPTY);
                update.setSwarmId(StrUtil.EMPTY);
            }
            update.setFailureMsg(StrUtil.EMPTY);
            super.update(update);
            //
            this.updateSwarmStatus(dockerInfoModel.getId(), update.getStatus(), update.getFailureMsg());
            return true;
        } catch (Exception e) {
            log.error("监控 docker 异常", e);
            this.updateStatus(dockerInfoModel.getId(), 0, e.getMessage());
            return false;
        }
    }

    /**
     * 更新 容器状态
     *
     * @param id     ID
     * @param status 状态值
     * @param msg    错误消息
     */
    private void updateStatus(String id, int status, String msg) {
        DockerInfoModel dockerInfoModel = new DockerInfoModel();
        dockerInfoModel.setId(id);
        dockerInfoModel.setStatus(status);
        dockerInfoModel.setFailureMsg(msg);
        super.update(dockerInfoModel);
        //
        this.updateSwarmStatus(id, status, msg);
    }

    private void updateSwarmStatus(String id, int status, String msg) {
        //
        DockerSwarmInfoMode dockerSwarmInfoMode = new DockerSwarmInfoMode();
        dockerSwarmInfoMode.setStatus(status);
        dockerSwarmInfoMode.setFailureMsg(msg);
        dockerSwarmInfoService.update(dockerSwarmInfoService.dataBeanToEntity(dockerSwarmInfoMode), Entity.create().set("dockerId", id));
    }

    /**
     * 根据 tag 查询 容器
     *
     * @param workspaceId 工作空间
     * @param tag         tag
     * @param status      状态
     * @return list
     */
    public List<DockerInfoModel> queryByTag(String workspaceId, Integer status, String tag) {
        Condition workspaceIdCondition = new Condition("workspaceId", workspaceId);
        Condition statusCondition = new Condition("status", status);
        if (StrUtil.isEmpty(tag)) {
            return super.findByCondition(workspaceIdCondition, statusCondition);
        } else {
            Condition tagCondition = new Condition(" instr(tags,'" + tag + "')", "");
            tagCondition.setPlaceHolder(false);
            tagCondition.setOperator("");
            return super.findByCondition(workspaceIdCondition, statusCondition, tagCondition);
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
        return (int) super.count(sql, workspaceId, tag);
    }

    /**
     * docker 绑定集群
     *
     * @param joinSwarmDocker docker
     * @param tag             标签
     * @param swarm           集群信息
     * @param swarmId         集群ID
     */
    public void bindDockerSwarm(DockerInfoModel joinSwarmDocker, String tag, JSONObject swarm, String swarmId) {
        DockerInfoModel dockerInfoModel = new DockerInfoModel();
        dockerInfoModel.setSwarmId(swarmId);
        //
        if (swarm != null) {
            String swarmNodeId = swarm.getString("nodeID");
            dockerInfoModel.setSwarmNodeId(swarmNodeId);
        }
        dockerInfoModel.setId(joinSwarmDocker.getId());
        this.update(dockerInfoModel);
    }

    /**
     * 更新集群 标签
     *
     * @param swarmId 集群ID
     * @param tag     新增标签
     * @param delTag  删除标签
     */
    public void updateDockerSwarmTag(String swarmId, String tag, String delTag) {
        DockerInfoModel queryWhere = new DockerInfoModel();
        queryWhere.setSwarmId(swarmId);
        List<DockerInfoModel> dockerInfoModels = this.listByBean(queryWhere);
        for (DockerInfoModel dockerInfoModel : dockerInfoModels) {
            // 处理标签
            Collection<String> allTag = StrUtil.splitTrim(dockerInfoModel.getTags(), StrUtil.COLON);
            allTag = ObjectUtil.defaultIfNull(allTag, new ArrayList<>());
            if (StrUtil.isNotEmpty(delTag)) {
                allTag.remove(delTag);
            }
            if (!allTag.contains(tag)) {
                allTag.add(tag);
            }
            allTag = allTag.stream().filter(StrUtil::isNotEmpty).collect(Collectors.toSet());
            String newTags = CollUtil.join(allTag, StrUtil.COLON, StrUtil.COLON, StrUtil.COLON);
            //
            Entity where = Entity.create().set("id", dockerInfoModel.getId());
            Entity update = Entity.create().set("tags", newTags);
            this.update(update, where);
        }

    }

    /**
     * 解绑集群信息
     *
     * @param id docker id
     */
    public void unbind(String id) {
        DockerInfoModel update = new DockerInfoModel();
        update.setId(id);
        update.setSwarmId(StrUtil.EMPTY);
        update.setSwarmNodeId(StrUtil.EMPTY);
        this.update(update);
    }

    /**
     * 获取集群 docker 信息
     *
     * @param id      集群ID
     * @param request 请求对象
     * @return map
     */
    public Map<String, Object> getBySwarmPluginMap(String id, HttpServletRequest request) {
        DockerSwarmInfoMode swarmInfoMode1 = dockerSwarmInfoService.getByKey(id, request);
        Assert.notNull(swarmInfoMode1, "没有对应的集群");
        //
        DockerInfoModel managerSwarmDocker = this.getByKey(swarmInfoMode1.getDockerId(), request);
        Assert.notNull(managerSwarmDocker, "对应的 docker 不存在");
        return managerSwarmDocker.toParameter();
    }

    /**
     * 获取集群 docker 信息
     *
     * @param id 集群ID
     * @return map
     */
    public Map<String, Object> getBySwarmPluginMap(String id) {
        DockerSwarmInfoMode swarmInfoMode1 = dockerSwarmInfoService.getByKey(id);
        Assert.notNull(swarmInfoMode1, "没有对应的集群");
        //
        DockerInfoModel managerSwarmDocker = this.getByKey(swarmInfoMode1.getDockerId());
        Assert.notNull(managerSwarmDocker, "对应的 docker 不存在");
        return managerSwarmDocker.toParameter();
    }
}
