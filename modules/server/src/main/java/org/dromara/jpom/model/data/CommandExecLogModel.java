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

import cn.hutool.core.annotation.PropIgnore;
import cn.hutool.core.io.FileUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseEnum;
import org.dromara.jpom.model.BaseWorkspaceModel;

import java.io.File;

/**
 * @author bwcx_jzy
 * @since 2021/12/22
 */
@TableName(value = "COMMAND_EXEC_LOG", name = "命令执行记录", parents = CommandModel.class)
@Data
@EqualsAndHashCode(callSuper = true)
public class CommandExecLogModel extends BaseWorkspaceModel {

    /**
     * 命令ID
     */
    private String commandId;

    /**
     * 批次ID
     */
    private String batchId;

    /**
     * ssh Id
     */
    private String sshId;

    /**
     * @see Status
     */
    private Integer status;

    /**
     * 命令名称
     */
    private String commandName;

    /**
     * ssh 名称
     */
    private String sshName;

    /**
     * 参数
     */
    private String params;

    /**
     * 触发类型 {0，手动，1 自动触发}
     */
    private Integer triggerExecType;

    /**
     * 日志文件是否存在
     */
    @PropIgnore
    private Boolean hasLog;

    public File logFile() {
        return FileUtil.file(CommandExecLogModel.logFileDir(this.getCommandId()), batchId, this.getId() + ".log");
    }

    /**
     * log 存储目录
     *
     * @param commandId 命令ID
     * @return 文件
     */
    public static File logFileDir(String commandId) {
        return FileUtil.file(JpomApplication.getInstance().getDataPath(), "command_log", commandId);
    }

    @Getter
    public enum Status implements BaseEnum {
        /**
         *
         */
        ING(0, "执行中"),
        DONE(1, "执行结束"),
        ERROR(2, "执行错误"),
        SESSION_ERROR(3, "会话异常"),
        ;
        private final int code;
        private final String desc;

        Status(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }
    }
}
