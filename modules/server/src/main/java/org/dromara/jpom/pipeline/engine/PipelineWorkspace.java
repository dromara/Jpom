/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.engine;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.pipeline.model.PipelineSource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 流水线工作区布局
 * <pre>
 * data/pipeline/
 *   workspace/&lt;pipelineId&gt;/&lt;runNumber&gt;/&lt;mount&gt;   每个源一个子目录
 *   artifacts/&lt;pipelineId&gt;/&lt;runNumber&gt;/&lt;stageId&gt;/ 阶段产物
 *   log/&lt;pipelineId&gt;/&lt;runNumber&gt;.log                运行日志
 * </pre>
 * <p>
 * 布局集中在这里，是为了让「路径规则」只有一处：阶段执行器、发布目标、清理逻辑都从这取，
 * 不允许各自拼路径（拼接式路径是路径穿越与不一致的温床）。挂载目录已在定义校验里禁止 {@code ..}，
 * 这里再做一次规范化确认，双保险。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
public final class PipelineWorkspace {

    private final File root;

    public PipelineWorkspace(File dataDir) {
        this.root = FileUtil.file(dataDir, "pipeline");
    }

    /**
     * 运行工作区根目录
     *
     * @param pipelineId 流水线
     * @param runNumber  运行编号
     * @return 目录
     */
    public File workspace(String pipelineId, int runNumber) {
        return FileUtil.mkdir(FileUtil.file(root, "workspace", safe(pipelineId), String.valueOf(runNumber)));
    }

    /**
     * 单个仓库源的挂载目录
     *
     * @param pipelineId 流水线
     * @param runNumber  运行编号
     * @param source     仓库源
     * @return 目录
     */
    public File mountDir(String pipelineId, int runNumber, PipelineSource source) {
        File base = this.workspace(pipelineId, runNumber);
        String mount = StrUtil.trimToEmpty(source.getMount());
        if (StrUtil.isEmpty(mount)) {
            return base;
        }
        File dir = FileUtil.file(base, mount);
        String basePath = FileUtil.getAbsolutePath(base);
        String target = FileUtil.getAbsolutePath(dir);
        if (!StrUtil.startWith(target, basePath)) {
            // 挂载目录逃出工作区：拒绝（定义校验已拦一次，这里防手工改库）
            throw new IllegalArgumentException("mount outside workspace: " + mount);
        }
        return FileUtil.mkdir(dir);
    }

    /**
     * 阶段产物目录
     *
     * @param pipelineId 流水线
     * @param runNumber  运行编号
     * @param stageId    阶段
     * @return 目录
     */
    public File artifactDir(String pipelineId, int runNumber, String stageId) {
        return FileUtil.mkdir(FileUtil.file(root, "artifacts", safe(pipelineId), String.valueOf(runNumber), safe(stageId)));
    }

    /**
     * 运行日志文件
     *
     * @param pipelineId 流水线
     * @param runNumber  运行编号
     * @return 文件
     */
    public File logFile(String pipelineId, int runNumber) {
        File dir = FileUtil.mkdir(FileUtil.file(root, "log", safe(pipelineId)));
        return FileUtil.file(dir, runNumber + ".log");
    }

    /**
     * 列出本次运行所有源的挂载目录（诊断/日志用）
     *
     * @param pipelineId 流水线
     * @param runNumber  运行编号
     * @param sources    源
     * @return 目录列表
     */
    public List<File> mountDirs(String pipelineId, int runNumber, List<PipelineSource> sources) {
        List<File> list = new ArrayList<>();
        for (PipelineSource source : sources) {
            list.add(this.mountDir(pipelineId, runNumber, source));
        }
        return list;
    }

    /**
     * 目录名安全化（id 会进文件路径）
     */
    private static String safe(String value) {
        String text = StrUtil.nullToEmpty(value);
        if (text.contains("..") || text.contains(StrUtil.SLASH) || text.contains("\\")) {
            throw new IllegalArgumentException("invalid path segment: " + value);
        }
        return text;
    }
}
