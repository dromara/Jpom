package org.dromara.jpom.controller.script;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.BaseAgentController;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.model.data.ScriptLibraryModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy1
 * @since 2024/6/1
 */
@RestController
@RequestMapping(value = "/script-library")
@Slf4j
public class ScriptLibraryController extends BaseAgentController {

    private final File globalScriptDir;

    public ScriptLibraryController(JpomApplication jpomApplication) {
        this.globalScriptDir = FileUtil.file(jpomApplication.getDataPath(), "global-script");
    }

    @RequestMapping(value = "list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<List<ScriptLibraryModel>> list() {
        List<File> list = FileUtil.loopFiles(globalScriptDir, 1, pathname -> StrUtil.equals("json", FileUtil.extName(pathname)));
        List<ScriptLibraryModel> modelList = list.stream()
            .map(file -> {
                try {
                    String string = FileUtil.readUtf8String(file);
                    ScriptLibraryModel scriptModel = JSONObject.parseObject(string, ScriptLibraryModel.class);
                    // 以文件名为脚本标签
                    scriptModel.setTag(FileUtil.mainName(file));
                    // 脚本内容不返回
                    scriptModel.setScript(null);
                    return scriptModel;
                } catch (Exception e) {
                    log.error(I18nMessageUtil.get("i18n.read_global_script_file_error.0d4c"), e);
                }
                return null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        return JsonMessage.success("", modelList);
    }

    @RequestMapping(value = "get", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<ScriptLibraryModel> get(@ValidatorItem String id) {
        File file = FileUtil.file(globalScriptDir, id + ".json");
        if (FileUtil.exist(file)) {
            String string = FileUtil.readUtf8String(file);
            ScriptLibraryModel scriptModel = JSONObject.parseObject(string, ScriptLibraryModel.class);
            // 以文件名为脚本标签
            scriptModel.setTag(FileUtil.mainName(file));
            return JsonMessage.success("", scriptModel);
        }
        return JsonMessage.fail(I18nMessageUtil.get("i18n.missing_script_message.af89"));
    }

    @RequestMapping(value = "save", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<String> save(@ValidatorItem String id,
                                     @ValidatorItem(msg = "i18n.script_content_cannot_be_empty.49be") String script,
                                     String description,
                                     String version) {
        File file = FileUtil.file(globalScriptDir, id + ".json");
        ScriptLibraryModel scriptModel = new ScriptLibraryModel();
        scriptModel.setId(id);
        scriptModel.setScript(script);
        scriptModel.setDescription(description);
        scriptModel.setVersion(version);
        FileUtil.writeUtf8String(JSONObject.toJSONString(scriptModel), file);
        return JsonMessage.success(I18nMessageUtil.get("i18n.save_succeeded.3b10"));
    }

    @RequestMapping(value = "del", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<String> del(@ValidatorItem String id) {
        File file = FileUtil.file(globalScriptDir, id + ".json");
        FileUtil.del(file);
        return JsonMessage.success(I18nMessageUtil.get("i18n.delete_success.0007"));
    }
}
