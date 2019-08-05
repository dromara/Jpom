package svnkit;


import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import java.io.File;

/**
 * @author bwcx_jzy
 * @date 2019/8/4
 */
public class SVNKit {

    /**
     * SVN路径
     */
    public static final String SvnPath = "svn://gitee.com/keepbx/Jpom-demo-case";

    /**
     * SVN检出到目标路径
     */
    public static final String TargetPath = "D:\\tttt";
    // public static final String TargetPath = "/root/soft/resources";

    /**
     * SVN用户名
     */
    public static final String SvnUserName = "jiangzeyin";

    /**
     * SVN用户密码
     */
    public static final String SvnPassWord = "love1593503371";

    // 更新状态 true:没有程序在执行更新，反之则反
    public static Boolean DoUpdateStatus = true;

    // 声明SVN客户端管理类
    private static SVNClientManager ourClientManager;

    /**
     * SVN检出
     *
     * @return Boolean
     */
    public static Boolean checkOut() {
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

        // 相关变量赋值
        SVNURL repositoryURL = null;
        try {
            repositoryURL = SVNURL.parseURIEncoded(SvnPath);
        } catch (SVNException e) {
            e.printStackTrace();
            return false;
        }

        DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true);

        // 实例化客户端管理类
        ourClientManager = SVNClientManager.newInstance(options, SvnUserName, SvnPassWord);

        // 要把版本库的内容check out到的目录
        File wcDir = new File(TargetPath);

        // 通过客户端管理类获得updateClient类的实例。
        SVNUpdateClient updateClient = ourClientManager.getUpdateClient();

        updateClient.setIgnoreExternals(false);

        // 执行check out 操作，返回工作副本的版本号。
        long workingVersion = -1;
        try {
            if (wcDir.exists()) {
                ourClientManager.getWCClient().doCleanup(wcDir);
                System.out.println("cleanup");
            }
            workingVersion = updateClient.doCheckout(repositoryURL, wcDir, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, true);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        System.out.println("把版本：" + workingVersion + " check out 到目录：" + wcDir + "中。");
        return true;

    }

    /**
     * 解除svn Luck
     *
     * @return Boolean
     */
    public static Boolean doCleanup() {
        DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true);
        // 实例化客户端管理类
        ourClientManager = SVNClientManager.newInstance(options, SvnUserName, SvnPassWord);

        // 要把版本库的内容check out到的目录
        File wcDir = new File(TargetPath);
        if (wcDir.exists()) {
            try {
                ourClientManager.getWCClient().doCleanup(wcDir);
            } catch (SVNException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    /**
     * 更新svn
     *
     * @return int(- 1更新失败 ， 1成功 ， 0有程序在占用更新)
     */
    public static int doUpdate() {
        if (!SVNKit.DoUpdateStatus) {
            System.out.println("更新程序已经在运行中，不能重复请求！");
            return 0;
        }
        SVNKit.DoUpdateStatus = false;
        /*
         * For using over http:// and https://
         */
        try {
            DAVRepositoryFactory.setup();

            DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true);
            // 实例化客户端管理类
            ourClientManager = SVNClientManager.newInstance(options, SvnUserName, SvnPassWord);
            // 要更新的文件
            File updateFile = new File(TargetPath);
            // 获得updateClient的实例
            SVNUpdateClient updateClient = ourClientManager.getUpdateClient();
            updateClient.setIgnoreExternals(false);
            // 执行更新操作
            long versionNum = updateClient.doUpdate(updateFile, SVNRevision.HEAD, SVNDepth.INFINITY, false, false);
            System.out.println("工作副本更新后的版本：" + versionNum);
            DoUpdateStatus = true;
            return 1;
        } catch (SVNException e) {
            DoUpdateStatus = true;
            e.printStackTrace();
            return -1;
        }
    }


    public static void main(String[] args) {
        System.out.println(checkOut());

//        System.out.println(doCleanup());
        // System.out.println(doCleanup());
    }

}