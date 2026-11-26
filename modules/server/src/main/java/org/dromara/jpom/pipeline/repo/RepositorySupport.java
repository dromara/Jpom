/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.repo;

import org.dromara.jpom.model.data.RepositoryModel;

/**
 * 仓库类型判定（git / svn）集中一处，避免各处重复写 {@code repoType == null || ordinal} 的判断
 *
 * @author jpom
 * @since 2.11.15.3
 */
final class RepositorySupport {

    private RepositorySupport() {
    }

    static boolean isGit(RepositoryModel repository) {
        if (repository == null) {
            return false;
        }
        Integer repoType = repository.getRepoType();
        return repoType == null || repoType == RepositoryModel.RepoType.Git.ordinal();
    }
}
