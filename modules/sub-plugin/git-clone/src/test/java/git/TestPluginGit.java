/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package git;

import cn.hutool.core.util.RuntimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.plugin.DefaultGitPluginImpl;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class TestPluginGit {


    @Test
    public void testHttp() throws Exception {
        Map<String, Object> map = new HashMap<>();
//        map.put("gitProcessType", "JGit");
        map.put("gitProcessType", "SystemGit");
//        map.put("url", "git@gitee.com:keepbx/Jpom-demo-case.git");//git@github.com:emqx/emqx-operator.git
        map.put("url", "https://gitee.com/keepbx/Jpom-demo-case");
        map.put("reduceProgressRatio", 1);
        map.put("timeout", 60);
        map.put("depth", 0);
        map.put("protocol", 1);
        map.put("username", "");
        map.put("password", "");
        map.put("rsaFile", new File("C:\\Users\\hong_\\.ssh\\id_rsa_gitee"));
        map.put("savePath", new File("C:\\Users\\hong_\\Desktop\\test\\Jpom-demo-case"));
        map.put("branchName", "master");
        map.put("tagName", "1.1");
        map.put("logWriter", new PrintWriter("C:\\Users\\hong_\\Desktop\\test\\aaa.txt"));
//        Object obj = new DefaultGitPluginImpl().execute("branchAndTagList", map);
//        Object obj = new DefaultGitPluginImpl().execute("pull", map);
        Object obj = new DefaultGitPluginImpl().execute("pullByTag", map);
        if (obj instanceof String[]) {
            String[] strs = (String[]) obj;
            for (String str : strs) {
                System.err.println(str);
            }
            return;
        }
        System.err.println(obj);
    }

    @Test
    public void testHttp2() throws Exception {
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

    @Test
    public void testSsh() {
        String repoUrl = "git@gitee.com:keepbx/Jpom-demo-case.git";
        String privateKeyPath = "C:/Users/bwcx_/.ssh/id_rsa";
//        String localPath = "/path/to/local/repo";

        try {
            ProcessBuilder pb = new ProcessBuilder("git", "ls-remote", "git@gitee.com/keepbx/Jpom-demo-case.git");
//            pb.environment().put("GIT_SSH_COMMAND", "ssh -i " + privateKeyPath + " -o StrictHostKeyChecking=no");
            Map<String, String> environment = pb.environment();
            environment.put("GIT_SSH_COMMAND", "ssh -o StrictHostKeyChecking=no git@gitee.com -o PubkeyAuthentication=yes  -o PreferredAuthentications=publickey -o IdentityFile=C:/Users/bwcx_/.ssh/id_rsa -o IdentitiesOnly=yes ");
            environment.put("GIT_SSH_VARIANT", "ssh");
            Process process = pb.start();
            String result = RuntimeUtil.getResult(process);
            String errorResult = RuntimeUtil.getErrorResult(process);
            System.out.println(result);
            System.out.println(errorResult);
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Git pull success!");
            } else {
                System.out.println("Git pull failed!");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
