package io.jpom.model.data;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import io.jpom.model.BaseModel;
import io.jpom.system.JpomRuntimeException;

import java.io.File;
import java.io.InputStream;

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
        if (path == null) {
            return null;
        }
        return FileUtil.normalize(path + "/");
    }

    /**
     * 检测路径是否正确
     *
     * @return path
     */
    public String pathAndCheck() {
        String path = getPath();
        if (path == null) {
            return null;
        }
        if (isTomcatRoot(path)) {
            return path;
        }
        throw new RuntimeException(String.format("没有在路径：%s 下检测到Tomcat", path));
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
        if (StrUtil.isEmpty(appBase)) {
            return FileUtil.normalize(path + "/webapps/");
        }
        return FileUtil.normalize(appBase + "/");
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
    private static boolean isTomcatRoot(String path) {
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

    /**
     * 初始化
     */
    public void initTomcat() {
        String tomcatPath = pathAndCheck();
        String appBase = getAppBase();
        if (StrUtil.isEmpty(appBase) || StrUtil.SLASH.equals(appBase)) {
            File webapps = FileUtil.file(tomcatPath, "webapps");
            setAppBase(webapps.getAbsolutePath());
        } else {
            String path = FileUtil.normalize(appBase);
            if (FileUtil.isAbsolutePath(path)) {
                // appBase如：/project/、D:/project/
                setAppBase(path);
            } else {
                // appBase填写的是对相路径如：project/dir
                File webapps = FileUtil.file(tomcatPath, path);
                setAppBase(webapps.getAbsolutePath());
            }
        }
        InputStream inputStream = ResourceUtil.getStream("classpath:/bin/jpomAgent.zip");
        if (inputStream == null) {
            throw new JpomRuntimeException("jpomAgent.zip不存在");
        }
        // 解压代理工具到tomcat的appBase目录下
        ZipUtil.unzip(inputStream, new File(getAppBase()), CharsetUtil.CHARSET_UTF_8);
    }
}
