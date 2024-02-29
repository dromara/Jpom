/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.system;

import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.model.BaseJsonModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 网络端口信息实体
 *
 * @author bwcx_jzy
 * @since 2019/4/10
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class NetstatModel extends BaseJsonModel {
    /**
     * 协议
     */
    private String protocol;
    private String receive = StrUtil.DASHED;
    private String send = StrUtil.DASHED;
    /**
     * 端口
     */
    private String local;
    private String foreign;
    private String status;
    private String name;
}
