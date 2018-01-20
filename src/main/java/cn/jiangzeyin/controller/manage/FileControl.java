package cn.jiangzeyin.controller.manage;


import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.service.manage.ManageService;
import cn.jiangzeyin.system.SystemBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping(value = "/file/")
public class FileControl {

    /**
     * 文件管理页面
     * @param id
     * @return
     */
    @RequestMapping(value = "filemanage")
    public String fileManage(String id) {

//        File file_
//        List<File> list_file = new ArrayList<File>();
//        request.setAttribute("projectInfo", JSONObject.toJSONString(list_file));
        return "manage/filemanage";
    }

    /**
     * 读取启动文件
     * @return
     */
    @RequestMapping(value = "getRunBoot")
    @ResponseBody
    public String getRunBoot() {

        File file = new File(SystemBean.getInstance().getEnvironment().getProperty("command.conf"));

        if (!file.exists()) {
            return JsonMessage.getString(500, "启动文件不存在");
        }

        StringBuffer sb = new StringBuffer();
        // 读取文件
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String str_temp;

            while ((str_temp = br.readLine()) != null) {

                sb.append(str_temp + "\r\n");
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Map<String, String> map = new HashMap<>();
        map.put("content", sb.toString());
        return JsonMessage.getString(200, "success", map);
    }

    /**
     * 修改启动文件
     * @param content
     * @return
     */
    @RequestMapping(value = "saveRunBoot")
    @ResponseBody
    public String saveRunBoot(String content) {
        File file = new File(SystemBean.getInstance().getEnvironment().getProperty("command.conf"));

        if (!file.exists()) {
            return JsonMessage.getString(500, "启动文件不存在");
        }

        // 写入文件
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(content.getBytes());
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return JsonMessage.getString(200, "success");
    }


}
