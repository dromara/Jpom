package io.jpom.controller.manage.vo;

import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2021/12/16
 */
public class DiffFileVo {

	private String id;
	private List<DiffItem> data;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<DiffItem> getData() {
		return data;
	}

	public void setData(List<DiffItem> data) {
		this.data = data;
	}

	public static class DiffItem {
		/**
		 * 名称
		 */
		private String name;
		/**
		 * 文件签名
		 */
		private String sha1;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getSha1() {
			return sha1;
		}

		public void setSha1(String sha1) {
			this.sha1 = sha1;
		}
	}
}
