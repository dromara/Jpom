/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.build;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.StrUtil;
import io.jpom.util.AntPathUtil;
import io.jpom.util.FileUtils;
import lombok.Data;
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
            FileUtils.checkSlip(getPath(), e -> new IllegalArgumentException("产物目录不能越级：" + e.getMessage()));
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
        Assert.notNull(resultDirFile, "resultDirFile 不能为空");
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
