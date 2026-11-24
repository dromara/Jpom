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

import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.model.BaseEnum;

/**
 * 构建方式（构建在哪里、用什么手段执行）。
 * <p>
 * 「节点构建」是第三种构建方式，不是本地构建的附加参数：执行位置本身就决定了可用能力
 * （容器构建依赖 Server 本机 docker 资产，只能在本机；节点构建依赖 Go 插件端的构建能力），
 * 因此新增构建方式只需在本枚举追加一项 + 在装配点（{@code BuildRunnerFactory}）注册执行器，
 * 不引入新的布尔开关。
 *
 * @author jpom
 * @since 2.11.15
 */
public enum BuildMode implements BaseEnum {
	/**
	 * 本地构建：在 Server 进程内执行脚本
	 */
	Local(0, "本地构建"),
	/**
	 * 容器构建：在 Server 本机 docker 容器内执行
	 */
	Container(1, "容器构建"),
	/**
	 * 节点构建：把拉代码、执行命令、收集产物交给构建节点（Go 插件端）
	 */
	Node(2, "节点构建"),
	;
	private final int code;
	private final String desc;

	BuildMode(int code, String desc) {
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

	/**
	 * 是否节点构建（构建节点执行）
	 *
	 * @param code 构建方式
	 * @return true 表示节点构建
	 */
	public static boolean isNode(Integer code) {
		return code != null && code == Node.code;
	}

	/**
	 * 是否容器构建
	 *
	 * @param code 构建方式
	 * @return true 表示容器构建
	 */
	public static boolean isContainer(Integer code) {
		return code != null && code == Container.code;
	}

	/**
	 * 规范化构建方式：兼容 2.11.14.3 之前（该 beta 未发布）用「buildMode=0 + buildNodeId 非空」表达节点构建的数据
	 *
	 * @param code        构建方式
	 * @param buildNodeId 构建节点
	 * @return 规范化后的构建方式
	 */
	public static int normalize(Integer code, String buildNodeId) {
		if (StrUtil.isNotEmpty(buildNodeId) && !isContainer(code)) {
			return Node.code;
		}
		return code == null ? Local.code : code;
	}
}
