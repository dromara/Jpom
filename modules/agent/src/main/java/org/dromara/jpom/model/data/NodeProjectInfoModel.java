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
package org.dromara.jpom.model.data;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.common.commander.CommandOpResult;
import org.dromara.jpom.model.RunMode;
import org.dromara.jpom.system.JpomRuntimeException;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;

/**
 * 项目配置信息实体
 *
 * @author bwcx_jzy
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NodeProjectInfoModel extends BaseWorkspaceModel {
    /**
     * 分组
     */
    private String group;
    /**
     * 项目路径
     */
    private String lib;
    /**
     * 授权目录
     */
    private String whitelistDirectory;
    /**
     * 日志目录
     */
    private String logPath;
    /**
     * java 模式运行的 class
     */
    private String mainClass;
    /**
     * jvm 参数
     */
    private String jvm;
    /**
     * java main 方法参数
     */
    private String args;
    /**
     * WebHooks
     */
    private String token;
    /**
     * 项目运行模式
     */
    private RunMode runMode;
    /**
     * 软链的父级项目id
     */
    private String linkId;
    /**
     * 节点分发项目，不允许在项目管理中编辑
     */
    private Boolean outGivingProject;
    /**
     * -Djava.ext.dirs=lib -cp conf:run.jar
     * 填写【lib:conf】
     */
    private String javaExtDirsCp;
    /**
     * 项目自动启动
     */
    private Boolean autoStart;
    /**
     * dsl yml 内容
     *
     * @see DslYmlDto
     */
    private String dslContent;
    /**
     * dsl 环境变量
     */
    private String dslEnv;
    /**
     * 最后一次执行 reload 结果
     */
    private CommandOpResult lastReloadResult;
    //  ---------------- 中转字段 start
    /**
     * 是否可以重新加载
     */
    private Boolean canReload;
    /**
     * DSL 流程信息统计
     */
    private List<JSONObject> dslProcessInfo;
    /**
     * 实际运行的命令
     */
    private String runCommand;
    //  ---------------- 中转字段 end


    public String javaExtDirsCp() {
        return StrUtil.emptyToDefault(javaExtDirsCp, StrUtil.EMPTY);
    }

    public boolean isOutGivingProject() {
        return outGivingProject != null && outGivingProject;
    }

    public String mainClass() {
        return StrUtil.emptyToDefault(mainClass, StrUtil.EMPTY);
    }

    public String whitelistDirectory() {
        if (StrUtil.isEmpty(whitelistDirectory)) {
            throw new JpomRuntimeException("恢复授权数据异常或者没有选择授权目录");
        }
        return whitelistDirectory;
    }

    public String allLib() {
        String directory = this.whitelistDirectory();
        return FileUtil.file(directory, this.getLib()).getAbsolutePath();
    }

    public String logPath() {
        return StrUtil.emptyToDefault(this.logPath, StrUtil.EMPTY);
    }

    /**
     * 默认
     *
     * @return url token
     */
    public String token() {
        // 兼容旧数据
        if ("no".equalsIgnoreCase(this.token)) {
            return "";
        }
        return StrUtil.emptyToDefault(token, StrUtil.EMPTY);
    }

    /**
     * 获取当前 dsl 配置
     *
     * @return DslYmlDto
     */
    public DslYmlDto dslConfig() {
        String dslContent = this.getDslContent();
        if (StrUtil.isEmpty(dslContent)) {
            return null;
        }
        return DslYmlDto.build(dslContent);
    }

    /**
     * 必须存在 dsl 配置
     *
     * @return DslYmlDto
     */
    public DslYmlDto mustDslConfig() {
        DslYmlDto dslYmlDto = this.dslConfig();
        Assert.notNull(dslYmlDto, "未配置 dsl 信息（项目信息错误）");
        return dslYmlDto;
    }

    /**
     * 获取 dsl 流程信息
     *
     * @param opt 操作
     * @return 结果
     */
    public DslYmlDto.BaseProcess tryDslProcess(String opt) {
        DslYmlDto build = dslConfig();
        return tryDslProcess(build, opt);
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
    public DslYmlDto.BaseProcess getDslProcess(String opt) {
        DslYmlDto.BaseProcess baseProcess = this.tryDslProcess(opt);
        Assert.notNull(baseProcess, "DSL 未配置运行管理或者未配置 " + opt + " 流程");
        return baseProcess;
    }
}
