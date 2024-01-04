import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.Tailer;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ReflectUtil;
import org.dromara.jpom.util.CommandUtil;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author bwcx_jzy
 * @since 24/1/3 003
 */
public class TestProcess {

    @Test
    public void testWin() throws IOException {
        AtomicReference<Process> start = new AtomicReference<>();
        // 执行线程
        Thread thread = new Thread(() -> {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.redirectErrorStream(true);
            processBuilder.command("ping 127.0.0.1 -t".split(" "));
            try {
                start.set(processBuilder.start());
                try (InputStream inputStream = start.get().getInputStream()) {
                    IoUtil.readLines(inputStream, CharsetUtil.CHARSET_GBK, new Tailer.ConsoleLineHandler());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        ThreadUtil.execute(thread);
        while (true) {
            Process process = start.get();
            if (process != null) {
                break;
            }
            // System.out.println("waiting...");
        }
        ThreadUtil.sleep(5, TimeUnit.SECONDS);
        // 关闭线程
        Thread thread2 = new Thread(() -> {
            while (true) {
                Process process = start.get();
                if (process.isAlive()) {
                    process.destroy();
                    Object handle = ReflectUtil.getFieldValue(process, "handle");
                    System.out.println(handle);
                    try {
                        process.waitFor(1, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        System.out.println(e.getMessage());
                    }
                } else {
                    System.out.println("成功终止");
                    break;
                }
            }
        });
        thread2.run();
    }

    @Test
    public void testLinux() {
        AtomicReference<Process> start = new AtomicReference<>();
        AtomicBoolean running = new AtomicBoolean(true);
        // 执行线程
        Thread thread = new Thread(() -> {
            running.set(true);
            try {
                ProcessBuilder processBuilder = new ProcessBuilder();
                processBuilder.redirectErrorStream(true);
//                String s = "cd /mnt/d/System-Data/Documents/jpom/server/data/build/5a68e19578654cb2965433584ce84f4c/source && mvn clean package";
                String[] command = new String[]{"bash", "/home/user/test.sh"};
//                String[] command = ArrayUtil.append(new String[]{"/bin/bash", "-c"}, s);
                System.out.println(Arrays.toString(command));
                processBuilder.command(command);
                try {
                    start.set(processBuilder.start());
                    try (InputStream inputStream = start.get().getInputStream()) {
                        IoUtil.readLines(inputStream, CharsetUtil.CHARSET_UTF_8, new Tailer.ConsoleLineHandler());
                    }
                    int waitFor = start.get().waitFor();
                    System.out.println("线程结束：" + waitFor);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } finally {
                running.set(false);
            }
        });
        ThreadUtil.execute(thread);
        while (true) {
            Process process = start.get();
            if (process != null) {

                break;
            }
            if (!running.get()) {
                System.out.println("线程关闭");
                break;
            }
            // System.out.println("waiting...");
        }
        Process process = start.get();
        if (process == null) {
            return;
        }
        ThreadUtil.sleep(20, TimeUnit.SECONDS);
        // 关闭线程
        Thread thread2 = new Thread(() -> {
            while (true) {
                if (process.isAlive()) {
                    Object handle = CommandUtil.tryGetProcessId(process);
                    System.out.println(handle);
                    process.destroy();
                    try {
                        process.waitFor(1, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        System.out.println(e.getMessage());
                    }
                } else {
                    System.out.println("成功终止");
                    break;
                }
            }
        });
        thread2.run();
    }
}
