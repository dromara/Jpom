package io.jpom.model.data;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import io.jpom.model.BaseModel;

import java.nio.charset.Charset;
import java.util.List;

/**
 * ssh 信息
 *
 * @author bwcx_jzy
 * @date 2019/8/9
 */
public class SshModel extends BaseModel {
	private String host;
	private int port;
	private String user;
	private String password;
	/**
	 * 编码格式
	 */
	private String charset;

	/**
	 * 文件目录
	 */
	private List<String> fileDirs;

	/**
	 * ssh 私钥
	 */
	private String privateKey;

	private ConnectType connectType;

	/**
	 * 临时缓存model
	 */
	private BaseModel nodeModel;

	/**
	 * 不允许执行的命令
	 */
	private String notAllowedCommand;

	public String getNotAllowedCommand() {
		return notAllowedCommand;
	}

	public void setNotAllowedCommand(String notAllowedCommand) {
		this.notAllowedCommand = notAllowedCommand;
	}

	public ConnectType getConnectType() {
		if (connectType == null) {
			return ConnectType.PASS;
		}
		return connectType;
	}

	public void setConnectType(ConnectType connectType) {
		this.connectType = connectType;
	}

	public String getPrivateKey() {
		return privateKey;
	}

	public void setPrivateKey(String privateKey) {
		this.privateKey = privateKey;
	}

	public BaseModel getNodeModel() {
		return nodeModel;
	}

	public void setNodeModel(BaseModel nodeModel) {
		if (nodeModel == null) {
			return;
		}
		this.nodeModel = new BaseModel() {
			@Override
			public String getName() {
				return nodeModel.getName();
			}

			@Override
			public String getId() {
				return nodeModel.getId();
			}
		};
	}

	public List<String> getFileDirs() {
		return fileDirs;
	}

	public void setFileDirs(List<String> fileDirs) {
		if (fileDirs != null) {
			for (int i = fileDirs.size() - 1; i >= 0; i--) {
				String s = fileDirs.get(i);
				fileDirs.set(i, FileUtil.normalize(s));
			}
		}
		this.fileDirs = fileDirs;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public Charset getCharsetT() {
		Charset charset;
		try {
			charset = Charset.forName(this.getCharset());
		} catch (Exception e) {
			charset = CharsetUtil.CHARSET_UTF_8;
		}
		return charset;
	}

	/**
	 * 检查是否包含禁止命令
	 *
	 * @param sshItem   实体
	 * @param inputItem 输入的命令
	 * @return false 存在禁止输入的命令
	 */
	public static boolean checkInputItem(SshModel sshItem, String inputItem) {
		// 检查禁止执行的命令
		String notAllowedCommand = sshItem.getNotAllowedCommand();
		List<String> split = StrUtil.split(notAllowedCommand, StrUtil.COMMA);
		for (String s : split) {
			s = s.toLowerCase();
			if (StrUtil.startWithAny(inputItem.toLowerCase(), s + StrUtil.SPACE, ("&" + s + StrUtil.SPACE), StrUtil.SPACE + s + StrUtil.SPACE)) {
				return false;
			}
		}
		return true;
	}

	public enum ConnectType {
		/**
		 * 账号密码
		 */
		PASS,
		/**
		 * 密钥
		 */
		PUBKEY
	}
}
