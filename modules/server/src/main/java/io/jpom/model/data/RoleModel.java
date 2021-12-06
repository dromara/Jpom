///*
// * The MIT License (MIT)
// *
// * Copyright (c) 2019 码之科技工作室
// *
// * Permission is hereby granted, free of charge, to any person obtaining a copy of
// * this software and associated documentation files (the "Software"), to deal in
// * the Software without restriction, including without limitation the rights to
// * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
// * the Software, and to permit persons to whom the Software is furnished to do so,
// * subject to the following conditions:
// *
// * The above copyright notice and this permission notice shall be included in all
// * copies or substantial portions of the Software.
// *
// * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
// * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
// * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
// * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
// * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// */
//package io.jpom.model.data;
//
//import cn.hutool.core.util.StrUtil;
//import com.alibaba.fastjson.annotation.JSONField;
//import io.jpom.model.BaseModel;
//import io.jpom.plugin.ClassFeature;
//import io.jpom.plugin.MethodFeature;
//
//import java.util.*;
//
///**
// * 角色
// *
// * @author bwcx_jzy
// * @date 2019/8/15
// */
//public class RoleModel extends BaseModel {
//
//	/**
//	 * 角色使用功能
//	 */
//	private List<RoleFeature> features;
//
//	/**
//	 * 是否有新增权限
//	 */
//	private Boolean canAdd;
//
//	/**
//	 * 旧版本数据
//	 */
//	@JSONField(serialize = false, deserialize = false)
//	private Map<ClassFeature, List<TreeLevel>> dynamicData;
//
//	/**
//	 * 动态数据
//	 */
//	private Map<ClassFeature, List<TreeLevel>> dynamicData2;
//
//	/**
//	 * 修改时间
//	 */
//	private String updateTime;
//
//	private int bindCount;
//
//	public Boolean getCanAdd() {
//		return canAdd;
//	}
//
//	public void setCanAdd(Boolean canAdd) {
//		this.canAdd = canAdd;
//	}
//
//	public int getBindCount() {
//		return bindCount;
//	}
//
//	public void setBindCount(int bindCount) {
//		this.bindCount = bindCount;
//	}
//
//	public String getUpdateTime() {
//		return updateTime;
//	}
//
//	public void setUpdateTime(String updateTime) {
//		this.updateTime = updateTime;
//	}
//
//
//	public Map<ClassFeature, List<TreeLevel>> getDynamicData2() {
//		return dynamicData2;
//	}
//
//	public boolean contains(ClassFeature classFeature, String dataId) {
//		Map<ClassFeature, List<TreeLevel>> dynamicData = getDynamicData2();
//		if (dynamicData == null) {
//			return false;
//		}
//		ClassFeature root = classFeature;
//		while (root.getParent() != null) {
//			root = root.getParent();
//		}
//		List<TreeLevel> treeLevels = dynamicData.get(root);
//		return forTree(treeLevels, classFeature, dataId);
//	}
//
//	public Set<String> getTreeData(ClassFeature classFeature, String dataId) {
//		Map<ClassFeature, List<TreeLevel>> dynamicData = getDynamicData2();
//		if (dynamicData == null) {
//			return new HashSet<>();
//		}
//		ClassFeature root = classFeature;
//		while (root.getParent() != null) {
//			root = root.getParent();
//		}
//		List<TreeLevel> treeLevels = dynamicData.get(root);
//		return forTreeList(treeLevels, classFeature, dataId, null);
//	}
//
//	private boolean forTree(List<TreeLevel> treeLevels, ClassFeature classFeature, String dataId) {
//		if (treeLevels == null || treeLevels.isEmpty()) {
//			return false;
//		}
//		for (TreeLevel treeLevel : treeLevels) {
//			ClassFeature nowFeature = ClassFeature.valueOf(treeLevel.getClassFeature());
//			if (nowFeature == classFeature && StrUtil.equals(treeLevel.getData(), dataId)) {
//				// 是同一个功能
//				return true;
//			}
//			if (nowFeature != classFeature) {
//				List<TreeLevel> children = treeLevel.getChildren();
//				if (forTree(children, classFeature, dataId)) {
//					return true;
//				}
//			}
//		}
//		return false;
//	}
//
//	private Set<String> forTreeList(List<TreeLevel> treeLevels, ClassFeature classFeature, String dataId, String parentDataId) {
//		Set<String> dataIds = new HashSet<>();
//		if (treeLevels == null || treeLevels.isEmpty()) {
//			return dataIds;
//		}
//		for (TreeLevel treeLevel : treeLevels) {
//			ClassFeature nowFeature = ClassFeature.valueOf(treeLevel.getClassFeature());
//			if (nowFeature == classFeature && (dataId == null || StrUtil.equals(parentDataId, dataId))) {
//				// 是同一个功能
////                System.out.println(dataId + "  " + parentDataId);
//				dataIds.add(treeLevel.getData());
//			}
//			if (nowFeature != classFeature) {
//				List<TreeLevel> children = treeLevel.getChildren();
//				Set<String> strings = forTreeList(children, classFeature, dataId, treeLevel.getData());
//				if (!strings.isEmpty()) {
//					return strings;
//				}
//			}
//		}
//		return dataIds;
//	}
//
//	public void setDynamicData2(Map<ClassFeature, List<TreeLevel>> dynamicData) {
//		this.dynamicData2 = dynamicData;
//	}
//
//	public List<RoleFeature> getFeatures() {
//		return features;
//	}
//
//	public void setFeatures(List<RoleFeature> features) {
//		this.features = features;
//	}
//
//	public List<MethodFeature> getMethodFeature(ClassFeature feature) {
//		List<RoleFeature> features = getFeatures();
//		for (RoleFeature roleFeature : features) {
//			if (roleFeature.getFeature() == feature) {
//				return roleFeature.getMethodFeatures();
//			}
//		}
//		return null;
//	}
//
//	public static class RoleFeature {
//		private ClassFeature feature;
//		private List<MethodFeature> methodFeatures;
//
//		public ClassFeature getFeature() {
//			return feature;
//		}
//
//		public void setFeature(ClassFeature feature) {
//			this.feature = feature;
//		}
//
//		public List<MethodFeature> getMethodFeatures() {
//			return methodFeatures;
//		}
//
//		public void setMethodFeatures(List<MethodFeature> methodFeatures) {
//			this.methodFeatures = methodFeatures;
//		}
//	}
//
//	public static class TreeLevel {
//		/**
//		 * 功能数据id
//		 */
//		private String data;
//		/**
//		 * 子级
//		 */
//		private List<TreeLevel> children;
//		/**
//		 * 当前功能
//		 */
//		private String classFeature;
//
//		public String getClassFeature() {
//			return classFeature;
//		}
//
//		public void setClassFeature(String classFeature) {
//			this.classFeature = classFeature;
//		}
//
//		public String getData() {
//			return data;
//		}
//
//		public void setData(String data) {
//			this.data = data;
//		}
//
//		public List<TreeLevel> getChildren() {
//			return children;
//		}
//
//		public void setChildren(List<TreeLevel> children) {
//			this.children = children;
//		}
//
//		public TreeLevel() {
//		}
//
//		@Override
//		public boolean equals(Object o) {
//			if (this == o) {
//				return true;
//			}
//			if (o == null || getClass() != o.getClass()) {
//				return false;
//			}
//			TreeLevel treeLevel = (TreeLevel) o;
//			return Objects.equals(data, treeLevel.data) &&
//					Objects.equals(children, treeLevel.children) &&
//					Objects.equals(classFeature, treeLevel.classFeature);
//		}
//
//		@Override
//		public int hashCode() {
//			return Objects.hash(data, children, classFeature);
//		}
//	}
//}
