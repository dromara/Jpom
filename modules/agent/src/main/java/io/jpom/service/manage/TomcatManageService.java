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
package io.jpom.service.manage;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.controller.tomcat.TomcatOp;
import io.jpom.model.data.TomcatInfoModel;
import io.jpom.system.JpomRuntimeException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author bwcx_jzy
 * @since 2019/7/21
 */
@Service
public class TomcatManageService {

    @Resource
    private TomcatEditService tomcatEditService;

    /**
     * 查询tomcat状态
     *
     * @param id tomcat的id
     * @return tomcat状态0表示未运行，1表示运行中
     */
    public int getTomcatStatus(String id) {
        int result = 0;
        TomcatInfoModel tomcatInfoModel = tomcatEditService.getItem(id);
        String url = String.format("http://127.0.0.1:%d/", tomcatInfoModel.getPort());
        HttpRequest httpRequest = new HttpRequest(url);
        // 设置超时时间为3秒
        httpRequest.setConnectionTimeout(3000);
        try (HttpResponse httpResponse = httpRequest.execute()) {

            result = 1;
        } catch (Exception ignored) {
        }

        return result;
    }

    /**
     * 查询tomcat的项目列表
     *
     * @param id tomcat的id
     * @return tomcat的项目列表
     */
    public JSONArray getTomcatProjectList(String id) {
        TomcatInfoModel tomcatInfoModel = tomcatEditService.getItem(id);
        String body = tomcatCmd(tomcatInfoModel, "text/list");

        String[] result = body.replace(StrUtil.CRLF, "$")
            .replace("\n", "$")
            .split("\\$");

        JSONArray jsonArray = new JSONArray();

        for (int i = 1; i < result.length; i++) {
            String str = result[i];
            JSONObject jsonObject = new JSONObject();
            String[] strs = str.split(StrUtil.COLON);
            if (strs[0].endsWith("jpomAgent")) {
                continue;
            }

            jsonObject.put("path", StrUtil.SLASH.equals(strs[0]) ? "/ROOT" : strs[0]);
            jsonObject.put("status", strs[1]);
            jsonObject.put("session", strs[2]);

            jsonArray.add(jsonObject);
        }

        return jsonArray;
    }

    /**
     * 访问tomcat Url
     *
     * @param tomcatInfoModel tomcat信息
     * @param cmd             命令
     * @return 访问结果
     */
    private String tomcatCmd(TomcatInfoModel tomcatInfoModel, String cmd) {
        String url = String.format("http://127.0.0.1:%d/jpomAgent/%s", tomcatInfoModel.getPort(), cmd);
        HttpRequest httpRequest = HttpUtil.createGet(url);
        // new HttpRequest(url);
        // 设置超时时间为3秒
        httpRequest.timeout(3000);
        String body = "";

        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (httpResponse.isOk()) {
                body = httpResponse.body();
            }
            if (httpResponse.getStatus() == HttpStatus.HTTP_NOT_FOUND) {
                // 没有插件
                tomcatInfoModel.initTomcat();
                throw new JpomRuntimeException("tomcat 未初始化，已经重新初始化请稍后再试");
            }
        } catch (JpomRuntimeException jpom) {
            throw jpom;
        } catch (Exception ignored) {
        }

        return body;
    }

    /**
     * tomcat项目管理
     *
     * @param id       tomcat id
     * @param path     项目路径
     * @param tomcatOp 执行的操作 start=启动项目 stop=停止项目 relaod=重启项目
     * @return 操作结果
     */
    public JsonMessage tomcatProjectManage(String id, String path, TomcatOp tomcatOp) {
        TomcatInfoModel tomcatInfoModel = tomcatEditService.getItem(id);
        String result = tomcatCmd(tomcatInfoModel, String.format("text/%s?path=%s", tomcatOp.name(), path));

        if (result.startsWith("OK")) {
            return new JsonMessage(200, "操作成功");
        } else {
            return new JsonMessage(500, "操作失败:" + result);
        }
    }
}
