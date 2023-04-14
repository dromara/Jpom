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
package org.dromara.jpom.git;

import cn.keepbx.jpom.plugins.IPlugin;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.ApplicationStartTest;
import org.dromara.jpom.plugin.PluginFactory;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2023/4/10
 */
@Slf4j
public class TestGit extends ApplicationStartTest {


    @Test
    public void test1() {
        System.out.println(1);
    }

    @Test
    public void test() throws Exception {
        IPlugin plugin = PluginFactory.getPlugin("git-clone");
        Map<String, Object> map = new HashMap<>();
//        map.put("gitProcessType", "JGit");
        map.put("gitProcessType", "SystemGit");
        map.put("url", "git@gitee.com:keepbx/Jpom-demo-case.git");//git@github.com:emqx/emqx-operator.git
//        map.put("url", "https://gitee.com/keepbx/Jpom-demo-case");
//        map.put("rsaFile", FileUtil.file(FileUtil.getUserHomePath(), ".ssh", "id_rsa"));
        map.put("reduceProgressRatio", 1);
        map.put("timeout", 60);
        map.put("protocol", 1);
        map.put("username", "");
        map.put("password", "");
        Object obj = plugin.execute("branchAndTagList", map);
        System.err.println(obj);
    }
}
