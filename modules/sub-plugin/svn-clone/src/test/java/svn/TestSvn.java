/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package svn;

import org.dromara.jpom.plugin.SvnKitUtil;
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
        map.put("username", "a");
        map.put("password", "a");
        map.put("protocol", "ssh");
        //File rsaFile = BuildUtil.getRepositoryRsaFile(repositoryModel);
        //map.put("rsaFile", rsaFile);

        String[] s = SvnKitUtil.checkOut(map, new File("/test/tt"));
        System.out.println(Arrays.toString(s));
    }
}
