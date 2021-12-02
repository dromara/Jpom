/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 码之科技工作室
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
