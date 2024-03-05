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

import org.dromara.jpom.common.Const;
import org.dromara.jpom.model.docker.DockerInfoModel;
import org.dromara.jpom.service.docker.DockerInfoService;
import org.junit.jupiter.api.Test;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2022/2/6
 */
public class DockerInfoTest extends ApplicationStartTest {

    @Resource
    private DockerInfoService dockerInfoService;

    @Test
    public void testQueryTag() {
        int sdfsd = dockerInfoService.countByTag(Const.WORKSPACE_DEFAULT_ID, "sdfsd");

        System.out.println(sdfsd);
    }

    @Test
    public void testQueryTag2() {
        List<DockerInfoModel> models = dockerInfoService.queryByTag(Const.WORKSPACE_DEFAULT_ID, "sdfsd");
        System.out.println(models);
    }
}
