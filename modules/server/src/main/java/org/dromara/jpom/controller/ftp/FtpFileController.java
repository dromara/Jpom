/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package org.dromara.jpom.controller.ftp;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.StrUtil;
import java.util.List;
import java.util.function.BiFunction;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.func.assets.controller.BaseFtpFileController;
import org.dromara.jpom.func.assets.model.MachineFtpModel;
import org.dromara.jpom.model.data.FtpModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.util.FileUtils;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ftp 文件管理
 *
 * @author wxy
 * @since 2025/06/04
 */
@RestController
@RequestMapping("node/ftp")
@Feature(cls = ClassFeature.FTP_FILE)
@Slf4j
public class FtpFileController extends BaseFtpFileController {


    @Override
    protected <T> T checkConfigPath(String id, BiFunction<MachineFtpModel, ItemConfig, T> function) {
        FtpModel ftpModel = ftpService.getByKey(id);
        Assert.notNull(ftpModel, I18nMessageUtil.get("i18n.no_corresponding_ssh.aa68"));
        MachineFtpModel machineFtpModel = machineFtpServer.getByKey(ftpModel.getMachineFtpId(), false);
        return function.apply(machineFtpModel, ftpModel);
    }

    @Override
    protected <T> T checkConfigPathChildren(String id, String path, String children, BiFunction<MachineFtpModel, ItemConfig, T> function) {
        FileUtils.checkSlip(path);
        Opt.ofBlankAble(children).ifPresent(FileUtils::checkSlip);

        FtpModel ftpModel = ftpService.getByKey(id);
        Assert.notNull(ftpModel, I18nMessageUtil.get("i18n.no_corresponding_ssh.aa68"));
        List<String> fileDirs = ftpModel.fileDirs();
        String normalize = FileUtil.normalize(StrUtil.SLASH + path + StrUtil.SLASH);
        //
        Assert.state(CollUtil.contains(fileDirs, normalize), I18nMessageUtil.get("i18n.cannot_operate_current_directory.aa3d"));
        MachineFtpModel machineFtpModel = machineFtpServer.getByKey(ftpModel.getMachineFtpId(), false);
        return function.apply(machineFtpModel, ftpModel);
    }
}
