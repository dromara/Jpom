package TestA;

/**
 * @author bwcx_jzy
 * @date 2019/8/28
 */
public class TestVersion {
    public static void main(String[] args) {
        String version = TestVersion.class.getPackage().getImplementationVersion();
        System.out.println(version);
    }
}
