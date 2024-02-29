/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import org.dromara.jpom.util.CommandUtil;
import org.junit.Test;

import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2019/9/28
 */
public class TestJavaPath {

    @Test
    public void t() {
        String command = CommandUtil.execSystemCommand("\"C:\\Program Files\\Java\\jdk1.8.0_211\\bin\\java.exe\"  -version");
        System.out.println(command);



        command = CommandUtil.execSystemCommand("\"C:\\Program Files\\Java\\jdk-11.0.3\\bin\\java.exe\"  -version");
        System.out.println(command);


    }

    @Test
    public void t1() {
        Map<String, String> getenv = System.getenv();
        System.out.println(getenv.get("JAVA_HOME"));
    }
}
