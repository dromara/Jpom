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
	/**
	 * 状态 0 , 异常离线 1 正常
	 */
	private Integer status;
	/**
	 * 错误消息
	 */
	private String failureMsg;

	@Tolerate
	public DockerSwarmInfoMode() {
	}
}
