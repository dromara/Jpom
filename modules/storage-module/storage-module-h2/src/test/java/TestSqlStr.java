/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import org.junit.Test;

import java.io.InputStream;

/**
 * @author bwcx_jzy
 * @since 2023/1/6
 */
public class TestSqlStr {

    @Test
    public void test() {
        InputStream stream = ResourceUtil.getStream("sql/h2-db-v1.0.sql");
        String sql = IoUtil.readUtf8(stream);
        System.out.println(sql);
    }
}
