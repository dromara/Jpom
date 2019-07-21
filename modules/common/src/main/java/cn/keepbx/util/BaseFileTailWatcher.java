package cn.keepbx.util;

import java.io.File;
import java.io.IOException;

/**
 * @author bwcx_jzy
 * @date 2019/7/21
 */
public abstract class BaseFileTailWatcher {
    protected FileTailWatcherRun tailWatcherRun;
    protected File logFile;

    public BaseFileTailWatcher(File logFile) throws IOException {
        this.logFile = logFile;
        this.tailWatcherRun = new FileTailWatcherRun(logFile, this::sendAll);
    }

    /**
     * 有新的日志
     *
     * @param msg 日志
     */
    protected abstract void sendAll(String msg);


    /**
     * 关闭
     */
    protected void close() {
        this.tailWatcherRun.close();
    }
}
