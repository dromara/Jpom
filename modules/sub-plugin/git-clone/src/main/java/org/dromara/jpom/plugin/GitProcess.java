package org.dromara.jpom.plugin;

import cn.hutool.core.lang.Tuple;

/**
 * Git处理
 * <br>
 * Created By Hong on 2023/3/31
 **/
public interface GitProcess {

    /**
     * 分支和标签列表
     */
    Tuple branchAndTagList() throws Exception;

    /**
     * 拉取指定分支
     */
    Object pull() throws Exception;

    /**
     * 谱曲指定标签
     */
    Object pullByTag() throws Exception;

}
