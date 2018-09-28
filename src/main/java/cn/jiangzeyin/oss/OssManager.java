package cn.jiangzeyin.oss;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;

import java.net.URL;
import java.util.List;

/**
 * Created by jiangzeyin on 2018/9/28.
 */
public class OssManager {

    public void list(String name) {
        OSSClient ossClient = getOSSClient();
        String prefix = String.format("%s%s", getKeyPrefix(), name);
        ObjectListing objectListing = ossClient.listObjects(getBucketName(), prefix);

        List<OSSObjectSummary> sums = objectListing.getObjectSummaries();
        for (OSSObjectSummary s : sums) {
            System.out.println(JSONObject.toJSON(s));
            System.out.println("\t" + s.getKey());
        }
        // 关闭OSSClient。
        ossClient.shutdown();
    }

    private OSSClient getOSSClient() {
        JSONObject config = getConfig();
        String endpoint = String.format("http://%s", config.getString("endpoint"));
        // 阿里云主账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM账号进行API访问或日常运维，请登录 https://ram.console.aliyun.com 创建RAM账号。
        String accessKeyId = config.getString("accessKeyId");
        String accessKeySecret = config.getString("accessKeySecret");
        // 创建OSSClient实例。
        return new OSSClient(endpoint, accessKeyId, accessKeySecret);
    }

    private JSONObject getConfig() {
        String active = SpringUtil.getEnvironment().getProperty("spring.profiles.active");
        URL url = ResourceUtil.getResource("oss/" + active + ".json");
        if (url == null) {
            throw new IllegalArgumentException("请配置阿里云oss");
        }
        String json = FileUtil.readString(url, CharsetUtil.UTF_8);
        return JSON.parseObject(json);
    }

    private String getBucketName() {
        JSONObject config = getConfig();
        return config.getString("bucketName");
    }

    private String getKeyPrefix() {
        JSONObject config = getConfig();
        return config.getString("keyPrefix");
    }
}
