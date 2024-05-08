/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.system.oshi.OshiUtil;
import org.dromara.jpom.util.OshiUtils;
import org.junit.Test;
import oshi.hardware.NetworkIF;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy1
 * @since 2024/4/25
 */
public class OshiNetworkTest {

    @Test
    public void test() {
        List<NetworkIF> listBegin = OshiUtil.getNetworkIFs();
        System.out.println(listBegin.size());
        List<String> statExcludeNames = CollUtil.newArrayList("lo*", "ap*");
        List<String> statContainsOnlyNames = CollUtil.newArrayList("en*");
        listBegin = listBegin.stream()
            .filter(networkIF -> CollUtil.isEmpty(statExcludeNames) || !OshiUtils.isMatch(statExcludeNames, networkIF.getName()))
            .filter(networkIF -> CollUtil.isEmpty(statContainsOnlyNames) || OshiUtils.isMatch(statContainsOnlyNames, networkIF.getName()))
            .collect(Collectors.toList());
        for (NetworkIF anIf : listBegin) {
            System.out.println(anIf.getName());
        }
//        System.out.println(listBegin);
    }
}
