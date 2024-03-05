/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.setting.yaml.YamlUtil;
import org.dromara.jpom.db.DbExtConfig;
import org.junit.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileUrlResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2021/8/1
 */
public class TestYml {

    @Test
    public void testCharest() {
        System.out.println(Charset.forName("UTF8"));
        System.out.println(Charset.forName("UTF-8"));
    }

    @Test
    public void test2() {
        InputStream stream = ResourceUtil.getStream("bin/extConfig.yml");
        //String s = IoUtil.readUtf8(stream);
        //System.out.println(s);
        Dict dict = YamlUtil.load(stream, Dict.class);
        Object db = dict.get("db");
        StringWriter writer = new StringWriter();
        YamlUtil.dump(db, writer);
        ByteArrayInputStream inputStream = IoUtil.toStream(writer.toString(), CharsetUtil.CHARSET_UTF_8);
        DbExtConfig dbExtConfig1 = YamlUtil.load(inputStream, DbExtConfig.class);
        System.out.println(dbExtConfig1);
    }


    @Test
    public void test() throws IOException {
        String path = "D:\\Idea\\Jpom\\modules\\agent\\src\\main\\resources\\bin\\extConfig.yml";
        YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader();
//		ByteArrayResource resource = new ByteArrayResource(StringUtil.deleteComment(content).getBytes(StandardCharsets.UTF_8));
        URL resource = ResourceUtil.getResource(path);
        List<PropertySource<?>> test = yamlPropertySourceLoader.load("test", new FileUrlResource(path));
        PropertySource<?> propertySource = test.get(0);
        System.out.println(propertySource);

//        Object user = propertySource.getProperty(Const.AUTHORIZE_USER_KEY);
//        System.out.println(user);
    }
}
