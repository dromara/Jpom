package cn.jiangzeyin.service.manage;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * Created by jiangzeyin on 2018/9/28.
 */
@Service
public class CommandService {

    public File getCommandFile() {
        File file = new File(getCommandPath());
        if (!file.exists()) {
            throw new RuntimeException("启动文件不存在");
        }
        return file;
    }

    public String getCommandPath() {
        String command = SpringUtil.getEnvironment().getProperty("boot-online.command");
        if (StrUtil.isEmpty(command)) {
            throw new RuntimeException("请配置命令文件");
        }
        return command;
    }
}
