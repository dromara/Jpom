/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.model.outgiving;

import io.jpom.model.BaseEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 节点项目
 *
 * @author jiangzeyin
 * @since 2019/4/22
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OutGivingNodeProject extends BaseNodeProject {

    private String lastOutGivingTime;
    private Integer status;
    private String result;

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
        Cancel(4, "取消分发"),
        ;
        private final int code;
        private final String desc;

        Status(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }
    }
}
