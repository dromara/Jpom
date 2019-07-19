package cn.keepbx.jpom.controller.build;

import cn.hutool.core.io.FileUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorConfig;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import cn.keepbx.build.BuildUtil;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.interceptor.UrlPermission;
import cn.keepbx.jpom.model.Role;
import cn.keepbx.jpom.model.data.BuildModel;
import cn.keepbx.jpom.model.log.BuildHistoryLog;
import cn.keepbx.jpom.model.log.UserOperateLogV1;
import cn.keepbx.jpom.service.build.BuildService;
import cn.keepbx.jpom.service.dblog.DbBuildHistoryLogService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * @author bwcx_jzy
 * @date 2019/7/18
 */
@RestController
@RequestMapping(value = "/build")
public class BuildHistoryController extends BaseServerController {

    @Resource
    private BuildService buildService;
    @Resource
    private DbBuildHistoryLogService dbBuildHistoryLogService;

    /**
     * 开始构建
     *
     * @param logId id
     * @return json
     * @throws IOException e
     */
    @RequestMapping(value = "delete_log.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @UrlPermission(value = Role.System, optType = UserOperateLogV1.OptType.EditBuildLog)
    public String delete(@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据")) String logId) throws IOException, SQLException {
        BuildHistoryLog buildHistoryLog = dbBuildHistoryLogService.getByKey(logId);
        if (buildHistoryLog == null) {
            return JsonMessage.getString(405, "没有对应构建记录");
        }
        BuildModel item = buildService.getItem(buildHistoryLog.getBuildDataId());
        if (item == null) {
            return JsonMessage.getString(404, "没有对应数据");
        }
        File logFile = BuildUtil.getLogFile(item.getId(), buildHistoryLog.getBuildNumberId());
        File dataFile = logFile.getParentFile();
        if (dataFile.exists()) {
            boolean s = FileUtil.del(dataFile);
            if (!s) {
                return JsonMessage.getString(500, "清理文件失败");
            }
        }
        int count = dbBuildHistoryLogService.delByKey(logId);
        return JsonMessage.getString(200, "删除成功", count);
    }

}
