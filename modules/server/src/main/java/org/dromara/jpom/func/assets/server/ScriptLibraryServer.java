package org.dromara.jpom.func.assets.server;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.PatternPool;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.func.assets.model.ScriptLibraryModel;
import org.dromara.jpom.service.h2db.BaseDbService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author bwcx_jzy1
 * @since 2024/6/1
 */
@Service
@Slf4j
public class ScriptLibraryServer extends BaseDbService<ScriptLibraryModel> {

    private final Pattern pattern = PatternPool.get("G@\\(\"(.*?)\"\\)", Pattern.DOTALL);

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
                ScriptLibraryModel where = new ScriptLibraryModel();
                where.setTag(tag);
                List<ScriptLibraryModel> libraryModels = this.listByBean(where);
                scriptLibraryModel = CollUtil.getFirst(libraryModels);
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
}
