package io.jpom.build;

import cn.hutool.core.util.StrUtil;
import cn.hutool.setting.yaml.YamlUtil;
import io.jpom.model.BaseJsonModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * docker 构建 配置
 * <p>
 * https://www.jianshu.com/p/54cfa5721d5f
 *
 * @author bwcx_jzy
 * @since 2022/1/25
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DockerYmlDsl extends BaseJsonModel {

	/**
	 * 镜像 from
	 */
	private String image;
	/**
	 * tcp://127.0.0.1:2375
	 * <p>
	 * 主机地址
	 */
	private String dockerHost;
	/**
	 * docker api
	 */
	private String apiVersion;
	/**
	 * bind mounts 将宿主机上的任意位置的文件或者目录挂在到容器 （--mount type=bind,src=源目录,dst=目标目录）
	 */
	private List<String> binds;
	/**
	 * 入口点
	 */
	private List<String> entrypoints;
	/**
	 * 执行的命令
	 */
	private List<String> cmds;
	/**
	 * 环境变量
	 */
	private Map<String, String> env;

	public void check() {
		Assert.hasText(getImage(), "请填写镜像名称");
		//
		Assert.notEmpty(getEntrypoints(), "请填写entrypoint");
		Assert.hasText(getDockerHost(), "请填写 docker host");
	}

	/**
	 * 构建对象
	 *
	 * @param yml yml 内容
	 * @return DockerYmlDsl
	 */
	public static DockerYmlDsl build(String yml) {
		yml = StrUtil.replace(yml, StrUtil.TAB, StrUtil.SPACE + StrUtil.SPACE);
		InputStream inputStream = new ByteArrayInputStream(yml.getBytes());
		return YamlUtil.load(inputStream, DockerYmlDsl.class);
	}
}
