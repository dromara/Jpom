package org.dromara.jpom.build.pipeline.actuator;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.environment.EnvironmentUtils;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.build.pipeline.model.config.Repository;
import org.dromara.jpom.build.pipeline.model.config.stage.StageExecCommand;
import org.dromara.jpom.system.ExtConfigBean;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;

/**
 * @author bwcx_jzy
 * @since 2024/4/10
 */
public class ExecActuator extends BaseActuator {


    private final StageExecCommand stageExec;
    private String workingDir;

    private Charset charset;
    private File scriptFile;

    public ExecActuator(StageExecCommand stageExec, Map<String, Repository> repositoryMap) {
        super(repositoryMap);
        this.stageExec = stageExec;
        try {
            charset = ExtConfigBean.getConsoleLogCharset();
        } catch (Exception e) {
            // 直接执行，使用默认编码格式
            charset = CharsetUtil.systemCharset();
        }
    }

    @Override
    public void run() throws IOException {
        String repoTag = stageExec.getRepoTag();
        scriptFile = JpomApplication.getInstance().parseScript(stageExec.getCommands());
        //
        Map<String, String> procEnvironment = EnvironmentUtils.getProcEnvironment();
        Map<String, String> env = stageExec.getEnv();
        Optional.ofNullable(env).ifPresent(procEnvironment::putAll);
        //
        final LogOutputStream logOutputStream = new LogOutputStream(1, charset) {
            @Override
            protected void processLine(String line, int logLevel) {

            }
        };
        // 重定向stdout和stderr到文件
        PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(logOutputStream, logOutputStream);
    }

    @Override
    public void close() throws Exception {
        FileUtil.del(scriptFile);
    }
}
