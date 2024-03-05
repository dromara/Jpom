/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package jdbc.conn.h2.test;

import org.h2.tools.Backup;
import org.h2.tools.ConvertTraceFile;
import org.junit.Test;

import java.sql.SQLException;

/**
 * @author bwcx_jzy
 * @since 2022/1/18
 */
public class TestBackup {

	@Test
	public void testBackup() throws SQLException {
		Backup backup = new Backup();
		backup.runTool(
				"-dir", "/Users/user/jpom/server/db-back/",
				"-db", "Server");
	}

	@Test
	public void convertTrace() throws SQLException {
		ConvertTraceFile convertTraceFile = new ConvertTraceFile();
		convertTraceFile.runTool("-traceFile", "/Users/user/jpom/server/db/Server.trace.db");
	}

	@Test
	public void convertTrace2() throws SQLException {
		ConvertTraceFile convertTraceFile = new ConvertTraceFile();
		convertTraceFile.runTool("-traceFile", "/Users/user/fsdownload/Server.trace.db");
	}


}
