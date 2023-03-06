package io.jpom.func.assets.model;

import cn.hutool.core.annotation.PropIgnore;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import io.jpom.JpomApplication;
import io.jpom.model.BaseGroupNameModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;
import top.jpom.h2db.TableName;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @see io.jpom.model.docker.DockerInfoModel
 * @since 2023/3/3
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "MACHINE_DOCKER_INFO", name = "机器DOCKER信息")
@Data
@NoArgsConstructor
public class MachineDockerModel extends BaseGroupNameModel {
    /**
     * 地址
     */
    private String host;
    /**
     * 开启 tls 验证
     */
    private Boolean tlsVerify;
    /**
     * 证书路径
     */
    @PropIgnore
    private Boolean certExist;
    /**
     * 状态 0 , 异常离线 1 正常
     */
    private Integer status;
    /**
     * 错误消息
     */
    private String failureMsg;
    /**
     * docker 版本
     */
    private String dockerVersion;
    /**
     * 最后心跳时间
     */
    private Long lastHeartbeatTime;
    /**
     * 超时时间，单位 秒
     */
    private Integer heartbeatTimeout;
    /**
     * 仓库账号
     */
    private String registryUsername;

    /**
     * 仓库密码
     */
    private String registryPassword;

    /**
     * 仓库邮箱
     */
    private String registryEmail;

    /**
     * 仓库地址
     */
    private String registryUrl;

    /**
     * 集群ID
     */
    private String swarmId;
    /**
     * 集群节点ID
     */
    private String swarmNodeId;
    /**
     * 集群的创建时间
     */
    private Long swarmCreatedAt;
    /**
     * 集群的更新时间
     */
    private Long swarmUpdatedAt;
    /**
     * 节点 地址
     */
    private String swarmNodeAddr;
    /**
     * 集群管理员
     */
    private Boolean swarmControlAvailable;

    public void setFailureMsg(String failureMsg) {
        this.failureMsg = StrUtil.maxLength(failureMsg, 240);
    }

    public boolean isControlAvailable() {
        return swarmControlAvailable != null && swarmControlAvailable;
    }

    /**
     * 生成证书路径
     *
     * @return path
     */
    public String generateCertPath() {
        String dataPath = JpomApplication.getInstance().getDataPath();
        String host = this.getHost();
        Assert.hasText(host, "host empty");
        host = SecureUtil.sha1(host);
        File docker = FileUtil.file(dataPath, "docker", "tls-cert", host);
        return FileUtil.getAbsolutePath(docker);
    }

    /**
     * 插件 插件参数 map
     *
     * @return 插件需要使用到到参数
     */
    public Map<String, Object> toParameter() {
        Map<String, Object> parameter = new HashMap<>(10);
        parameter.put("dockerHost", this.getHost());
//        parameter.put("apiVersion", this.getApiVersion());
        parameter.put("registryUsername", this.getRegistryUsername());
        parameter.put("registryPassword", this.getRegistryPassword());
        parameter.put("registryEmail", this.getRegistryEmail());
        parameter.put("registryUrl", this.getRegistryUrl());
        parameter.put("timeout", this.getHeartbeatTimeout());
        if (this.getTlsVerify()) {
            parameter.put("dockerCertPath", this.generateCertPath());
        }
        return parameter;
    }

    public void restSwarm() {
        this.setSwarmId(StrUtil.EMPTY);
        this.setSwarmNodeId(StrUtil.EMPTY);
        this.setSwarmCreatedAt(0L);
        this.setSwarmUpdatedAt(0L);
        this.setSwarmNodeAddr(StrUtil.EMPTY);
        this.setSwarmControlAvailable(false);
    }

}
