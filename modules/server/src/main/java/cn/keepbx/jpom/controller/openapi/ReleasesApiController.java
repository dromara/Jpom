package cn.keepbx.jpom.controller.openapi;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.HttpUtil;
import cn.jiangzeyin.controller.base.AbstractController;
import cn.keepbx.jpom.common.Type;
import cn.keepbx.jpom.common.interceptor.NotLogin;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 获取最新安装包
 *
 * @author bwcx_jzy
 * @date 2019/8/19
 */
@RestController
@RequestMapping(value = "api")
@NotLogin
public class ReleasesApiController extends AbstractController {

    @RequestMapping(value = "releases/{type}")
    public void releases(@PathVariable String type) throws IOException {
        HttpServletResponse response = getResponse();
        // 验证类型
        if (StrUtil.equalsAny(type, true, Type.Agent.name(), Type.Server.name())) {
            ServletUtil.write(response, "没有发布版", MediaType.TEXT_HTML_VALUE);
            return;
        }
        // 查询最新版
        String body = HttpUtil.createGet("https://api.github.com/repos/jiangzeyin/Jpom/releases/latest")
                .execute()
                .body();
        JSONObject jsonObject = JSONObject.parseObject(body);
        String tagName = jsonObject.getString("tag_name");
        if (StrUtil.isEmpty(tagName)) {
            ServletUtil.write(response, "没有发布版", MediaType.TEXT_HTML_VALUE);
            return;
        }
        tagName = tagName.replace("v", "").replace("V", "");
        String url = String.format("https://jpom-releases.oss-cn-hangzhou.aliyuncs.com/%s-%s-release.zip", type.toLowerCase(), tagName);
        // 重定向
        response.sendRedirect(url);
    }
}
