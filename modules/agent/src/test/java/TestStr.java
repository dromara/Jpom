import cn.hutool.core.lang.RegexPool;
import cn.hutool.core.util.ReUtil;
import org.junit.Test;

/**
 * @author bwcx_jzy
 * @since Created Time 2021/8/4
 */
public class TestStr {

	@Test
	public void test() {
		System.out.println(ReUtil.get(RegexPool.NUMBERS, "7499.1 total", 0));
	}
}
