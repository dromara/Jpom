/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.StrUtil;

import java.util.List;

/**
 * Created by bwcx_jzy on 2019/2/26.
 */
public class RunFile {
    public static void main(String[] args) {
//        https://www.jianshu.com/p/3ae2f024c291
//        List<String> list = FileUtil.listFileNames("D:\\SystemDocument\\Documents\\Tencent Files\\1593503371\\Image\\Group");
//        List<File> list1 = FileUtil.loopFiles("D:\\SystemDocument\\Documents\\Tencent Files\\1593503371\\Image\\Group");
//        System.out.println(list.size());
//        System.out.println(list1.size());

//        System.out.println(FileUtil.readableFileSize(FileUtil.size(new File("D:\\SystemDocument\\Documents\\Tencent Files\\1593503371\\Image"))));
//        System.out.println(list);
//        String msg = exec("/test.sh");
//        System.out.println(msg);
        List<String> list = StrSplitter.splitTrim(":::18000", StrUtil.COLON, true);
        System.out.println(list);
    }
}
