package cn.keepbx.jpom.controller.build;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.util.CharsetUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorConfig;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import cn.keepbx.build.BuildManage;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.interceptor.UrlPermission;
import cn.keepbx.jpom.model.BaseEnum;
import cn.keepbx.jpom.model.Role;
import cn.keepbx.jpom.model.data.BuildModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.model.data.UserOperateLogV1;
import cn.keepbx.jpom.service.build.BuildService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * @author bwcx_jzy
 * @date 2019/7/16
 */
@RestController
@RequestMapping(value = "/build")
public class BuildManageController extends BaseServerController {

    @Resource
    private BuildService buildService;

    /**
     * 开始构建
     *
     * @param id id
     * @return json
     * @throws IOException e
     */
    @RequestMapping(value = "start.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @UrlPermission(value = Role.ServerManager, optType = UserOperateLogV1.OptType.StartBuild)
    public String start(@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据")) String id) throws IOException {
        BuildModel item = buildService.getItem(id);
        if (item == null) {
            return JsonMessage.getString(404, "没有对应数据");
        }
        BuildModel.Status nowStatus = BaseEnum.getEnum(BuildModel.Status.class, item.getStatus());
        Objects.requireNonNull(nowStatus);
        if (BuildModel.Status.Ing == nowStatus ||
                BuildModel.Status.PubIng == nowStatus) {
            return JsonMessage.getString(501, "当前还在：" + nowStatus.getDesc());
        }
        //
        item.setBuildId(item.getBuildId() + 1);
        UserModel userModel = getUser();
        String optUserName = UserModel.getOptUserName(userModel);
        item.setModifyUser(optUserName);
        buildService.updateItem(item);
        BuildManage.create(item, userModel);
        return JsonMessage.getString(200, "开始构建中", item.getBuildId());
    }

    /**
     * 取消构建
     *
     * @param id id
     * @return json
     * @throws IOException e
     */
    @RequestMapping(value = "cancel.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @UrlPermission(value = Role.ServerManager, optType = UserOperateLogV1.OptType.CancelBuild)
    public String cancel(@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据")) String id) throws IOException {
        BuildModel item = buildService.getItem(id);
        if (item == null) {
            return JsonMessage.getString(404, "没有对应数据");
        }
        BuildModel.Status nowStatus = BaseEnum.getEnum(BuildModel.Status.class, item.getStatus());
        Objects.requireNonNull(nowStatus);
        if (BuildModel.Status.Ing != nowStatus) {
            return JsonMessage.getString(501, "当前不在进行中");
        }
        boolean status = BuildManage.cancel(item.getId());
        if (!status) {
            item.setStatus(BuildModel.Status.Cancel.getCode());
            buildService.updateItem(item);
        }
        return JsonMessage.getString(200, "取消成功");
    }

    /**
     * 获取构建的日志
     *
     * @param id      id
     * @param buildId 构建编号
     * @param line    需要获取的行号
     * @return json
     * @throws IOException e
     */
    @RequestMapping(value = "getNowLog.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getNowLog(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据") String id,
                            @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "没有buildId") int buildId,
                            @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "line") int line) throws IOException {
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
        JSONObject data = new JSONObject();
        data.put("run", item.getStatus() == BuildModel.Status.Ing.getCode() || item.getStatus() == BuildModel.Status.PubIng.getCode());
        // 读取文件
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
        // 下次应该获取的行数
        data.put("line", readCount[0] + 1);
        data.put("getLine", linesInt);
        data.put("dataLines", lines);
        return JsonMessage.getString(200, "ok", data);
    }
}
