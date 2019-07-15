import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.keepbx.jpom.BaseJpomApplication;
import cn.keepbx.util.GitUtil;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.*;
import java.util.List;

/**
 * @author bwcx_jzy
 * @date 2019/7/9
 **/
public class TestGit {
    private static UsernamePasswordCredentialsProvider usernamePasswordCredentialsProvider =
            new UsernamePasswordCredentialsProvider("1", "1");


    public static void main(String[] args) throws GitAPIException, IOException {

        File file = new File("D:\\test\\git");

        String url = "https://gitee.com/jiangzeyin/test.git";

        List<String> l = GitUtil.branchList(url, file, usernamePasswordCredentialsProvider);
        System.out.println(l);
        //checkOutPull(url, file, "master");
        //
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(file);
        processBuilder.command("cmd", "/c", "mvn clean package");

        Process process = processBuilder.start();
        {
            InputStream inputStream = process.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, BaseJpomApplication.getCharset());
            BufferedReader results = new BufferedReader(inputStreamReader);
            IoUtil.readLines(results, new LineHandler() {
                @Override
                public void handle(String line) {
                    System.out.println(line);
                }
            });
        }
        {
            InputStream errorInputStream = process.getErrorStream();
            InputStreamReader inputStreamReader = new InputStreamReader(errorInputStream, BaseJpomApplication.getCharset());
            BufferedReader results = new BufferedReader(inputStreamReader);
            IoUtil.readLines(results, new LineHandler() {
                @Override
                public void handle(String line) {
                    System.out.println(line);
                }
            });
        }


    }


}
