/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
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
