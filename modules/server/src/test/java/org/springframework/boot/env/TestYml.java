/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.springframework.boot.env;

import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2019/8/17
 */
public class TestYml {

    public static void main(String[] args) throws IOException {

        FileSystemResource fileSystemResource = new FileSystemResource("D:\\Idea\\Jpom\\modules\\agent\\target\\agent-2.4.3-release\\extConfig.yml");

        OriginTrackedYamlLoader originTrackedYamlLoader = new OriginTrackedYamlLoader(fileSystemResource);
        List<Map<String, Object>> load = originTrackedYamlLoader.load();
        System.out.println(load);
    }
}
