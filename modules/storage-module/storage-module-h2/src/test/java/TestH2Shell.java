/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
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
