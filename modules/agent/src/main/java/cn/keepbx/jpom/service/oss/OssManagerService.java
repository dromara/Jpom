package cn.keepbx.jpom.service.oss;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.keepbx.jpom.common.BaseDataService;
import cn.keepbx.jpom.system.AgentConfigBean;
import cn.keepbx.jpom.util.JsonFileUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 阿里云oss
 *
 * @author jiangzeyin
 * @date 2018/9/28
 */
@Service
public class OssManagerService extends BaseDataService {

    public File download(String key) {
        File file = AgentConfigBean.getInstance().getTempPath();
        //getTempPath();
        file = FileUtil.file(file, key);
        OSSClient ossClient = getOSSClient(getConfig());
        //   SimplifiedObjectMeta objectMeta = ossClient.getSimplifiedObjectMeta(getBucketName(), key);
        // 下载OSS文件到本地文件。如果指定的本地文件存在会覆盖，不存在则新建。
        ossClient.getObject(new GetObjectRequest(getBucketName(), key), file);
        // 关闭OSSClient。
        ossClient.shutdown();
        return file;
    }

    public JSONArray list(String name) {
        OSSClient ossClient;
        String prefix = String.format("%s%s", getKeyPrefix(), name);
        ListObjectsRequest request;
        List<OSSObjectSummary> summaryList = new ArrayList<>();
        String nextMarker = null;
        do {
            ossClient = getOSSClient(getConfig());
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

    public URL getUrl(String key) {
        // 创建OSSClient实例。
        OSSClient ossClient = getOSSClient(getConfig());
        // 设置URL过期时间。
        Date expiration = new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10));
        // 生成以GET方法访问的签名URL，访客可以直接通过浏览器访问相关内容。
        return ossClient.generatePresignedUrl(getBucketName(), key, expiration);
    }

    public OSSClient getOSSClient(JSONObject config) {
        String endpoint = String.format("http://%s", config.getString("endpoint"));
        // 阿里云主账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM账号进行API访问或日常运维，
        // 请登录 https://ram.console.aliyun.com 创建RAM账号。
        String accessKeyId = config.getString("accessKeyId");
        String accessKeySecret = config.getString("accessKeySecret");
        // 创建OSSClient实例。
        return new OSSClient(endpoint, accessKeyId, accessKeySecret);
    }

    public JSONObject getConfig() {
        return getJSONObject(AgentConfigBean.ALI_OSS);
    }

    private String getBucketName() {
        JSONObject config = getConfig();
        return config.getString("bucketName");
    }

    private String getKeyPrefix() {
        JSONObject config = getConfig();
        return config.getString("keyPrefix");
    }

    public void save(JSONObject jsonObject) {
        String path = getDataFilePath(AgentConfigBean.ALI_OSS);
        JsonFileUtil.saveJson(path, jsonObject);
    }
}
