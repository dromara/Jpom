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
package io.jpom.updater.utils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Map;

/**
 * xxx测试类
 *
 * @author hjk
 * @since 12/19/2022
 */
public class FreeMarkerTest {

    @Test
    public void config() throws IOException, TemplateException {
        File file = new File("src/main/resources/agent");

        Configuration cfg = FreeMarkerUtil.createCustomConfiguration(file);

        Yaml customYaml = YamlUtil.createCustomYaml();
        try (FileInputStream fileInputStream = new FileInputStream("src/test/resources/agent/extConfig.yml")) {
            Map<String, Object> root = customYaml.load(fileInputStream);

            Template template = cfg.getTemplate("application.yml.ftlh");
            template.dump(System.out);

            System.out.println("****************************************************************");

            Writer out = new OutputStreamWriter(System.out);
            template.process(root, out);
        }


//        Template template = cfg.getTemplate("a.ftlh");
//        template.dump(System.out);
//        HashMap<Object, Object> objectObjectHashMap = new HashMap<>();
////        objectObjectHashMap.put("name", "Lucy");
//        Writer out = new OutputStreamWriter(System.out);
//        template.process(objectObjectHashMap, out);
    }
}
