import io.jpom.util.SvnKitUtil;
import org.tmatesoft.svn.core.SVNException;

import java.io.File;

/**
 * @author bwcx_jzy
 * @date 2019/8/6
 */
public class TestSvn {
    public static void main(String[] args) throws SVNException {
        String s = SvnKitUtil.checkOut("svn://gitee.com/keepbx/Jpom-demo-case",
                "", "", new File("/test/tt"));
        System.out.println(s);
    }
}
