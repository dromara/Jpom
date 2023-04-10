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
     * 谱曲指定标签
     */
    Object pullByTag() throws Exception;

}
