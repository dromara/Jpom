package cn.keepbx.jpom;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.util.ArgsUtil;
import cn.keepbx.jpom.util.CommandUtil;
import cn.keepbx.jpom.util.JvmUtil;
import com.sun.tools.attach.VirtualMachine;

import java.io.IOException;

/**
 * 命令行关闭Jpom
 *
 * @author jiangzeyin
 * @date 2019/4/7
 */
public class JpomClose {
    private static JpomClose jpomManager;

    public static void main(String[] args) throws Exception {
        if (args == null || args.length <= 0) {
            Console.error("请传入正确的参数");
            return;
        }
        String tag = ArgsUtil.getArgsValue(args, "jpom.applicationTag");
        if (StrUtil.isEmpty(tag)) {
            Console.error("请传入对应：jpom.applicationTag");
            return;
        }
        // 事件
        String event = ArgsUtil.getArgsValue(args, "event");
        if ("stop".equalsIgnoreCase(event)) {
            String status = JpomClose.getInstance().status(tag);
            if (!status.contains(StrUtil.COLON)) {
                Console.error("Jpom并没有运行");
                return;
            }
            String msg = JpomClose.getInstance().stop(tag);
            Console.log(msg);
        } else if ("status".equalsIgnoreCase(event)) {
            String status = JpomClose.getInstance().status(tag);
            Console.log(status);
        } else {
            Console.error("event error:" + event);
        }
    }

    /**
     * 单利模式
     *
     * @return JpomClose
     */
    public static JpomClose getInstance() {
        if (jpomManager != null) {
            return jpomManager;
        }
        if (BaseJpomApplication.OS_INFO.isLinux()) {
            jpomManager = new Linux();
        } else {
            jpomManager = new Windows();
        }
        return jpomManager;
    }


    public String stop(String tag) throws IOException {
        VirtualMachine virtualMachine = JvmUtil.getVirtualMachine(tag);
        if (virtualMachine == null) {
            return null;
        }
        return virtualMachine.id();
    }

    public String status(String tag) throws IOException {
        VirtualMachine virtualMachine = JvmUtil.getVirtualMachine(tag);
        if (virtualMachine == null) {
            return "Jpom并没有运行";
        }
        return "Jpom运行中:" + virtualMachine.id();
    }


    private static class Windows extends JpomClose {

        @Override
        public String stop(String tag) throws IOException {
            String pid = super.stop(tag);
            if (pid == null) {
                return "stop";
            }
            String cmd = String.format("taskkill /F /PID %s", pid);
            return CommandUtil.execCommand(cmd);
        }
    }

    private static class Linux extends JpomClose {

        @Override
        public String stop(String tag) throws IOException {
            String pid = super.stop(tag);
            if (pid == null) {
                return "stop";
            }
            String cmd = String.format("kill  %s", pid);
            return CommandUtil.execCommand(cmd);
        }
    }
}
