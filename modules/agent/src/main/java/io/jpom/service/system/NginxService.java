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
package io.jpom.service.system;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.odiszapc.nginxparser.NgxBlock;
import com.github.odiszapc.nginxparser.NgxConfig;
import com.github.odiszapc.nginxparser.NgxEntry;
import com.github.odiszapc.nginxparser.NgxParam;
import io.jpom.model.data.AgentWhitelist;
import io.jpom.service.BaseDataService;
import io.jpom.service.WhitelistDirectoryService;
import io.jpom.system.AgentConfigBean;
import io.jpom.util.JsonFileUtil;
import io.jpom.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Arno
 */
@Service
@Slf4j
public class NginxService extends BaseDataService {

    private final WhitelistDirectoryService whitelistDirectoryService;

    public NginxService(WhitelistDirectoryService whitelistDirectoryService) {
        this.whitelistDirectoryService = whitelistDirectoryService;
    }

    public List<JSONObject> list(String whitePath, String fileName, String showAll) {
        AgentWhitelist agentWhitelist = whitelistDirectoryService.getWhitelist();
        if (agentWhitelist == null) {
            return null;
        }
        List<String> ngxDirectory = agentWhitelist.getNginx();
        if (ngxDirectory == null) {
            return null;
        }
        File normalize = FileUtil.file(whitePath, fileName);
        File[] files = FileUtil.ls(normalize.getAbsolutePath());
        if (files == null || files.length <= 0) {
            return null;
        }
        String[] suffixes = Convert.toBool(showAll, false) ? new String[]{".conf", ".conf_back"} : new String[]{".conf"};
        return Arrays.stream(files).map(file -> {
            String name = file.getName();
            if (file.isDirectory()) {
                return null;
            }
            if (!StrUtil.endWithAnyIgnoreCase(name, suffixes)) {
                return null;
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("path", whitePath);
            long time = file.lastModified();
            jsonObject.put("time", DateUtil.date(time).toString());
            jsonObject.put("name", name);
            jsonObject.put("relativePath", FileUtil.normalize(fileName + StrUtil.SLASH + name));
//				if (FileUtil.isEmpty(file)) {
//					continue;
//				}
//				jsonObject.put("name", name + "【文件夹】");
//				jsonObject.put("isDirectory", true);


            try {
                NgxConfig config = NgxConfig.read(file.getPath());
                List<NgxEntry> server = config.findAll(NgxBlock.class, "server");
                JSONObject data = findSeverName(server);
                if (data != null) {
                    jsonObject.putAll(data);
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }

            return jsonObject;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 获取nginx树型图列表
     *
     * @return JSONArray
     */
    public JSONArray tree() {
        JSONArray treeArray = new JSONArray();
        AgentWhitelist agentWhitelist = whitelistDirectoryService.getWhitelist();
        if (agentWhitelist == null) {
            return treeArray;
        }
        List<String> ngxDirectory = agentWhitelist.getNginx();
        if (ngxDirectory == null) {
            return treeArray;
        }
        for (String str : ngxDirectory) {
            JSONObject object = addChild(str, "");
            if (object != null) {
                object.put("title", str);
                //object.put("spread", true);
                treeArray.add(object);
            }
        }
        return treeArray;
    }

    /**
     * 扫描目录下所有nginx配置文件
     *
     * @param whitePath 白名单路径
     * @param fileName  文件路径
     */
    private JSONObject addChild(String whitePath, String fileName) {
        File parentFile = StrUtil.isNotEmpty(fileName) ? FileUtil.file(whitePath, fileName) : FileUtil.file(whitePath);
        if (!FileUtil.isDirectory(parentFile)) {
            return null;
        }
        String absolutePath = parentFile.getAbsolutePath();
        File[] files = FileUtil.ls(absolutePath);
        if (files == null || files.length <= 0) {
            return null;
        }
        JSONObject object = new JSONObject();
        String parentName = parentFile.getName();
        object.put("title", parentName);
        object.put("whitePath", whitePath);
        object.put("path", fileName);
        JSONArray array = new JSONArray();
        for (File file : files) {
            String name = StringUtil.delStartPath(file, whitePath, true);
            if (file.isDirectory()) {
                if (FileUtil.isEmpty(file)) {
                    continue;
                }
                JSONObject child = addChild(whitePath, name);
                if (child != null) {
                    array.add(child);
                }
            } else {
//				String fName = file.getName();
//				if (fName.endsWith(".conf")) {
//					JSONObject child = new JSONObject();
//					child.put("title", fName);
//					child.put("whitePath", whitePath);
//					child.put("path", name);
//					array.add(child);
//				}
            }
        }
        object.put("children", array);
        return object;
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
                    if (locationMain == null) {
                        return;
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
            log.error(e.getMessage(), e);
        }
        return jsonObject;
    }

    /**
     * 获取nginx配置
     * name 修改后的服务名
     * status 状态：开启 open/ 关闭close
     */
    public JSONObject getNgxConf() {
        JSONObject object = getJSONObject(AgentConfigBean.NGINX_CONF);
        if (object == null) {
            object = new JSONObject();
            object.put("name", "nginx");
            save(object);
        }
        return object;
    }


    public String getServiceName() {
        JSONObject ngxConf = getNgxConf();
        return ngxConf.getString("name");
    }

    public void save(JSONObject object) {
        String dataFilePath = getDataFilePath(AgentConfigBean.NGINX_CONF);
        JsonFileUtil.saveJson(dataFilePath, object);
    }

}
