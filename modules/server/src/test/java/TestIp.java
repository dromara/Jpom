import cn.hutool.core.net.NetUtil;
import org.junit.Test;

/**
 * @author bwcx_jzy
 * @since Created Time 2021/8/4
 */
public class TestIp {

	@Test
	public void test() {
		System.out.println(NetUtil.getLocalhostStr());
		System.out.println(NetUtil.getLocalhost().getHostAddress());
	}

	@Test
	public void test1() {

	}
}
