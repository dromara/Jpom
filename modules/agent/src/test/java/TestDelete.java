/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.io.FileUtil;
import org.dromara.jpom.model.data.NodeProjectInfoModel;
import org.junit.Test;

/**
 * @author bwcx_jzy
 * @since 2023/3/14
 */
public class TestDelete {

    @Test
    public void test() {
        boolean del = FileUtil.del("C:\\Users\\bwcx_\\jpom\\agent\\data\\project_file_backup");
        System.out.println(del);
    }

    @Test
    public void test2() {
        boolean del = FileUtil.del("D:\\data\\jpom\\a");
        System.out.println(del);
    }

    @Test
    public void testBeanCopy(){
        NodeProjectInfoModel nodeProjectInfoModel = new NodeProjectInfoModel();
        nodeProjectInfoModel.setName("ss");
        NodeProjectInfoModel nodeProjectInfoModel1 = new NodeProjectInfoModel();
        nodeProjectInfoModel1.setOutGivingProject(true);
        BeanUtil.copyProperties(nodeProjectInfoModel, nodeProjectInfoModel1, CopyOptions.create().ignoreNullValue());
        System.out.println(nodeProjectInfoModel1.toString());
    }
}
