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

import org.dromara.jpom.core.release.ReleaseTarget;
import org.dromara.jpom.model.enums.BuildReleaseMethod;

/**
 * FTP 发布：支持多目标与增量同步。
 * <p>
 * 等价重构说明（2.11.16）：实现体委托 {@link ReleaseManage} 既有方法，行为与重构前逐一相同；
 * 装配点是 {@code ReleaseTargetRegistry}，新增发布方式只需新增实现类 + 注册一行。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
public class FtpReleaseTarget implements ReleaseTarget {

    @Override
    public int method() {
        return BuildReleaseMethod.Ftp.getCode();
    }

    @Override
    public String release(ReleaseManage manage) throws Exception {
        manage.doFtp();
        return null;
    }
}