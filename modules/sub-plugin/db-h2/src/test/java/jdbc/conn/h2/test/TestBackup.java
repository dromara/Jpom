/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
