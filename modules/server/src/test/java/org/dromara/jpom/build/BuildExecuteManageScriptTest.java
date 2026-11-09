/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.build;

import org.dromara.jpom.util.CommandUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link BuildExecuteManage#buildScriptContent(String, String, boolean, String)} 的行为级回归（Gitee ICW9KZ）。
 * <p>
 * 背景：构建脚本 = 执行模板 + 用户脚本，原来仅在最后按整体退出码判断是否失败，
 * 严格构建下中间命令失败仍会继续执行后续命令。
 * 这里直接断言组合后的最终脚本内容：只有“严格构建 + sh”才在模板之后、用户脚本之前注入
 * {@code set -e} / {@code set -o pipefail}；非严格模式与 Windows bat 语义不变。
 *
 * @author jpom
 */
public class BuildExecuteManageScriptTest {

    private static final String TEMPLATE = "#!/bin/bash\n# 模板：环境初始化\n";
    private static final String SCRIPT = "npm install\nnpm run build\n";

    /**
     * 严格构建 + sh：在模板之后、用户脚本之前注入 set -e / set -o pipefail，中间命令失败立即中断。
     */
    @Test
    public void testStrictShInjectsSetEAndPipefail() {
        String content = BuildExecuteManage.buildScriptContent(TEMPLATE, SCRIPT, true, CommandUtil.SUFFIX_UNIX);

        assertTrue(content.contains("set -e\n"), "严格 sh 构建应注入 set -e：" + content);
        assertTrue(content.contains("set -o pipefail\n"), "bash 下应同时注入 set -o pipefail：" + content);
        int templateIndex = content.indexOf("# 模板：环境初始化");
        int strictIndex = content.indexOf("set -e");
        int scriptIndex = content.indexOf("npm install");
        assertTrue(templateIndex >= 0 && strictIndex > templateIndex,
            "set -e 必须位于模板之后（模板内 source 环境失败不应中断）：" + content);
        assertTrue(scriptIndex > strictIndex, "set -e 必须位于用户脚本之前：" + content);
        assertTrue(content.endsWith(SCRIPT), "用户脚本内容必须完整保留在末尾：" + content);
        // 非 bat 模式不注入 Windows 指令
        assertFalse(content.contains("@echo off"));
    }

    /**
     * 非严格构建 + sh：语义保持原样（模板 + 用户脚本），不注入 set -e。
     */
    @Test
    public void testNonStrictShKeepsOriginalSemantics() {
        String content = BuildExecuteManage.buildScriptContent(TEMPLATE, SCRIPT, false, CommandUtil.SUFFIX_UNIX);
        assertEquals(TEMPLATE + SCRIPT, content, "非严格构建不应改变脚本内容");
        assertFalse(content.contains("set -e"));
        assertFalse(content.contains("pipefail"));
    }

    /**
     * 严格构建 + Windows bat：语义保持原样（bat 使用 cmd 执行，不能注入 bash 指令）。
     */
    @Test
    public void testStrictBatKeepsOriginalSemantics() {
        String content = BuildExecuteManage.buildScriptContent(TEMPLATE, SCRIPT, true, CommandUtil.SUFFIX_WINDOWS);
        assertEquals(TEMPLATE + SCRIPT, content, "Windows 批处理不应注入 bash 的 set -e");
        assertFalse(content.contains("set -e"));
    }

    /**
     * 模板文件末尾没有换行时，注入内容必须另起一行，避免拼出非法命令。
     */
    @Test
    public void testTemplateWithoutTrailingNewline() {
        String template = "#!/bin/bash";
        String content = BuildExecuteManage.buildScriptContent(template, SCRIPT, true, CommandUtil.SUFFIX_UNIX);
        assertTrue(content.startsWith("#!/bin/bash\nset -e\n"), "缺少换行时应补充换行，实际：" + content);
        assertTrue(content.endsWith(SCRIPT));
    }

    /**
     * 模板以换行结尾时不应再插入多余空行（保持脚本整洁）。
     */
    @Test
    public void testTemplateWithTrailingNewlineNotDoubleBlank() {
        String content = BuildExecuteManage.buildScriptContent(TEMPLATE, SCRIPT, true, CommandUtil.SUFFIX_UNIX);
        assertTrue(content.startsWith(TEMPLATE + "set -e\n"), "模板以换行结尾时应直接拼接 set -e，实际：" + content);
    }
}
