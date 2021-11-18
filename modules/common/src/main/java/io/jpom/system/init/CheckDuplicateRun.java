package io.jpom.system.init;

import cn.hutool.core.util.ObjectUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import io.jpom.JpomApplication;
import io.jpom.common.JpomManifest;
import io.jpom.util.JvmUtil;

/**
 * @author bwcx_jzy
 * @date 2019/9/5
 */
class CheckDuplicateRun {

	static void check() {
		try {
			Class<?> appClass = JpomApplication.getAppClass();
			String pid = String.valueOf(JpomManifest.getInstance().getPid());
			Integer mainClassPid = JvmUtil.findMainClassPid(appClass.getName());
			if (pid.equals(ObjectUtil.toString(mainClassPid))) {
				return;
			}
			DefaultSystemLog.getLog().warn("Jpom 程序建议一个机器上只运行一个对应的程序：" + JpomApplication.getAppType() + "  pid:" + mainClassPid);
		} catch (Exception e) {
			DefaultSystemLog.getLog().error("检查异常", e);
		}
	}
}

