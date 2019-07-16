package cn.keepbx.jpom.controller.build;

import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorConfig;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import cn.keepbx.build.BuildManage;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.model.BaseEnum;
import cn.keepbx.jpom.model.data.BuildModel;
import cn.keepbx.jpom.service.build.BuildService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Objects;

/**
 * @author bwcx_jzy
 * @date 2019/7/16
 */
@RestController
@RequestMapping(value = "/build")
public class BuildManageController extends BaseServerController {

    @Resource
    private BuildService buildService;

    @RequestMapping(value = "start.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String start(@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "删除失败")) String id) throws IOException {
        BuildModel item = buildService.getItem(id);
        if (item == null) {
            return JsonMessage.getString(404, "没有对应数据");
        }
        BuildModel.Status nowStatus = BaseEnum.getEnum(BuildModel.Status.class, item.getStatus());
        Objects.requireNonNull(nowStatus);
        if (BuildModel.Status.No != nowStatus &&
                BuildModel.Status.Success != nowStatus &&
                BuildModel.Status.PubSuccess != nowStatus) {
            return JsonMessage.getString(501, "当前还在：" + nowStatus.getDesc());
        }
        //
        item.setBuildId(item.getBuildId() + 1);
        buildService.updateItem(item);
        BuildManage.create(item);
        return JsonMessage.getString(200, "ok:" + item.getBuildId());
    }
}
