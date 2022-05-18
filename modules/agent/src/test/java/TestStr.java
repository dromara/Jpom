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
import cn.hutool.core.lang.PatternPool;
import cn.hutool.core.lang.RegexPool;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ReUtil;
import io.jpom.common.commander.impl.LinuxSystemCommander;
import org.junit.Test;

import java.util.regex.Pattern;

/**
 * @author bwcx_jzy
 * @since Created Time 2021/8/4
 */
public class TestStr {

    @Test
    public void test() {
        System.out.println(ReUtil.get(RegexPool.NUMBERS, "7499.1 total", 0));
    }


    @Test
    public void testFile() {
//		String path = "/www/server/panel/"
        Pattern pattern = PatternPool.get("1", Pattern.DOTALL);
        System.out.println(ReUtil.isMatch(".end", "123end"));
        System.out.println(ReUtil.isMatch("^.+\\.(?i)(txt)$", "a.txt"));
    }

    @Test
    public void test2() {
        System.out.println(String.format("%.2f", (float) 1 / (float) 2 * 100));
        System.out.println(NumberUtil.div(1, 2));
    }

    @Test
    public void testParse() {
        String linuxCpu = LinuxSystemCommander.getLinuxCpu("%Cpu(s):  0.0 us,  0.0 sy,  0.0 ni,100.0 id,  0.0 wa,  0.0 hi,  0.0 si,  0.0 st\n");
        System.out.println(linuxCpu);
    }
}
