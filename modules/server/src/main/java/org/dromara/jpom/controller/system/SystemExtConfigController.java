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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNode;
import cn.hutool.core.lang.tree.TreeUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.system.ExtConfigBean;
import org.dromara.jpom.util.StringUtil;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 配置文件管理
 *
 * @author bwcx_jzy
 * @since 2023/1/04
 */
@RestController
@RequestMapping(value = "system/ext-conf")
@Feature(cls = ClassFeature.SYSTEM_EXT_CONFIG)
@SystemPermission
@Slf4j
public class SystemExtConfigController extends BaseServerController {

    /**
     * 获取外部配置文件的 数据
     *
     * @param parentMap 父级 map
     * @return map
     */
    private Map<String, TreeNode<String>> listDir(Map<String, TreeNode<String>> parentMap) {
        File configResourceDir = ExtConfigBean.getConfigResourceDir();
        if (configResourceDir == null) {
            return MapUtil.newHashMap();
        }
        List<File> files = FileUtil.loopFiles(configResourceDir);
        return files.stream()
            .filter(FileUtil::isFile)
            .map(file -> {
                String path = StringUtil.delStartPath(file, configResourceDir.getAbsolutePath(), true);
                return this.buildItemTreeNode(path);
            })
            .peek(node -> {
                String id = node.getId();
                this.buildParent(parentMap, id);
                //
                Map<String, Object> extra = node.getExtra();
                extra.put("defaultConfig", false);
            }).collect(Collectors.toMap(TreeNode::getId, node -> node));
    }

    /**
     * 插件单个 node 对象
     *
     * @param path 路径
     * @return tree node
     */
    private TreeNode<String> buildItemTreeNode(String path) {

        List<String> list = StrUtil.splitTrim(path, StrUtil.SLASH);
        int size = list.size();
        String parentId = size > 1 ? CollUtil.join(CollUtil.sub(list, 0, size - 1), StrUtil.SLASH) : StrUtil.SLASH;

        return new TreeNode<>(path, parentId, CollUtil.getLast(list), 0).setExtra(MapUtil.of("isLeaf", true));
    }

    @GetMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<Tree<String>> list() throws Exception {
        Map<String, TreeNode<String>> parentMap = new LinkedHashMap<>(10);
        // root 节点
        parentMap.put(StrUtil.SLASH, new TreeNode<>(StrUtil.SLASH, null, I18nMessageUtil.get("i18n.root_path.1396"), 0));
        Map<String, TreeNode<String>> listDir = this.listDir(parentMap);
        //
        PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = pathMatchingResourcePatternResolver.getResources("classpath*:/config_default/**");
        List<TreeNode<String>> classPathList = Arrays.stream(resources)
            .filter(resource -> {
                if (resource.isFile()) {
                    // 本地运行可能出现文件夹的 item
                    try {
                        if (resource.getFile().isDirectory()) {
                            return false;
                        }
                    } catch (IOException e) {
                        throw Lombok.sneakyThrow(e);
                    }
                }
                return true;
            })
            .map(resource -> {
                try {
                    String path = resource.getURL().getPath();
                    if (StrUtil.endWith(path, StrUtil.SLASH)) {
                        // 目录
                        return null;
                    }
                    String itemPath = StrUtil.subAfter(path, "/config_default/", false);
                    //log.debug("测试：{} {}", path, itemPath);
                    return this.buildItemTreeNode(itemPath);
                } catch (IOException e) {
                    throw Lombok.sneakyThrow(e);
                }
            })
            .filter(Objects::nonNull)
            .peek(node -> {
                String id = node.getId();
                this.buildParent(parentMap, id);
                //
                node.setName(StrUtil.format(I18nMessageUtil.get("i18n.default_value.1aa9"), node.getName()));
                Map<String, Object> extra = node.getExtra();
                extra.put("defaultConfig", true);
                extra.put("hasDefault", true);
            })
            // 过滤 dir 已经存在的
            .filter(node -> {
                TreeNode<String> treeNode = listDir.get(node.getId());
                if (treeNode != null) {
                    Map<String, Object> extra = treeNode.getExtra();
                    extra.put("hasDefault", true);
                    return false;
                }
                return true;
            })
            .collect(Collectors.toList());

        List<TreeNode<String>> allList = new ArrayList<>();
        allList.addAll(parentMap.values());
        allList.addAll(classPathList);
        allList.addAll(listDir.values());
        // 过滤主配置文件
        allList = allList.stream().peek(node -> {
            Map<String, Object> extra = node.getExtra();
            if (extra == null) {
                return;
            }
            extra.put("disabled", StrUtil.equals(node.getId(), Const.FILE_NAME));
        }).collect(Collectors.toList());
        Tree<String> stringTree = TreeUtil.buildSingle(allList, StrUtil.SLASH);
        stringTree.setName(StrUtil.SLASH);

        return JsonMessage.success("", stringTree);
    }

    @GetMapping(value = "get-item", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<Object> getItem(@ValidatorItem String name) {
        InputStream resourceInputStream = ExtConfigBean.getConfigResourceInputStream(name);
        String s = IoUtil.readUtf8(resourceInputStream);
        return JsonMessage.success("", s);
    }

    @PostMapping(value = "save-item", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<Object> saveItem(@ValidatorItem String name, @ValidatorItem String content) {
        File configResourceFile = ExtConfigBean.getConfigResourceFile(name);
        Assert.notNull(configResourceFile, I18nMessageUtil.get("i18n.cannot_edit_corresponding_config_file.8d10"));
        FileUtil.writeUtf8String(content, configResourceFile);
        return JsonMessage.success(I18nMessageUtil.get("i18n.modify_success.69be"));
    }

    @GetMapping(value = "get-default-item", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<Object> getDefaultItem(@ValidatorItem String name) {
        InputStream resourceInputStream = ExtConfigBean.getDefaultConfigResourceInputStream(name);
        String s = IoUtil.readUtf8(resourceInputStream);
        return JsonMessage.success("", s);
    }

    @GetMapping(value = "add-item", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<Object> addItem(@ValidatorItem String name) {
        boolean existConfigResource = ExtConfigBean.existConfigResource(name);
        Assert.state(!existConfigResource, I18nMessageUtil.get("i18n.config_file_already_exists.c5fe"));
        File resourceFile = ExtConfigBean.getConfigResourceFile(name);
        Assert.notNull(resourceFile, I18nMessageUtil.get("i18n.cannot_create_config_file_in_environment.55bb"));
        FileUtil.touch(resourceFile);
        return JsonMessage.success(I18nMessageUtil.get("i18n.create_success.04a6"));
    }

    private void buildParent(Map<String, TreeNode<String>> parentMap, String path) {
        List<String> list = StrUtil.splitTrim(path, StrUtil.SLASH);
        for (int i = 0; i < list.size() - 1; i++) {
            String name = list.get(i);
            String pathId = CollUtil.join(CollUtil.sub(list, 0, i + 1), StrUtil.SLASH);
            String parentId = i > 0 ? CollUtil.join(CollUtil.sub(list, 0, i), StrUtil.SLASH) : StrUtil.SLASH;
            parentMap.computeIfAbsent(pathId, s -> new TreeNode<>(s, parentId, name, 0));
        }
    }
}
