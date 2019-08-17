package org.springframework.boot.env;

import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * yml springboot工具，依赖
 *
 * @author bwcx_jzy
 * @date 2019/8/17
 */
public class YmlUtil {

    public static List<Map<String, Object>> load(File file) {
        FileSystemResource fileSystemResource = new FileSystemResource(file);
        OriginTrackedYamlLoader originTrackedYamlLoader = new OriginTrackedYamlLoader(fileSystemResource);
        return originTrackedYamlLoader.load();
    }
}
