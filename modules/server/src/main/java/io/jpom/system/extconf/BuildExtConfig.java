package io.jpom.system.extconf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author bwcx_jzy
 * @since 2022/7/7
 */
@Configuration
@ConfigurationProperties(prefix = "build")
@Data
public class BuildExtConfig {

    /**
     * 构建最多保存多少份历史记录
     */
    private int maxHistoryCount = 1000;

    /**
     * 每一项构建最多保存的历史份数
     */
    private int itemMaxHistoryCount = 50;

    private Boolean checkDeleteCommand;

    /**
     * 构建线程池大小,小于 1 则为不限制，默认大小为 5
     */
    private int poolSize = 5;

    /**
     * 构建任务等待数量，超过此数量将取消构建任务，值最小为 1
     */
    private int poolWaitQueue = 10;


    public boolean checkDeleteCommand() {
        return checkDeleteCommand != null && checkDeleteCommand;
    }

}
