package org.dromara.jpom.func.assets.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.extra.servlet.ServletUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
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
@Feature(cls = ClassFeature.SYSTEM_ASSETS_MACHINE_DOCKER)
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
        Assert.hasText(scriptLibraryModel.getTag(), "标签不能为空");
        Assert.hasText(scriptLibraryModel.getScript(), "脚本不能为空");
        //
        Entity entity = Entity.create();
        entity.set("tag", scriptLibraryModel.getTag());
        String id = scriptLibraryModel.getId();
        if (StrUtil.isNotEmpty(id)) {
            entity.set("id", StrUtil.format(" <> {}", id));
        }
        Assert.state(!scriptLibraryServer.exists(entity), "标签已存在");
        String oldIds = StrUtil.EMPTY;
        if (StrUtil.isNotEmpty(id)) {
            scriptLibraryServer.updateById(scriptLibraryModel);
        } else {
            ScriptLibraryModel libraryModel = scriptLibraryServer.getByKey(id);
            Assert.notNull(libraryModel, "数据不存在");
            Assert.state(StrUtil.equals(libraryModel.getTag(), scriptLibraryModel.getTag()), "脚本标签不能修改");
            oldIds = libraryModel.getMachineIds();
            scriptLibraryServer.insert(scriptLibraryModel);
        }
        // 同步到机器节点
        this.syncMachineNodeScript(scriptLibraryModel, oldIds, request);
        return JsonMessage.success("操作成功");
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
            Assert.notNull(byKey, "没有找到对应的节点");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", scriptModel.getId());
            jsonObject.put("description", scriptModel.getDescription());
            jsonObject.put("tag", scriptModel.getTag());
            jsonObject.put("script", scriptModel.getScript());
            jsonObject.put("version", scriptModel.getVersion());
            JsonMessage<String> jsonMessage = NodeForward.request(byKey, NodeUrl.SCRIPT_LIBRARY_SAVE, jsonObject);
            Assert.state(jsonMessage.success(), "处理 " + byKey.getName() + " 节点同步脚本库失败" + jsonMessage.getMsg());
        }
    }

    private void syncDelMachineNodeScriptLibrary(String id, Collection<String> delNode) {
        for (String machineId : delNode) {
            MachineNodeModel byKey = machineNodeServer.getByKey(machineId);
            if (byKey == null) {
                // 机器可能被删除了
                // 避免无法删除脚本库的清空
                continue;
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", id);
            JsonMessage<String> request = NodeForward.request(byKey, NodeUrl.SCRIPT_LIBRARY_DEL, jsonObject);
            Assert.state(request.getCode() == 200, "处理 " + byKey.getName() + " 节点删除脚本库失败" + request.getMsg());
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
        return JsonMessage.success("删除成功");
    }
}
