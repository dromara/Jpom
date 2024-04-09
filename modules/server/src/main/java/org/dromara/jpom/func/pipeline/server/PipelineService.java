package org.dromara.jpom.func.pipeline.server;

import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.build.pipeline.model.PipelineDataModel;
import org.dromara.jpom.service.h2db.BaseWorkspaceService;
import org.springframework.stereotype.Service;

/**
 * @author bwcx_jzy
 * @since 2024/4/9
 */
@Service
@Slf4j
public class PipelineService extends BaseWorkspaceService<PipelineDataModel> {
}
