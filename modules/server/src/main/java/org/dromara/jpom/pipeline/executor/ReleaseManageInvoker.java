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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import org.dromara.jpom.build.BuildExtraModule;
import org.dromara.jpom.build.BuildUtil;
import org.dromara.jpom.build.ReleaseManage;
import org.dromara.jpom.model.EnvironmentMapBuilder;
import org.dromara.jpom.model.data.BuildInfoModel;
import org.dromara.jpom.model.user.UserModel;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 发布执行：把流水线阶段的目标转成既有的 {@link ReleaseManage} 调用
 * <p>
 * 关键取舍：**不重写 7 种发布实现**。做法是把阶段产物铺到在线构建认识的产物布局
 * （{@link PipelineArtifactStager}，用流水线 id 当构建 id、运行号当构建编号），
 * 再用一个合成 {@link BuildInfoModel} + {@link BuildExtraModule} 调用同样的入口，
 * 于是 SSH / SSH 项目 / FTP / 分发 / 项目 / Docker / 本地命令的行为与老构建逐字一致。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
@Service
public class ReleaseManageInvoker implements PipelineReleaseInvoker {

    @Override
    public String release(ReleaseRequest request) throws Exception {
        if (StrUtil.isEmpty(request.getArtifactPath())) {
            return "no artifact";
        }
        PipelineArtifactStager.stage(request.getPipelineId(), request.getRunNumber(), request.getArtifactPath());
        BuildInfoModel synthetic = new BuildInfoModel();
        synthetic.setId(request.getPipelineId());
        // 名称带「流水线名 + 运行号」：文件中心的 name/description 用的就是这个名称，
        // 否则同一流水线多次运行的产物在文件中心里完全同名，无法区分是哪一次
        String pipelineName = StrUtil.emptyToDefault(request.getPipelineName(), request.getPipelineId());
        synthetic.setName(pipelineName + " #" + request.getRunNumber());
        synthetic.setWorkspaceId(request.getWorkspaceId());
        synthetic.setBuildId(request.getRunNumber());
        synthetic.setResultDirFile(StrUtil.SLASH);
        // 文件中心发布支持自定义别名码：目标配置里的 aliasCode 落到合成构建信息上（其余发布方式忽略）
        Object aliasCode = request.getExtra() == null ? null : request.getExtra().get("aliasCode");
        if (aliasCode != null && StrUtil.isNotBlank(String.valueOf(aliasCode))) {
            synthetic.setAliasCode(String.valueOf(aliasCode));
        }
        BuildExtraModule extra = this.toExtraModule(request, synthetic);
        EnvironmentMapBuilder env = new EnvironmentMapBuilder(10);
        env.putStr(request.getVariables());
        ReleaseManage manage = ReleaseManage.builder()
            .buildNumberId(request.getRunNumber())
            .buildExtraModule(extra)
            .userModel(UserModel.EMPTY)
            .logId(request.getPipelineId() + "#" + request.getRunNumber())
            .logRecorder(request.getLogRecorder() == null ? BuildUtil.emptyLogRecorder() : request.getLogRecorder())
            .buildEnv(env)
            .build();
        return manage.start(null, synthetic);
    }

    /**
     * 目标附加配置 → BuildExtraModule（复用同一批字段，避免另开一套发布参数）
     */
    private BuildExtraModule toExtraModule(ReleaseRequest request, BuildInfoModel synthetic) {
        BuildExtraModule extra = new BuildExtraModule();
        Map<String, Object> config = request.getExtra();
        if (CollUtil.isNotEmpty(config)) {
            BuildExtraModule parsed = JSON.parseObject(JSON.toJSONString(config), BuildExtraModule.class);
            if (parsed != null) {
                extra = parsed;
            }
        }
        extra.setId(request.getPipelineId());
        extra.setName(synthetic.getName());
        extra.setWorkspaceId(request.getWorkspaceId());
        extra.setReleaseMethod(request.getMethod());
        extra.setReleaseMethodDataId(request.getTargetId());
        extra.setResultDirFile(StrUtil.SLASH);
        return extra;
    }
}