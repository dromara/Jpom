/*
 * Copyright (c) 2019 Of Him Code Technology Studio
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

import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2023/2/12
 */
public class TestProcessesList {

    @Test
    public void test() {
        List<JSONObject> processes = OshiUtils.getProcesses("java", 20);
        System.out.println(processes);
    }
}
