/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
//package org.dromara.jpom.plugin;
//
//import cn.hutool.core.io.FileUtil;
//import cn.hutool.core.lang.Tuple;
//import cn.hutool.crypto.SecureUtil;
//import org.dromara.jpom.system.JpomRuntimeException;
//import org.eclipse.jgit.api.CloneCommand;
//import org.eclipse.jgit.api.Git;
//import org.eclipse.jgit.api.ListBranchCommand;
//import org.eclipse.jgit.api.PullCommand;
//import org.eclipse.jgit.api.errors.GitAPIException;
//import org.eclipse.jgit.api.errors.TransportException;
//import org.eclipse.jgit.lib.Constants;
//import org.eclipse.jgit.lib.Ref;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
///**
// * 兼容低版本的分支获取
// *
// * @author bwcx_jzy
// * @since 2024/4/12
// */
//public class JGitOldUtil {
//
//    /**
//     * 删除重新clone
//     *
//     * @param url       url
//     * @param file      文件
//     * @param parameter 参数
//     * @return git
//     * @throws GitAPIException api
//     * @throws IOException     删除文件失败
//     */
//    private static Git reClone(String url, Map<String, Object> parameter, File file) throws GitAPIException, IOException {
//        if (!FileUtil.clean(file)) {
//            FileUtil.del(file.toPath());
//            //throw new IOException("del error:" + file.getPath());
//        }
//        CloneCommand cloneCommand = Git.cloneRepository();
//
//        CloneCommand command = cloneCommand.setURI(url)
//            .setDirectory(file);
//        JGitUtil.setCredentials(command, parameter);
//        return command
//            .call();
//    }
//
//    private static Git initGit(String url, Map<String, Object> parameter, File file) throws IOException, GitAPIException {
//        Git git;
//        if (FileUtil.file(file, Constants.DOT_GIT).exists()) {
//            if (JGitUtil.checkRemoteUrl(url, file)) {
//                git = Git.open(file);
//                //
//                PullCommand pull = git.pull();
//                JGitUtil.setCredentials(pull, parameter);
//                pull.call();
//            } else {
//                git = reClone(url, parameter, file);
//            }
//        } else {
//            git = reClone(url, parameter, file);
//        }
//        return git;
//    }
//
//    /**
//     * 获取仓库远程的所有分支
//     *
//     * @param url       远程url
//     * @param file      仓库clone到本地的文件夹
//     * @param parameter 参数
//     * @return list
//     * @throws GitAPIException api
//     * @throws IOException     IO
//     */
//    private static Tuple branchList(String url, Map<String, Object> parameter, File file) throws Exception {
//        try (Git git = initGit(url, parameter, file)) {
//            //
//            List<Ref> list = git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call();
//            List<String> all = new ArrayList<>(list.size());
//
//            list.forEach(ref -> {
//                String name = ref.getName();
//                if (name.startsWith(Constants.R_REMOTES + Constants.DEFAULT_REMOTE_NAME)) {
//                    all.add(name.substring((Constants.R_REMOTES + Constants.DEFAULT_REMOTE_NAME).length() + 1));
//                }
//            });
//            List<Ref> call = git.tagList().call();
//            List<String> tag = new ArrayList<>(call.size());
//            call.forEach(ref -> {
//                String name = ref.getName();
//                if (name.startsWith(Constants.R_TAGS)) {
//                    tag.add(name.substring((Constants.R_TAGS).length()));
//                }
//            });
//            return new Tuple(all, tag);
//        } catch (TransportException t) {
//            JGitUtil.checkTransportException(t, file, null);
//        }
//        return null;
//    }
//
//    public static Tuple getBranchList(String url, Map<String, Object> parameter) throws Exception {
//        //  生成临时路径
//        String tempId = SecureUtil.md5(url);
//        File tmpDir = FileUtil.getTmpDir();
//        File gitFile = FileUtil.file(tmpDir, "jpom", "git-temp", tempId);
//        try {
//            Tuple list = branchList(url, parameter, gitFile);
//            if (list == null) {
//                throw new JpomRuntimeException("该仓库还没有任何分支");
//            }
//            return list;
//        } catch (org.eclipse.jgit.errors.RepositoryNotFoundException ignored) {
//            FileUtil.del(gitFile);
//        }
//        return null;
//    }
//}
