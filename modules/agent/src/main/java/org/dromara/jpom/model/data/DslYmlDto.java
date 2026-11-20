/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.data;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.setting.yaml.YamlUtil;
import cn.keepbx.jpom.model.BaseJsonModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
     * 配置
     */
    private Config config;

    /**
     * 判断是否包含指定流程
     *
     * @param opt 流程名
     * @return true
     */
    public boolean hasRunProcess(String opt) {
        DslYmlDto.Run run = this.getRun();
        if (run == null) {
            return false;
        }
        DslYmlDto.BaseProcess baseProcess = (DslYmlDto.BaseProcess) ReflectUtil.getFieldValue(run, opt);
        return baseProcess != null;
    }

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
        private Restart restart;
        private Reload reload;
        /**
         * 文件变动是否执行重新加载
         */
        private Boolean fileChangeReload;
        /**
         * 在指定目录执行
         */
        private String execPath;
    }

    /**
     * 重新加载
     *
     * @see org.dromara.jpom.socket.ConsoleCommandOp
     */
    public static class Reload extends BaseProcess {

    }

    /**
     * 启动流程
     *
     * @see org.dromara.jpom.socket.ConsoleCommandOp
     */
    public static class Start extends BaseProcess {

    }

    /**
     * 获取状态流程
     *
     * @see org.dromara.jpom.socket.ConsoleCommandOp
     */
    public static class Status extends BaseProcess {

    }

    /**
     * 停止流程
     *
     * @see org.dromara.jpom.socket.ConsoleCommandOp
     */
    public static class Stop extends BaseProcess {

    }

    /**
     * 重启流程
     *
     * @see org.dromara.jpom.socket.ConsoleCommandOp
     */
    public static class Restart extends BaseProcess {

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
         * 执行脚本的环境变量
         */
        private Map<String, String> scriptEnv;
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

        /**
         * 项目文件备份路径
         */
        private String backupPath;

        /**
         * 是否开启差异备份，默认使用差异备份
         */
        private Boolean diffBackup;

        /**
         * 构建发布时保留的完整版本快照个数，0 关闭，未配置则使用全局配置
         */
        private Integer versionCount;

        /**
         * 版本快照存储路径，未配置则使用全局配置
         */
        private String versionPath;
    }

    @Data
    public static class Config {
        /**
         * 是否自动将控制台日志文件备份
         */
        private Boolean autoBackToFile;

        /**
         * 项目运行日志文件列表
         * <p>
         * 配置后项目控制台可以直接选择并实时查看这些日志文件（DSL 项目启动的应用通常自己管理日志文件，
         * 不写入 Jpom 控制台日志）
         */
        private List<LogFile> log;
    }

    /**
     * 项目运行日志文件配置
     */
    @Data
    public static class LogFile {
        /**
         * 显示名称（控制台下拉框中的名称），未配置时使用文件名
         */
        private String name;
        /**
         * 日志文件路径，相对项目路径或绝对路径，支持 ${PROJECT_PATH}、${PROJECT_ID}、${PROJECT_NAME} 变量
         */
        private String path;
        /**
         * 文件编码，未配置时使用项目配置的日志编码（再回退到全局配置）
         */
        private String charset;
    }


    /**
     * 获取 dsl 流程信息
     *
     * @param opt 操作
     * @return 结果
     */
    public static DslYmlDto.BaseProcess tryDslProcess(DslYmlDto build, String opt) {
        return Optional.ofNullable(build)
            .map(DslYmlDto::getRun)
            .map(run -> (DslYmlDto.BaseProcess) ReflectUtil.getFieldValue(run, opt))
            .orElse(null);
    }

    /**
     * 获取 dsl 流程信息
     *
     * @param opt 操作
     * @return 结果
     */
    public DslYmlDto.BaseProcess tryDslProcess(String opt) {
        return tryDslProcess(this, opt);
    }

    /**
     * 获取 dsl 流程信息
     *
     * @param opt 操作
     * @return 结果
     */
    public DslYmlDto.BaseProcess getDslProcess(String opt) {
        DslYmlDto.BaseProcess baseProcess = this.tryDslProcess(opt);
        Assert.notNull(baseProcess, StrUtil.format(I18nMessageUtil.get("i18n.dsl_not_configured.8a57"), opt));
        return baseProcess;
    }
}
