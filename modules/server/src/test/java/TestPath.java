/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.comparator.VersionComparator;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.junit.Test;
import org.springframework.util.AntPathMatcher;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2019/8/24
 */
public class TestPath {

    public static void main(String[] args) {
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        System.out.println(antPathMatcher.match("/s/**/sss.html", "//s/s/s/sss.html"));
        System.out.println(antPathMatcher.match("/s/*.html", "/s/sss.html"));
        System.out.println(antPathMatcher.match("2.*", "2.5"));


//        StrUtil.similar()


        System.out.println(antPathMatcher.matchStart("/s/**/sss.html", "/s"));
    }

    @Test
    public void testSort() {
        ArrayList<String> list = CollUtil.newArrayList("dev", "master", "v1.1", "v0.4", "v.1", "v3.5.2", "v3.6", "v3.5.3");
        list.sort((o1, o2) -> VersionComparator.INSTANCE.compare(o2, o1));
        list.forEach(System.out::println);
    }

    @Test
    public void testFilePath() {
//		List<File> strings = FileUtil.loopFiles("~/jpom");
//		for (File string : strings) {
//			System.out.println(string);
//		}
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        List<String> paths = new ArrayList<>();
        File rootFile = FileUtil.file("~/jpom");
        String matchStr = "/agent/**/log";
        matchStr = FileUtil.normalize(StrUtil.SLASH + matchStr);
        String finalMatchStr = matchStr;
        FileUtil.walkFiles(rootFile.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                return this.test(file);
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes exc) throws IOException {
                return this.test(dir);
            }

            private FileVisitResult test(Path path) {
                String subPath = FileUtil.subPath(FileUtil.getAbsolutePath(rootFile), path.toFile());
                subPath = FileUtil.normalize(StrUtil.SLASH + subPath);
                if (antPathMatcher.match(finalMatchStr, subPath)) {
                    paths.add(subPath);
                    return FileVisitResult.TERMINATE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
        paths.forEach(System.out::println);

        //
        //System.out.println(antPathMatcher.isPattern("sdfsadf"));
        //Optional<String> first = paths.stream().filter(s -> antPathMatcher.match("agent/**/log", s)).findFirst();
        //System.out.println(first.get());
    }

    @Test
    public void test() throws MalformedURLException {
        URL url = new URL("jar:file:/home/jpom/server/lib/server-2.4.8.jar!/BOOT-INF/classes!/");
        String file = url.getFile();
        String x = StrUtil.subBefore(file, "!", false);
        System.out.println(x);
        System.out.println(FileUtil.file(x));

        System.out.println(StrUtil.subBefore("sssddsf", "!", false));

    }

    @Test
    public void testWinPath(){
        File file = FileUtil.file("/D:/");

    }
}
