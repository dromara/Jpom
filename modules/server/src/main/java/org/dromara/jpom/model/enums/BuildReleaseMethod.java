/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.enums;

import org.dromara.jpom.model.BaseEnum;

/**
 * @author bwcx_jzy
 * @since 2021/8/27
 */
public enum BuildReleaseMethod implements BaseEnum {
	/**
	 * 发布
	 */
	No(0, "不发布"),
	Outgiving(1, "节点分发"),
	Project(2, "项目"),
	Ssh(3, "SSH"),
	LocalCommand(4, "本地命令行"),
	DockerImage(5, "Docker镜像"),
	Ftp(6, "FTP"),
	/**
	 * SSH 项目（不依赖插件端，产物直接落到项目目录并重启）
	 */
	SshProject(7, "SSH 项目"),
	/**
	 * 文件中心（产物存入系统文件中心，可被分发/下载复用）
	 * <p>
	 * 追加码（2.11.16）：0–7 的语义不变；流水线发布阶段与在线构建都能选到这一种方式。
	 * </p>
	 */
	FileStorage(8, "文件中心"),
	;
	private final int code;
	private final String desc;

	BuildReleaseMethod(int code, String desc) {
		this.code = code;
		this.desc = desc;
	}

	@Override
	public int getCode() {
		return code;
	}

	@Override
	public String getDesc() {
		return desc;
	}
}
