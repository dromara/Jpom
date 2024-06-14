/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.PatternPool;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.util.CommandUtil;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
		ReUtil.get(pattern, execSystemCommand, matcher -> System.out.println(matcher.group()));
//		System.out.println(allGroups);
		Map<String, String> allGroupNames = ReUtil.getAllGroupNames(pattern, execSystemCommand);
		for (Map.Entry<String, String> stringStringEntry : allGroupNames.entrySet()) {
			System.out.println(stringStringEntry.getKey() + "  " + stringStringEntry.getValue());
		}

		//	System.out.println("-----");
		//	System.out.println(ReUtil.get("^[^\\r\\n]*\\r?\\n?(\\S{1})(\\S{1})$", execSystemCommand, 0));
	}
}
