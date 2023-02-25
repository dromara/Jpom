package io.jpom.func;

import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import io.jpom.common.BaseServerController;
import io.jpom.common.JsonMessage;
import io.jpom.model.BaseGroupNameModel;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.service.h2db.BaseDbService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2023/2/25
 */
public abstract class BaseGroupNameController extends BaseServerController {

    private final BaseDbService<? extends BaseGroupNameModel> dbService;

    protected BaseGroupNameController(BaseDbService<? extends BaseGroupNameModel> dbService) {
        this.dbService = dbService;
    }

    @GetMapping(value = "list-group", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<List<String>> listGroup() {
        String sql = "select `groupName` from " + dbService.getTableName() + " group by `groupName`";
        List<Entity> list = dbService.query(sql);
        // 筛选字段
        List<String> collect = list.stream()
            .map(entity -> {
                Object obj = entity.get("groupName");
                return StrUtil.toStringOrNull(obj);
            })
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        return JsonMessage.success("", collect);
    }
}
