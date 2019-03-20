package cn.keepbx.jpom.service.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.common.BaseOperService;
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
        JSONArray ngxDirectory = whitelistDirectoryService.getNgxDirectory();
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
                try {
                    NgxConfig config = NgxConfig.read(itemFile.getPath());
                    NgxBlock http = config.findBlock("http");
                    String severName;
                    if (null != http) {
                        List<NgxEntry> server = http.findAll(NgxBlock.class, "server");
                        severName = findSeverName(server);
                        if (StrUtil.isNotEmpty(severName)) {
                            jsonObject.put("domain", severName);
                            array.add(jsonObject);
                            continue;
                        }
                    }
                    List<NgxEntry> server = config.findAll(NgxBlock.class, "server");
                    severName = findSeverName(server);
                    jsonObject.put("domain", severName);
                } catch (IOException e) {
                    DefaultSystemLog.ERROR().error(e.getMessage(), e);
                }
                array.add(jsonObject);
            }
        }
        return array;
    }

    public String paresName(String whitePath, String itemAbsPath) {
        String path = itemAbsPath.substring(itemAbsPath.indexOf(whitePath) + whitePath.length());
        path = FileUtil.normalize(path);
        if (path.startsWith(StrUtil.SLASH)) {
            path = path.substring(1);
        }
        return path;
    }

    @Override
    public Object getItem(String id) {
        return null;
    }


    /**
     * 获取域名
     *
     * @param server server块
     * @return 域名
     */
    private String findSeverName(List<NgxEntry> server) {
        if (null == server || server.size() <= 0) {
            return null;
        }
        for (NgxEntry ngxEntry : server) {
            if (null != ngxEntry) {
                NgxBlock ngxBlock = (NgxBlock) ngxEntry;
                NgxParam serverName = ngxBlock.findParam("server_name");
                if (null != serverName) {
                    return serverName.getValue();
                }
            }
        }
        return null;
    }


    /**
     * 解析nginx
     *
     * @param path nginx路径
     */
    public JSONObject resolveNgx(String path) {
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
