/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.system;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNode;
import cn.hutool.core.lang.tree.TreeUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.func.system.model.ClusterInfoModel;
import org.dromara.jpom.func.system.service.ClusterInfoService;
import org.dromara.jpom.model.BaseWorkspaceModel;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.WorkspaceModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.service.system.SystemParametersServer;
import org.dromara.jpom.service.system.WorkspaceService;
import org.dromara.jpom.service.user.UserBindWorkspaceService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author bwcx_jzy
 * @since 2021/12/3
 */
@RestController
@Feature(cls = ClassFeature.SYSTEM_WORKSPACE)
@RequestMapping(value = "/system/workspace/")
@SystemPermission
public class WorkspaceController extends BaseServerController {

    private final WorkspaceService workspaceService;
    private final UserBindWorkspaceService userBindWorkspaceService;
    private final SystemParametersServer systemParametersServer;
    private final ClusterInfoService clusterInfoService;

    public WorkspaceController(WorkspaceService workspaceService,
                               UserBindWorkspaceService userBindWorkspaceService,
                               SystemParametersServer systemParametersServer,
                               ClusterInfoService clusterInfoService) {
        this.workspaceService = workspaceService;
        this.userBindWorkspaceService = userBindWorkspaceService;
        this.systemParametersServer = systemParametersServer;
        this.clusterInfoService = clusterInfoService;
    }

    /**
     * 编辑工作空间
     *
     * @param name        工作空间名称
     * @param description 描述
     * @return json
     */
    @PostMapping(value = "/edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> create(String id,
                                       @ValidatorItem String name,
                                       @ValidatorItem String description,
                                       String group,
                                       @ValidatorItem(msg = "i18n.select_cluster.f8c3") String clusterInfoId) {
        //
        ClusterInfoModel clusterInfoModel = clusterInfoService.getByKey(clusterInfoId);
        Assert.notNull(clusterInfoModel, I18nMessageUtil.get("i18n.cluster_not_exist.4098"));
        this.checkInfo(id, name);
        //
        WorkspaceModel workspaceModel = new WorkspaceModel();
        workspaceModel.setName(name);
        workspaceModel.setDescription(description);
        workspaceModel.setGroup(group);
        workspaceModel.setClusterInfoId(clusterInfoModel.getId());
        if (StrUtil.isEmpty(id)) {
            // 创建
            workspaceService.insert(workspaceModel);
        } else {
            workspaceModel.setId(id);
            workspaceService.updateById(workspaceModel);
        }
        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }

    private void checkInfo(String id, String name) {
        Entity entity = Entity.create();
        entity.set("name", name);
        if (StrUtil.isNotEmpty(id)) {
            entity.set("id", StrUtil.format(" <> {}", id));
        }
        boolean exists = workspaceService.exists(entity);
        Assert.state(!exists, I18nMessageUtil.get("i18n.workspace_name_already_exists.0f82"));
    }

    /**
     * 工作空间分页列表
     *
     * @return json
     */
    @PostMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<WorkspaceModel>> list(HttpServletRequest request) {
        PageResultDto<WorkspaceModel> listPage = workspaceService.listPage(request);
        return JsonMessage.success("", listPage);
    }

    /**
     * 查询所有的分组
     *
     * @return list
     */
    @GetMapping(value = "list-group-all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<String>> listGroupAll() {
        List<String> listGroup = workspaceService.listGroup();
        return JsonMessage.success("", listGroup);
    }

    /**
     * 查询工作空间列表
     *
     * @return json
     */
    @GetMapping(value = "/list_all")
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<WorkspaceModel>> listAll() {
        List<WorkspaceModel> list = workspaceService.list();
        return JsonMessage.success("", list);
    }

    /**
     * 删除工作空间前检查
     *
     * @param id 工作空间 ID
     * @return json
     */
    @GetMapping(value = "pre-check-delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    @SystemPermission(superUser = true)
    public IJsonMessage<Tree<String>> preCheckDelete(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.data_id_cannot_be_empty.403b") String id) {
        //
        Assert.state(!StrUtil.equals(id, Const.WORKSPACE_DEFAULT_ID), I18nMessageUtil.get("i18n.cannot_delete_default_workspace.0c06"));
        // 判断是否存在关联数据
        Set<Class<?>> classes = BaseWorkspaceModel.allTableClass();

        List<TreeNode<String>> nodes = new ArrayList<>(classes.size());
        for (Class<?> aClass : classes) {
            TableName tableName = aClass.getAnnotation(TableName.class);
            Class<?> parents = tableName.parents();
            //
            String parent = Optional.of(parents)
                .map(aClass1 -> aClass1 != Void.class ? aClass1 : null)
                .map(aClass1 -> {
                    TableName tableName1 = aClass1.getAnnotation(TableName.class);
                    return tableName1.value();
                })
                .orElse(StrUtil.EMPTY);
            //
            String sql = "select  count(1) as cnt from " + tableName.value() + " where workspaceId=?";
            Number number = workspaceService.queryNumber(sql, id);

            TreeNode<String> treeNode = new TreeNode<>(tableName.value(), parent, I18nMessageUtil.get(tableName.nameKey()), 0);
            //
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("workspaceBind", tableName.workspaceBind());
            jsonObject.put("count", ObjectUtil.defaultIfNull(number, Number::intValue, 0));
            treeNode.setExtra(jsonObject);
            nodes.add(treeNode);
        }
        Tree<String> stringTree = TreeUtil.buildSingle(nodes, StrUtil.EMPTY);
        stringTree.setName(StrUtil.EMPTY);
        return new JsonMessage<>(200, "", stringTree);
    }

    /**
     * 删除工作空间
     *
     * @param id 工作空间 ID
     * @return json
     */
    @GetMapping(value = "/delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    @SystemPermission(superUser = true)
    public IJsonMessage<String> delete(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.data_id_cannot_be_empty.403b") String id) {
        //
        Assert.state(!StrUtil.equals(id, Const.WORKSPACE_DEFAULT_ID), I18nMessageUtil.get("i18n.cannot_delete_default_workspace.0c06"));
        // 判断是否存在关联数据
        Set<Class<?>> classes = BaseWorkspaceModel.allTableClass();

        List<Class<?>> autoDeleteClass = new ArrayList<>();
        for (Class<?> aClass : classes) {
            TableName tableName = aClass.getAnnotation(TableName.class);
            int workspaceBind = tableName.workspaceBind();
            if (workspaceBind == 2) {
                // 先忽略不执行自动删除
                autoDeleteClass.add(aClass);
            } else if (workspaceBind == 3) {
                // 父级不存在自动删除
                Class<?> parents = tableName.parents();
                Assert.state(parents != Void.class, I18nMessageUtil.get("i18n.table_info_configuration_error.b050"));
                TableName tableName1 = parents.getAnnotation(TableName.class);
                Assert.notNull(tableName1, I18nMessageUtil.get("i18n.parent_table_info_config_error.2f52") + aClass);
                //
                String sql = "select  count(1) as cnt from " + tableName1.value() + " where workspaceId=?";
                int cnt = ObjectUtil.defaultIfNull(workspaceService.queryNumber(sql, id), Number::intValue, 0);
                Assert.state(cnt <= 0, StrUtil.format(I18nMessageUtil.get("i18n.associated_data_and_exist_in_workspace.5fa7"), I18nMessageUtil.get(tableName.nameKey()), I18nMessageUtil.get(tableName1.nameKey())));
                // 等待自动删除
                autoDeleteClass.add(aClass);
            } else {
                // 其他严格检查的情况
                String sql = "select  count(1) as cnt from " + tableName.value() + " where workspaceId=?";
                int cnt = ObjectUtil.defaultIfNull(workspaceService.queryNumber(sql, id), Number::intValue, 0);
                Assert.state(cnt <= 0, I18nMessageUtil.get("i18n.associated_data_exists_in_workspace.8827") + I18nMessageUtil.get(tableName.nameKey()));
            }
        }
        // 判断用户绑定关系
        boolean workspace = userBindWorkspaceService.existsWorkspace(id);
        Assert.state(!workspace, I18nMessageUtil.get("i18n.user_or_group_bindings_exist_in_workspace.d57b"));
        // 最后执行自动删除
        StringBuilder autoDelete = new StringBuilder(StrUtil.EMPTY);
        for (Class<?> aClass : autoDeleteClass) {
            TableName tableName = aClass.getAnnotation(TableName.class);
            // 自动删除
            String sql = "delete from " + tableName.value() + " where workspaceId=?";
            int execute = workspaceService.execute(sql, id);
            if (execute > 0) {
                autoDelete.append(StrUtil.format(I18nMessageUtil.get("i18n.auto_delete_data.ca62"), tableName.value(), execute));
            }
        }
        // 删除缓存
        String menusConfigKey = StrUtil.format("menus_config_{}", id);
        systemParametersServer.delByKey(menusConfigKey);
        String whitelistConfigKey = StrUtil.format("node_whitelist_{}", id);
        systemParametersServer.delByKey(whitelistConfigKey);
        systemParametersServer.delByKey(StrUtil.format("node_config_{}", id));
        // 删除信息
        workspaceService.delByKey(id);
        return new JsonMessage<>(200, I18nMessageUtil.get("i18n.delete_success_with_colon.d44a") + autoDelete);
    }

    /**
     * 加载菜单配置
     *
     * @return json
     */
    @RequestMapping(value = "get_menus_config", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<JSONObject> getMenusConfig(String workspaceId, HttpServletRequest request) {
        WorkspaceModel workspaceModel = workspaceService.getByKey(workspaceId);
        Assert.notNull(workspaceModel, I18nMessageUtil.get("i18n.workspace_not_exist.a6fd"));
        JSONObject config = systemParametersServer.getConfigDefNewInstance(StrUtil.format("menus_config_{}", workspaceId), JSONObject.class);
        //"classpath:/menus/index.json"
        //"classpath:/menus/node-index.json"
        String language = I18nMessageUtil.tryGetNormalLanguage();
        config.put("serverMenus", this.readMenusJson("classpath:/menus/" + language + "/index.json"));
        return JsonMessage.success("", config);
    }

    @PostMapping(value = "save_menus_config.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<Object> saveMenusConfig(String serverMenuKeys, String nodeMenuKeys, String workspaceId) {
        WorkspaceModel workspaceModel = workspaceService.getByKey(workspaceId);
        Assert.notNull(workspaceModel, I18nMessageUtil.get("i18n.workspace_not_exist.a6fd"));
        //
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("nodeMenuKeys", StrUtil.splitTrim(nodeMenuKeys, StrUtil.COMMA));
        jsonObject.put("serverMenuKeys", StrUtil.splitTrim(serverMenuKeys, StrUtil.COMMA));
        String format = StrUtil.format("menus_config_{}", workspaceId);
        systemParametersServer.upsert(format, jsonObject, format);
        //
        return JsonMessage.success(I18nMessageUtil.get("i18n.modify_success.69be"));
    }

    private JSONArray readMenusJson(String path) {
        // 菜单
        InputStream inputStream = ResourceUtil.getStream(path);
        String json = IoUtil.read(inputStream, CharsetUtil.CHARSET_UTF_8);
        return JSONArray.parseArray(json);
    }
}
