package cn.keepbx.jpom.model.data;

import cn.hutool.core.io.FileUtil;
import cn.keepbx.jpom.model.BaseModel;

import java.io.File;

/**
 * tomcat 对象实体
 *
 * @author lf
 */
public class TomcatInfoModel extends BaseModel {

    private String path;
    private int port;
    private int status;
    private String appBase;
    private String creator;
    private String createTime;
    private String modifyUser;
    private String modifyTime;

    public String getPath() {
        return path;
    }

    public String pathAndCheck() {
        String path = getPath();
        if (path == null) {
            return null;
        }
        path = FileUtil.normalize(path + "/");
        if (isTomcatRoot(path)) {
            return path;
        }
        throw new RuntimeException("tomcat path error:" + getPath());
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getAppBase() {
        return appBase;
    }

    public void setAppBase(String appBase) {
        this.appBase = appBase;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getModifyUser() {
        return modifyUser;
    }

    public void setModifyUser(String modifyUser) {
        this.modifyUser = modifyUser;
    }

    public String getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(String modifyTime) {
        this.modifyTime = modifyTime;
    }


    /**
     * 判断是否是Tomcat的根路径
     *
     * @return 返回是否是Tomcat根路径
     */
    public static boolean isTomcatRoot(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return false;
        }
        if (file.isFile()) {
            return false;
        }
        File[] files = file.listFiles();
        if (files == null) {
            return false;
        }
        // 判断该目录下是否
        for (File child : files) {
            if ("bin".equals(child.getName()) && child.isDirectory()) {
                File[] binFiles = child.listFiles();
                if (binFiles == null) {
                    return false;
                }
                for (File binChild : binFiles) {
                    if ("bootstrap.jar".equals(binChild.getName()) && binChild.isFile()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
