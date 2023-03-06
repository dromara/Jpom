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

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * desc
 *
 * @author hjk
 * @date 12/19/2022
 */
public class YamlUtil {

    // https://blog.csdn.net/baidu_28291037/article/details/126291308

    public static Yaml createCustomYaml() {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setProcessComments(true);

        CustomRepresenter customRepresenter = new CustomRepresenter(dumperOptions);
        return new Yaml(customRepresenter, dumperOptions);
    }

    @SuppressWarnings("deprecation")
    static class CustomRepresenter extends Representer {

        public CustomRepresenter(DumperOptions dumperOptions) {
            super(dumperOptions);
            super.nullRepresenter = new RepresentNull();
        }

        private class RepresentNull implements Represent {
            @Override
            public Node representData(Object data) {
                return representScalar(Tag.NULL, "");
            }
        }
    }

    private static Map<String, Map<String, Object>> properties;
    private static FileInputStream fis = null;
    private static File f = null;
//    static {
//        try {
//            f=new File("F:\\autoInterface\\src\\resources\\application.yaml");
//            fis = new FileInputStream(f);
//            properties = new HashMap<>();
//            Yaml yaml = new Yaml();
//            properties = yaml.load(fis);
//            System.out.println("properties的值为："+properties);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        finally {
//            try {
//                fis.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }

    /**
     * 获取字段名下的key对应的值
     *
     * @param section 字段名
     * @param key     键
     * @return
     */
    public Object getValueByKey(String section, String key) {
        System.out.println("properties的值为：" + properties);
        Map<String, Object> rootproperty = properties.get(section);
        System.out.println("rootproperty的值为：" + rootproperty);
        return rootproperty.getOrDefault(key, "");
    }

    /**
     * yaml文件修改
     *
     * @param section
     * @param key
     * @param value
     * @return
     */
    public boolean updateYaml(String section, String key, Object value) {
        Yaml yaml = new Yaml();
        Map<String, Object> before_property = properties.get(section);
        before_property.remove(key);
        before_property.put(key, value);
        try {
            yaml.dump(properties, new FileWriter(f));
            return true;
        } catch (Exception e) {

        }
        return false;
    }

    /**
     * yaml文件新增
     *
     * @param section
     * @param key
     * @param value
     * @throws IOException
     */
    public void write(String section, String key, String value) throws IOException {
        Map<String, Map<String, Object>> map = new HashMap<String, Map<String, Object>>();
        Map<String, Object> mapchild = new HashMap<String, Object>();
        mapchild.put(key, value);
        map.put(section, mapchild);
        Yaml yml = new Yaml();
        FileWriter writer = new FileWriter("F:\\autoInterface\\src\\resources\\application.yaml", true);
        BufferedWriter buffer = new BufferedWriter(writer);
        buffer.newLine();
        yml.dump(map, buffer);
        buffer.close();
        writer.close();
    }

    public static void main(String[] args) throws IOException {
        YamlUtil yamlUtil = new YamlUtil();
        yamlUtil.updateYaml("login2", "token2", "value1");
    }

}
