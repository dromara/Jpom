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
//package io.jpom.controller.node.manage.recover;
//
//import cn.jiangzeyin.common.JsonMessage;
//import io.jpom.common.BaseServerController;
//import io.jpom.common.forward.NodeForward;
//import io.jpom.common.forward.NodeUrl;
//import io.jpom.permission.ClassFeature;
//import io.jpom.permission.Feature;
//import io.jpom.permission.MethodFeature;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestMethod;
//import org.springframework.web.bind.annotation.ResponseBody;
//
//import java.io.IOException;
//import java.util.List;
//
///**
// * 项目管理
// *
// * @author Administrator
// */
//@Controller
//@RequestMapping(value = "/node/manage/recover")
//@Feature(cls = ClassFeature.PROJECT_RECOVER)
//public class ProjectRecoverControl extends BaseServerController {
//
////    /**
////     * 展示项目页面
////     *
////     * @return page
////     */
////    @RequestMapping(value = "list.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
////    @Feature(method = MethodFeature.LIST)
////    public String projectInfo() {
////        List list = NodeForward.requestData(getNode(), NodeUrl.Manage_Recover_List_Data, getRequest(), List.class);
////        setAttribute("array", list);
////        return "node/manage/project_recover";
////    }
//
//    /**
//     * @author Hotstrip
//     * get recover list
//     * 项目回收列表
//     * @return
//     * @throws IOException
//     */
//    @RequestMapping(value = "recover-list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
//    @ResponseBody
//    @Feature(method = MethodFeature.LIST)
//    public String recoverList() throws IOException {
//        List list = NodeForward.requestData(getNode(), NodeUrl.Manage_Recover_List_Data, getRequest(), List.class);
//        return JsonMessage.getString(200, "success", list);
//    }
//
//    @RequestMapping(value = "data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
//    @ResponseBody
//    @Feature(method = MethodFeature.LIST)
//    public String project() throws IOException {
//        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_Recover_Item_Data).toString();
//    }
//
//}
