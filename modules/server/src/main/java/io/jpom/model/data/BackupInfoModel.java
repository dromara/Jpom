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
package io.jpom.model.data;

import io.jpom.model.BaseUserModifyDbModel;
import io.jpom.service.h2db.TableName;

/**
 * @author Hotstrip
 * @since 2021-11-18
 * Backup info with H2 database
 */
@TableName(value = "BACKUP_INFO", name = "数据备份")
public class BackupInfoModel extends BaseUserModifyDbModel {

	/**
	 * 备份名称
	 */
	private String name;
	/**
	 * 文件地址
	 */
	private String filePath;
	/**
	 * 备份类型{0: 全量, 1: 部分, 2: 导入, 3 自动}
	 */
	private Integer backupType;
	/**
	 * 文件大小
	 */
	private Long fileSize;
	/**
	 * SHA1SUM
	 */
	private String sha1Sum;

	/**
	 * 状态{0: 默认; 1: 成功; 2: 失败}
	 */
	private Integer status;

	/**
	 * 服务端版本
	 */
	private String version;

	/**
	 * 打包时间
	 */
	private Long baleTimeStamp;

	public Long getBaleTimeStamp() {
		return baleTimeStamp;
	}

	public void setBaleTimeStamp(Long baleTimeStamp) {
		this.baleTimeStamp = baleTimeStamp;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public Integer getBackupType() {
		return backupType;
	}

	public void setBackupType(Integer backupType) {
		this.backupType = backupType;
	}

	public Long getFileSize() {
		return fileSize;
	}

	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}

	public String getSha1Sum() {
		return sha1Sum;
	}

	public void setSha1Sum(String sha1Sum) {
		this.sha1Sum = sha1Sum;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}
}
