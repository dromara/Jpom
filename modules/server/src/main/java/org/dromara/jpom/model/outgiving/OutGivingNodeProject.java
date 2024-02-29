/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.outgiving;

import cn.hutool.core.util.ObjectUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.dromara.jpom.model.BaseEnum;

/**
 * 节点项目
 *
 * @author bwcx_jzy
 * @since 2019/4/22
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OutGivingNodeProject extends BaseNodeProject {

    /**
     * 排序值
     */
    private Integer sortValue;

    /**
     * 是否禁用
     */
    private Boolean disabled;

    public Boolean getDisabled() {
        return ObjectUtil.defaultIfNull(disabled, false);
    }

    /**
     * 状态
     */
    @Getter
    public enum Status implements BaseEnum {
        /**
         *
         */
        No(0, "未分发"),
        Ing(1, "分发中"),
        Ok(2, "分发成功"),
        Fail(3, "分发失败"),
        Cancel(4, "系统取消分发"),
        Prepare(5, "准备分发"),
        ArtificialCancel(6, "手动取消分发"),
        ;
        private final int code;
        private final String desc;

        Status(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }
    }
}
