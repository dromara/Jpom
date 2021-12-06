/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 码之科技工作室
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
//package io.jpom.model.vo;
//
//import cn.hutool.core.io.FileUtil;
//import io.jpom.build.BuildUtil;
//import io.jpom.model.data.BuildInfoModel;
//import io.jpom.model.log.BuildHistoryLog;
//
//import java.io.File;
//
///**
// * 构建产物vo
// *
// * @author bwcx_jzy
// * @date 2019/7/17
// */
//public class BuildHistoryLogVo extends BuildHistoryLog {
//
//	private String releaseDesc;
//	/**
//	 * 是否存在构建产物
//	 */
//	private boolean hashFile;
//	/**
//	 * 是否存在日志
//	 */
//	private boolean hasLog;
//
//	public boolean isHasLog() {
//		File file = BuildUtil.getLogFile(getBuildDataId(), getBuildNumberId());
//		hasLog = FileUtil.exist(file);
//		return hasLog;
//	}
//
//	public void setHasLog(boolean hasLog) {
//		this.hasLog = hasLog;
//	}
//
//	public boolean isHashFile() {
//		File file = BuildUtil.getHistoryPackageFile(getBuildDataId(), getBuildNumberId(), getResultDirFile());
//		hashFile = FileUtil.exist(file);
//		return hashFile;
//	}
//
//	public void setHashFile(boolean hashFile) {
//		this.hashFile = hashFile;
//	}
//
//	public String getBuildIdStr() {
//		return BuildInfoModel.getBuildIdStr(getBuildNumberId());
//	}
//
//	public void setReleaseDesc(String releaseDesc) {
//		this.releaseDesc = releaseDesc;
//	}
//
////	/**
////	 * 发布描述
////	 *
////	 * @return 描述
////	 */
////	public String getReleaseDesc() {
////		if (releaseDesc == null) {
////			int releaseMethod = getReleaseMethod();
////			BuildReleaseMethod releaseMethod1 = BaseEnum.getEnum(BuildReleaseMethod.class, releaseMethod);
////			if (releaseMethod1 == null) {
////				return BuildReleaseMethod.No.getDesc();
////			}
////			String releaseMethodDataId = getReleaseMethodDataId();
////			switch (releaseMethod1) {
////				case Project: {
////					String[] datas = releaseMethodDataId.split(":");
////					return String.format("【%s】节点【%s】项目", datas[0], datas[1]);
////				}
////				case Outgiving: {
////					OutGivingServer outGivingServer = SpringUtil.getBean(OutGivingServer.class);
////					OutGivingModel item = outGivingServer.getItem(releaseMethodDataId);
////					if (item == null) {
////						return "-";
////					}
////					return "【" + item.getName() + "】分发";
////				}
////				case No:
////				default:
////					return releaseMethod1.getDesc();
////			}
////		}
////		return releaseDesc;
////	}
//
//}
