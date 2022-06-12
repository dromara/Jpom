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
package io.jpom.model.data;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import io.jpom.model.BaseWorkspaceModel;
import io.jpom.service.h2db.TableName;
import io.jpom.util.StringUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * ssh 信息
 *
 * @author bwcx_jzy
 * @since 2019/8/9
 */
@TableName(value = "SSH_INFO", name = "SSH 信息")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class SshModel extends BaseWorkspaceModel {

    private String name;
    private String host;
    private Integer port;
    private String user;
    private String password;
    /**
     * 编码格式
     */
    private String charset;

    /**
     * 文件目录
     */
    private String fileDirs;

    /**
     * ssh 私钥
     */
    private String privateKey;

    private String connectType;

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
    private Integer timeout;

    public SshModel(String id) {
        this.setId(id);
    }

    public SshModel(String id, String workspaceId) {
        this.setId(id);
        this.setWorkspaceId(workspaceId);
    }

    public ConnectType connectType() {
        return EnumUtil.fromString(ConnectType.class, this.connectType, ConnectType.PASS);
    }

    public List<String> fileDirs() {
        return StringUtil.jsonConvertArray(this.fileDirs, String.class);
    }

    public void fileDirs(List<String> fileDirs) {
        if (fileDirs != null) {
            for (int i = fileDirs.size() - 1; i >= 0; i--) {
                String s = fileDirs.get(i);
                fileDirs.set(i, FileUtil.normalize(s));
            }
            this.fileDirs = JSONArray.toJSONString(fileDirs);
        } else {
            this.fileDirs = null;
        }
    }

    public byte[] password() {
        byte[] pas = null;
        if (StrUtil.isNotEmpty(this.getPassword())) {
            pas = this.getPassword().getBytes();
        }
        return pas;
    }

    /**
     * 超时时间
     *
     * @return 最小值 1 分钟
     */
    public int timeout() {
        if (this.timeout == null) {
            return (int) TimeUnit.SECONDS.toMillis(5);
        }
        return (int) TimeUnit.SECONDS.toMillis(Math.max(1, this.timeout));
    }

    public Charset charset() {
        return CharsetUtil.parse(this.getCharset(), CharsetUtil.CHARSET_UTF_8);
    }

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

    public enum ConnectType {
        /**
         * 账号密码
         */
        PASS,
        /**
         * 密钥
         */
        PUBKEY
    }
}
