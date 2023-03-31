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
package org.dromara.jpom.updater;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.updater.enums.JpomTypeEnum;
import org.dromara.jpom.updater.update.V29xTo210x;
import org.dromara.jpom.updater.utils.JpomVersionUtil;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/**
 * Jpom 更新器主启动类
 *
 * @author hjk
 * @since 12/18/2022
 */
public class Updater {

    public static final String VERSION = "1.0.0";

    public static void main(String[] args) throws IOException {
        // NOTE: 防止出现中文乱码，在控制台输出的内容统一用英文
        System.out.println(
            StrUtil.format(
                "Welcome to use Jpom updater, version: {}, run dir: {}",
                VERSION,
                new File("").getAbsolutePath()
            )
        );

        File upgradeFile = JpomVersionUtil.getUpgradeFile();
        if (!upgradeFile.exists()) {
            System.out.println(StrUtil.format("cannot find upgrade.json in the dir: {}, Please make sure run the jar on the Jpom root dir", upgradeFile.getAbsolutePath()));
            return;
        }

        JpomTypeEnum jpomType = JpomVersionUtil.getJpomType(upgradeFile);
        if (jpomType == JpomTypeEnum.UNKNOWN) {
            System.out.println("unknown Jpom type from upgrade.json");
            return;
        }
        String jpomVersion = JpomVersionUtil.getJpomVersion(upgradeFile);
        System.out.println(StrUtil.format(
            "detect Jpom type: {}, version: {}, from upgrade.json",
            jpomType.getDesc(),
            jpomVersion
        ));
        System.out.println("==============================");
        System.out.println("1. update from 2.9.x to 2.10.x");
//        System.out.println("2. update from 2.8.x to 2.9.x");
        System.out.println("Please input which step you want to continue: ");

        try (Scanner scanner = new Scanner(System.in)) {
            boolean flag = true;
            while (flag) {
                String inputCommand = scanner.nextLine();
                if (!StrUtil.isNumeric(inputCommand)) {
                    System.out.println(StrUtil.format("Please enter a number, your input is: {}", inputCommand));
                    continue;
                }
                Integer command = Convert.toInt(inputCommand);
                switch (command) {
                    case 1: {
                        if (StrUtil.compareVersion(jpomVersion, "2.9.0") < 0) {
                            System.out.println(StrUtil.format("Your current version is: {}, less than 2.9.0, please update first!", jpomVersion));
                            break;
                        }
                        V29xTo210x.showUpdateTips();
                        try (Scanner scanner2 = new Scanner(System.in)) {
                            String inputCommand2 = scanner2.nextLine();
                            if (!StrUtil.isNumeric(inputCommand2)) {
                                System.out.println(StrUtil.format("Please enter a number, your input is: {}", inputCommand2));
                                continue;
                            }
                            Integer command2 = Convert.toInt(inputCommand2);
                            switch (command2) {
                                case 0: {
                                    System.out.println("00000");
                                    break;
                                }
                                case 1: {
                                    System.out.println("11111");
                                    break;
                                }
                                case 2: {
                                    System.out.println("222");
                                    if (jpomType == JpomTypeEnum.AGENT) {
//                                        V29xTo210x.Agent.updateConfig();
                                    }
                                    break;
                                }
                                default: {
                                    System.out.println("no this command");
                                    V29xTo210x.showUpdateTips();
                                    break;
                                }
                            }
                        }
                        flag = false;
                        break;
                    }
                    default: {
                        System.out.println(StrUtil.format("no this command: {}", command));
                        break;
                    }
                }
            }
        }
    }
}
