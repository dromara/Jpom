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
package org.dromara.jpom.updater.utils;

import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

/**
 * xxx测试类
 *
 * @author hjk
 * @since 12/19/2022
 */
public class YamlUtilTest {

    @Test
    public void changeKey() throws FileNotFoundException {
        System.out.println("aa");

        Yaml yaml = YamlUtil.createCustomYaml();
        File file = new File(".");
        System.out.println(file.getAbsolutePath());
        InputStream inputStream = new FileInputStream(new File("src/test/resources/extConfig.yml"));
        Map<String, Object> load = yaml.load(inputStream);

        // change root
        Map<String, Object> jpom = (Map<String, Object>) load.get("jpom");

        // remove user to jpom.user
        jpom.put("user", load.get("user"));
        load.remove("user");

        // remove db to jpom.db
        jpom.put("db", load.get("db"));
        load.remove("db");

        System.out.println(load);
        System.out.println(load.getClass());

        System.out.println(yaml.dump(load));
    }
}
