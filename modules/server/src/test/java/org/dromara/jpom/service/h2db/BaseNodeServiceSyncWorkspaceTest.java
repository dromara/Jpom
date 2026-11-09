/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.h2db;

import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.model.BaseNodeModel;
import org.dromara.jpom.model.node.NodeScriptCacheModel;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link BaseNodeService#resolveSyncWorkspaceItems(List, String, java.util.function.Predicate)} 的行为级回归（Gitee ICH1VK）。
 * <p>
 * 背景：重装 Jpom 服务端后，插件端项目/脚本仍带着旧的 {@code workspaceId}（服务端已不存在该工作空间）。
 * {@code syncExecuteNode} 原来的两个过滤器会把这些数据静默丢弃，用户看到“拉取到 N 个、更新 0 个”。
 * 这里直接断言 {@code syncExecuteNode} 用来决定最终落库列表的包级方法的输出，覆盖三种场景：
 * <ol>
 *     <li>工作空间不存在 → 回退到节点当前工作空间并重新计算主键；</li>
 *     <li>工作空间存在但不同（共享机器多空间）→ 仍然过滤，不串工作空间；</li>
 *     <li>GLOBAL 数据 → 不受节点工作空间限制，保持原逻辑。</li>
 * </ol>
 * 只测“回退/过滤/GLOBAL”的最终列表，不依赖 Spring/数据库。
 *
 * @author jpom
 */
public class BaseNodeServiceSyncWorkspaceTest {

    private static final String NODE_ID = "node-1";
    private static final String NODE_WORKSPACE = "ws-node";

    private static NodeScriptCacheModel model(String workspaceId, String scriptId) {
        NodeScriptCacheModel item = new NodeScriptCacheModel();
        item.setWorkspaceId(workspaceId);
        item.setNodeId(NODE_ID);
        item.dataId(scriptId);
        // 模拟 syncExecuteNode 中 fullData 已按旧工作空间生成的旧主键
        item.setId(item.fullId());
        return item;
    }

    private static Set<String> existingWorkspaces(String... workspaceIds) {
        return new HashSet<>(Arrays.asList(workspaceIds));
    }

    private static List<NodeScriptCacheModel> resolve(List<NodeScriptCacheModel> items, Set<String> exists) {
        return BaseNodeService.resolveSyncWorkspaceItems(items, NODE_WORKSPACE, exists::contains);
    }

    private static Set<String> workspaceIds(List<NodeScriptCacheModel> models) {
        return models.stream()
            .map(NodeScriptCacheModel::getWorkspaceId)
            .collect(Collectors.toSet());
    }

    /**
     * 场景一：旧工作空间已不存在（重装服务端）→ 回退到节点当前工作空间，并重新计算主键，不能再被“更新 0 个”。
     */
    @Test
    public void testMissingWorkspaceFallsBackToNodeWorkspace() {
        NodeScriptCacheModel stale = model("removed-ws", "script-1");
        String staleId = stale.getId();
        NodeScriptCacheModel normal = model(NODE_WORKSPACE, "script-2");
        NodeScriptCacheModel global = model(ServerConst.WORKSPACE_GLOBAL, "script-3");

        List<NodeScriptCacheModel> result = resolve(Arrays.asList(stale, normal, global), existingWorkspaces(NODE_WORKSPACE));

        assertEquals(3, result.size(), "空间不存在的数据应回退后参与同步，而不是被丢弃：" + result);
        assertEquals(NODE_WORKSPACE, stale.getWorkspaceId(), "旧工作空间应回退到节点当前工作空间");
        assertEquals(BaseNodeModel.fullId(NODE_WORKSPACE, NODE_ID, "script-1"), stale.getId(),
            "回退后必须按新的工作空间重新计算 fullId，否则写入后仍查不到");
        assertNotEquals(staleId, stale.getId(), "旧工作空间对应的主键不应继续使用");
        assertEquals(NODE_WORKSPACE, normal.getWorkspaceId());
        assertEquals(ServerConst.WORKSPACE_GLOBAL, global.getWorkspaceId());
        assertEquals(existingWorkspaces(NODE_WORKSPACE, ServerConst.WORKSPACE_GLOBAL), workspaceIds(result));
    }

    /**
     * 场景二：工作空间仍然存在但和节点不同（共享机器多工作空间）→ 仍按原逻辑过滤，不能把别的空间数据同步进来。
     */
    @Test
    public void testExistingDifferentWorkspaceStillFiltered() {
        NodeScriptCacheModel foreign = model("ws-other", "script-1");
        String foreignId = foreign.getId();
        NodeScriptCacheModel same = model(NODE_WORKSPACE, "script-2");

        List<NodeScriptCacheModel> result = resolve(Arrays.asList(foreign, same), existingWorkspaces(NODE_WORKSPACE, "ws-other"));

        assertEquals(1, result.size(), "存在但不同的工作空间数据仍应被过滤：" + result);
        assertSame(same, result.get(0));
        assertEquals("ws-other", foreign.getWorkspaceId(), "可过滤数据的 workspaceId 不应被改写");
        assertEquals(foreignId, foreign.getId(), "可过滤数据的主键不应被改写");
    }

    /**
     * 场景三：GLOBAL 数据不受节点工作空间限制，即使 workspaceExists 对其返回 false 也保持原逻辑。
     */
    @Test
    public void testGlobalWorkspaceUnaffected() {
        NodeScriptCacheModel global = model(ServerConst.WORKSPACE_GLOBAL, "script-1");
        String idBefore = global.getId();

        List<NodeScriptCacheModel> result = resolve(Collections.singletonList(global), existingWorkspaces(NODE_WORKSPACE));

        assertEquals(1, result.size(), "GLOBAL 数据必须保持参与同步：" + result);
        assertEquals(ServerConst.WORKSPACE_GLOBAL, global.getWorkspaceId());
        assertEquals(idBefore, global.getId(), "GLOBAL 数据的主键不应被改写");
    }

    /**
     * 空列表/节点工作空间为空时不能抛异常（旧实现为静默丢弃）。
     */
    @Test
    public void testEmptyItemsAndEmptyNodeWorkspace() {
        assertTrue(BaseNodeService.<NodeScriptCacheModel>resolveSyncWorkspaceItems(Collections.emptyList(), NODE_WORKSPACE, id -> true).isEmpty());

        NodeScriptCacheModel stale = model("removed-ws", "script-1");
        List<NodeScriptCacheModel> result = BaseNodeService.resolveSyncWorkspaceItems(
            Collections.singletonList(stale), null, id -> false);
        assertTrue(result.isEmpty(), "节点工作空间为空时保持丢弃（与原逻辑一致）：" + result);
    }
}
