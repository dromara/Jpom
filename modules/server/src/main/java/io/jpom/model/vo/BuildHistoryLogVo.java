package io.jpom.model.vo;

import cn.hutool.core.io.FileUtil;
import io.jpom.build.BuildUtil;
import io.jpom.model.data.BuildInfoModel;
import io.jpom.model.log.BuildHistoryLog;

import java.io.File;

/**
 * 构建产物vo
 *
 * @author bwcx_jzy
 * @date 2019/7/17
 */
public class BuildHistoryLogVo extends BuildHistoryLog {

	private String releaseDesc;
	/**
	 * 是否存在构建产物
	 */
	private boolean hashFile;
	/**
	 * 是否存在日志
	 */
	private boolean hasLog;

	public boolean isHasLog() {
		File file = BuildUtil.getLogFile(getBuildDataId(), getBuildNumberId());
		hasLog = FileUtil.exist(file);
		return hasLog;
	}

	public void setHasLog(boolean hasLog) {
		this.hasLog = hasLog;
	}

	public boolean isHashFile() {
		File file = BuildUtil.getHistoryPackageFile(getBuildDataId(), getBuildNumberId(), getResultDirFile());
		hashFile = FileUtil.exist(file);
		return hashFile;
	}

	public void setHashFile(boolean hashFile) {
		this.hashFile = hashFile;
	}

	public String getBuildIdStr() {
		return BuildInfoModel.getBuildIdStr(getBuildNumberId());
	}

	public void setReleaseDesc(String releaseDesc) {
		this.releaseDesc = releaseDesc;
	}

//	/**
//	 * 发布描述
//	 *
//	 * @return 描述
//	 */
//	public String getReleaseDesc() {
//		if (releaseDesc == null) {
//			int releaseMethod = getReleaseMethod();
//			BuildReleaseMethod releaseMethod1 = BaseEnum.getEnum(BuildReleaseMethod.class, releaseMethod);
//			if (releaseMethod1 == null) {
//				return BuildReleaseMethod.No.getDesc();
//			}
//			String releaseMethodDataId = getReleaseMethodDataId();
//			switch (releaseMethod1) {
//				case Project: {
//					String[] datas = releaseMethodDataId.split(":");
//					return String.format("【%s】节点【%s】项目", datas[0], datas[1]);
//				}
//				case Outgiving: {
//					OutGivingServer outGivingServer = SpringUtil.getBean(OutGivingServer.class);
//					OutGivingModel item = outGivingServer.getItem(releaseMethodDataId);
//					if (item == null) {
//						return "-";
//					}
//					return "【" + item.getName() + "】分发";
//				}
//				case No:
//				default:
//					return releaseMethod1.getDesc();
//			}
//		}
//		return releaseDesc;
//	}

}
