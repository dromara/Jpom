package io.jpom.controller.system;

import cn.hutool.core.io.FileUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseAgentController;
import io.jpom.common.commander.AbstractProjectCommander;
import io.jpom.socket.AgentFileTailWatcher;
import io.jpom.system.ConfigBean;
import io.jpom.util.JvmUtil;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

/**
 * 缓存管理
 *
 * @author bwcx_jzy
 * @date 2019/7/20
 */
@RestController
@RequestMapping(value = "system")
public class AgentCacheManageController extends BaseAgentController {

    /**
     * 缓存信息
     *
     * @return json
     */
    @RequestMapping(value = "cache", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String cache() {
        JSONObject jsonObject = new JSONObject();
        //
        File file = ConfigBean.getInstance().getTempPath();
        String fileSize = FileUtil.readableFileSize(FileUtil.size(file));
        jsonObject.put("fileSize", fileSize);
        //
        jsonObject.put("pidName", AbstractProjectCommander.PID_JPOM_NAME.size());
        jsonObject.put("pidPort", AbstractProjectCommander.PID_PORT.size());

        int oneLineCount = AgentFileTailWatcher.getOneLineCount();
        jsonObject.put("readFileOnLineCount", oneLineCount);
        //
        jsonObject.put("pidError", JvmUtil.PID_ERROR.size());
        return JsonMessage.getString(200, "ok", jsonObject);
    }

    /**
     * 清空缓存
     *
     * @param type 缓存类型
     * @return json
     */
    @RequestMapping(value = "clearCache", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String clearCache(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "类型错误") String type) {
        switch (type) {
            case "pidPort":
                AbstractProjectCommander.PID_PORT.clear();
                break;
            case "pidName":
                AbstractProjectCommander.PID_JPOM_NAME.clear();
                break;
            case "fileSize":
                boolean clean = FileUtil.clean(ConfigBean.getInstance().getTempPath());
                if (!clean) {
                    return JsonMessage.getString(504, "清空文件缓存失败");
                }
                break;
            case "pidError":
                JvmUtil.PID_ERROR.clear();
                break;
            default:
                return JsonMessage.getString(405, "没有对应类型：" + type);

        }
        return JsonMessage.getString(200, "清空成功");
    }
}
