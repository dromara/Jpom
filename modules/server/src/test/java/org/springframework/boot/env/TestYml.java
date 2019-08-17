package org.springframework.boot.env;

import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @date 2019/8/17
 */
public class TestYml {

    public static void main(String[] args) throws IOException {

        FileSystemResource fileSystemResource = new FileSystemResource("D:\\Idea\\Jpom\\modules\\agent\\target\\agent-2.4.3-release\\extConfig.yml");

        OriginTrackedYamlLoader originTrackedYamlLoader = new OriginTrackedYamlLoader(fileSystemResource);
        List<Map<String, Object>> load = originTrackedYamlLoader.load();
        System.out.println(load);
    }
}
