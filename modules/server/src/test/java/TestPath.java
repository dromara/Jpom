import org.springframework.util.AntPathMatcher;

/**
 * @author bwcx_jzy
 * @date 2019/8/24
 */
public class TestPath {

    public static void main(String[] args) {
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        System.out.println(antPathMatcher.match("/s/**/sss.html", "//s/s/s/sss.html"));
    }
}
