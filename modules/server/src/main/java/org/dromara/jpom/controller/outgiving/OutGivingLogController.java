/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.outgiving;

import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.log.OutGivingLog;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.service.outgiving.DbOutGivingLogService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 分发日志
 *
 * @author bwcx_jzy
 * @since 2019/7/20
 */
@RestController
@RequestMapping(value = "/outgiving")
@Feature(cls = ClassFeature.OUTGIVING_LOG)
public class OutGivingLogController extends BaseServerController {

    private final DbOutGivingLogService dbOutGivingLogService;

    public OutGivingLogController(DbOutGivingLogService dbOutGivingLogService) {
        this.dbOutGivingLogService = dbOutGivingLogService;
    }


    @RequestMapping(value = "log_list_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<OutGivingLog>> listData(HttpServletRequest request) {
        PageResultDto<OutGivingLog> pageResult = dbOutGivingLogService.listPage(request);
        return JsonMessage.success(I18nMessageUtil.get("i18n.get_success.fb55"), pageResult);
    }
}
