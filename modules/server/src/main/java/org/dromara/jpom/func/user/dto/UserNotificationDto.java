/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.user.dto;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.springframework.util.Assert;

/**
 * @author bwcx_jzy1
 * @since 2024/4/20
 */
@Data
public class UserNotificationDto {
    /**
     * 是否开启公告
     */
    private Boolean enabled;
    /**
     * 是否可以关闭
     */
    private Boolean closable;
    /**
     * 公告级别
     */
    private Level level;
    /**
     * 公告标题
     */
    private String title;
    /**
     * 公告内容
     */
    private String content;

    public enum Level {
        info, warning, error
    }

    public void verify() {
        if (this.enabled != null && this.enabled) {
            Assert.state(!StrUtil.isAllBlank(this.title, this.content), I18nMessageUtil.get("i18n.configure_announcement_title_or_content.7894"));
        }
    }
}
