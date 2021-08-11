package io.jpom.model.data;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseJpomController;
import io.jpom.model.BaseJsonModel;
import io.jpom.system.ExtConfigBean;
import org.springframework.util.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 白名单
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
public class AgentWhitelist extends BaseJsonModel {
	/**
	 * 项目目录白名单、日志文件白名单
	 */
	private List<String> project;
	/**
	 * ssl 证书文件白名单
	 */
	private List<String> certificate;
	/**
	 * nginx 配置文件 白名单
	 */
	private List<String> nginx;

	/**
	 * 运行编辑的后缀文件
	 */
	private List<String> allowEditSuffix;

	/**
	 * 运行远程下载的 host
	 */
	private Set<String> allowRemoteDownloadHost;

	public Set<String> getAllowRemoteDownloadHost() {
		return allowRemoteDownloadHost;
	}

	public void setAllowRemoteDownloadHost(Set<String> allowRemoteDownloadHost) {
		this.allowRemoteDownloadHost = allowRemoteDownloadHost;
	}

	public List<String> getAllowEditSuffix() {
		return allowEditSuffix;
	}

	public void setAllowEditSuffix(List<String> allowEditSuffix) {
		this.allowEditSuffix = allowEditSuffix;
	}

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
	public static List<String> covertToArray(List<String> list, String errorMsg) {
		if (list == null) {
			return null;
		}
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
				throw new IllegalArgumentException(errorMsg);
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
	public static String convertToLine(Collection<String> jsonArray) {
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

	/**
	 * 将字符串转为 list
	 *
	 * @param value    字符串
	 * @param errorMsg 错误消息
	 * @return list
	 */
	public static List<String> parseToList(String value, String errorMsg) {
		return parseToList(value, false, errorMsg);
	}

	/**
	 * 将字符串转为 list
	 *
	 * @param value    字符串
	 * @param required 是否为必填
	 * @param errorMsg 错误消息
	 * @return list
	 */
	public static List<String> parseToList(String value, boolean required, String errorMsg) {
		if (required) {
			Assert.hasLength(value, errorMsg);
		} else {
			if (StrUtil.isEmpty(value)) {
				return null;
			}
		}
		List<String> list = StrSplitter.splitTrim(value, StrUtil.LF, true);
		Assert.notEmpty(list, errorMsg);
		return list;
	}
}
