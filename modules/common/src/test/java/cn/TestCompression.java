/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package cn;

import cn.hutool.core.io.FileUtil;
import org.dromara.jpom.util.CompressionFileUtil;
import org.junit.Test;

import java.io.File;

/**
 * @author bwcx_jzy
 * @since 2023/2/13
 */
public class TestCompression {

    @Test
    public void test() {
        File file = new File("D:\\System-Data\\Documents\\WeChat Files\\A22838106\\FileStorage\\File\\2023-02\\$R7JOOR8.tar.gz");
        File dir = FileUtil.file("D:\\temp\\unc");
        CompressionFileUtil.unCompress(file, dir);
    }

    @Test
    public void test2(){
       File file = new File("/Users/user/压缩文件.tbz");
        File dir = FileUtil.file("/Users/user/unc");
        CompressionFileUtil.unCompress(file, dir);
    }
}
