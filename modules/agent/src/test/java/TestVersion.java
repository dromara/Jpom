import cn.hutool.core.util.StrUtil;

/**
 * @author bwcx_jzy
 * @date 2019/8/4
 */
public class TestVersion {
    public static void main(String[] args) {
        System.out.println(StrUtil.compareVersion("2.4.3", "2.4.2"));
    }
}
