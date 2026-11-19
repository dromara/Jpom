/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.TestI18nSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * SSH/FTP 文件管理路径解析回归测试（2026-11-17 安全报告）。
 * <p>
 * 背景：{@code download / read / update / delete / rename / upload-sharding / chmod} 等入口曾把客户端传入的
 * {@code name}/{@code newname}/{@code fileName} 直接拼进 {@code FileUtil.normalize(...)}，{@code ../} 可穿越
 * 管理员为资产配置的 {@code fileDirs} 白名单。修复后统一走 {@link FileUtils#resolveChildPath}，
 * 这里锁定“逐段校验 + normalize 后仍在白名单内”的行为。
 *
 * @author jpom
 * @since 2026/11/18
 */
public class FileUtilsResolveChildPathTest {

    @BeforeAll
    public static void initI18n() {
        TestI18nSupport.init();
    }

    private static String unix(String path) {
        return StrUtil.replace(path, "\\", "/");
    }

    @Test
    public void testNormalPath() {
        Assertions.assertEquals("/home/app/demo.txt", unix(FileUtils.resolveChildPath("/home/app", "/", "demo.txt")));
        Assertions.assertEquals("/home/app/demo.txt", unix(FileUtils.resolveChildPath("/home/app", "", "demo.txt")));
        Assertions.assertEquals("/home/app/sub/dir/demo.txt", unix(FileUtils.resolveChildPath("/home/app", "sub/dir", "demo.txt")));
        // 普通名称：中文、空格、点号开头都允许
        Assertions.assertEquals("/home/app/正常 文件名.txt", unix(FileUtils.resolveChildPath("/home/app", "/", "正常 文件名.txt")));
        Assertions.assertEquals("/home/app/.hidden", unix(FileUtils.resolveChildPath("/home/app", "/", ".hidden")));
        // shell 元字符只是普通文件名字符
        Assertions.assertEquals("/home/app/cs123;id", unix(FileUtils.resolveChildPath("/home/app", "/", "cs123;id")));
        // 前端传入的 allowPathParent 来自 fileDirs()，带结尾 "/"，结果必须一致
        Assertions.assertEquals("/home/app/demo.txt", unix(FileUtils.resolveChildPath("/home/app/", "/", "demo.txt")));
        Assertions.assertEquals("/home/app", unix(FileUtils.resolveChildDir("/home/app/", StrUtil.EMPTY)));
    }

    @Test
    public void testRejectFileNameTraversal() {
        String[] names = new String[]{"../etc/passwd", "../../../../etc/passwd", "..", ".", "a/b", "/etc/passwd", "a\\b", "..\\..\\etc", "a\0b", "a\rb", "a\nb", ""};
        for (String name : names) {
            Assertions.assertThrows(IllegalArgumentException.class,
                () -> FileUtils.resolveChildPath("/home/app", "/", name), "应当拒绝文件名：" + name);
        }
    }

    @Test
    public void testRejectNextPathTraversal() {
        String[] nextPaths = new String[]{"../etc", "a/../../b", "sub/..", "..\\windows", "a\0b"};
        for (String nextPath : nextPaths) {
            Assertions.assertThrows(IllegalArgumentException.class,
                () -> FileUtils.resolveChildPath("/home/app", nextPath, "demo.txt"), "应当拒绝子目录：" + nextPath);
        }
    }

    @Test
    public void testResolveChildDir() {
        Assertions.assertEquals("/home/app", unix(FileUtils.resolveChildDir("/home/app", "/")));
        Assertions.assertEquals("/home/app", unix(FileUtils.resolveChildDir("/home/app", "")));
        Assertions.assertEquals("/home/app/sub/dir", unix(FileUtils.resolveChildDir("/home/app", "sub/dir")));
        Assertions.assertThrows(IllegalArgumentException.class, () -> FileUtils.resolveChildDir("/home/app", "../etc"));
    }

    /**
     * 反证：修复前的裸拼接确实会越界，{@link FileUtils#resolveChildPath} 不单是“重排代码”。
     * 如果有人把实现改回 {@code FileUtil.normalize(parent + "/" + name)}，上面的用例会立即失败。
     */
    @Test
    public void testNaiveConcatenationWouldEscape() {
        String parent = "/home/app";
        String naive = FileUtil.normalize(parent + StrUtil.SLASH + StrUtil.SLASH + "../etc/passwd");
        Assertions.assertFalse(unix(naive).startsWith(parent + "/"), "裸拼接应当越界（反证）：" + naive);
        Assertions.assertThrows(IllegalArgumentException.class, () -> FileUtils.resolveChildPath(parent, "/", "../etc/passwd"));
    }
}
