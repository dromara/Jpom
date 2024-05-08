/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import cn.hutool.core.util.StrUtil;
import org.junit.Test;

/**
 * @author bwcx_jzy
 * @since 2019/8/4
 */
public class TestVersion {
    public static void main(String[] args) {
        System.out.println(StrUtil.compareVersion("2.4.3", "2.4.2"));
    }

    @Test
    public void test() {
        System.out.println(StrUtil.compareVersion("2.11.5", "2.11.5.1"));
    }
}
