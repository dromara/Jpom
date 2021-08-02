package io.jpom.system.init;

import cn.hutool.core.lang.Console;
import cn.hutool.core.net.NetUtil;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import io.jpom.common.JpomManifest;
import io.jpom.common.Type;
import io.jpom.system.ConfigBean;

/**
 * @author bwcx_jzy
 * @since Created Time 2021/8/2
 */
@PreLoadClass(value = Integer.MAX_VALUE)
public class ConsoleStartSuccess {

	/**
	 * 输出启动成功的 日志
	 */
	@PreLoadMethod(value = Integer.MAX_VALUE)
	private static void success() {
		Type type = JpomManifest.getInstance().getType();
		if (type == Type.Server) {
			Console.log(type + "启动成功,Can use happily => http://{}:{}", NetUtil.getLocalhostStr(), ConfigBean.getInstance().getPort());
		} else if (type == Type.Agent) {
			Console.log(type + "启动成功,请到服务端配置使用,当前节点地址 => http://{}:{}", NetUtil.getLocalhostStr(), ConfigBean.getInstance().getPort());
		}
	}
}
