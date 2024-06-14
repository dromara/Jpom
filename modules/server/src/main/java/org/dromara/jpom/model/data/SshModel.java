/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.data;

import cn.hutool.core.annotation.PropIgnore;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.func.assets.controller.BaseSshFileController;
import org.dromara.jpom.func.assets.model.MachineSshModel;
import org.dromara.jpom.model.BaseGroupModel;
import org.dromara.jpom.util.StringUtil;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ssh 信息
 *
 * @author bwcx_jzy
 * @since 2019/8/9
 */
@TableName(value = "SSH_INFO",
    nameKey = "i18n.ssh_info.ebe6")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class SshModel extends BaseGroupModel implements BaseSshFileController.ItemConfig {

    private String name;
    @Deprecated
    private String host;
    @Deprecated
    private Integer port;
    @Deprecated
    private String user;
    @Deprecated
    private String password;
    /**
     * 编码格式
     */
    @Deprecated
    private String charset;

    /**
     * ssh 私钥
     */
    @Deprecated
    private String privateKey;
    @Deprecated
    private String connectType;
    /**
     * 文件目录
     */
    private String fileDirs;
    /**
     * 不允许执行的命令
     */
    private String notAllowedCommand;
    /**
     * 允许编辑的后缀文件
     */
    private String allowEditSuffix;
    /**
     * 节点超时时间
     */
    @Deprecated
    private Integer timeout;

    /**
     * ssh id
     */
    private String machineSshId;

    @PropIgnore
    private MachineSshModel machineSsh;

    @PropIgnore
    private NodeModel linkNode;

    @PropIgnore
    private WorkspaceModel workspace;

    public SshModel(String id) {
        this.setId(id);
    }


    @Override
    public List<String> fileDirs() {
        List<String> strings = StringUtil.jsonConvertArray(this.fileDirs, String.class);
        return Optional.ofNullable(strings)
                .map(strings1 -> strings1.stream()
                        .map(s -> FileUtil.normalize(StrUtil.SLASH + s + StrUtil.SLASH))
                        .collect(Collectors.toList()))
                .orElse(null);
    }

    public void fileDirs(List<String> fileDirs) {
        if (fileDirs != null) {
            for (int i = fileDirs.size() - 1; i >= 0; i--) {
                String s = fileDirs.get(i);
                fileDirs.set(i, FileUtil.normalize(s));
            }
            this.fileDirs = JSONArray.toJSONString(fileDirs);
        } else {
            this.fileDirs = StrUtil.EMPTY;
        }
    }


    @Override
    public List<String> allowEditSuffix() {
        return StringUtil.jsonConvertArray(this.allowEditSuffix, String.class);
    }

    public void allowEditSuffix(List<String> allowEditSuffix) {
        if (allowEditSuffix == null) {
            this.allowEditSuffix = null;
        } else {
            this.allowEditSuffix = JSONArray.toJSONString(allowEditSuffix);
        }
    }

    /**
     * 检查是否包含禁止命令
     *
     * @param sshItem   实体
     * @param inputItem 输入的命令
     * @return false 存在禁止输入的命令
     */
    public static boolean checkInputItem(SshModel sshItem, String inputItem) {
        // 检查禁止执行的命令
        String notAllowedCommand = StrUtil.emptyToDefault(sshItem.getNotAllowedCommand(), StrUtil.EMPTY).toLowerCase();
        if (StrUtil.isEmpty(notAllowedCommand)) {
            return true;
        }
        List<String> split = StrUtil.splitTrim(notAllowedCommand, StrUtil.COMMA);
        inputItem = inputItem.toLowerCase();
        List<String> commands = StrUtil.splitTrim(inputItem, StrUtil.CR);
        commands.addAll(StrUtil.split(inputItem, "&"));
        for (String s : split) {
            //
            boolean anyMatch = commands.stream().anyMatch(item -> StrUtil.startWithAny(item, s + StrUtil.SPACE, ("&" + s + StrUtil.SPACE), StrUtil.SPACE + s + StrUtil.SPACE));
            if (anyMatch) {
                return false;
            }
            //
            anyMatch = commands.stream().anyMatch(item -> StrUtil.equals(item, s));
            if (anyMatch) {
                return false;
            }
        }
        return true;
    }
}
