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
package io.jpom;

import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.plugin.PluginFactory;
import io.jpom.system.ServerExtConfigBean;
import io.jpom.system.extconf.DbExtConfig;
import io.jpom.system.init.InitDb;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import javax.annotation.Resource;

/**
 * @author Hotstrip
 * Test application start, then you can use such as service instance to test your methods
 */
@SpringBootTest(classes = {JpomServerApplication.class, InitDb.class, PluginFactory.class})
@AutoConfigureMockMvc
@EnableAutoConfiguration
@ContextConfiguration(initializers = PluginFactory.class)
@Slf4j
public class ApplicationStartTest {

    @Autowired
    protected MockMvc mockMvc;

    @Resource
    protected DbExtConfig dbExtConfig;


    @Test
    public void testApplicationStart() {
        log.info("Jpom Server Application started.....");
    }

    @Test
    public void testServerExtConfigBean() {
        ServerExtConfigBean serverExtConfigBean = SpringUtil.getBean(ServerExtConfigBean.class);
        System.out.println(serverExtConfigBean);
    }

}
