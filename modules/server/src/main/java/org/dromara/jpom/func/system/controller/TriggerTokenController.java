package org.dromara.jpom.func.system.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.BaseIdModel;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.user.TriggerTokenLogBean;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.service.ITriggerToken;
import org.dromara.jpom.service.user.TriggerTokenLogServer;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 24/1/17 017
 */
@RestController
@RequestMapping(value = "system/trigger")
@Feature(cls = ClassFeature.SYSTEM_CACHE)
@SystemPermission
public class TriggerTokenController {

    private final TriggerTokenLogServer triggerTokenLogServer;

    public TriggerTokenController(TriggerTokenLogServer triggerTokenLogServer) {
        this.triggerTokenLogServer = triggerTokenLogServer;
    }

    @GetMapping(value = "all-type", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<JSONObject>> allType() {
        List<JSONObject> jsonObjects = triggerTokenLogServer.allType();
        return JsonMessage.success("", jsonObjects);
    }

    @GetMapping(value = "delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<String> delete(String id) {
        triggerTokenLogServer.delete(id);
        return JsonMessage.success("删除成功");
    }

    /**
     * 分页列表
     *
     * @return json
     */
    @PostMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<TriggerTokenLogBean>> list(HttpServletRequest request) {
        PageResultDto<TriggerTokenLogBean> listPage = triggerTokenLogServer.listPage(request);
        listPage.each(triggerTokenLogBean -> {
            String type = triggerTokenLogBean.getType();
            ITriggerToken byType = triggerTokenLogServer.getByType(type);
            if (byType == null) {
                triggerTokenLogBean.setDataName("ERROR:类型不存在" + type);
            } else {
                BaseIdModel byKey = byType.getByKey(triggerTokenLogBean.getDataId());
                if (byKey == null) {
                    triggerTokenLogBean.setDataName("ERROR:关联数据丢失");
                } else {
                    Object name = BeanUtil.getProperty(byKey, "name");
                    if (name == null) {
                        triggerTokenLogBean.setDataName("ERROR:关联数据名称不存在");
                    } else {
                        triggerTokenLogBean.setDataName(name.toString());
                    }
                }
            }
        });
        return JsonMessage.success("", listPage);
    }
}
