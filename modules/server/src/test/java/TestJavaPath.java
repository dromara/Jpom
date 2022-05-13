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
import io.jpom.util.CommandUtil;
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
