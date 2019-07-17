package cn.keepbx.jpom.controller.build;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorConfig;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.model.BaseEnum;
import cn.keepbx.jpom.model.data.BuildHistoryLog;
import cn.keepbx.jpom.model.data.BuildModel;
import cn.keepbx.jpom.model.vo.BuildHistoryLogVo;
import cn.keepbx.jpom.service.build.BuildService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.IOException;
import java.sql.SQLException;

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

    @RequestMapping(value = "history.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String logList() {
        JSONArray jsonArray = BaseEnum.toJSONArray(BuildModel.Status.class);
        setAttribute("status", jsonArray);
        return "build/history";
    }

    @RequestMapping(value = "history_list.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String historyList(String time, String status,
                              @ValidatorConfig(value = {
                                      @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "limit error")
                              }, defaultVal = "10") int limit,
                              @ValidatorConfig(value = {
                                      @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "page error")
                              }, defaultVal = "1") int page) throws SQLException {
        Page pageObj = new Page(page, limit);
        Entity entity = Entity.create(BuildHistoryLog.TABLE_NAME);
        pageObj.addOrder(new Order("startTime".toUpperCase(), Direction.DESC));
        // 时间
        if (StrUtil.isNotEmpty(time)) {
            String[] val = StrUtil.split(time, "~");
            if (val.length == 2) {
                DateTime startDateTime = DateUtil.parse(val[0], DatePattern.NORM_DATETIME_FORMAT);
                entity.set("startTime".toUpperCase(), ">= " + startDateTime.getTime());

                DateTime endDateTime = DateUtil.parse(val[1], DatePattern.NORM_DATETIME_FORMAT);
                if (startDateTime.equals(endDateTime)) {
                    endDateTime = DateUtil.endOfDay(endDateTime);
                }
                entity.set("startTime ".toUpperCase(), "<= " + endDateTime.getTime());
            }
        }
        BaseEnum anEnum = null;
        if (StrUtil.isNotEmpty(status)) {
            Integer integer = Convert.toInt(status);
            if (integer != null) {
                anEnum = BaseEnum.getEnum(BuildModel.Status.class, integer);
            }
        }

        if (anEnum != null) {
            entity.set("status".toUpperCase(), anEnum.getCode());
        }

        PageResult<Entity> pageResult = Db.use().page(entity, pageObj);
        CopyOptions copyOptions = new CopyOptions();
        copyOptions.setIgnoreError(true);
        copyOptions.setIgnoreCase(true);
        JSONArray jsonArray = new JSONArray();
        pageResult.forEach(entity1 -> {
            BuildHistoryLogVo v1 = BeanUtil.mapToBean(entity1, BuildHistoryLogVo.class, copyOptions);
            String dataId = v1.getBuildDataId();
            try {
                BuildModel item = buildService.getItem(dataId);
                v1.setBuildName(item.getName());
            } catch (IOException ignored) {
            }
            jsonArray.add(v1);
        });
        JSONObject jsonObject = JsonMessage.toJson(200, "获取成功", jsonArray);
        jsonObject.put("total", pageResult.getTotal());
        return jsonObject.toString();
    }
}
