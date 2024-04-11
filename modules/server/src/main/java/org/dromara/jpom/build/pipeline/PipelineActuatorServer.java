package org.dromara.jpom.build.pipeline;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.build.pipeline.model.PipelineDataModel;
import org.dromara.jpom.build.pipeline.model.config.PipelineConfig;
import org.dromara.jpom.common.ILoadEvent;
import org.dromara.jpom.configuration.BuildExtConfig;
import org.dromara.jpom.configuration.PipelineExtConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * @author bwcx_jzy
 * @since 2024/4/11
 */
@Service
@Slf4j
public class PipelineActuatorServer implements ILoadEvent {

    private final PipelineExtConfig pipelineExtConfig;
    private final JpomApplication jpomApplication;

    public PipelineActuatorServer(BuildExtConfig buildExtConfig,
                                  JpomApplication jpomApplication) {
        this.pipelineExtConfig = buildExtConfig.getPipeline();
        this.jpomApplication = jpomApplication;
    }

    /**
     * ${data-path}/pipeline/${workspaceId}/${pipelineId}/source/${concurrentTag}-${repoTag}
     *
     * @return 默认目录
     */
    private File defaultDataDir() {
        return FileUtil.file(jpomApplication.getDataPath(), "pipeline");
    }

    /**
     * 获取当使用的目录
     *
     * @return 用户配置优先
     */
    private File buildDataDir() {
        String storageLocation = pipelineExtConfig.getStorageLocation();
        if (StrUtil.isEmpty(storageLocation)) {
            return defaultDataDir();
        }
        return FileUtil.file(storageLocation);
    }

    /**
     * ${data-path}/pipeline/${workspaceId}/${pipelineId}
     *
     * @param pipelineDataModel 流水线信息
     * @return file
     */
    public File buildPipelineDataDir(PipelineDataModel pipelineDataModel) {
        String workspaceId = pipelineDataModel.getWorkspaceId();
        String id = pipelineDataModel.getId();
        return FileUtil.file(this.buildDataDir(), workspaceId, id);
    }

    public String concurrentTag(PipelineDataModel pipelineDataModel) {
        return null;
    }

    public PipelineConfig toPipelineConfig(PipelineDataModel pipelineDataModel) {
        String jsonConfig = pipelineDataModel.getJsonConfig();
        PipelineConfig pipelineConfig = PipelineConfig.fromJson(jsonConfig);
        //
        pipelineConfig.verify("");
        return pipelineConfig;
    }

    public void build(PipelineDataModel pipelineDataModel) {
        PipelineConfig pipelineConfig = this.toPipelineConfig(pipelineDataModel);

    }

    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) throws Exception {
        File defaultDataDir = this.defaultDataDir();
        File buildDataDir = this.buildDataDir();
        if (!FileUtil.equals(defaultDataDir, buildDataDir)) {
            // 目录发生变化
            if (FileUtil.exist(defaultDataDir) && FileUtil.isDirectory(defaultDataDir)) {
                log.info("pipeline storageLocation change, move data to {}", buildDataDir);
                FileUtil.mkdir(buildDataDir);
                FileUtil.moveContent(defaultDataDir, buildDataDir, true);
            }
        }
    }
}
