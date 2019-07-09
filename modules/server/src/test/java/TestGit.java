import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.BaseJpomApplication;
import cn.keepbx.jpom.socket.ConsoleCommandOp;
import cn.keepbx.jpom.util.CommandUtil;
import com.alibaba.fastjson.JSONObject;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.RemoteListCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author bwcx_jzy
 * @date 2019/7/9
 **/
public class TestGit {
    static UsernamePasswordCredentialsProvider usernamePasswordCredentialsProvider =
            new UsernamePasswordCredentialsProvider("1", "1");

//    private static final String REMOTE_TAG = "jpomRemote";

    public static void main(String[] args) throws GitAPIException, IOException {

        File file = new File("D:\\test\\git");

        String url = "https://gitee.com/jiangzeyin/test.git";

        List<String> l = branchList(url, file);
        System.out.println(l);
        checkOutPull(url, file, "master");
        //
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(file);
        processBuilder.command("cmd", "/c", "mvn clean package");

        Process process = processBuilder.start();
        {
            InputStream inputStream = process.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, BaseJpomApplication.getCharset());
            BufferedReader results = new BufferedReader(inputStreamReader);
            IoUtil.readLines(results, new LineHandler() {
                @Override
                public void handle(String line) {
                    System.out.println(line);
                }
            });
        }
        {
            InputStream errorInputStream = process.getErrorStream();
            InputStreamReader inputStreamReader = new InputStreamReader(errorInputStream, BaseJpomApplication.getCharset());
            BufferedReader results = new BufferedReader(inputStreamReader);
            IoUtil.readLines(results, new LineHandler() {
                @Override
                public void handle(String line) {
                    System.out.println(line);
                }
            });
        }


    }

    private static boolean checkRemoteUrl(String url, File file) throws IOException, GitAPIException {
        Git git = Git.open(file);
        RemoteListCommand remoteListCommand = git.remoteList();
        boolean urlTrue = false;
        List<RemoteConfig> list = remoteListCommand.call();
        end:
        for (RemoteConfig remoteConfig : list) {
            for (URIish urIish : remoteConfig.getURIs()) {
                if (urIish.toString().equals(url)) {
                    urlTrue = true;
                    break end;
                }
            }
        }
        return urlTrue;
    }

    private static Git reClone(String url, File file) throws GitAPIException, IOException {
        if (!FileUtil.clean(file)) {
            throw new IOException("del error:" + file.getPath());
        }
        return Git.cloneRepository()
                .setURI(url)
                .setDirectory(file)
                .setCredentialsProvider(usernamePasswordCredentialsProvider)
                .call();
    }

    public static List<String> branchList(String url, File file) throws GitAPIException, IOException {
        Git git;
        if (FileUtil.file(file, ".git").exists()) {
            if (checkRemoteUrl(url, file)) {
                git = Git.open(file);
                //
                git.pull().setCredentialsProvider(usernamePasswordCredentialsProvider).call();
            } else {
                git = reClone(url, file);
            }
        } else {
            git = reClone(url, file);
        }
        //
        List<Ref> list = git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call();
        List<String> all = new ArrayList<>(list.size());
        list.forEach(ref -> {
            String name = ref.getName();
            if (name.startsWith(Constants.R_REMOTES + Constants.DEFAULT_REMOTE_NAME)) {
                all.add(name.substring((Constants.R_REMOTES + Constants.DEFAULT_REMOTE_NAME).length() + 1));
            }
        });
        return all;
    }

    public static void checkOutPull(String url, File file, String branchName) throws IOException, GitAPIException {
        boolean r = checkRemoteUrl(url, file);
        if (!r) {
            reClone(url, file);
        }
        Git git = Git.open(file);
        // 判断本地是否存在对应分支
        List<Ref> list = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
        boolean createBranch = true;
        for (Ref ref : list) {
            String name = ref.getName();
            if (name.startsWith(Constants.R_HEADS + branchName)) {
                createBranch = false;
                break;
            }
        }
        // 切换分支
        if (!StrUtil.equals(git.getRepository().getBranch(), branchName)) {
            git.checkout().
                    setCreateBranch(createBranch).
                    setName(branchName).
                    setForceRefUpdate(true).
                    setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM).
                    call();
        }
        git.pull().setCredentialsProvider(usernamePasswordCredentialsProvider).call();
    }
}
