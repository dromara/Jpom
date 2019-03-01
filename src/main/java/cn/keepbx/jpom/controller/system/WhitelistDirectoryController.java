package cn.keepbx.jpom.controller.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrSpliter;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.model.UserModel;
import cn.keepbx.jpom.service.system.SystemService;
import cn.keepbx.jpom.system.ConfigBean;
import com.alibaba.fastjson.JSONArray;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;

/**
 * 白名单目录
 *
 * @author jiangzeyin
 * @date 2019/2/28
 */
@Controller
@RequestMapping(value = "/system")
public class WhitelistDirectoryController extends BaseController {
    @Resource
    private SystemService systemService;

    /**
     * 页面
     */
    @RequestMapping(value = "whitelistDirectory", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String whitelistDirectory() {
        setAttribute("value", systemService.getWhitelistDirectoryLine());
        return "system/whitelistDirectory";
    }

    /**
     * 保存接口
     *
     * @param value 值
     * @return json
     */
    @RequestMapping(value = "whitelistDirectory_submit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String whitelistDirectorySubmit(String value) {
        if (ConfigBean.getInstance().safeMode) {
            return JsonMessage.getString(401, "安全模式下不能修改白名单目录");
        }
        UserModel userName = getUser();
        if (!userName.isManage()) {
            return JsonMessage.getString(401, "你没有权限修改白名单目录");
        }
        JsonMessage jsonMessage = save(value);
        return jsonMessage.toString();
    }

    public JsonMessage save(String value) {
        if (StrUtil.isEmpty(value)) {
            return new JsonMessage(401, "白名单不能为空");
        }
        List<String> list = StrSpliter.splitTrim(value, "\n", true);
        if (list == null || list.size() <= 0) {
            return new JsonMessage(401, "白名单不能为空");
        }

        JSONArray jsonArray = new JSONArray();
        for (String s : list) {
            String val = String.format("/%s/", s);
            val = val.replace("../", "");
            val = FileUtil.normalize(val);
            jsonArray.add(val);
        }
        String error = findStartsWith(jsonArray, 0);
        if (error != null) {
            return new JsonMessage(401, "白名单目录中不能存在包含关系：" + error);
        }
        systemService.saveWhitelistDirectory(jsonArray);
        return new JsonMessage(200, "保存成功");
    }


    private String findStartsWith(JSONArray jsonArray, int start) {
        String str = jsonArray.getString(start);
        int len = jsonArray.size();
        for (int i = 0; i < len; i++) {
            if (i == start) {
                continue;
            }
            String findStr = jsonArray.getString(i);
            if (findStr.startsWith(str)) {
                return str;
            }
        }
        if (start < len - 1) {
            return findStartsWith(jsonArray, start + 1);
        }
        return null;
    }
}
