package io.jpom.controller.system;

import cn.hutool.core.io.FileUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import io.jpom.build.BuildUtil;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.common.interceptor.OptLog;
import io.jpom.controller.LoginControl;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.socket.ServiceFileTailWatcher;
import io.jpom.system.ConfigBean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 缓存管理
 *
 * @author bwcx_jzy
 * @date 2019/7/20
 */
@Controller
@RequestMapping(value = "system")
@Feature(cls = ClassFeature.SYSTEM)
public class CacheManageController extends BaseServerController {

//    @RequestMapping(value = "cache.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @Feature(method = MethodFeature.CACHE)
//    public String cache() {
//        if (tryGetNode() == null) {
//            //
//            File file = ConfigBean.getInstance().getTempPath();
//            String fileSize = FileUtil.readableFileSize(FileUtil.size(file));
//            setAttribute("cacheFileSize", fileSize);
//
//            int size = LoginControl.LFU_CACHE.size();
//            setAttribute("ipSize", size);
//            int oneLineCount = ServiceFileTailWatcher.getOneLineCount();
//            setAttribute("readFileOnLineCount", oneLineCount);
//
//            File buildDataDir = BuildUtil.getBuildDataDir();
//            if (buildDataDir.exists()) {
//                fileSize = FileUtil.readableFileSize(FileUtil.size(buildDataDir));
//                setAttribute("cacheBuildFileSize", fileSize);
//            } else {
//                setAttribute("cacheBuildFileSize", 0);
//            }
//        }
//        return "system/cache";
//    }

	/**
	 * @return
	 * @author Hotstrip
	 * get server's cache data
	 * 获取 Server 的缓存数据
	 */
	@RequestMapping(value = "server-cache", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	public String serverCache() {
		Map<String, Object> map = new HashMap<>();
		String fileSize = FileUtil.readableFileSize(BuildUtil.tempFileCacheSize);
		map.put("cacheFileSize", fileSize);

		int size = LoginControl.LFU_CACHE.size();
		map.put("ipSize", size);
		int oneLineCount = ServiceFileTailWatcher.getOneLineCount();
		map.put("readFileOnLineCount", oneLineCount);


		fileSize = FileUtil.readableFileSize(BuildUtil.buildCacheSize);
		map.put("cacheBuildFileSize", fileSize);

		return JsonMessage.getString(200, "ok", map);
	}

	/**
	 * 获取节点中的缓存
	 *
	 * @return json
	 */
	@RequestMapping(value = "node_cache.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.CACHE)
	public String nodeCache() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.Cache).toString();
	}

	/**
	 * 清空缓存
	 *
	 * @param type 类型
	 * @return json
	 */
	@RequestMapping(value = "clearCache.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	@OptLog(UserOperateLogV1.OptType.ClearCache)
	@Feature(method = MethodFeature.CACHE)
	public String clearCache(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "类型错误") String type) {
		switch (type) {
			case "serviceCacheFileSize":
				boolean clean = FileUtil.clean(ConfigBean.getInstance().getTempPath());
				if (!clean) {
					return JsonMessage.getString(504, "清空文件缓存失败");
				}
				break;
			case "serviceIpSize":
				LoginControl.LFU_CACHE.clear();
				break;
			default:
				return NodeForward.request(getNode(), getRequest(), NodeUrl.ClearCache).toString();

		}
		return JsonMessage.getString(200, "清空成功");
	}
}
