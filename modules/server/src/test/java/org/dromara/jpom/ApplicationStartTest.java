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

import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.db.DbExtConfig;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.system.db.InitDb;
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
        //  ServerExtConfigBean serverExtConfigBean = SpringUtil.getBean(ServerExtConfigBean.class);
        //System.out.println(serverExtConfigBean);
    }

}
