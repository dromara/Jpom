/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.build;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.util.AntPathUtil;
import org.dromara.jpom.util.FileUtils;
import org.springframework.util.Assert;

import java.io.File;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2023/2/8
 */
@Data
public class ResultDirFileAction {
    /**
     * 配置的产物路径（或者 ant 表达式）
     */
    private String path;

    /**
     * 产物匹配模式
     */
    private Type type;

    /**
     * ant 文件上传模式
     */
    private AntFileUploadMode antFileUploadMode;

    /**
     * ant 使用指定路径下的文件
     */
    private String antSubMatch;

    public String antSubMatch() {
        if (StrUtil.isEmpty(this.antSubMatch)) {
            // 兼容默认数据，未配置
            return StrUtil.EMPTY;
        }
        String normalize = FileUtil.normalize(this.antSubMatch);
        //需要包裹成目录结构
        return StrUtil.wrapIfMissing(normalize, StrUtil.SLASH, StrUtil.SLASH);
    }

    public ResultDirFileAction(String resultDirFile) {
        // 存在路径 表达式
        if (StrUtil.contains(resultDirFile, StrUtil.COLON)) {
            List<String> resultDirFiles = StrUtil.splitTrim(resultDirFile, StrUtil.COLON);
            String first = CollUtil.getFirst(resultDirFiles);
            this.setPath(first);
            this.setType(AntPathUtil.ANT_PATH_MATCHER.isPattern(first) ? Type.ANT_PATH : Type.ORIGINAL);
            if (this.getType() == Type.ANT_PATH) {
                // 文件保留方式
                String antFileUploadModeStr = CollUtil.get(resultDirFiles, 1);
                antFileUploadModeStr = StrUtil.nullToDefault(antFileUploadModeStr, StrUtil.EMPTY).toUpperCase();
                AntFileUploadMode fileUploadMode = EnumUtil.fromString(AntFileUploadMode.class, antFileUploadModeStr, AntFileUploadMode.KEEP_DIR);
                this.setAntFileUploadMode(fileUploadMode);
                // ant 使用二级路径
                String antFileUploadPath = CollUtil.get(resultDirFiles, 2);
                this.setAntSubMatch(StrUtil.nullToDefault(antFileUploadPath, StrUtil.EMPTY));
            }
        } else {
            this.setPath(resultDirFile);
            this.setType(AntPathUtil.ANT_PATH_MATCHER.isPattern(resultDirFile) ? Type.ANT_PATH : Type.ORIGINAL);
            if (this.getType() == Type.ANT_PATH) {
                this.setAntFileUploadMode(AntFileUploadMode.KEEP_DIR);
                this.setAntSubMatch(StrUtil.EMPTY);
            }
        }
    }

    /**
     * ant 模式使用 normalize 方法格式化不规范的路径
     *
     * @see AntPathUtil#antPathMatcher(File, String)
     */
    public void check() {
        if (this.getType() == Type.ORIGINAL) {
            FileUtils.checkSlip(getPath(), e -> new IllegalArgumentException(I18nMessageUtil.get("i18n.product_directory_cannot_skip_levels.3ad4") + e.getMessage()));
        } else if (this.getType() == Type.ANT_PATH) {
            // ant 模式存在特殊字符，直接判断会发生异常并且判断不到
        }
    }

    /**
     * 解析产物路径
     *
     * @param resultDirFile 产物配置
     * @return ResultDirFileAction
     */
    public static ResultDirFileAction parse(String resultDirFile) {
        Assert.notNull(resultDirFile, I18nMessageUtil.get("i18n.result_dir_file_required.5f02"));
        return new ResultDirFileAction(resultDirFile);
    }

    public enum AntFileUploadMode {
        /**
         * 保留文件夹层级
         */
        KEEP_DIR,
        /**
         * 将所有文件合并到同一个文件夹
         */
        SAME_DIR,
    }

    public enum Type {
        /**
         * 模糊匹配模式
         */
        ANT_PATH,
        /**
         * 原始目录
         */
        ORIGINAL
    }
}
