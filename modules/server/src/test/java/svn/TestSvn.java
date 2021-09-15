package svn;

import io.jpom.model.data.RepositoryModel;
import io.jpom.model.enums.GitProtocolEnum;
import io.jpom.util.SvnKitUtil;
import org.tmatesoft.svn.core.SVNException;

import java.io.File;

/**
 * @author bwcx_jzy
 * @date 2019/8/6
 */
public class TestSvn {
	public static void main(String[] args) throws SVNException {

		RepositoryModel repositoryModel = new RepositoryModel();
		repositoryModel.setGitUrl("svn://gitee.com/keepbx/Jpom-demo-case");
		repositoryModel.setRepoType(RepositoryModel.RepoType.Svn.getCode());
		repositoryModel.setUserName("a");
		repositoryModel.setPassword("a");
		repositoryModel.setProtocol(GitProtocolEnum.SSH.getCode());

		String s = SvnKitUtil.checkOut(repositoryModel, new File("/test/tt"));
		System.out.println(s);
	}
}
