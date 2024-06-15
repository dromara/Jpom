package org.dromara.jpom.service.script;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.data.ScriptLibraryModel;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Objects;
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

    public ScriptLibraryService(JpomApplication jpomApplication) {
        this.globalScriptDir = FileUtil.file(jpomApplication.getDataPath(), "global-script");
    }

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
}
