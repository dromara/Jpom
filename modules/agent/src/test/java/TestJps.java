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
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.PatternPool;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import io.jpom.util.CommandUtil;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author bwcx_jzy
 * @since 2019/9/8
 */
public class TestJps {

	@Test
	public void test() {
		String execSystemCommand = CommandUtil.execSystemCommand("jps -v");
		List<String> list = StrSplitter.splitTrim(execSystemCommand, StrUtil.LF, true);
		for (String item : list) {
			System.out.println("******************************");
			System.out.println(item);
		}
	}

	@Test
	public void test1() {
		String execSystemCommand = CommandUtil.execSystemCommand("jps");
		System.out.println(Arrays.toString(FileUtil.getLineSeparator().getBytes(StandardCharsets.UTF_8)));
		System.out.println("======");
		System.out.println(execSystemCommand);
		String regex = "^[^\\r\\n]*\\r?\\n(\\S)(\\S) (.*)";

		Pattern r = Pattern.compile(regex);
		Matcher m = r.matcher(execSystemCommand);
		if (m.matches()) {
			for (int i = 0; i < m.groupCount(); i++) {
				System.out.println(m.group(i + 1));

			}
		}


		String str = "afda\r\nad\r\nsss";
		r = Pattern.compile(regex);
		m = r.matcher(str);
		if (m.matches()) {
			for (int i = 0; i < m.groupCount(); i++) {
				System.out.println(m.group(i + 1));

			}
		}


//		System.out.println(ReUtil.get(regex, execSystemCommand, 0));
//		System.out.println("----");
		final Pattern pattern = PatternPool.get(regex, Pattern.DOTALL);
		ReUtil.get(pattern, execSystemCommand, new Consumer<Matcher>() {
			@Override
			public void accept(Matcher matcher) {
				System.out.println(matcher.group());
			}
		});
//		System.out.println(allGroups);
		Map<String, String> allGroupNames = ReUtil.getAllGroupNames(pattern, execSystemCommand);
		for (Map.Entry<String, String> stringStringEntry : allGroupNames.entrySet()) {
			System.out.println(stringStringEntry.getKey() + "  " + stringStringEntry.getValue());
		}

		//	System.out.println("-----");
		//	System.out.println(ReUtil.get("^[^\\r\\n]*\\r?\\n?(\\S{1})(\\S{1})$", execSystemCommand, 0));
	}
}
