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
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.StrUtil;

import java.util.List;

/**
 * Created by jiangzeyin on 2019/2/26.
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
