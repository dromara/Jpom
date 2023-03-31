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
package org.dromara.jpom.updater.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import org.dromara.jpom.updater.enums.JpomTypeEnum;
import org.dromara.jpom.updater.model.UpgradeJSON;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * Jpom 版本号工具类
 *
 * @author hjk
 * @since 12/19/2022
 */
public interface JpomVersionUtil {

    String LIB_DIRECTORY = "lib/";

    String UPGRADE_FILE_NAME = "upgrade.json";

    String UPGRADE_FILE_PATH = LIB_DIRECTORY + UPGRADE_FILE_NAME;

    static UpgradeJSON convertUpgradeJSONFile(File upgradeJSONFile) {
        String jsonString = FileUtil.readString(upgradeJSONFile, StandardCharsets.UTF_8);
        return JSON.parseObject(jsonString, UpgradeJSON.class);
    }

    static String getJpomAgentVersion() {
        return getJpomAgentVersion(getUpgradeFile());
    }

    static JpomTypeEnum getJpomType(File upgradeJSONFile) {
        UpgradeJSON upgradeJSON = convertUpgradeJSONFile(upgradeJSONFile);
        if (upgradeJSON.isAgentJar()) {
            return JpomTypeEnum.AGENT;
        }
        if (upgradeJSON.isServerJar()) {
            return JpomTypeEnum.SERVER;
        }
        return JpomTypeEnum.UNKNOWN;
    }

    static String getJpomVersion(File upgradeJSONFile) {
        UpgradeJSON upgradeJSON = convertUpgradeJSONFile(upgradeJSONFile);
        return upgradeJSON.getVersion();
    }

    static File getUpgradeFile() {
        System.out.println(StrUtil.format("current dir: {}", new File("").getAbsolutePath()));
        return new File(UPGRADE_FILE_PATH);
    }

    static String getJpomAgentVersion(File upgradeJSONFile) {
        UpgradeJSON upgradeJSON = convertUpgradeJSONFile(upgradeJSONFile);
        Assert.isTrue(upgradeJSON.isAgentJar(), "不是 Agent 包");
        return upgradeJSON.getVersion();
    }

    static String getJpomAgentVersion(String path) {
        return getJpomAgentVersion(new File(path));
    }
}
