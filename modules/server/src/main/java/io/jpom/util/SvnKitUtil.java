package io.jpom.util;

import cn.hutool.core.io.FileUtil;
import io.jpom.system.JpomRuntimeException;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.wc.*;

import java.io.File;

/**
 * svn 工具
 *
 * @author bwcx_jzy
 * @date 2019/8/6
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

    /**
     * 判断当前仓库url是否匹配
     *
     * @param wcDir    仓库路径
     * @param url      url
     * @param userName 用户名
     * @param userPwd  密码
     * @return true 匹配
     * @throws SVNException 异常
     */
    private static Boolean checkUrl(File wcDir, String url, String userName, String userPwd) throws SVNException {
        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(userName, userPwd.toCharArray());
        DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true);
        // 实例化客户端管理类
        SVNClientManager clientManager = SVNClientManager.newInstance(options, authManager);
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
     * @param userName   用户名
     * @param userPwd    密码
     * @param svnPath    仓库路径
     * @param targetPath 目录
     * @return Boolean
     * @throws SVNException svn
     */
    public static String checkOut(String svnPath, String userName, String userPwd, File targetPath) throws SVNException {
        DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true);
        // 实例化客户端管理类
        SVNClientManager ourClientManager = SVNClientManager.newInstance(options, userName, userPwd);
        try {
            if (targetPath.exists()) {
                if (!FileUtil.file(targetPath, SVNFileUtil.getAdminDirectoryName()).exists()) {
                    if (!FileUtil.del(targetPath)) {
                        FileUtil.del(targetPath.toPath());
                    }
                } else {
                    // 判断url是否变更
                    if (!checkUrl(targetPath, svnPath, userName, userPwd)) {
                        if (!FileUtil.del(targetPath)) {
                            FileUtil.del(targetPath.toPath());
                        }
                    } else {
                        ourClientManager.getWCClient().doCleanup(targetPath);
                    }
                }
            }
            return checkOut(ourClientManager, svnPath, targetPath);
        } finally {
            ourClientManager.dispose();
        }
    }

    private static String checkOut(SVNClientManager ourClientManager, String url, File targetPath) throws SVNException {
        // 通过客户端管理类获得updateClient类的实例。
        SVNUpdateClient updateClient = ourClientManager.getUpdateClient();
        updateClient.setIgnoreExternals(false);
        // 相关变量赋值
        SVNURL svnurl = SVNURL.parseURIEncoded(url);
        try {
            // 要把版本库的内容check out到的目录
            long workingVersion = updateClient.doCheckout(svnurl, targetPath, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, true);
            return String.format("把版本：%s check out ", workingVersion);
        } catch (SVNAuthenticationException s) {
            throw new JpomRuntimeException("账号密码不正常", s);
        }
    }
}
