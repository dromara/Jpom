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
package io.jpom.func.assets.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.StrUtil;
import io.jpom.func.assets.model.MachineSshModel;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.SystemPermission;
import io.jpom.util.FileUtils;
import io.jpom.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.function.BiFunction;

/**
 * @author bwcx_jzy
 * @since 2023/2/27
 */
@RestController
@RequestMapping(value = "/system/assets/ssh-file")
@Feature(cls = ClassFeature.SSH_FILE)
@Slf4j
@SystemPermission
public class MachineSshFileController extends BaseSshFileController {
    @Override
    protected <T> T checkConfigPath(String id, BiFunction<MachineSshModel, ItemConfig, T> function) {
        MachineSshModel machineSshModel = machineSshServer.getByKey(id, false);
        Assert.notNull(machineSshModel, "没有对应的ssh");
        return function.apply(machineSshModel, new ItemConfig() {
            @Override
            public List<String> allowEditSuffix() {
                return StringUtil.jsonConvertArray(machineSshModel.getAllowEditSuffix(), String.class);
            }

            @Override
            public List<String> fileDirs() {
                return CollUtil.newArrayList(StrUtil.SLASH);
            }
        });
    }

    @Override
    protected <T> T checkConfigPathChildren(String id, String path, String children, BiFunction<MachineSshModel, ItemConfig, T> function) {
        FileUtils.checkSlip(path);
        Opt.ofBlankAble(children).ifPresent(FileUtils::checkSlip);
        //
        MachineSshModel machineSshModel = machineSshServer.getByKey(id, false);
        return function.apply(machineSshModel, new ItemConfig() {
            @Override
            public List<String> allowEditSuffix() {
                return StringUtil.jsonConvertArray(machineSshModel.getAllowEditSuffix(), String.class);
            }

            @Override
            public List<String> fileDirs() {
                return CollUtil.newArrayList(StrUtil.SLASH);
            }
        });
    }
}
