/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.dblog;

import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.model.data.RepositoryModel;
import org.dromara.jpom.service.h2db.BaseGlobalOrWorkspaceService;
import org.springframework.stereotype.Service;

/**
 * @author Hotstrip
 * Repository service
 */
@Service
public class RepositoryService extends BaseGlobalOrWorkspaceService<RepositoryModel> {

    @Override
    protected void fillSelectResult(RepositoryModel repositoryModel) {
        if (repositoryModel == null) {
            return;
        }
        if (!StrUtil.startWithIgnoreCase(repositoryModel.getPassword(), ServerConst.REF_WORKSPACE_ENV)) {
            // 隐藏密码字段
            repositoryModel.setPassword(null);
        }
        repositoryModel.setRsaPrv(null);
    }
}
