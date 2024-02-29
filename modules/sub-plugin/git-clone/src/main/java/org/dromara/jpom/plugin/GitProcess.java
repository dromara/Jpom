/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.plugin;

import cn.hutool.core.lang.Tuple;

/**
 * Git处理
 * <br>
 * Created By Hong on 2023/3/31
 *
 * @author Hong
 */
public interface GitProcess {

    /**
     * 分支和标签列表
     *
     * @return tuple
     * @throws Exception 异常
     */
    Tuple branchAndTagList() throws Exception;

    /**
     * 拉取指定分支
     *
     * @return 拉取结果
     * @throws Exception 异常
     */
    String[] pull() throws Exception;

    /**
     * 拉取指定标签
     *
     * @return 拉取结果
     * @throws Exception 异常
     */
    String[] pullByTag() throws Exception;

}
