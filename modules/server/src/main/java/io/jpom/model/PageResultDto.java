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
package io.jpom.model;

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

	@SuppressWarnings({"rawtypes"})
	public static final PageResultDto EMPTY = new PageResultDto<>(1, 10, 0);

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

	public PageResultDto(PageResultDto<?> pageResult) {
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
