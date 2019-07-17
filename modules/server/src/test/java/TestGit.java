import cn.keepbx.util.GitUtil;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;

/**
 * @author bwcx_jzy
 * @date 2019/7/9
 **/
public class TestGit {


    public static void main(String[] args) throws GitAPIException, IOException {

        File file = new File("D:\\Idea\\Fast-boot");

        String msg = GitUtil.getLastCommitMsg(file, "v1.4");
        System.out.println(msg);
    }


}
