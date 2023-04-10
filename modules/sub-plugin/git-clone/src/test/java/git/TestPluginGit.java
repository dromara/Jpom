package git;

import org.dromara.jpom.plugin.DefaultGitPluginImpl;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class TestPluginGit {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestGitEnv.class);

    @Test
    public void testHttp() throws Exception {
        Map<String, Object> map = new HashMap<>();
//        map.put("gitProcessType", "JGit");
        map.put("gitProcessType", "SystemGit");
        map.put("url", "git@gitee.com:keepbx/Jpom-demo-case.git");//git@github.com:emqx/emqx-operator.git
//        map.put("url", "https://gitee.com/keepbx/Jpom-demo-case");
        map.put("reduceProgressRatio", 1);
        map.put("timeout", 60);
        map.put("protocol", 1);
        map.put("username", "");
        map.put("password", "");
        map.put("rsaFile", new File("C:\\Users\\hong_\\.ssh\\id_rsa_gitee"));
        map.put("savePath", new File("C:\\Users\\hong_\\Desktop\\test\\Jpom-demo-case"));
        map.put("branchName", "master");
        map.put("tagName", "1.1");
        map.put("logWriter", new PrintWriter("C:\\Users\\hong_\\Desktop\\test\\aaa.txt"));
        Object obj = new DefaultGitPluginImpl().execute("branchAndTagList", map);
//        Object obj = new DefaultGitPluginImpl().execute("pull", map);
//        Object obj = new DefaultGitPluginImpl().execute("pullByTag", map);
        if (obj instanceof String[]) {
            String[] strs = (String[]) obj;
            for (String str : strs) {
                System.err.println(str);
            }
            return;
        }
        System.err.println(obj);
    }
}
