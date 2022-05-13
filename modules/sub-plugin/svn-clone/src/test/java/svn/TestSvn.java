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
package svn;

import io.jpom.plugin.SvnKitUtil;
import org.tmatesoft.svn.core.SVNException;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2019/8/6
 */
public class TestSvn {
    public static void main(String[] args) throws SVNException {

//		RepositoryModel repositoryModel = new RepositoryModel();
//		repositoryModel.setGitUrl("svn://gitee.com/keepbx/Jpom-demo-case");
//		repositoryModel.setRepoType(RepositoryModel.RepoType.Svn.getCode());
//		repositoryModel.setUserName("a");
//		repositoryModel.setPassword("a");
//		repositoryModel.setProtocol(GitProtocolEnum.SSH.getCode());

        Map<String, Object> map = new HashMap<>(10);
        map.put("url", "svn://gitee.com/keepbx/Jpom-demo-case");
        map.put("userName", "a");
        map.put("password", "a");
        map.put("protocol", "ssh");
        //File rsaFile = BuildUtil.getRepositoryRsaFile(repositoryModel);
        //map.put("rsaFile", rsaFile);

        String[] s = SvnKitUtil.checkOut(map, new File("/test/tt"));
        System.out.println(Arrays.toString(s));
    }
}
