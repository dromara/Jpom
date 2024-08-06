/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
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
