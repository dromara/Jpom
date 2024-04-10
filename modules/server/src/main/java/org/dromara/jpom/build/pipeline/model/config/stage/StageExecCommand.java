package org.dromara.jpom.build.pipeline.model.config.stage;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.build.pipeline.model.config.BaseStage;
import org.springframework.util.Assert;

import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2024/4/8
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class StageExecCommand extends BaseStage {

    /**
     * 执行的脚本
     * <p>
     * |：文中自动换行，默认仅保留一行空行
     * <p>
     * |+：文中自动换行，保留字符串后面所有的空行
     * <p>
     * |-：文中自动换行，删除字符串后面所有的空行
     * <p>
     * >：文中不自动换行，默认仅保留一行空行
     * <p>
     * >+：文中不自动换行，保留字符串后面所有的空行
     * <p>
     * >-：文中不自动换行，删除字符串后面所有的空行
     */
    private String commands;
    /**
     * 环境变量
     */
    private Map<String, String> env;
    /**
     * 脚本执行超时时间
     */
    private Integer timeout;

    @Override
    public StageExecCommand verify(String prefix) {
        super.verify(prefix);
        Assert.hasText(this.commands, prefix + "阶段执行的脚本不能为空");
        return this;
    }
}
