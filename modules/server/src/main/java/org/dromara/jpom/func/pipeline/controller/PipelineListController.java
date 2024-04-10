package org.dromara.jpom.func.pipeline.controller;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.build.pipeline.model.PipelineDataModel;
import org.dromara.jpom.build.pipeline.model.config.PipelineConfig;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.func.pipeline.server.PipelineService;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * @author bwcx_jzy
 * @since 2024/4/9
 */
@RestController
@RequestMapping(value = "/build/pipeline")
@Feature(cls = ClassFeature.BUILD_PIPELINE)
@Slf4j
public class PipelineListController extends BaseServerController {

    private final PipelineService pipelineService;

    public PipelineListController(PipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    /**
     * 分页列表
     *
     * @return json
     */
    @PostMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<PipelineDataModel>> list(HttpServletRequest request) {
        //
        PageResultDto<PipelineDataModel> listPage = pipelineService.listPage(request);
        return JsonMessage.success("", listPage);
    }

    /**
     * 获取单个流水线
     *
     * @return json
     */
    @GetMapping(value = "get", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PipelineDataModel> get(String id, HttpServletRequest request) {
        //
        PipelineDataModel buildInfoModel = pipelineService.getByKey(id, request);
        Assert.notNull(buildInfoModel, "不存在对应的流水线");
        return JsonMessage.success("", buildInfoModel);
    }

    @RequestMapping(value = "edit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> updateBuild(@RequestBody JSONObject data,
                                            HttpServletRequest request) {
        String id = data.getString("id");
        String name = data.getString("name");
        Assert.notBlank(name, "流水线名称不能为空");
        String group = data.getString("group");
        String jsonConfig = data.getString("jsonConfig");
        Assert.notBlank(jsonConfig, "流水线配置不能为空");
        try {
            PipelineConfig.fromJson(jsonConfig).verify("");
        } catch (Exception e) {
            log.error("流水线配置格式错误", e);
            return JsonMessage.fail("流水线配置格式错误");
        }
//        @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "流水线名称不能为空") String name,
//        String group, String jsonConfig,
        PipelineDataModel pipelineDataModel = new PipelineDataModel();
        pipelineDataModel.setName(name);
        pipelineDataModel.setGroup(group);
        pipelineDataModel.setJsonConfig(jsonConfig);
        if (StrUtil.isEmpty(id)) {
            pipelineService.insert(pipelineDataModel);
        } else {
            pipelineDataModel.setId(id);
            pipelineService.updateById(pipelineDataModel, request);
        }
        return JsonMessage.success("操作成功");
    }

    /**
     * 删除流水线信息
     *
     * @param id 构建ID
     * @return json
     */
    @GetMapping(value = "delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<Object> delete(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据id") String id, HttpServletRequest request) {
        pipelineService.delByKey(id, request);
        return JsonMessage.success("删除成功");
    }
}
