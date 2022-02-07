import org.junit.Test;

import java.util.Formatter;

/**
 * @author bwcx_jzy
 * @since 2022/2/7
 */
public class TestFormatter {

	@Test
	public void test() {
		System.out.println(String.format("${a}", "1"));
		Formatter formatter = new Formatter();
		System.out.println(formatter.format("${a}", "1"));
	}
}
