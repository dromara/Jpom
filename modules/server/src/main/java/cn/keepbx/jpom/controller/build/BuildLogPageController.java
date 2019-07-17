package cn.keepbx.jpom.controller.build;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.util.CharsetUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorConfig;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import cn.keepbx.build.BuildManage;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.model.data.BuildModel;
import cn.keepbx.jpom.service.build.BuildService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * @author bwcx_jzy
 * @date 2019/7/17
 **/
@Controller
@RequestMapping(value = "/build")
public class BuildLogPageController extends BaseServerController {

    @Resource
    private BuildService buildService;

    @RequestMapping(value = "logPage.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String logPage() {
        return "build/logPage";
    }

    @RequestMapping(value = "getNowLog.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String getNowLog(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据") String id,
                            @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "没有buildId") int buildId,
                            String line) throws IOException {
        BuildModel item = buildService.getItem(id);
        if (item == null) {
            return JsonMessage.getString(404, "没有对应数据");
        }
        if (buildId > item.getBuildId()) {
            return JsonMessage.getString(405, "还没有对应的构建记录");
        }
        File file = BuildManage.getLogFile(item, buildId);
        if (file.isDirectory()) {
            return JsonMessage.getString(300, "日志文件错误");
        }
        if (!file.exists()) {
            if (buildId == item.getBuildId()) {
                return JsonMessage.getString(201, "还没有日志文件");
            }
            return JsonMessage.getString(300, "日志文件不存在");
        }
        int linesInt = Convert.toInt(line, 1);
        List<String> lines = new LinkedList<>();
        final int[] readCount = {0};
        FileUtil.readLines(file, CharsetUtil.CHARSET_UTF_8, (LineHandler) line1 -> {
            readCount[0]++;
            if (readCount[0] < linesInt) {
                return;
            }
            lines.add(line1);
        });
        JSONObject data = new JSONObject();
        data.put("line", readCount[0]);
        data.put("dataLines", lines);
        return JsonMessage.getString(200, "ok", data);
    }
}
