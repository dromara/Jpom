package io.jpom.controller.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.Resource;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.json.JSONObject;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.DockerUtil;
import io.jpom.common.BaseServerController;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.SystemPermission;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "build-config")
@Feature(cls = ClassFeature.BUILD_CONFIG)
@SystemPermission
@Slf4j
public class BuildConfigController extends BaseServerController {

    @SneakyThrows
    @GetMapping("runs")
    public String getRuns() {
        // runs/%s/Dockerfile
        Map<String, String> runs = new HashMap<>();
        String folder = "runs";
        for (String filePath : DockerUtil.FILE_PATHS) {
            String dockerFileDir = filePath + folder;
            try {
                File directory = ResourceUtils.getFile(dockerFileDir);
                if (directory.exists() && directory.isDirectory()) {
                    File[] files = directory.listFiles((dir, name) -> dir.isDirectory() && Objects.requireNonNull(dir.listFiles((run_dir, run_name) -> run_dir.isDirectory())).length > 0);
                    for (File file : files) {
                        File dockerfile = new File(file, "Dockerfile");
                        if (dockerfile.exists() && dockerfile.isFile()) {
                            runs.putIfAbsent(file.getName(), dockerfile.getAbsolutePath());
                        }
                    }
                }
            } catch (FileNotFoundException e) {
                log.debug("{} not found", dockerFileDir);
            }
        }
        String defaultRuns = "ubuntu-latest";
        if (Objects.isNull(runs.get(defaultRuns))) {
            // jar内文件无法获取绝对路径，此处把jar 内 dockerfile 放到数据目录下
            Path dockerfile = FileSystems.getDefault().getPath(DockerUtil.FILE_PATHS[0], "runs", defaultRuns, "Dockerfile");
            String path = dockerfile.toString();
            Resource resourceObj = ResourceUtil.getResourceObj("runs/" + defaultRuns + "/Dockerfile");
            InputStream stream = resourceObj.getStream();
            String content = IOUtils.toString(stream, StandardCharsets.UTF_8);
            FileUtil.writeString(content, path, StandardCharsets.UTF_8);
            runs.put(defaultRuns, path);
        }
        List<JSONObject> collect = runs.entrySet().stream().map(e ->
            new JSONObject().set("name", e.getKey()).set("path", e.getValue()).set("content", FileUtil.readUtf8String(e.getValue()))
        ).collect(Collectors.toList());
        return JsonMessage.getString(200, "ok", collect);
    }

    @PostMapping("runs")
    public String updateRuns(String name, String content) {
        String dataPath = DockerUtil.FILE_PATHS[0];
        Path dockerfile = FileSystems.getDefault().getPath(dataPath, "runs", name, "Dockerfile");
        FileUtil.writeString(content, dockerfile.toString(), StandardCharsets.UTF_8);
        return JsonMessage.getString(200, "ok");
    }

    @DeleteMapping("runs/{name}")
    public String deleteRuns(@PathVariable String name) {
        String dataPath = DockerUtil.FILE_PATHS[0];
        Path dockerfile = FileSystems.getDefault().getPath(dataPath, "runs", name);
        if (!Files.exists(dockerfile)) {
            return JsonMessage.getString(400, "文件不存在");
        }
        FileUtil.del(dockerfile);
        return JsonMessage.getString(200, "ok");
    }
}
