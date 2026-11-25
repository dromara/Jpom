/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.executor;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.build.BuildUtil;

import java.io.File;

/**
 * 产物搬运：把流水线阶段的产物铺到「发布目标认识的目录」
 * <p>
 * 在线构建的 7 种发布实现都按 {@link BuildUtil#getHistoryPackageFile(String, int, String)} 找产物。
 * 流水线不重写这 7 种实现，而是在发布前把本阶段产物铺到同一布局下（用流水线 id 当构建 id、
 * 运行号当构建编号，天然不与真实构建冲突），于是 SSH / SSH 项目 / FTP / 分发 / 项目 / Docker
 * 全部原样复用。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
public final class PipelineArtifactStager {

    private PipelineArtifactStager() {
    }

    /**
     * 把产物目录铺到发布根目录
     *
     * @param pipelineId 流水线 ID（作为构建 ID 使用）
     * @param runNumber  运行编号（作为构建编号使用）
     * @param artifactPath 阶段产物目录
     * @return 发布根目录（{@code resultDirFile = /}）
     */
    public static File stage(String pipelineId, int runNumber, String artifactPath) {
        File source = FileUtil.file(StrUtil.nullToEmpty(artifactPath));
        if (!source.exists()) {
            throw new IllegalArgumentException("artifact not found: " + artifactPath);
        }
        File target = BuildUtil.getHistoryPackageFile(pipelineId, runNumber, StrUtil.SLASH);
        FileUtil.mkdir(target);
        FileUtil.copyContent(source, target, true);
        return target;
    }
}
