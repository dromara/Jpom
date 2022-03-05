package io.jpom.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.extra.servlet.ServletUtil;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.net.URL;

/**
 * robots 接口
 *
 * @author bwcx_jzy
 * @since 2022/3/5
 */
@RestController
public class RobotsController {

    @GetMapping(value = "robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public void robots(HttpServletResponse response) {
        URL resource = ResourceUtil.getResource("robots.txt");
        String readString = FileUtil.readString(resource, CharsetUtil.CHARSET_UTF_8);
        ServletUtil.write(response, readString, MediaType.TEXT_PLAIN_VALUE);
    }
}
