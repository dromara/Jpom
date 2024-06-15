package org.dromara.jpom.func.assets.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.db.Entity;
import cn.hutool.extra.servlet.ServletUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.func.assets.model.ScriptLibraryModel;
import org.dromara.jpom.func.assets.server.ScriptLibraryServer;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;

/**
 * @author bwcx_jzy1
 * @since 2024/6/1
 */
@RestController
@RequestMapping(value = "/system/assets/script-library")
@Feature(cls = ClassFeature.SYSTEM_ASSETS_GLOBAL_SCRIPT)
@SystemPermission
@Slf4j
public class ScriptLibraryController extends BaseServerController {

    private final ScriptLibraryServer scriptLibraryServer;

    public ScriptLibraryController(ScriptLibraryServer scriptLibraryServer) {
        this.scriptLibraryServer = scriptLibraryServer;
    }

    @PostMapping(value = "list-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<ScriptLibraryModel>> listJson(HttpServletRequest request) {
        PageResultDto<ScriptLibraryModel> pageResultDto = scriptLibraryServer.listPage(request);
        return JsonMessage.success("", pageResultDto);
    }

    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> save(HttpServletRequest request) {
        ScriptLibraryModel scriptLibraryModel = ServletUtil.toBean(request, ScriptLibraryModel.class, true);
        String tag = scriptLibraryModel.getTag();
        Assert.hasText(tag, I18nMessageUtil.get("i18n.mark_cannot_be_empty.1927"));
        Validator.validateGeneral(tag, 4, 20, I18nMessageUtil.get("i18n.mark_must_contain_letters_numbers_underscores.667d"));
        Assert.hasText(scriptLibraryModel.getScript(), I18nMessageUtil.get("i18n.script_cannot_be_empty.f566"));
        //
        Entity entity = Entity.create();
        entity.set("tag", tag);
        String id = scriptLibraryModel.getId();
        if (StrUtil.isNotEmpty(id)) {
            entity.set("id", StrUtil.format(" <> {}", id));
        }
        Assert.state(!scriptLibraryServer.exists(entity), I18nMessageUtil.get("i18n.mark_already_exists.0ccc"));
        String oldIds = StrUtil.EMPTY;
        String version = StrUtil.sub(SecureUtil.md5(scriptLibraryModel.getScript()), 0, 6);
        if (StrUtil.isNotEmpty(id)) {
            ScriptLibraryModel libraryModel = scriptLibraryServer.getByKey(id);
            Assert.notNull(libraryModel, I18nMessageUtil.get("i18n.data_does_not_exist.b201"));
            Assert.state(StrUtil.equals(libraryModel.getTag(), tag), I18nMessageUtil.get("i18n.script_tag_modification_not_allowed.cb75"));
            oldIds = libraryModel.getMachineIds();
            if (StrUtil.equals(libraryModel.getScript(), scriptLibraryModel.getScript())) {
                // 内容没有变化不
                scriptLibraryModel.setVersion(null);
            } else {
                // 自动生成版本号
                String libraryModelVersion = libraryModel.getVersion();
                List<String> list = StrUtil.splitTrim(libraryModelVersion, "#");
                int nextIncVersion = Convert.toInt(list.get(0), -2) + 1;
                scriptLibraryModel.setVersion(StrUtil.format("{}#{}", nextIncVersion, version));
            }
            scriptLibraryServer.updateById(scriptLibraryModel);
            if (scriptLibraryModel.getVersion() == null) {
                scriptLibraryModel.setVersion(libraryModel.getVersion());
            }
        } else {
            scriptLibraryModel.setVersion(StrUtil.format("1#{}", version));
            scriptLibraryServer.insert(scriptLibraryModel);
        }
        // 同步到机器节点
        this.syncMachineNodeScript(scriptLibraryModel, oldIds, request);
        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }

    private void syncMachineNodeScript(ScriptLibraryModel scriptModel, String oldMachineIds, HttpServletRequest request) {
        List<String> oldNodeIds = StrUtil.splitTrim(oldMachineIds, StrUtil.COMMA);
        List<String> newNodeIds = StrUtil.splitTrim(scriptModel.getMachineIds(), StrUtil.COMMA);
        Collection<String> delNode = CollUtil.subtract(oldNodeIds, newNodeIds);
        // 删除
        this.syncDelMachineNodeScriptLibrary(scriptModel.getTag(), delNode);
        // 更新
        for (String machineId : newNodeIds) {
            MachineNodeModel byKey = machineNodeServer.getByKey(machineId);
            Assert.notNull(byKey, I18nMessageUtil.get("i18n.no_node_found.6f85"));
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", scriptModel.getTag());
            jsonObject.put("description", scriptModel.getDescription());
            jsonObject.put("tag", scriptModel.getTag());
            jsonObject.put("script", scriptModel.getScript());
            jsonObject.put("version", scriptModel.getVersion());
            JsonMessage<String> jsonMessage = NodeForward.request(byKey, NodeUrl.SCRIPT_LIBRARY_SAVE, jsonObject);
            String message = StrUtil.format(I18nMessageUtil.get("i18n.handle_node_synchronization_script_library_failure.14e4"), byKey.getName(), jsonMessage.getMsg());
            Assert.state(jsonMessage.success(), message);
        }
    }

    private void syncDelMachineNodeScriptLibrary(String tag, Collection<String> delNode) {
        for (String machineId : delNode) {
            MachineNodeModel byKey = machineNodeServer.getByKey(machineId);
            if (byKey == null) {
                // 机器可能被删除了
                // 避免无法删除脚本库的清空
                continue;
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", tag);
            JsonMessage<String> request = NodeForward.request(byKey, NodeUrl.SCRIPT_LIBRARY_DEL, jsonObject);
            String message = StrUtil.format(I18nMessageUtil.get("i18n.handle_node_deletion_script_library_failure.4205"), byKey.getName(), request.getMsg());
            Assert.state(request.getCode() == 200, message);
        }
    }

    @RequestMapping(value = "del", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<String> del(String id, HttpServletRequest request) {
        ScriptLibraryModel server = scriptLibraryServer.getByKey(id);
        if (server != null) {
            // 删除节点中的脚本
            String nodeIds = server.getMachineIds();
            List<String> delNode = StrUtil.splitTrim(nodeIds, StrUtil.COMMA);
            this.syncDelMachineNodeScriptLibrary(server.getTag(), delNode);
            scriptLibraryServer.delByKey(id);
        }
        return JsonMessage.success(I18nMessageUtil.get("i18n.delete_success.0007"));
    }
}
