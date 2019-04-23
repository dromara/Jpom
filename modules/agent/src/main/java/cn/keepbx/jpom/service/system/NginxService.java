package cn.keepbx.jpom.service.system;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.common.BaseOperService;
import cn.keepbx.jpom.model.data.AgentWhitelist;
import cn.keepbx.jpom.service.WhitelistDirectoryService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.odiszapc.nginxparser.NgxBlock;
import com.github.odiszapc.nginxparser.NgxConfig;
import com.github.odiszapc.nginxparser.NgxEntry;
import com.github.odiszapc.nginxparser.NgxParam;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

/**
 * @author Arno
 */
@Service
public class NginxService extends BaseOperService {

    @Resource
    private WhitelistDirectoryService whitelistDirectoryService;

    @Override
    public JSONArray list() {
        AgentWhitelist agentWhitelist = whitelistDirectoryService.getWhitelist();
        if (agentWhitelist == null) {
            return null;
        }
        List<String> ngxDirectory = agentWhitelist.getNginx();
        if (ngxDirectory == null) {
            return null;
        }
        JSONArray array = new JSONArray();
        for (Object o : ngxDirectory) {
            String parentPath = o.toString();
            File whiteDir = new File(parentPath);
            if (!whiteDir.isDirectory()) {
                continue;
            }
            List<File> list = null;
            try {
                //获得指定目录下所有文件
                list = FileUtil.loopFiles(whiteDir, pathname -> pathname.getName().endsWith(".conf"));
            } catch (Exception e) {
                DefaultSystemLog.ERROR().error(e.getMessage(), e);
            }
            if (list == null || list.size() <= 0) {
                continue;
            }
            String absPath = whiteDir.getAbsolutePath();
            for (File itemFile : list) {
                String itemAbsPath = itemFile.getAbsolutePath();
                String name = paresName(absPath, itemAbsPath);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("path", parentPath);
                jsonObject.put("name", name);
                long time = itemFile.lastModified();
                jsonObject.put("time", DateUtil.date(time).toString());
                try {
                    NgxConfig config = NgxConfig.read(itemFile.getPath());
                    List<NgxEntry> server = config.findAll(NgxBlock.class, "server");
                    JSONObject data = findSeverName(server);
                    if (data != null) {
                        jsonObject.putAll(data);
                    }
                } catch (IOException e) {
                    DefaultSystemLog.ERROR().error(e.getMessage(), e);
                }
                array.add(jsonObject);
            }
        }
        return array;
    }

    public String paresName(String whitePath, String itemAbsPath) {
        File file = new File(whitePath);
        whitePath = file.getAbsolutePath();
        String path = itemAbsPath.substring(itemAbsPath.indexOf(whitePath) + whitePath.length());
        path = FileUtil.normalize(path);
        if (path.startsWith(StrUtil.SLASH)) {
            path = path.substring(1);
        }
        return path;
    }

    @Override
    public void addItem(Object o) {

    }

    @Override
    public boolean updateItem(Object o) throws Exception {
        return false;
    }

    /**
     * 获取域名
     *
     * @param server server块
     * @return 域名
     */
    private JSONObject findSeverName(List<NgxEntry> server) {
        if (null == server) {
            return null;
        }
        JSONObject jsonObject = new JSONObject();
        HashSet<String> serverNames = new HashSet<>();
        HashSet<String> location = new HashSet<>();
        HashSet<String> listen = new HashSet<>();
        for (NgxEntry ngxEntry : server) {

            NgxBlock ngxBlock = (NgxBlock) ngxEntry;
            NgxParam serverName = ngxBlock.findParam("server_name");
            if (null != serverName) {
                serverNames.add(serverName.getValue());
            }
            List<NgxEntry> locationAll = ngxBlock.findAll(NgxBlock.class, "location");
            if (locationAll != null) {
                locationAll.forEach(ngxEntry1 -> {
                    NgxBlock ngxBlock1 = (NgxBlock) ngxEntry1;
                    if (!StrUtil.SLASH.equals(ngxBlock1.getValue())) {
                        return;
                    }
                    NgxParam locationMain = ngxBlock1.findParam("proxy_pass");
                    if (locationMain == null) {
                        locationMain = ngxBlock1.findParam("root");
                    }
                    if (locationMain == null) {
                        locationMain = ngxBlock1.findParam("alias");
                    }
                    location.add(locationMain.getValue());
                });
            }
            // 监听的端口
            NgxParam listenParm = ngxBlock.findParam("listen");
            if (listenParm != null) {
                listen.add(listenParm.getValue());
            }
        }
        jsonObject.put("serverCount", server.size());
        jsonObject.put("server_name", CollUtil.join(serverNames, StrUtil.COMMA));
        jsonObject.put("location", CollUtil.join(location, StrUtil.COMMA));
        jsonObject.put("listen", CollUtil.join(listen, StrUtil.COMMA));
        return jsonObject;
    }


    /**
     * 解析nginx
     *
     * @param path nginx路径
     */
    @Override
    public JSONObject getItem(String path) {
        JSONObject jsonObject = new JSONObject();
        try {
            NgxConfig conf = NgxConfig.read(path);
            NgxParam cache = conf.findParam("http", "proxy_cache_path");
            if (cache != null) {
                String value = cache.getValue();
                String[] split = value.split(" ");
                jsonObject.put("cachePath", split[0].trim());
                String maxSize = split[3];
                String size = maxSize.substring("max_size=".length(), maxSize.length() - 1);
                jsonObject.put("cacheSize", size);
                String inactive = split[4];
                String time = inactive.substring("inactive=".length(), inactive.length() - 1);
                jsonObject.put("inactive", time);
            }
            List<NgxEntry> list = conf.findAll(NgxBlock.class, "server");
            if (list == null) {
                return jsonObject;
            }
            boolean main = true;
            for (NgxEntry ngxEntry : list) {
                NgxBlock block = (NgxBlock) ngxEntry;
                NgxParam certificate = block.findParam("ssl_certificate");
                NgxParam key = block.findParam("ssl_certificate_key");
                NgxParam listen = block.findParam("listen");
                NgxParam serverName = block.findParam("server_name");
                NgxParam location = block.findParam("location", "proxy_pass");
                if (certificate != null && main) {
                    main = false;
                    jsonObject.put("cert", certificate.getValue());
                    jsonObject.put("key", key.getValue());
                    jsonObject.put("port", listen.getValue());
                    jsonObject.put("domain", serverName.getValue());
                    jsonObject.put("location", location.getValue());
                }
                NgxParam rewrite = block.findParam("rewrite");
                if (rewrite != null) {
                    jsonObject.put("convert", true);
                }
                if (main) {
                    jsonObject.put("port", listen.getValue());
                    jsonObject.put("domain", serverName.getValue());
                    if (null != location) {
                        jsonObject.put("location", location.getValue());
                    }
                }

            }
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return jsonObject;
    }
}
