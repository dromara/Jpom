/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package org.dromara.jpom.func.assets.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.StrUtil;
import java.util.List;
import java.util.function.BiFunction;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.func.assets.model.MachineFtpModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.util.FileUtils;
import org.dromara.jpom.util.StringUtil;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wxyShine
 * @since 2025/05/28
 */
@RestController
@RequestMapping(value = "/system/assets/ftp-file")
@Feature(cls = ClassFeature.FTP_FILE)
@Slf4j
@SystemPermission
public class MachineFtpFileController extends BaseFtpFileController {
    @Override
    protected <T> T checkConfigPath(String id, BiFunction<MachineFtpModel, ItemConfig, T> function) {
        MachineFtpModel machineFtpModel = machineFtpServer.getByKey(id, false);
        Assert.notNull(machineFtpModel, I18nMessageUtil.get("i18n.no_ftp_server_correspondence.1af7"));
        return function.apply(machineFtpModel, new ItemConfig() {
            @Override
            public List<String> allowEditSuffix() {
                return StringUtil.jsonConvertArray(machineFtpModel.getAllowEditSuffix(), String.class);
            }

            @Override
            public List<String> fileDirs() {
                return CollUtil.newArrayList(StrUtil.SLASH);
            }
        });
    }

    @Override
    protected <T> T checkConfigPathChildren(String id, String path, String children, BiFunction<MachineFtpModel, ItemConfig, T> function) {
        FileUtils.checkSlip(path);
        Opt.ofBlankAble(children).ifPresent(FileUtils::checkSlip);
        //
        MachineFtpModel machineFtpModel = machineFtpServer.getByKey(id, false);
        return function.apply(machineFtpModel, new ItemConfig() {
            @Override
            public List<String> allowEditSuffix() {
                return StringUtil.jsonConvertArray(machineFtpModel.getAllowEditSuffix(), String.class);
            }

            @Override
            public List<String> fileDirs() {
                return CollUtil.newArrayList(StrUtil.SLASH);
            }
        });
    }
}
