package cn.jiangzeyin.oss;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jiangzeyin on 2018/9/28.
 */
@Service
public class OssManagerService {

    public JSONArray list(String name) {
        OSSClient ossClient;
        String prefix = String.format("%s%s", getKeyPrefix(), name);
        ListObjectsRequest request;
        List<OSSObjectSummary> summaryList = new ArrayList<>();
        String nextMarker = null;
        do {
            ossClient = getOSSClient();
            request = new ListObjectsRequest();
            request.setBucketName(getBucketName());
            request.setPrefix(prefix);
            request.setMaxKeys(1);
            request.setMarker(nextMarker);
            ObjectListing objectListing = ossClient.listObjects(request);
            List<OSSObjectSummary> sums = objectListing.getObjectSummaries();
//
            summaryList.addAll(sums);
            nextMarker = objectListing.getNextMarker();
            // 关闭OSSClient。
            ossClient.shutdown();
        } while (nextMarker != null);
        summaryList.sort((o1, o2) -> o2.getLastModified().compareTo(o1.getLastModified()));
        JSONArray jsonArray = new JSONArray();
        summaryList.forEach(ossObjectSummary -> {
            JSONObject jsonObject = new JSONObject();
            String newKey = ossObjectSummary.getKey().substring(prefix.length());
            jsonObject.put("shortKey", newKey);
            jsonObject.put("key", ossObjectSummary.getKey());
            jsonObject.put("time", DateUtil.date(ossObjectSummary.getLastModified()).toString());
            jsonObject.put("size", FileUtil.readableFileSize(ossObjectSummary.getSize()));
            jsonArray.add(jsonObject);
        });
        return jsonArray;
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
