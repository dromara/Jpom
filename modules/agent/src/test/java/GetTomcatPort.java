import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.File;

/**
 * @author bwcx_jzy
 * @date 2019/5/30
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
