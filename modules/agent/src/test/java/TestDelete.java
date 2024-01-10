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
