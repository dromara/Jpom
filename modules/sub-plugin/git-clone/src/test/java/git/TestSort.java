package git;

import cn.hutool.core.comparator.VersionComparator;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2023/9/15
 */
public class TestSort {

    @Test
    public void test() {
        File file = FileUtil.file("D:\\System-Data\\Documents\\WeChat Files\\A22838106\\FileStorage\\File\\2023-09\\list");
        List<String> list = FileUtil.readLines(file, CharsetUtil.CHARSET_UTF_8);
        //
        list = list.stream()
            .map(StrUtil::trim).map(name -> {
                //String name = ref.getName();
                if (name.startsWith("remotes/origin/")) {
                    return name.substring("remotes/origin/".length());
                }
                return null;
            })
            .filter(Objects::nonNull)
            .sorted((o1, o2) -> {
                int compare = VersionComparator.INSTANCE.compare(o2, o1);
                System.out.println(compare);
                System.out.println(o1 + "  " + o2);
                return compare;
            })
            .collect(Collectors.toList());
        //list.sort((o1, o2) -> VersionComparator.INSTANCE.compare(o2, o1));
        System.out.println(list);
    }

    @Test
    public void test2() {
        List<String> list = new ArrayList<>();
        list.add("lester-hotfix-202308002-1");
        list.add("lester-invoice-release");
        list.sort(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                int compare = VersionComparator.INSTANCE.compare(o2, o1);
                return compare;
            }
        });

    }
}
