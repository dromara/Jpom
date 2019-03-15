import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;

/**
 * Created by jiangzeyin on 2019/3/1.
 */
public class TestString {
    public static void main(String[] args) {
        System.out.println("/jpom-project/rrr/s/s".startsWith("/jpom-project/rrr/s/"));

        System.out.println(FileUtil.normalize("/t/../..\\..\\.\\.\\"));

        System.out.println(Validator.isGeneral("+"));

        String liveCheck = "/sss/sss/";
        if (liveCheck.startsWith(StrUtil.SLASH)) {
            liveCheck = liveCheck.substring(1);
        }
        if (liveCheck.endsWith(StrUtil.SLASH)) {
            liveCheck = liveCheck.substring(0, liveCheck.length() - 1);
        }
        System.out.println(liveCheck);
//        System.out.println("/jpom-project/rrr/s/s".startsWith("/jpom-project/rrr/s/"));
        System.out.println(FileUtil.normalize("..\\../"));


        System.out.println("-Dappliction=jboot-test -Dbasedir=/jpom-project/jboot/lib".contains("-Dapplication=jboot-test"));
    }
}
