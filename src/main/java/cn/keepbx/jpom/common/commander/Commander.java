package cn.keepbx.jpom.common.commander;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.common.commander.impl.LinuxCommander;
import cn.keepbx.jpom.common.commander.impl.WindowsCommander;
import cn.keepbx.jpom.model.ProjectInfoModel;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Properties;

public abstract class Commander {

    private static Commander commander = null;
    protected Charset charset = null;

    /**
     * 实例化Commander
     * @return
     */
    public static Commander getInstance() {

        if (commander != null) {
            return commander;
        }

        String os = System.getProperty("os.name");

        if (os.toLowerCase().startsWith("linux")) {
            // Linux系统
            commander = new LinuxCommander();
        }

        if (os.toLowerCase().startsWith("windows")) {
            // Winddows系统
            commander = new WindowsCommander();
        }
        return commander;
    }

    /**
     * 启动
     * @return
     */
    public abstract String start(ProjectInfoModel projectInfoModel) throws Exception;

    /**
     * 停止
     * @return
     */
    public abstract String stop(String tag) throws Exception;

    /**
     * 重启
     * @return
     */
    public abstract String restart(ProjectInfoModel projectInfoModel) throws Exception;

    /**
     * 查看状态
     * @return
     */
    public String status(String tag) throws Exception {
        String result = "stopped";
        // 通过VirtualMachine.list()列出所有的java进程
        List<VirtualMachineDescriptor> listvm = VirtualMachine.list();

        for(VirtualMachineDescriptor vmd : listvm) {
            // 根据进程id查询启动属性，如果属性-Dapplication匹配，说明项目已经启动，并返回进程id
            Properties properties = VirtualMachine.attach(vmd.id()).getAgentProperties();
            if (properties.getProperty("sun.jvm.args").contains("-Dapplication=" + tag)) {
                result = "running:" + vmd.id();
            }
        }
        return result;
    }

    /**
     * 执行命令
     * @param command
     * @return
     * @throws Exception
     */
    protected String exec(String command) throws Exception {
        String result;
        // 执行命令
        Process process = Runtime.getRuntime().exec(command);
        InputStream is;
        if (process.waitFor() == 0) {
            is = process.getInputStream();
        } else {
            is = process.getErrorStream();
        }

        // 读取io为字符串
        result = IoUtil.read(is, charset);
        is.close();
        process.destroy();
        if (StrUtil.isEmpty(result)) {
            result = "没有返回任何执行结果";
        }

        return result;
    }

}
