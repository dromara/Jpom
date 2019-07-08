package cn.keepbx.jpom.controller.script;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import cn.keepbx.jpom.common.BaseAgentController;
import cn.keepbx.jpom.model.data.ScriptModel;
import cn.keepbx.jpom.service.script.ScriptServer;
import cn.keepbx.jpom.system.AgentConfigBean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;

/**
 * 脚本管理
 *
 * @author jiangzeyin
 * @date 2019/4/24
 */
@RestController
@RequestMapping(value = "/script")
public class ScriptController extends BaseAgentController {
    @Resource
    private ScriptServer scriptServer;

    @RequestMapping(value = "list.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String list() throws IOException {
        return JsonMessage.getString(200, "", scriptServer.list());
    }

    @RequestMapping(value = "item.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String item(String id) throws IOException {
        return JsonMessage.getString(200, "", scriptServer.getItem(id));
    }

    @RequestMapping(value = "save.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String save(ScriptModel scriptModel, String type) throws IOException {
        if (scriptModel == null) {
            return JsonMessage.getString(405, "没有数据");
        }
        boolean safe = checkPathSafe(scriptModel.getId());
        if (!safe) {
            return JsonMessage.getString(405, "id规则不合法");
        }
        if (StrUtil.isEmpty(scriptModel.getContext())) {
            return JsonMessage.getString(405, "内容为空");
        }
        //

        ScriptModel eModel = scriptServer.getItem(scriptModel.getId());
        if ("add".equalsIgnoreCase(type)) {
            if (eModel != null) {
                return JsonMessage.getString(405, "id已经存在啦");
            }
            File file = scriptModel.getFile(true);
            if (file.exists() || file.isDirectory()) {
                return JsonMessage.getString(405, "当地id路径文件已经存在来，请修改");
            }
            scriptServer.addItem(scriptModel);
            return JsonMessage.getString(200, "添加成功");
        }
        if (eModel == null) {
            return JsonMessage.getString(405, "对应数据不存在");
        }
        eModel.setName(scriptModel.getName());
        eModel.setContext(scriptModel.getContext());
        boolean b = scriptServer.updateItem(eModel);
        if (b) {
            return JsonMessage.getString(200, "修改成功");
        }
        return JsonMessage.getString(500, "修改失败");
    }

    @RequestMapping(value = "del.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String del(String id) throws IOException {
        scriptServer.deleteItem(id);
        return JsonMessage.getString(200, "删除成功");
    }

    @RequestMapping(value = "upload.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String upload() throws IOException {
        MultipartFileBuilder multipartFileBuilder = createMultipart()
                .addFieldName("file").setFileExt("bat", "sh");
        multipartFileBuilder.setSavePath(AgentConfigBean.getInstance().getTempPathName());
        multipartFileBuilder.setUseOriginalFilename(true);
        String path = multipartFileBuilder.save();
        File file = FileUtil.file(path);
        String context = FileUtil.readString(path, CharsetUtil.CHARSET_UTF_8);
        if (StrUtil.isEmpty(context)) {
            return JsonMessage.getString(405, "脚本内容为空");
        }
        String id = file.getName();
        ScriptModel eModel = scriptServer.getItem(id);
        if (eModel != null) {
            return JsonMessage.getString(405, "对应脚本模板已经存在啦");
        }
        eModel = new ScriptModel();
        eModel.setId(id);
        eModel.setName(id);
        eModel.setContext(context);
        file = eModel.getFile(true);
        if (file.exists() || file.isDirectory()) {
            return JsonMessage.getString(405, "当地id路径文件已经存在来，请修改");
        }
        scriptServer.addItem(eModel);
        return JsonMessage.getString(200, "导入成功");
    }
}
