/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.File;

/**
 * @author bwcx_jzy
 * @since 2019/5/30
 **/
public class GetTomcatPort {
    public static void main(String[] args) {
        String path = "C:\\SoftWare\\apache-tomcat-7.0.94";
        File file = FileUtil.file(path, "conf", "server.xml");
        Document document = XmlUtil.readXML(file);
        NodeList nodeList = document.getElementsByTagName("Service");
        System.out.println(nodeList.getLength());
        String name = nodeList.item(0).getAttributes().getNamedItem("name").getNodeValue();
        System.out.println(name);
        System.out.println(document);
    }
}
