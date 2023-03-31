/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.JsonMessage;
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
    public JsonMessage<Tree<String>> list() throws Exception {
        Map<String, TreeNode<String>> parentMap = new LinkedHashMap<>(10);
        // root 节点
        parentMap.put(StrUtil.SLASH, new TreeNode<>(StrUtil.SLASH, null, "根路径", 0));
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
                node.setName(StrUtil.format("{} [默认]", node.getName()));
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

        return JsonMessage.success("ok", stringTree);
    }

    @GetMapping(value = "get-item", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<Object> getItem(@ValidatorItem String name) {
        InputStream resourceInputStream = ExtConfigBean.getConfigResourceInputStream(name);
        String s = IoUtil.readUtf8(resourceInputStream);
        return JsonMessage.success("", s);
    }

    @PostMapping(value = "save-item", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<Object> saveItem(@ValidatorItem String name, @ValidatorItem String content) {
        File configResourceFile = ExtConfigBean.getConfigResourceFile(name);
        Assert.notNull(configResourceFile, "不能编辑对应的配置文件");
        FileUtil.writeUtf8String(content, configResourceFile);
        return JsonMessage.success("修改成功");
    }

    @GetMapping(value = "get-default-item", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<Object> getDefaultItem(@ValidatorItem String name) {
        InputStream resourceInputStream = ExtConfigBean.getDefaultConfigResourceInputStream(name);
        String s = IoUtil.readUtf8(resourceInputStream);
        return JsonMessage.success("", s);
    }

    @GetMapping(value = "add-item", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<Object> addItem(@ValidatorItem String name) {
        boolean existConfigResource = ExtConfigBean.existConfigResource(name);
        Assert.state(!existConfigResource, "对应的配置文件已经存在啦");
        File resourceFile = ExtConfigBean.getConfigResourceFile(name);
        Assert.notNull(resourceFile, "当前环境不能创建配置文件");
        FileUtil.touch(resourceFile);
        return JsonMessage.success("创建成功");
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
