/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.extra.servlet.ServletUtil;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.data.NodeProjectInfoModel;
import org.dromara.jpom.service.manage.ProjectInfoService;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * agent 端
 *
 * @author bwcx_jzy
 * @since 2019/4/17
 */
public abstract class BaseAgentController extends BaseJpomController {

    @Resource
    protected ProjectInfoService projectInfoService;


    /**
     * 获取server 端操作人
     *
     * @param request req
     * @return name
     */
    private static String getUserName(HttpServletRequest request) {
        String name = ServletUtil.getHeaderIgnoreCase(request, Const.JPOM_SERVER_USER_NAME);
        name = CharsetUtil.convert(name, CharsetUtil.CHARSET_ISO_8859_1, CharsetUtil.CHARSET_UTF_8);
        name = StrUtil.emptyToDefault(name, StrUtil.DASHED);
        return URLUtil.decode(name, CharsetUtil.CHARSET_UTF_8);
    }

    /**
     * 获取server 端操作人
     *
     * @return name
     */
    public static String getNowUserName() {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (servletRequestAttributes == null) {
            return StrUtil.DASHED;
        }
        HttpServletRequest request = servletRequestAttributes.getRequest();
        return getUserName(request);
    }

    protected String getWorkspaceId() {
        return ServletUtil.getHeader(getRequest(), Const.WORKSPACE_ID_REQ_HEADER, CharsetUtil.CHARSET_UTF_8);
    }

    /**
     * 获取拦截器中缓存的项目信息
     *
     * @return NodeProjectInfoModel
     */
    protected NodeProjectInfoModel getProjectInfoModel() {
        String id = getParameter("id");
        NodeProjectInfoModel nodeProjectInfoModel = tryGetProjectInfoModel(id);
        Assert.notNull(nodeProjectInfoModel, I18nMessageUtil.get("i18n.get_project_info_failure.ddff") + id);
        return nodeProjectInfoModel;
    }

    /**
     * 根据 项目ID 获取项目信息
     *
     * @return NodeProjectInfoModel
     */
    protected NodeProjectInfoModel getProjectInfoModel(String id) {
        NodeProjectInfoModel nodeProjectInfoModel = tryGetProjectInfoModel(id);
        Assert.notNull(nodeProjectInfoModel, I18nMessageUtil.get("i18n.get_project_info_failure.ddff") + id);
        return nodeProjectInfoModel;
    }

    protected NodeProjectInfoModel tryGetProjectInfoModel() {
        String id = getParameter("id");
        return tryGetProjectInfoModel(id);
    }

    protected NodeProjectInfoModel tryGetProjectInfoModel(String id) {
        if (StrUtil.isNotEmpty(id)) {
            return projectInfoService.getItem(id);
        }
        return null;
    }
}
