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
	 * 容器ID
	 */
	private String dokcerId;
	/**
	 * 镜像 from
	 */
	private String image;
	/**
	 * bind mounts 将宿主机上的任意位置的文件或者目录挂在到容器 （--mount type=bind,src=源目录,dst=目标目录）
	 * <p>
	 * <host path>:<container path>:<access mode>
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
		Assert.hasText(getDokcerId(), "请填写 docker Id");
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
