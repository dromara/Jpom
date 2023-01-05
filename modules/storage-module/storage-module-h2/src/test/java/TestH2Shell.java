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
import cn.hutool.core.io.FastByteArrayOutputStream;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.h2.tools.Shell;
import org.junit.Test;

import java.io.PrintStream;
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
        FastByteArrayOutputStream baos = new FastByteArrayOutputStream();
        shell.setOut(new PrintStream(baos));
        shell.runTool(params);
    }
}
