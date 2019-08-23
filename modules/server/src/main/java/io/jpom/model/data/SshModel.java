package io.jpom.model.data;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import io.jpom.model.BaseModel;

import java.nio.charset.Charset;
import java.util.List;

/**
 * ssh 信息
 *
 * @author bwcx_jzy
 * @date 2019/8/9
 */
public class SshModel extends BaseModel {
    private String host;
    private int port;
    private String user;
    private String password;
    /**
     * 编码格式
     */
    private String charset;

    /**
     * 文件目录
     */
    private List<String> fileDirs;

    /**
     * 临时缓存model
     */
    private BaseModel nodeModel;

    public BaseModel getNodeModel() {
        return nodeModel;
    }

    public void setNodeModel(BaseModel nodeModel) {
        if (nodeModel == null) {
            return;
        }
        this.nodeModel = new BaseModel() {
            @Override
            public String getName() {
                return nodeModel.getName();
            }

            @Override
            public String getId() {
                return nodeModel.getId();
            }
        };
    }

    public List<String> getFileDirs() {
        return fileDirs;
    }

    public void setFileDirs(List<String> fileDirs) {
        if (fileDirs != null) {
            for (int i = fileDirs.size() - 1; i >= 0; i--) {
                String s = fileDirs.get(i);
                fileDirs.set(i, FileUtil.normalize(s));
            }
        }
        this.fileDirs = fileDirs;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public Charset getCharsetT() {
        Charset charset;
        try {
            charset = Charset.forName(this.getCharset());
        } catch (Exception e) {
            charset = CharsetUtil.CHARSET_UTF_8;
        }
        return charset;
    }
}
