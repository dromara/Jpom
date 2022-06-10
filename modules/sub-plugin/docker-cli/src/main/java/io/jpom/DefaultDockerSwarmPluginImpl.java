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
package io.jpom;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.unit.DataSize;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.command.RemoveSwarmNodeCmdImpl;
import io.jpom.plugin.IDefaultPlugin;
import io.jpom.plugin.PluginConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * docker swarm
 *
 * @author bwcx_jzy
 * @since 2022/2/13
 */
@PluginConfig(name = "docker-cli:swarm")
@Slf4j
public class DefaultDockerSwarmPluginImpl implements IDefaultPlugin {

    @Override
    public Object execute(Object main, Map<String, Object> parameter) {
        String type = main.toString();
        switch (type) {
            case "inSpectSwarm":
                return this.inSpectSwarmCmd(parameter);
            case "tryInitializeSwarm":
                return this.tryInitializeSwarmCmd(parameter);
            case "joinSwarm":
                this.joinSwarmCmd(parameter);
                return null;
            case "listSwarmNodes":
                return this.listSwarmNodesCmd(parameter);
            case "leaveSwarm":
                this.leaveSwarmCmd(parameter);
                return null;
            case "updateSwarmNode":
                this.updateSwarmNodeCmd(parameter);
                return null;
            case "removeSwarmNode":
                this.removeSwarmNodeCmd(parameter);
                return null;
            case "listServices":
                return this.listServicesCmd(parameter);
            case "listTasks":
                return this.listTasksCmd(parameter);
            case "removeService":
                this.removeServiceCmd(parameter);
                return null;
            case "updateService":
                this.updateServiceCmd(parameter);
                return null;
            case "updateServiceImage":
                this.updateServiceImage(parameter);
                return null;
            case "logService":
                this.logServiceCmd(parameter);
                return null;
            case "logTask":
                this.logTaskCmd(parameter);
                return null;
            default:
                break;
        }
        return null;
    }

    private void logTaskCmd(Map<String, Object> parameter) {
        this.logServiceCmd(parameter, (String) parameter.get("taskId"), "task");
    }

    private void logServiceCmd(Map<String, Object> parameter) {
        this.logServiceCmd(parameter, (String) parameter.get("serviceId"), "service");
    }

    private void logServiceCmd(Map<String, Object> parameter, String id, String type) {
        DockerClient dockerClient = DockerUtil.build(parameter);
        Consumer<String> consumer = (Consumer<String>) parameter.get("consumer");
        try {

            LogSwarmObjectCmd logSwarmObjectCmd = StrUtil.equalsIgnoreCase(type, "Service") ? dockerClient.logServiceCmd(id) : dockerClient.logTaskCmd(id);

            Charset charset = (Charset) parameter.get("charset");
            Integer tail = (Integer) parameter.get("tail");

            // 获取日志
            if (tail != null && tail > 1) {
                logSwarmObjectCmd.withTail(tail);
            }
            logSwarmObjectCmd.withDetails(true).withStderr(true)
                .withFollow(true)
                .withStdout(true).exec(new ResultCallback.Adapter<Frame>() {
                    @Override
                    public void onNext(Frame object) {
                        byte[] payload = object.getPayload();
                        if (payload == null) {
                            return;
                        }
                        String s = new String(payload, charset);
                        consumer.accept(s);
                    }
                }).awaitCompletion();
        } catch (InterruptedException e) {
            consumer.accept("获取容器日志被中断:" + e);
        } finally {
            IoUtil.close(dockerClient);
        }
    }

    private ServiceSpec intServiceSpec(DockerClient dockerClient, String serviceId) {
        if (StrUtil.isEmpty(serviceId)) {
            return new ServiceSpec();
        }
        // 读取之前的信息-保留之前的信息-否则会全部替换
        InspectServiceCmd inspectServiceCmd = dockerClient.inspectServiceCmd(serviceId);
        Service service = inspectServiceCmd.exec();
        ServiceSpec spec = service.getSpec();
        return ObjectUtil.defaultIfNull(spec, new ServiceSpec());
    }

    public void updateServiceImage(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.build(parameter);
        try {
            String serviceId = (String) parameter.get("serviceId");
            String image = (String) parameter.get("image");
            //
            InspectServiceCmd inspectServiceCmd = dockerClient.inspectServiceCmd(serviceId);
            Service service = inspectServiceCmd.exec();
            ServiceSpec spec = service.getSpec();
            Assert.notNull(spec, "服务信息不完整不能操作");
            TaskSpec taskTemplate = spec.getTaskTemplate();
            Assert.notNull(taskTemplate, "服务信息不完整不能操作：-1");
            ContainerSpec templateContainerSpec = taskTemplate.getContainerSpec();
            Assert.notNull(templateContainerSpec, "服务信息不完整不能操作：-2");
            templateContainerSpec.withImage(image);
            //
            UpdateServiceCmd updateServiceCmd = dockerClient.updateServiceCmd(serviceId, spec);
            ResourceVersion version = service.getVersion();
            Assert.notNull(version, "服务信息不完整不能操作：-3");
            updateServiceCmd.withVersion(version.getIndex());
            updateServiceCmd.exec();
        } finally {
            IoUtil.close(dockerClient);
        }
    }

    /**
     * 更新 服务，如果不存在 id 则创建。需要传人版本号
     *
     * @param parameter 测试
     */
    public void updateServiceCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.build(parameter);
        try {
            String serviceId = (String) parameter.get("serviceId");
            ServiceSpec serviceSpec = this.intServiceSpec(dockerClient, serviceId);
            String name = (String) parameter.get("name");
            serviceSpec.withName(name);
            {
                String mode = (String) parameter.get("mode");
                ServiceMode serviceMode = EnumUtil.fromString(ServiceMode.class, mode);
                ServiceModeConfig serviceModeConfig = new ServiceModeConfig();
                if (serviceMode == ServiceMode.GLOBAL) {
                    serviceModeConfig.withGlobal(new ServiceGlobalModeOptions());
                } else if (serviceMode == ServiceMode.REPLICATED) {
                    Object replicas = parameter.get("replicas");
                    ServiceReplicatedModeOptions serviceReplicatedModeOptions = new ServiceReplicatedModeOptions();
                    serviceReplicatedModeOptions.withReplicas(Convert.toInt(replicas, 1));
                    serviceModeConfig.withReplicated(serviceReplicatedModeOptions);
                }
                serviceSpec.withMode(serviceModeConfig);
            }
            {
                TaskSpec taskSpec = ObjectUtil.defaultIfNull(serviceSpec.getTaskTemplate(), new TaskSpec());
                //
                ContainerSpec containerSpec = this.buildContainerSpec(parameter, taskSpec.getContainerSpec());
                taskSpec.withContainerSpec(containerSpec);
                //
                Map<String, Map<String, Object>> resources = (Map<String, Map<String, Object>>) parameter.get("resources");

                if (MapUtil.isNotEmpty(resources)) {
                    ResourceRequirements resourceRequirements = new ResourceRequirements();
                    ResourceSpecs limitsResourceSpecs = this.buildResourceSpecs(resources.get("limits"));
                    if (limitsResourceSpecs != null) {
                        resourceRequirements.withLimits(limitsResourceSpecs);
                    }
                    ResourceSpecs reservationsResourceSpecs = this.buildResourceSpecs(resources.get("reservations"));
                    if (reservationsResourceSpecs != null) {
                        resourceRequirements.withReservations(reservationsResourceSpecs);
                    }
                    if (ObjectUtil.isAllEmpty(resourceRequirements.getLimits(), resourceRequirements.getReservations())) {
                        taskSpec.withResources(null);
                    } else {
                        taskSpec.withResources(resourceRequirements);
                    }
                }
                serviceSpec.withTaskTemplate(taskSpec);
            }
            {
                EndpointSpec endpointSpec = this.buildEndpointSpec(parameter);
                serviceSpec.withEndpointSpec(endpointSpec);
            }
            {
                Map<String, Object> update = (Map<String, Object>) parameter.get("update");
                UpdateConfig updateConfig = this.buildUpdateConfig(update);
                serviceSpec.withUpdateConfig(updateConfig);
                Map<String, Object> rollback = (Map<String, Object>) parameter.get("rollback");
                UpdateConfig rollbackConfig = this.buildUpdateConfig(rollback);
                serviceSpec.withRollbackConfig(rollbackConfig);
            }
            String version = (String) parameter.get("version");
            if (StrUtil.isNotEmpty(serviceId)) {
                UpdateServiceCmd updateServiceCmd = dockerClient.updateServiceCmd(serviceId, serviceSpec);
                updateServiceCmd.withVersion(Convert.toLong(version, 0L));
                updateServiceCmd.exec();
            } else {
                CreateServiceCmd createServiceCmd = dockerClient.createServiceCmd(serviceSpec);
                createServiceCmd.exec();
            }
        } finally {
            IoUtil.close(dockerClient);
        }
    }

    private ResourceSpecs buildResourceSpecs(Map<String, Object> map) {
        if (MapUtil.isNotEmpty(map)) {
            ResourceSpecs resourceSpecs = new ResourceSpecs();
            Object nanoCpus = map.get("nanoCPUs");
            if (nanoCpus != null) {
                String text = nanoCpus.toString();
                if (StrUtil.isNotEmpty(text)) {
                    resourceSpecs.withNanoCPUs(Convert.toLong(nanoCpus, 1L));
                }
            }
            Object memoryBytes = map.get("memoryBytes");
            if (memoryBytes != null) {
                String text = memoryBytes.toString();
                if (StrUtil.isNotEmpty(text)) {
                    DataSize dataSize = DataSize.parse(text);
                    resourceSpecs.withMemoryBytes(dataSize.toBytes());
                }
            }
            if (ObjectUtil.isAllEmpty(resourceSpecs.getNanoCPUs(), resourceSpecs.getMemoryBytes())) {
                return null;
            }
            return resourceSpecs;
        }
        return null;
    }

    private EndpointSpec buildEndpointSpec(Map<String, Object> parameter) {
        String endpointResolutionModeStr = (String) parameter.get("endpointResolutionMode");
        EndpointResolutionMode endpointResolutionMode = EnumUtil.fromString(EndpointResolutionMode.class, endpointResolutionModeStr);
        EndpointSpec endpointSpec = new EndpointSpec();
        endpointSpec.withMode(endpointResolutionMode);
        Collection<Map<String, Object>> exposedPorts = (Collection) parameter.get("exposedPorts");
        if (CollUtil.isNotEmpty(exposedPorts)) {
            List<PortConfig> portConfigs = exposedPorts.stream()
                .map(stringStringMap -> {
                    Object port = stringStringMap.get("targetPort");
                    Object publicPort = stringStringMap.get("publishedPort");
                    if (ObjectUtil.hasEmpty(port, publicPort)) {
                        return null;
                    }
                    PortConfig portConfig = new PortConfig();
                    String mode = (String) stringStringMap.get("publishMode");
                    PortConfig.PublishMode publishMode = EnumUtil.fromString(PortConfig.PublishMode.class, mode);
                    portConfig.withPublishMode(publishMode);
                    String scheme = (String) stringStringMap.get("protocol");
                    PortConfigProtocol protocol = EnumUtil.fromString(PortConfigProtocol.class, scheme);
                    portConfig.withProtocol(protocol);
                    portConfig.withTargetPort(Convert.toInt(port, 0));
                    portConfig.withPublishedPort(Convert.toInt(publicPort, 0));
                    return portConfig;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            endpointSpec.withPorts(portConfigs);
        }
        return endpointSpec;
    }

    private ContainerSpec buildContainerSpec(Map<String, Object> parameter, ContainerSpec oldContainerSpec) {
        String image = (String) parameter.get("image");
        ContainerSpec containerSpec = ObjectUtil.defaultIfNull(oldContainerSpec, new ContainerSpec());
        //new ContainerSpec();
        containerSpec.withImage(image);
        //
        Collection<Map<String, String>> args = (Collection) parameter.get("args");
        if (CollUtil.isNotEmpty(args)) {
            List<String> value = args.stream()
                .map(stringStringMap -> stringStringMap.get("value"))
                .filter(StrUtil::isNotEmpty)
                .collect(Collectors.toList());
            containerSpec.withArgs(value);
        }
        Collection<Map<String, String>> envs = (Collection) parameter.get("envs");
        if (CollUtil.isNotEmpty(envs)) {
            List<String> value = envs.stream()
                .map(stringStringMap -> {
                    String name1 = stringStringMap.get("name");
                    String value1 = stringStringMap.get("value");
                    if (StrUtil.isEmpty(name1)) {
                        return null;
                    }
                    return StrUtil.format("{}={}", name1, value1);
                })
                .filter(StrUtil::isNotEmpty)
                .collect(Collectors.toList());
            containerSpec.withEnv(value);
        }
        Collection<Map<String, String>> commands = (Collection) parameter.get("commands");
        if (CollUtil.isNotEmpty(commands)) {
            List<String> value = commands.stream()
                .map(stringStringMap -> stringStringMap.get("value"))
                .filter(StrUtil::isNotEmpty)
                .collect(Collectors.toList());
            containerSpec.withCommand(value);
        }
        //
        Collection<Map<String, String>> volumes = (Collection<Map<String, String>>) parameter.get("volumes");
        if (CollUtil.isNotEmpty(volumes)) {
            List<Mount> value = volumes.stream()
                .map(stringStringMap -> {
                    String source = stringStringMap.get("source");
                    String target = stringStringMap.get("target");
                    if (StrUtil.hasBlank(source, target)) {
                        return null;
                    }
                    String type = stringStringMap.get("type");
                    MountType mountType = EnumUtil.fromString(MountType.class, type);
                    Mount mount = new Mount();
                    mount.withSource(source);
                    mount.withTarget(target);
                    mount.withType(mountType);
                    return mount;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            containerSpec.withMounts(value);
        }
        return containerSpec;
    }

    private UpdateConfig buildUpdateConfig(Map<String, Object> update) {
        if (MapUtil.isNotEmpty(update)) {
            UpdateConfig updateConfig = new UpdateConfig();
            String failureAction = (String) update.get("failureAction");
            if (StrUtil.isNotEmpty(failureAction)) {
                UpdateFailureAction updateFailureAction = EnumUtil.fromString(UpdateFailureAction.class, failureAction);
                updateConfig.withFailureAction(updateFailureAction);
            }
            String order = (String) update.get("order");
            if (StrUtil.isNotEmpty(order)) {
                UpdateOrder updateOrder = EnumUtil.fromString(UpdateOrder.class, order);
                updateConfig.withOrder(updateOrder);
            }
            Object parallelism = update.get("parallelism");
            if (parallelism != null) {
                updateConfig.withParallelism(Convert.toLong(parallelism));
            }
            Object delay = update.get("delay");
            if (delay != null) {
                updateConfig.withDelay(Convert.toLong(delay));
            }
            Object maxFailureRatio = update.get("maxFailureRatio");
            if (maxFailureRatio != null) {
                updateConfig.withMaxFailureRatio(Convert.toFloat(maxFailureRatio));
            }
            Object monitor = update.get("monitor");
            if (monitor != null) {
                updateConfig.withMonitor(Convert.toLong(monitor));
            }
            return updateConfig;
        }
        return null;
    }


    public void removeServiceCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.build(parameter);
        try {
            String serviceId = (String) parameter.get("serviceId");
            RemoveServiceCmd removeServiceCmd = dockerClient.removeServiceCmd(serviceId);
            removeServiceCmd.exec();
        } finally {
            IoUtil.close(dockerClient);
        }
    }

    private List<JSONObject> listTasksCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.build(parameter);
        try {
            ListTasksCmd listTasksCmd = dockerClient.listTasksCmd();
            String serviceId = (String) parameter.get("serviceId");
            String id = (String) parameter.get("id");
            if (StrUtil.isNotEmpty(serviceId)) {
                listTasksCmd.withServiceFilter(serviceId);
            }
            if (StrUtil.isNotEmpty(id)) {
                listTasksCmd.withIdFilter(id);
            }
            String name = (String) parameter.get("name");
            if (StrUtil.isNotEmpty(name)) {
                listTasksCmd.withNameFilter(name);
            }
            String node = (String) parameter.get("node");
            if (StrUtil.isNotEmpty(node)) {
                listTasksCmd.withNodeFilter(node);
            }
            String state = (String) parameter.get("state");
            if (StrUtil.isNotEmpty(state)) {
                TaskState taskState = EnumUtil.fromString(TaskState.class, state);
                listTasksCmd.withStateFilter(taskState);
            }
            List<Task> exec = listTasksCmd.exec();
            return exec.stream().map(swarmNode -> (JSONObject) JSONObject.toJSON(swarmNode)).collect(Collectors.toList());
        } finally {
            IoUtil.close(dockerClient);
        }
    }

    public List<JSONObject> listServicesCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.build(parameter);
        try {
            ListServicesCmd listServicesCmd = dockerClient.listServicesCmd();
            String id = (String) parameter.get("id");
            String name = (String) parameter.get("name");
            if (StrUtil.isNotEmpty(id)) {
                listServicesCmd.withIdFilter(CollUtil.newArrayList(id));
            }
            if (StrUtil.isNotEmpty(name)) {
                listServicesCmd.withNameFilter(CollUtil.newArrayList(name));
            }
            List<Service> exec = listServicesCmd.exec();
            return exec.stream().map(swarmNode -> (JSONObject) JSONObject.toJSON(swarmNode)).collect(Collectors.toList());
        } finally {
            IoUtil.close(dockerClient);
        }
    }

    private void removeSwarmNodeCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.build(parameter);
        try {
            DockerCmdExecFactory dockerCmdExecFactory = (DockerCmdExecFactory) ReflectUtil.getFieldValue(dockerClient, "dockerCmdExecFactory");
            Assert.notNull(dockerCmdExecFactory, "当前方法不被支持，暂时不能使用");
            String nodeId = (String) parameter.get("nodeId");
            RemoveSwarmNodeCmdImpl removeSwarmNodeCmd = new RemoveSwarmNodeCmdImpl(
                dockerCmdExecFactory.removeSwarmNodeCmdExec(), nodeId);
            removeSwarmNodeCmd.withForce(true);
            removeSwarmNodeCmd.exec();
        } finally {
            IoUtil.close(dockerClient);
        }
    }

    private void updateSwarmNodeCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.build(parameter);
        try {
            String nodeId = (String) parameter.get("nodeId");
            List<SwarmNode> nodes = dockerClient.listSwarmNodesCmd()
                .withIdFilter(CollUtil.newArrayList(nodeId)).exec();
            SwarmNode swarmNode = CollUtil.getFirst(nodes);
            Assert.notNull(swarmNode, "没有对应的节点");
            ObjectVersion version = swarmNode.getVersion();
            Assert.notNull(version, "对应的节点信息不完整不能继续");
            //
            String availabilityStr = (String) parameter.get("availability");
            String roleStr = (String) parameter.get("role");
            //
            SwarmNodeAvailability availability = EnumUtil.fromString(SwarmNodeAvailability.class, availabilityStr);
            SwarmNodeRole role = EnumUtil.fromString(SwarmNodeRole.class, roleStr);
            UpdateSwarmNodeCmd swarmNodeCmd = dockerClient.updateSwarmNodeCmd();
            swarmNodeCmd.withSwarmNodeId(nodeId);
            SwarmNodeSpec swarmNodeSpec = new SwarmNodeSpec();
            swarmNodeSpec.withAvailability(availability);
            swarmNodeSpec.withRole(role);
            swarmNodeCmd.withSwarmNodeSpec(swarmNodeSpec);
            swarmNodeCmd.withVersion(version.getIndex());
            swarmNodeCmd.exec();
        } finally {
            IoUtil.close(dockerClient);
        }
    }


    private void leaveSwarmCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.build(parameter);
        try {
            Object forceStr = parameter.get("force");
            boolean force = Convert.toBool(forceStr, false);
            LeaveSwarmCmd leaveSwarmCmd = dockerClient.leaveSwarmCmd();
            if (force) {
                leaveSwarmCmd.withForceEnabled(true);
            }
            leaveSwarmCmd.exec();
        } finally {
            IoUtil.close(dockerClient);
        }
    }


//	private List<JSONObject> listSwarmNodesCmd(Map<String, Object> parameter) {
//		DockerClient dockerClient = DockerUtil.build(parameter);
//		try {
//			LeaveSwarmCmd leaveSwarmCmd = dockerClient.leaveSwarmCmd();
//			leaveSwarmCmd.withForceEnabled(true)
//		} finally {
//			IoUtil.close(dockerClient);
//		}
//	}

    private List<JSONObject> listSwarmNodesCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.build(parameter);
        try {
            ListSwarmNodesCmd listSwarmNodesCmd = dockerClient.listSwarmNodesCmd();
            String id = (String) parameter.get("id");
            if (StrUtil.isNotEmpty(id)) {
                listSwarmNodesCmd.withIdFilter(StrUtil.splitTrim(id, StrUtil.COMMA));
            }
            String role = (String) parameter.get("role");
            if (StrUtil.isNotEmpty(role)) {
                listSwarmNodesCmd.withRoleFilter(StrUtil.splitTrim(role, StrUtil.COMMA));
            }
            String name = (String) parameter.get("name");
            if (StrUtil.isNotEmpty(name)) {
                listSwarmNodesCmd.withNameFilter(StrUtil.splitTrim(name, StrUtil.COMMA));
            }
            List<SwarmNode> exec = listSwarmNodesCmd.exec();
            return exec.stream().map(swarmNode -> {
                JSONObject jsonObject = (JSONObject) JSONObject.toJSON(swarmNode);
                jsonObject.remove("rawValues");
                return jsonObject;
            }).collect(Collectors.toList());
        } finally {
            IoUtil.close(dockerClient);
        }
    }


    private void joinSwarmCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.build(parameter);
        try {
            String token = (String) parameter.get("token");
            String remoteAddrs = (String) parameter.get("remoteAddrs");
            JoinSwarmCmd joinSwarmCmd = dockerClient.joinSwarmCmd()
                .withRemoteAddrs(StrUtil.splitTrim(remoteAddrs, StrUtil.COMMA))
                .withJoinToken(token);
            joinSwarmCmd.exec();
        } finally {
            IoUtil.close(dockerClient);
        }
    }

    private JSONObject tryInitializeSwarmCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.build(parameter);
        try {
            // 先尝试获取
            try {
                Swarm exec = dockerClient.inspectSwarmCmd().exec();
                JSONObject jsonObject = (JSONObject) JSONObject.toJSON(exec);
                if (jsonObject != null) {
                    return jsonObject;
                }
            } catch (Exception ignored) {
                //
            }
            // 尝试初始化
            SwarmSpec swarmSpec = new SwarmSpec();
            swarmSpec.withName("default");
            dockerClient.initializeSwarmCmd(swarmSpec).exec();
            // 获取信息
            Swarm exec = dockerClient.inspectSwarmCmd().exec();
            return (JSONObject) JSONObject.toJSON(exec);
        } finally {
            IoUtil.close(dockerClient);
        }
    }

    private JSONObject inSpectSwarmCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.build(parameter);
        try {
            Swarm exec = dockerClient.inspectSwarmCmd().exec();
            return (JSONObject) JSONObject.toJSON(exec);
        } finally {
            IoUtil.close(dockerClient);
        }
    }
}
