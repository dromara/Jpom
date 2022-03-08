import io.github.yedaxia.apidocs.Docs;
import io.github.yedaxia.apidocs.DocsConfig;

public class JApiTest {

    public static void main(String[] args) {
        DocsConfig config = new DocsConfig();
        config.setProjectPath("D:/project/Jpom/modules/server"); // root project path
        config.setProjectName("Jpom"); // project name
        config.setApiVersion("V1.0");       // api version
        config.setDocsPath("apidocs"); // api docs target path
        config.setAutoGenerate(Boolean.TRUE);  // auto generate
        Docs.buildHtmlDocs(config); // execute to generate
    }
}
