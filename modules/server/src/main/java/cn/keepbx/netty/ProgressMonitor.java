package cn.keepbx.netty;

import cn.hutool.core.io.FileUtil;
import com.jcraft.jsch.SftpProgressMonitor;

import java.util.Timer;
import java.util.TimerTask;


/**
 * sftp进度条显示
 *
 * @param
 * @Author: myzf
 * @Date: 2019/8/11 20:10
 */
public class ProgressMonitor extends TimerTask implements SftpProgressMonitor {

    private long progressInterval = 1 * 1000; // 默认间隔时间为5秒

    private boolean isEnd = false; // 记录传输是否结束

    private long transfered; // 记录已传输的数据总大小

    private long fileSize; // 记录文件总大小

    private Timer timer; // 定时器对象

    private boolean isScheduled = false; // 记录是否已启动timer记时器

    private int progressWidth = 20;

    private double rate = 0;


    private long before;

    private long speed;

    public ProgressMonitor(long fileSize) {
        this.fileSize = fileSize;
    }


    protected void printProgress(double rate, long speed) {
        int downWidth = (int) (progressWidth * rate);
        StringBuilder sb = new StringBuilder();
        sb.append("\r[");
        for (int i = 0; i < progressWidth; i++) {
            if (i < downWidth) {
                sb.append("■");
            } else {
                sb.append("□");
            }
        }
        sb.append("] " + String.format("%.2f", rate * 100) + "%    " + String.format("%8s", FileUtil.readableFileSize(speed)) + "/S");

        System.out.println(sb.toString());
    }

    @Override
    public void run() {
        if (!isEnd()) { // 判断传输是否已结束
            long transfered = getTransfered();
            if (transfered != fileSize) { // 判断当前已传输数据大小是否等于文件总大小
                speed = transfered - before;
                before = transfered;
                rate = transfered / (double) fileSize;
                printProgress(rate, speed);
            } else {
                setEnd(true); // 如果当前已传输数据大小等于文件总大小，说明已完成，设置end
            }
        } else {
            stop(); // 如果传输结束，停止timer记时器
            return;
        }
    }

    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
            isScheduled = false;
        }
    }

    public void start() {
        if (timer == null) {
            timer = new Timer();
        }
        timer.schedule(this, 0, progressInterval);
        isScheduled = true;
    }


    /**
     * 实现了SftpProgressMonitor接口的count方法
     */
    public boolean count(long count) {
        if (isEnd()) return false;
        if (!isScheduled) {
            start();
        }
        add(count);
        return true;
    }

    /**
     * 实现了SftpProgressMonitor接口的end方法
     */
    public void end() {
        setEnd(true);
        printProgress(1, speed);
    }

    private synchronized void add(long count) {
        transfered = transfered + count;
    }

    private synchronized long getTransfered() {
        return transfered;
    }

    public synchronized void setTransfered(long transfered) {
        this.transfered = transfered;
    }

    private synchronized void setEnd(boolean isEnd) {
        this.isEnd = isEnd;
    }

    private synchronized boolean isEnd() {
        return isEnd;
    }

    public void init(int op, String src, String dest, long max) {
        //   log.info("开始从sftp下载文件");
    }
}