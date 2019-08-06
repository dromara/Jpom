package cn.keepbx.util;

import cn.hutool.core.io.FileUtil;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.wc.*;

import java.io.File;

/**
 * svn 工具
 *
 * @author bwcx_jzy
 * @date 2019/8/6
 */
public class SVNKitUtil {

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
     * @param wcDir 仓库路径
     * @param url   url
     * @return true 匹配
     * @throws SVNException 异常
     */
    private static Boolean checkUrl(File wcDir, String url) throws SVNException {
        DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true);
        // 实例化客户端管理类
        SVNClientManager ourClientManager = SVNClientManager.newInstance(options);
        // 通过客户端管理类获得updateClient类的实例。
        SVNWCClient wcClient = ourClientManager.getWCClient();
        SVNInfo svnInfo = wcClient.doInfo(wcDir, SVNRevision.HEAD);
        String reUrl = svnInfo.getURL().toString();
        return reUrl.equals(url);
    }

    /**
     * SVN检出
     *
     * @return Boolean
     */
    public static String checkOut(String svnPath, String userName, String userPwd, File targetPath) throws SVNException {
        DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true);
        // 实例化客户端管理类
        SVNClientManager ourClientManager = SVNClientManager.newInstance(options, userName, userPwd);
        // 执行check out 操作，返回工作副本的版本号。
        if (targetPath.exists()) {
            ourClientManager.getWCClient().doCleanup(targetPath);
        }
        if (targetPath.exists()) {
            if (!FileUtil.file(targetPath, ".svn").exists()) {
                FileUtil.del(targetPath);
            } else {
                if (!checkUrl(targetPath, svnPath)) {
                    FileUtil.del(targetPath);
                }
            }
        }
        // 通过客户端管理类获得updateClient类的实例。
        SVNUpdateClient updateClient = ourClientManager.getUpdateClient();
        updateClient.setIgnoreExternals(false);
        // 相关变量赋值
        SVNURL svnurl = SVNURL.parseURIEncoded(svnPath);
        // 要把版本库的内容check out到的目录
        long workingVersion = updateClient.doCheckout(svnurl, targetPath, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, true);
        return String.format("把版本：%s check out ", workingVersion);
    }
}
