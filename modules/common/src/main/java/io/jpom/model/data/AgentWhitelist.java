package io.jpom.model.data;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseJpomController;
import io.jpom.model.BaseJsonModel;
import io.jpom.system.ExtConfigBean;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 白名单
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
public class AgentWhitelist extends BaseJsonModel {
	private List<String> project;
	private List<String> certificate;
	private List<String> nginx;

	public List<String> getProject() {
		return project;
	}

	public void setProject(List<String> project) {
		this.project = project;
	}

	public List<String> getCertificate() {
		return certificate;
	}

	public void setCertificate(List<String> certificate) {
		this.certificate = certificate;
	}

	public List<String> getNginx() {
		return nginx;
	}

	public void setNginx(List<String> nginx) {
		this.nginx = nginx;
	}

	/**
	 * 格式化，判断是否与jpom 数据路径冲突
	 *
	 * @param list list
	 * @return null 是有冲突的
	 */
	public static List<String> covertToArray(List<String> list) {
		List<String> array = new ArrayList<>();
		for (String s : list) {
			String val = String.format("/%s/", s);
			val = BaseJpomController.pathSafe(val);
			if (StrUtil.SLASH.equals(val)) {
				continue;
			}
			if (array.contains(val)) {
				continue;
			}
			// 判断是否保护jpom 路径
			if (val == null || val.startsWith(ExtConfigBean.getInstance().getPath())) {
				return null;
			}
			array.add(val);
		}
		return array;
	}

	/**
	 * 转换为字符串
	 *
	 * @param jsonArray jsonArray
	 * @return str
	 */
	public static String convertToLine(List<String> jsonArray) {
		try {
			return CollUtil.join(jsonArray, StrUtil.CRLF);
		} catch (Exception e) {
			DefaultSystemLog.getLog().error(e.getMessage(), e);
		}
		return "";
	}

	/**
	 * 判断是否在白名单列表中
	 *
	 * @param list list
	 * @param path 对应项
	 * @return false 不在列表中
	 */
	public static boolean checkPath(List<String> list, String path) {
		if (list == null) {
			return false;
		}
		if (StrUtil.isEmpty(path)) {
			return false;
		}
		File file1, file2 = FileUtil.file(path);
		for (String item : list) {
			file1 = FileUtil.file(item);
			if (FileUtil.pathEquals(file1, file2)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 解析出json 中的白名单字段
	 *
	 * @param projectInfo 项目的json对象
	 * @param path        要比较的白名单
	 * @return null 不是该白名单
	 */
	public static String getItemWhitelistDirectory(JSONObject projectInfo, String path) {
		String lib = projectInfo.getString("lib");
		if (lib.startsWith(path)) {
			String itemWhitelistDirectory = lib.substring(0, path.length());
			lib = lib.substring(path.length());

			projectInfo.put("lib", lib);
			return itemWhitelistDirectory;
		}
		return null;
	}
}
