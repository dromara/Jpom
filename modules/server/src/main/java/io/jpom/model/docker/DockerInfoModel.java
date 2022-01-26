package io.jpom.model.docker;

import cn.hutool.core.annotation.PropIgnore;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import io.jpom.model.BaseWorkspaceModel;
import io.jpom.service.h2db.TableName;
import io.jpom.system.ConfigBean;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Tolerate;
import org.springframework.util.Assert;

import java.io.File;

/**
 * @author bwcx_jzy
 * @since 2022/1/26
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "DOCKER_INFO", name = "docker 信息")
@Builder
public class DockerInfoModel extends BaseWorkspaceModel {
	/**
	 * 名称
	 */
	private String name;
	/**
	 * 地址
	 */
	private String host;
	/**
	 * 接口版本
	 */
	private String apiVersion;
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
	 * 心跳超时时间，单位 秒
	 */
	private Integer heartbeatTimeout;

	public void setFailureMsg(String failureMsg) {
		this.failureMsg = StrUtil.maxLength(failureMsg, 240);
	}

	@Tolerate
	public DockerInfoModel() {
	}

	/**
	 * 生成证书路径
	 *
	 * @return path
	 */
	public String generateCertPath() {
		String dataPath = ConfigBean.getInstance().getDataPath();
		String id = this.getId();
		Assert.hasText(id, "ID empty");
		File docker = FileUtil.file(dataPath, "docker", "tls-cert", id);
		return FileUtil.getAbsolutePath(docker);
	}

}
