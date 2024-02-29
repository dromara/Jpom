/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package oshi;

import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.util.OshiUtils;
import org.junit.Test;

/**
 * @author bwcx_jzy
 * @since 2023/2/18
 */
public class TestInfo {

    @Test
    public void test() {
        JSONObject systemInfo = OshiUtils.getSystemInfo();

        //System.out.println(processor);
        System.out.println(systemInfo);
    }

    @Test
    public void test2() {
        System.out.println(1L << 10);
        System.out.println(1_000L);
    }
}
