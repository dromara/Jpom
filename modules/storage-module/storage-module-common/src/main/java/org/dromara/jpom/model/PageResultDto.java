/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.PageUtil;
import cn.hutool.db.PageResult;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.function.Consumer;

/**
 * 分页查询结果对象
 *
 * @author bwcx_jzy
 * @since 2021/12/3
 */
@Data
public class PageResultDto<T> implements Serializable {

    /**
     * 结果
     */
    private List<T> result;
    /**
     * 页码
     */
    private Integer page;
    /**
     * 每页结果数
     */
    private Integer pageSize;
    /**
     * 总页数
     */
    private Integer totalPage;
    /**
     * 总数
     */
    private Integer total;

    public PageResultDto(PageResult<T> pageResult) {
        this.setPage(pageResult.getPage());
        this.setPageSize(pageResult.getPageSize());
        this.setTotalPage(pageResult.getTotalPage());
        this.setTotal(pageResult.getTotal());
    }

    public PageResultDto(int page, int pageSize, int total) {
        this.setPage(page);
        this.setPageSize(pageSize);
        this.setTotalPage(PageUtil.totalPage(total, pageSize));
        this.setTotal(total);
    }

    public void each(Consumer<T> consumer) {
        if (result == null) {
            return;
        }
        result.forEach(consumer);
    }

    public boolean isEmpty() {
        return CollUtil.isEmpty(getResult());
    }

    public T get(int index) {
        return CollUtil.get(getResult(), index);
    }
}
