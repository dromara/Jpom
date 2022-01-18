import org.h2.tools.Recover;
import org.junit.Test;

import java.sql.SQLException;

/**
 * @author bwcx_jzy
 * @since 2022/1/18
 */
public class TestH2Recover {

	@Test
	public void test() throws SQLException {
		String path = "/Users/user/Library/Containers/com.tencent.xinWeChat/Data/Library/Application Support/com.tencent.xinWeChat/2.0b4.0.9/e54b85b859057912ed9509c5ea6878fd/Message/MessageTemp/69852fb1bb9f685625ff2d92ea23c1af/File";
		Recover recover = new Recover();
		recover.runTool("-dir", path, "-db", "Server", "-trace", "-transactionLog");
	}
}
