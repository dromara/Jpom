package io.jpom.service.node.tomcat;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.NodeModel;
import io.jpom.permission.BaseDynamicService;
import io.jpom.plugin.ClassFeature;
import io.jpom.service.node.NodeService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * tomcat
 *
 * @author lf
 */
@Service
public class TomcatService implements BaseDynamicService {

    @Resource
    private NodeService nodeService;

    /**
     * 查询tomcat列表
     *
     * @param nodeModel 节点信息
     * @return tomcat的信息
     */
    public JSONArray getTomcatList(NodeModel nodeModel) {
        if (!nodeModel.isOpenStatus()) {
            return null;
        }
        JSONArray jsonArray = NodeForward.requestData(nodeModel, NodeUrl.Tomcat_List, JSONArray.class, null, null);
        return filter(jsonArray, ClassFeature.TOMCAT);
    }

    /**
     * 查询tomcat信息
     *
     * @param nodeModel 节点信息
     * @param id        tomcat的id
     * @return tomcat的信息
     */
    public JSONObject getTomcatInfo(NodeModel nodeModel, String id) {
        return NodeForward.requestData(nodeModel, NodeUrl.Tomcat_GetItem, JSONObject.class, "id", id);
    }


    public JSONArray getTomcatProjectList(NodeModel nodeModel, String id) {
        return NodeForward.requestData(nodeModel, NodeUrl.Tomcat_GetTomcatProjectList, JSONArray.class, "id", id);
    }

    /**
     * tomcat项目管理
     *
     * @param nodeModel 节点信息
     * @param request   请求信息
     * @return 操作结果
     */
    public String tomcatProjectManage(NodeModel nodeModel, HttpServletRequest request) {
        return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_TomcatProjectManage).toString();
    }

    /**
     * 新增Tomcat
     *
     * @param nodeModel 节点信息
     * @param request   请求信息
     * @return 操作结果
     */
    public String addTomcat(NodeModel nodeModel, HttpServletRequest request) {
        return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_Add).toString();
    }

    /**
     * 更新Tomcat信息
     *
     * @param nodeModel 节点信息
     * @param request   请求信息
     * @return 操作结果
     */
    public String updateTomcat(NodeModel nodeModel, HttpServletRequest request) {
        return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_Update).toString();
    }

    /**
     * 查询tomcat运行状态
     *
     * @param nodeModel 节点信息
     * @param request   请求信息
     * @return 操作结果
     */
    public String getTomcatStatus(NodeModel nodeModel, HttpServletRequest request) {
        return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_GetTomcatStatus).toString();
    }

    /**
     * 启动tomcat
     *
     * @param nodeModel 节点信息
     * @param request   请求信息
     * @return 操作结果
     */
    public String start(NodeModel nodeModel, HttpServletRequest request) {
        return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_Start).toString();
    }

    /**
     * 停止tomcat
     *
     * @param nodeModel 节点信息
     * @param request   请求信息
     * @return 操作结果
     */
    public String stop(NodeModel nodeModel, HttpServletRequest request) {
        return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_Stop).toString();
    }

    /**
     * 重启tomcat
     *
     * @param nodeModel 节点信息
     * @param request   请求信息
     * @return 操作结果
     */
    public String restart(NodeModel nodeModel, HttpServletRequest request) {
        return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_Restart).toString();
    }

    /**
     * 删除tomcat
     *
     * @param nodeModel 节点信息
     * @param request   请求信息
     * @return 操作结果
     */
    public String delete(NodeModel nodeModel, HttpServletRequest request) {
        return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_Delete).toString();
    }

    /**
     * 获取文件列表
     *
     * @param nodeModel 节点信息
     * @param request   请求信息
     * @return 操作结果
     */
    public String getFileList(NodeModel nodeModel, HttpServletRequest request) {
        return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_File_GetFileList).toString();
    }

    /**
     * 上传文件
     *
     * @param nodeModel 节点信息
     * @param request   请求信息
     * @return 操作结果
     */
    public String upload(NodeModel nodeModel, HttpServletRequest request) {
        return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_File_Upload).toString();
    }

    /**
     * 下载文件
     *
     * @param nodeModel 节点信息
     * @param request   请求信息
     * @param response  响应信息
     */
    public void download(NodeModel nodeModel, HttpServletRequest request, HttpServletResponse response) {
        NodeForward.requestDownload(nodeModel, request, response, NodeUrl.Tomcat_File_Download);
    }

    /**
     * 删除文件
     *
     * @param nodeModel 节点信息
     * @param request   请求信息
     * @return 操作结果
     */
    public String deleteFile(NodeModel nodeModel, HttpServletRequest request) {
        return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_File_DeleteFile).toString();
    }

    /**
     * 上传War包
     *
     * @param node         节点信息
     * @param multiRequest 请求信息
     * @return 操作结果
     */
    public String uploadWar(NodeModel node, MultipartHttpServletRequest multiRequest) {
        return NodeForward.requestMultipart(node, multiRequest, NodeUrl.Tomcat_File_UploadWar).toString();
    }

    @Override
    public JSONArray listToArray(String dataId) {
        NodeModel item = nodeService.getItem(dataId);
        if (item == null) {
            return null;
        }
        return getTomcatList(item);
    }
}
