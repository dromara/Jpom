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
@TableName(value = "COMMAND_EXEC_LOG",
    nameKey = "i18n.command_execution_record.56d5", parents = CommandModel.class)
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

    /**
     * 退出码
     */
    private Integer exitCode;

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
