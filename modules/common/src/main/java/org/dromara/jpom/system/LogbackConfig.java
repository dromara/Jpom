/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.system;

import ch.qos.logback.core.PropertyDefinerBase;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.system.SystemUtil;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.springframework.util.Assert;

import java.io.File;

/**
 * @author bwcx_jzy
 * @since 2022/12/7
 */
public abstract class LogbackConfig extends PropertyDefinerBase {
    static String JPOM_LOG = "JPOM_LOG";

    public static String getPath() {
        String jpomLog = SystemUtil.get(JPOM_LOG);
        Assert.hasText(jpomLog, I18nMessageUtil.get("i18n.jpom_log_not_configured.3153"));
        return jpomLog;
    }

    @Override
    public String getPropertyValue() {
        String jpomLog = SystemUtil.get(JPOM_LOG);
        return Opt.ofBlankAble(jpomLog).orElseGet(() -> {
            String locationPath = ClassUtil.getLocationPath(this.getClass());
            File file = FileUtil.file(FileUtil.getParent(locationPath, 2), "logs");
            String path = FileUtil.getAbsolutePath(file);
            System.out.println(path);
            SystemUtil.set(JPOM_LOG, path);
            return path;
        });
    }
}
