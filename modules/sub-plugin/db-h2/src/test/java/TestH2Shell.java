import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.h2.tools.Shell;
import org.junit.Test;

import java.sql.SQLException;

/**
 * @author bwcx_jzy
 * @since 2022/1/18
 */
public class TestH2Shell {

	@Test
	public void testShell() throws SQLException {
//
		Shell shell = new Shell();
		String sql = StrUtil.format("SCRIPT DROP to '{}'", FileUtil.file(".", "t.sql").getAbsoluteFile());
		String[] params = new String[]{
				"-url", "jdbc:h2:/Users/user/jpom/server/db/Server",
				"-user", "jpom",
				"-password", "jpom",
				"-driver", "org.h2.Driver",
				"-sql", sql
		};
		shell.runTool(params);
	}
}
