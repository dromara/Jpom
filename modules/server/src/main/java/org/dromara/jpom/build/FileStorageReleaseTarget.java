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

import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.core.release.ReleaseTarget;
import org.dromara.jpom.model.data.BuildInfoModel;
import org.dromara.jpom.model.enums.BuildReleaseMethod;

/**
 * 文件中心发布：把构建产物存进「系统管理 → 文件中心」，后续可被分发 / 手动下载复用
 * <p>
 * 装配点说明：本类是 {@link BuildReleaseMethod#FileStorage} 的实现，由
 * {@code ReleaseTargetRegistry} 注册一行即可被在线构建与流水线共同使用，调用方零改动。
 * 实际搬运复用 {@link ReleaseManage#syncToFileStorage(BuildInfoModel)}——与在线构建
 * 「同步到文件中心」开关同一段代码，不另写一套打包/MD5/保留天数逻辑。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
public class FileStorageReleaseTarget implements ReleaseTarget {

    @Override
    public int method() {
        return BuildReleaseMethod.FileStorage.getCode();
    }

    @Override
    public String release(ReleaseManage manage) throws Exception {
        BuildInfoModel buildInfoModel = manage.getBuildInfoModel();
        if (buildInfoModel == null) {
            return I18nMessageUtil.get("i18n.pipeline_file_storage_no_build_info");
        }
        ReleaseManage.FileStorageSyncResult result = manage.syncToFileStorage(buildInfoModel);
        if (result.available()) {
            return null;
        }
        String message = StrUtil.format(I18nMessageUtil.get("i18n.pipeline_file_storage_failed"), result.getFileName());
        manage.getLogRecorder().systemError(message);
        return message;
    }
}
