package io.jpom.model.docker;

import io.jpom.model.BaseWorkspaceModel;
import io.jpom.service.h2db.TableName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Tolerate;

/**
 * @author bwcx_jzy
 * @since 2022/2/13
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "DOCKER_SWARM_INFO", name = "docker 集群信息")
@Builder
public class DockerSwarmInfoMode extends BaseWorkspaceModel {
	/**
	 * 集群名称
	 */
	private String name;
	/**
	 * docker ID
	 */
	private String dockerId;
	/**
	 * 集群的创建时间
	 */
	private Long swarmCreatedAt;
	/**
	 * 集群ID
	 */
	private String swarmId;
	/**
	 * 集群的更新时间
	 */
	private Long swarmUpdatedAt;
	/**
	 * 集群容器标签
	 */
	private String tag;
	/**
	 * 节点 地址
	 */
	private String nodeAddr;

	@Tolerate
	public DockerSwarmInfoMode() {
	}
}
