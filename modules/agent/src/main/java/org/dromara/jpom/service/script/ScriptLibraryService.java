package org.dromara.jpom.service.script;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.PatternPool;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.data.ScriptLibraryModel;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy1
 * @since 2024/6/14
 */
@Service
@Slf4j
@Getter
public class ScriptLibraryService {
    /**
     * 脚本库目录
     */
    private final File globalScriptDir;
    private final Pattern pattern = PatternPool.get("G@\\(\"(.*?)\"\\)", Pattern.DOTALL);

    public ScriptLibraryService(JpomApplication jpomApplication) {
        this.globalScriptDir = FileUtil.file(jpomApplication.getDataPath(), "global-script");
    }

    /**
     * 引用替换
     *
     * @param script 脚本
     * @return 替换后的脚本
     */
    public String referenceReplace(String script) {
        if (StrUtil.isEmpty(script)) {
            return script;
        }
        Map<String, ScriptLibraryModel> map = new HashMap<>(3);
        Matcher matcher = pattern.matcher(script);
        StringBuffer modified = new StringBuffer();
        while (matcher.find()) {
            String tag = matcher.group(1);
            ScriptLibraryModel scriptLibraryModel = map.get(tag);
            if (scriptLibraryModel == null) {
                scriptLibraryModel = this.get(tag);
                if (scriptLibraryModel != null) {
                    map.put(tag, scriptLibraryModel);
                }
            }
            Assert.notNull(scriptLibraryModel, StrUtil.format(I18nMessageUtil.get("i18n.error_message.483d"), tag));
            matcher.appendReplacement(modified, scriptLibraryModel.getScript());
        }
        matcher.appendTail(modified);
        return modified.toString();
    }

    /**
     * 获取脚本库列表
     *
     * @return list
     */
    public List<ScriptLibraryModel> list() {
        List<File> list = FileUtil.loopFiles(globalScriptDir, 1, pathname -> StrUtil.equals("json", FileUtil.extName(pathname)));
        return list.stream()
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
    }

    /**
     * 获取脚本库
     *
     * @param id id
     * @return model
     */
    public ScriptLibraryModel get(String id) {
        if (StrUtil.isEmpty(id)) {
            return null;
        }
        File file = FileUtil.file(globalScriptDir, id + ".json");
        if (FileUtil.exist(file)) {
            String string = FileUtil.readUtf8String(file);
            ScriptLibraryModel scriptModel = JSONObject.parseObject(string, ScriptLibraryModel.class);
            // 以文件名为脚本标签
            scriptModel.setTag(FileUtil.mainName(file));
            return scriptModel;
        }
        return null;
    }
}
