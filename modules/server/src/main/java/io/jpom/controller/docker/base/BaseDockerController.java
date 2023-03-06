package io.jpom.controller.docker.base;

import io.jpom.common.BaseServerController;

import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2023/3/3
 */
public abstract class BaseDockerController extends BaseServerController {


    /**
     * 根据参数 id 获取 docker 信息
     *
     * @param id id
     * @return docker 信息
     */
    protected abstract Map<String, Object> toDockerParameter(String id);

}
