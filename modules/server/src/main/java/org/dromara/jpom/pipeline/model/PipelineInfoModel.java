/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseWorkspaceModel;

/**
 * 流水线配置（新模块，独立于在线构建的 BUILD_INFO）
 * <p>
 * 与 {@code BuildInfoModel} 的关系：**并存，不互写**。原构建保持原样，流水线是给
 * 「多仓库 / 多构建方式 / 多目标 / 多通知」准备的独立模型，两边互不影响；
 * 迁移方向只有一条：从 BUILD_INFO **只读导入**成一条流水线（不回写）。
 *
 * @author jpom
 * @since 2.11.16
 */
@TableName(value = "PIPELINE_INFO", nameKey = "i18n.pipeline_info.7c1e")
@Data
@EqualsAndHashCode(callSuper = true)
public class PipelineInfoModel extends BaseWorkspaceModel {

    /**
     * 流水线名称
     */
    private String name;
    /**
     * 分组
     * <p>
     * 字段名用 {@code groupName} 而不是 {@code group}：{@code BaseDbService} 以「类里有 group 字段」判定
     * 支持分组，并会在应用启动早期对所有这类表执行一次 {@code update ... set group=?}；
     * 新表此时还没建好会直接启动失败（实测 dev 启动 crash）。新代码统一用 groupName。
     * </p>
     */
    private String groupName;
    /**
     * 仓库源（JSON 数组，见 {@link PipelineSource}）
     */
    private String sourcesJson;
    /**
     * 阶段（JSON 数组，见 {@link PipelineStageDef}）
     */
    private String stagesJson;
    /**
     * 事件钩子（JSON 数组，见 {@code PipelineHookDef}）
     * <p>
     * 追加列（2.11.16）：旧记录为空表示没有钩子，读取时按空数组处理，不做数据迁移。
     * </p>
     */
    private String hooksJson;
    /**
     * 结构版本（后续升级按版本兼容读取）
     */
    private Integer schemaVersion;
    /**
     * 最近一次运行状态（复用构建状态码，取消三态同源）
     */
    private Integer status;
    /**
     * 状态说明
     */
    private String statusMsg;
    /**
     * 最近一次运行记录 ID
     */
    private String lastRunId;
    /**
     * 运行编号自增计数（每次运行 +1，产物目录按它分）
     */
    private Integer runNumber;
    /**
     * 触发器 token
     */
    private String triggerToken;
    /**
     * 排序
     */
    private Float sortValue;

    /**
     * 解析定义
     *
     * @return 定义（永不返回 null）
     */
    public PipelineDefinition definition() {
        // 事件钩子必须一起解析：只传源与阶段会让钩子永远为空（运行期事件不会触发）
        return PipelineDefinition.of(this.sourcesJson, this.stagesJson, this.hooksJson);
    }
}