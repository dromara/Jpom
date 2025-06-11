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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.func.assets.controller.BaseFtpFileController;
import org.dromara.jpom.func.assets.model.MachineFtpModel;
import org.dromara.jpom.model.BaseGroupModel;
import org.dromara.jpom.util.StringUtil;


/**
 * ftp信息
 *
 * @author wxyShine
 * @since 2025/05/29
 */
@TableName(value = "FTP_INFO",
    nameKey = "i18n.ftp_info_table.b177")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class FtpModel extends BaseGroupModel implements BaseFtpFileController.ItemConfig {

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
     * 文件目录
     */
    private String fileDirs;

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
     * ftp id
     */
    private String machineFtpId;

    @PropIgnore
    private MachineFtpModel machineFtp;


    @PropIgnore
    private WorkspaceModel workspace;

    public FtpModel(String id) {
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
}
