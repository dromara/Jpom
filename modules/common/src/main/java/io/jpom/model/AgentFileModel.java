package io.jpom.model;

/**
 * @author lf
 */
public class AgentFileModel extends BaseModel {
	/**
	 * 文件大小
	 */
	private long size = 0;
	/**
	 * 保存路径
	 */
	private String savePath;
	/**
	 * 版本号
	 */
	private String version;
	/**
	 * jar 打包时间
	 */
	private String timeStamp;

	public String getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public String getSavePath() {
		return savePath;
	}

	public void setSavePath(String savePath) {
		this.savePath = savePath;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
}
