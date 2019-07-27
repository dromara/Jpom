import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author bwcx_jzy
 * @date 2019/7/9
 **/
public class TestGit {


    public static void main(String[] args) throws GitAPIException, IOException, URISyntaxException {

        Git git = Git.init().setDirectory(new File("D:\\tttt")).call();
        RemoteConfig remoteConfig = Git.open(new File("D:\\tttt")).remoteAdd().setUri(new URIish("https://gitee.com/keepbx/Jpom.git")).call();
        System.out.println(remoteConfig);

        List<RemoteConfig> call = git.remoteList().call();
        System.out.println(call);
        git.pull().call();
        List<Ref> list = git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call();
        List<String> all = new ArrayList<>(list.size());
        list.forEach(ref -> {
            String name = ref.getName();
            if (name.startsWith(Constants.R_REMOTES + Constants.DEFAULT_REMOTE_NAME)) {
                all.add(name.substring((Constants.R_REMOTES + Constants.DEFAULT_REMOTE_NAME).length() + 1));
            }
        });
        System.out.println(all);
    }


}
