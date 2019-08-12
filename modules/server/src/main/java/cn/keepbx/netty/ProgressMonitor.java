package cn.keepbx.netty;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import com.jcraft.jsch.SftpProgressMonitor;

import java.io.PipedOutputStream;


/**
 * sftp进度条显示
 *
 * @author myzf
 * @date 2019/8/11 20:10
 */
public class ProgressMonitor implements SftpProgressMonitor {

    /**
     * 记录传输是否结束
     */
    private boolean isEnd = false;
    /**
     * 记录已传输的数据总大小
     */
    private long transfered;
    /**
     * 记录文件总大小
     */
    private long fileSize;

    private long before;
    private long speed;
    private PipedOutputStream pipedOutputStream;

    public ProgressMonitor(PipedOutputStream pipedOutputStream) {
        this.pipedOutputStream = pipedOutputStream;
    }

    private void printProgress(double rate, long speed) {
        int progressWidth = 20;
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
        sb.append("] ").append(String.format("%.2f", rate * 100)).append("%    ").append(String.format("%8s", FileUtil.readableFileSize(speed))).append("/S");

        System.out.println(sb.toString());
    }


    /**
     * 实现了SftpProgressMonitor接口的count方法
     */
    @Override
    public boolean count(long count) {
        if (isEnd()) {
            return false;
        }
        add(count);
        //
        // 判断传输是否已结束
        long transfered = getTransfered();
        if (transfered != fileSize) {
            // 判断当前已传输数据大小是否等于文件总大小
            speed = transfered - before;
            before = transfered;
            double rate = transfered / (double) fileSize;
            printProgress(rate, speed);
        } else {
            // 如果当前已传输数据大小等于文件总大小，说明已完成，设置end
            setEnd(true);
        }
        return true;
    }

    /**
     * 实现了SftpProgressMonitor接口的end方法
     */
    @Override
    public void end() {
        setEnd(true);
        printProgress(1, speed);
        IoUtil.close(pipedOutputStream);
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

    @Override
    public void init(int op, String src, String dest, long max) {
        this.fileSize = max;
    }
}