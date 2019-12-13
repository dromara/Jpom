package io.jpom.controller.manage;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.common.BaseAgentController;
import io.jpom.model.data.JdkInfoModel;
import io.jpom.service.manage.JdkInfoService;
import io.jpom.util.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.File;
import java.util.List;

/**
 * @author bwcx_jzy
 * @date 2019/11/1
 */
@RestController
@RequestMapping(value = "/manage/jdk/")
public class JdkListController extends BaseAgentController {

    @Resource
    private JdkInfoService jdkInfoService;

    @RequestMapping(value = "list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String list() {
        List<JdkInfoModel> list = jdkInfoService.list();
        return JsonMessage.getString(200, "", list);
    }

    @RequestMapping(value = "update", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String update(JdkInfoModel model) {
        String path = model.getPath();
        if (StrUtil.isEmpty(path)) {
            return JsonMessage.getString(400, "请填写jdk路径");
        }
        String newPath = FileUtil.normalize(path);
        File file = FileUtil.file(newPath);
        //去除bin
        if ("bin".equals(file.getName())) {
            newPath = file.getParentFile().getAbsolutePath();
        }
        if (!FileUtils.isJdkPath(newPath)) {
            return JsonMessage.getString(400, "路径错误，该路径不是jdk路径");
        }
        model.setPath(newPath);
        String id = model.getId();
        String jdkVersion = FileUtils.getJdkVersion(newPath);
        model.setVersion(jdkVersion);
        String name = model.getName();
        if (StrUtil.isEmpty(name)) {
            model.setName(jdkVersion);
        }
        if (StrUtil.isEmpty(id)) {
            model.setId(IdUtil.fastSimpleUUID());
            jdkInfoService.addItem(model);
            return JsonMessage.getString(200, "添加成功");
        }
        jdkInfoService.updateItem(model);
        return JsonMessage.getString(200, "修改成功");
    }

    @RequestMapping(value = "delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String delete(String id) {
        if (StrUtil.isEmpty(id)) {
            return JsonMessage.getString(400, "删除失败");
        }
        jdkInfoService.deleteItem(id);
        return JsonMessage.getString(200, "删除成功");
    }
}
