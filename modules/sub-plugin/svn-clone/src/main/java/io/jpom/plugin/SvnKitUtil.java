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
package io.jpom.plugin;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import io.jpom.system.JpomRuntimeException;
import io.jpom.util.CommandUtil;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.wc.*;

import java.io.File;
import java.util.Map;

/**
 * svn 工具
 * <p>
 * https://www.cnblogs.com/lekko/p/6005382.html
 *
 * @author bwcx_jzy
 * @since 2019/8/6
 */
public class SvnKitUtil {

    static {
        // 初始化库。 必须先执行此操作。具体操作封装在setupLibrary方法中。
        /*
         * For using over http:// and https://
         */
        DAVRepositoryFactory.setup();
        /*
         * For using over svn:// and svn+xxx://
         */
        SVNRepositoryFactoryImpl.setup();

        /*
         * For using over file:///
         */
        FSRepositoryFactory.setup();
    }

    private static final DefaultSVNOptions OPTIONS = SVNWCUtil.createDefaultOptions(true);

    /**
     * 对SVNKit连接进行认证，并获取连接
     *
     * @param map 仓库
     */
    public static SVNClientManager getAuthClient(Map<String, Object> map) {
        String protocol = (String) map.get("protocol");
        //repositoryModel.getProtocol();
        String userName = (String) map.get("userName");
        String password = StrUtil.emptyToDefault((CharSequence) map.get("password"), StrUtil.EMPTY);
        //
        ISVNAuthenticationManager authManager;
        //
        if (StrUtil.equalsIgnoreCase(protocol, "http")) {
            authManager = SVNWCUtil.createDefaultAuthenticationManager(userName, password.toCharArray());
        } else if (StrUtil.equalsIgnoreCase(protocol, "ssh")) {
            File dir = SVNWCUtil.getDefaultConfigurationDirectory();
            // ssh
            File rsaFile = (File) map.get("rsaFile");
            //BuildUtil.getRepositoryRsaFile(repositoryModel);
            char[] pwdEmpty = StrUtil.EMPTY.toCharArray();
            if (rsaFile == null) {
                authManager = SVNWCUtil.createDefaultAuthenticationManager(dir, userName, pwdEmpty, null, pwdEmpty, true);
            } else {
                if (StrUtil.isEmpty(password)) {
                    authManager = SVNWCUtil.createDefaultAuthenticationManager(dir, userName, pwdEmpty, rsaFile, pwdEmpty, true);
                } else {
                    authManager = SVNWCUtil.createDefaultAuthenticationManager(dir, userName, pwdEmpty, rsaFile, password.toCharArray(), true);
                }
            }
        } else {
            throw new IllegalStateException("不支持的协议类型");
        }
        // 实例化客户端管理类
        return SVNClientManager.newInstance(OPTIONS, authManager);
    }

    /**
     * 判断当前仓库url是否匹配
     *
     * @param wcDir 仓库路径
     * @param map   参数
     * @return true 匹配
     * @throws SVNException 异常
     */
    private static Boolean checkUrl(File wcDir, Map<String, Object> map) throws SVNException {
        String url = (String) map.get("url");
        // 实例化客户端管理类
        SVNClientManager clientManager = getAuthClient(map);
        try {
            // 通过客户端管理类获得updateClient类的实例。
            SVNWCClient wcClient = clientManager.getWCClient();
            SVNInfo svnInfo = null;
            do {
                try {
                    svnInfo = wcClient.doInfo(wcDir, SVNRevision.HEAD);
                } catch (SVNException svn) {
                    if (svn.getErrorMessage().getErrorCode() == SVNErrorCode.FS_NOT_FOUND) {
                        checkOut(clientManager, url, wcDir);
                    } else {
                        throw svn;
                    }
                }
            } while (svnInfo == null);
            String reUrl = svnInfo.getURL().toString();
            return reUrl.equals(url);
        } finally {
            clientManager.dispose();
        }
    }

    /**
     * SVN检出
     *
     * @param map        参数
     * @param targetPath 目录
     * @return Boolean
     * @throws SVNException svn
     */
    public static String[] checkOut(Map<String, Object> map, File targetPath) throws SVNException {
        // 实例化客户端管理类
        SVNClientManager ourClientManager = getAuthClient(map);
        String url = (String) map.get("url");
        String path = FileUtil.getAbsolutePath(targetPath);
        synchronized (StrUtil.concat(false, url, path).intern()) {
            try {
                if (targetPath.exists()) {
                    if (!FileUtil.file(targetPath, SVNFileUtil.getAdminDirectoryName()).exists()) {
                        CommandUtil.systemFastDel(targetPath);
                    } else {
                        // 判断url是否变更
                        if (!checkUrl(targetPath, map)) {
                            CommandUtil.systemFastDel(targetPath);
                        } else {
                            ourClientManager.getWCClient().doCleanup(targetPath);
                        }
                    }
                }
                return checkOut(ourClientManager, url, targetPath);
            } finally {
                ourClientManager.dispose();
            }
        }
    }

    /**
     * SVN检出
     *
     * @param ourClientManager svn client
     * @param url              仓库地址
     * @param targetPath       保存目录
     * @return String[] 第一个元素为版本号，第二个元素为描述
     * @throws SVNException svn 异常
     */
    private static String[] checkOut(SVNClientManager ourClientManager, String url, File targetPath) throws SVNException {
        // 通过客户端管理类获得updateClient类的实例。
        SVNUpdateClient updateClient = ourClientManager.getUpdateClient();
        updateClient.setIgnoreExternals(false);
        // 相关变量赋值
        SVNURL svnurl = SVNURL.parseURIEncoded(url);
        try {
            // 要把版本库的内容check out到的目录
            long workingVersion = updateClient.doCheckout(svnurl, targetPath, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, true);
            return new String[]{workingVersion + "", String.format("把版本：%s check out ", workingVersion)};
        } catch (SVNAuthenticationException s) {
            throw new JpomRuntimeException("账号密码不正确", s);
        }
    }
}
