/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2022/6/10
 */
public class TestH2Sql {

    @Test
    public void test() {
        File sqlFile = FileUtil.file("/Users/user/jpom/server/db/backup/20220609131122.sql");
        List<String> list = FileUtil.readLines(sqlFile, StandardCharsets.UTF_8);
        list = list.stream().map(s -> {
            if (StrUtil.startWith(s, "CREATE PRIMARY KEY SYSTEM_LOB_STREAM_PRIMARY_KEY ON SYSTEM_LOB_STREAM(ID, PART);")) {
                return "-- " + s;
            }
            return s;
        }).collect(Collectors.toList());
        FileUtil.writeLines(list, sqlFile, StandardCharsets.UTF_8);
    }
}
