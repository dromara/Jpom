/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.log;

import cn.hutool.core.io.FileUtil;
import org.dromara.jpom.model.data.DslYmlDto;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DSL 运行日志文件配置解析与路径解析测试
 *
 * @author Jpom
 * @since 2.11.15
 */
public class DslProjectLogFileProviderTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    /**
     * config.log 必须能被 yml 解析为列表（兼容旧配置：没有 config/log 时为 null）
     */
    @Test
    public void testParseDslLogConfig() {
        String yml = "description: dsl\n" +
            "run:\n" +
            "  start:\n" +
            "    scriptId: start.sh\n" +
            "config:\n" +
            "  autoBackToFile: true\n" +
            "  log:\n" +
            "    - name: app\n" +
            "      path: logs/app.log\n" +
            "    - path: /var/log/app.log\n" +
            "      charset: GBK\n";
        DslYmlDto dslYmlDto = DslYmlDto.build(yml);
        Assert.assertNotNull(dslYmlDto.getConfig());
        Assert.assertTrue(dslYmlDto.getConfig().getAutoBackToFile());
        List<DslYmlDto.LogFile> logFiles = dslYmlDto.getConfig().getLog();
        Assert.assertEquals(2, logFiles.size());
        Assert.assertEquals("app", logFiles.get(0).getName());
        Assert.assertEquals("logs/app.log", logFiles.get(0).getPath());
        Assert.assertEquals("GBK", logFiles.get(1).getCharset());
        // 旧配置没有 config.log
        DslYmlDto old = DslYmlDto.build("run:\n  start:\n    scriptId: start.sh\n");
        Assert.assertNull(old.getConfig() == null ? null : old.getConfig().getLog());
    }

    /**
     * 日志标识必须与 Go 端一致（md5(name + "\n" + path)），保证两端切换节点后前端仍能命中
     */
    @Test
    public void testLogKey() {
        DslYmlDto.LogFile logFile = new DslYmlDto.LogFile();
        logFile.setName("app");
        logFile.setPath("logs/app.log");
        Assert.assertEquals("573e1c83583b4149816d301d8fc98944", DslProjectLogFileProvider.logKey(logFile));
    }

    @Test
    public void testResolveRelativePath() throws Exception {
        File lib = temporaryFolder.newFolder("lib");
        File appLog = FileUtil.file(lib, "logs/app.log");
        FileUtil.touch(appLog);
        File file = DslProjectLogFileProvider.resolveFile(lib, "logs/app.log", new HashMap<>());
        Assert.assertNotNull(file);
        Assert.assertEquals(appLog.getAbsolutePath(), FileUtil.getAbsolutePath(file));
    }

    @Test
    public void testResolveVariablePath() {
        Map<String, String> variables = new HashMap<>();
        variables.put("PROJECT_PATH", "/data/demo");
        File file = DslProjectLogFileProvider.resolveFile(new File("/tmp"), "${PROJECT_PATH}/logs/app.log", variables);
        Assert.assertNotNull(file);
        Assert.assertEquals(new File("/data/demo/logs/app.log").getAbsolutePath(), FileUtil.getAbsolutePath(file));
    }

    @Test
    public void testResolveAbsolutePath() {
        File absolute = new File(tempDirAbsolute(), "app.log");
        File file = DslProjectLogFileProvider.resolveFile(new File("/tmp"), absolute.getAbsolutePath(), new HashMap<>());
        Assert.assertNotNull(file);
        Assert.assertEquals(absolute.getAbsolutePath(), FileUtil.getAbsolutePath(file));
    }

    private String tempDirAbsolute() {
        return FileUtil.file(FileUtil.getTmpDir(), "jpom-dsl-log-test").getAbsolutePath();
    }

    @Test
    public void testResolveEscapeAndBlank() throws Exception {
        File lib = temporaryFolder.newFolder("lib2");
        // 相对路径不允许跳出项目目录
        Assert.assertNull(DslProjectLogFileProvider.resolveFile(lib, "../app.log", new HashMap<>()));
        Assert.assertNull(DslProjectLogFileProvider.resolveFile(lib, "logs/../../app.log", new HashMap<>()));
        // 空路径、空项目路径
        Assert.assertNull(DslProjectLogFileProvider.resolveFile(lib, "  ", new HashMap<>()));
        Assert.assertNull(DslProjectLogFileProvider.resolveFile(null, "logs/app.log", new HashMap<>()));
    }
}
