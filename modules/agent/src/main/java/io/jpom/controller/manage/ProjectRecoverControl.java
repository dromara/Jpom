package io.jpom.controller.manage;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.common.BaseJpomController;
import io.jpom.model.data.ProjectRecoverModel;
import io.jpom.service.manage.ProjectRecoverService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

/**
 * 回收站管理
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
@RestController
@RequestMapping(value = "/manage/recover")
public class ProjectRecoverControl extends BaseJpomController {
    @Resource
    private ProjectRecoverService projectRecoverService;

    @RequestMapping(value = "list_data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String projectInfo() {
        List<ProjectRecoverModel> projectInfoModels = projectRecoverService.list();
        return JsonMessage.getString(200, "", projectInfoModels);
    }

    @RequestMapping(value = "item_data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String project(String id) throws IOException {
        if (StrUtil.isEmpty(id)) {
            return JsonMessage.getString(400, "项目id错误");
        }
        ProjectRecoverModel item = projectRecoverService.getItem(id);
        return JsonMessage.getString(200, "", item);
    }
}
