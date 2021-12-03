package io.jpom.model;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.PageUtil;
import cn.hutool.db.PageResult;

import java.io.Serializable;
import java.util.List;

/**
 * 分页查询结果对象
 *
 * @author bwcx_jzy
 * @since 2021/12/3
 */
public class PageResultDto<T> implements Serializable {

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

	public List<T> getResult() {
		return result;
	}

	public void setResult(List<T> result) {
		this.result = result;
	}

	public Integer getPage() {
		return page;
	}

	public void setPage(Integer page) {
		this.page = page;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

	public Integer getTotalPage() {
		return totalPage;
	}

	public void setTotalPage(Integer totalPage) {
		this.totalPage = totalPage;
	}

	public Integer getTotal() {
		return total;
	}

	public void setTotal(Integer total) {
		this.total = total;
	}

	public boolean isEmpty() {
		return CollUtil.isEmpty(getResult());
	}

	public T get(int index) {
		return CollUtil.get(getResult(), index);
	}
}
