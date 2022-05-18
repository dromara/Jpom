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
package io.jpom.model.data;

import cn.hutool.core.util.StrUtil;
import cn.hutool.setting.yaml.YamlUtil;
import io.jpom.model.BaseJsonModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * dsl yml 配置
 *
 * @author bwcx_jzy
 * @since 2022/1/15
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DslYmlDto extends BaseJsonModel {

    /**
     * 描述
     */
    private String description;

    /**
     * 运行
     */
    private Run run;

    /**
     * 文件相关配置
     */
    private FileConfig file;

    /**
     * 构建对象
     *
     * @param yml yml 内容
     * @return DslYmlDto
     */
    public static DslYmlDto build(String yml) {
        InputStream inputStream = new ByteArrayInputStream(yml.getBytes());
        return YamlUtil.load(inputStream, DslYmlDto.class);
    }

    /**
     * 运行管理
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Run extends BaseJsonModel {
        private Start start;
        private Status status;
        private Stop stop;
    }


    /**
     * 启动流程
     */
    public static class Start extends BaseProcess {

    }

    /**
     * 获取状态流程
     */
    public static class Status extends BaseProcess {

    }

    /**
     * 停止流程
     */
    public static class Stop extends BaseProcess {

    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class BaseProcess extends BaseJsonModel {
        /**
         * 脚本 ID
         */
        private String scriptId;
        /**
         * 执行参数
         */
        private String scriptArgs;

        /**
         * 通过 脚本模版运行
         *
         * @return true
         */
        public boolean runByScript() {
            return StrUtil.isNotEmpty(this.getScriptId());
        }
    }

    @Data
    public static class FileConfig {
        /**
         * 保留文件备份数量
         */
        private Integer backupCount;

        /**
         * 指定备份文件后缀，如果未指定则备份所有类型文件
         */
        private String[] backupSuffix;
    }
}
