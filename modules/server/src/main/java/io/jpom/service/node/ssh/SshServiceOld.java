//package io.jpom.service.node.ssh;
//
//import cn.hutool.core.util.IdUtil;
//import com.alibaba.fastjson.JSONArray;
//import com.alibaba.fastjson.JSONObject;
//import io.jpom.common.BaseOperService;
//import io.jpom.model.data.NodeModel;
//import io.jpom.model.data.SshModel;
//import io.jpom.permission.BaseDynamicService;
//import io.jpom.service.node.NodeOld1Service;
//import io.jpom.system.ServerConfigBean;
//import org.springframework.stereotype.Service;
//
//import javax.annotation.Resource;
//import java.util.List;
//
///**
// * @author bwcx_jzy
// * @date 2019/8/9
// */
//@Service
//public class SshServiceOld extends BaseOperService<SshModel> implements BaseDynamicService {
//
//	@Resource
//	private NodeOld1Service nodeServiceOld;
//
//	public SshServiceOld() {
//		super(ServerConfigBean.SSH_LIST);
//	}
//
//	@Override
//	public void addItem(SshModel sshModel) {
//		sshModel.setId(IdUtil.fastSimpleUUID());
//		super.addItem(sshModel);
//	}
//
//	@Override
//	public JSONArray listToArray(String dataId) {
//		return (JSONArray) JSONArray.toJSON(this.list());
//	}
////
////	@Override
////	public List<SshModel> list() {
////		return (List<SshModel>) filter(super.list(), ClassFeature.SSH);
////	}
//
//	public JSONArray listSelect(String nodeId) {
//		// 查询ssh
//		List<SshModel> sshModels = list();
//		List<NodeModel> list = nodeServiceOld.list();
//		JSONArray sshList = new JSONArray();
//		if (sshModels == null) {
//			return sshList;
//		}
//		sshModels.forEach(sshModel -> {
//			String sshModelId = sshModel.getId();
//			JSONObject jsonObject = new JSONObject();
//			jsonObject.put("id", sshModelId);
//			jsonObject.put("name", sshModel.getName());
//			if (list != null) {
//				for (NodeModel nodeModel : list) {
////					if (!StrUtil.equals(nodeId, nodeModel.getId()) && StrUtil.equals(sshModelId, nodeModel.getSshId())) {
////						jsonObject.put("disabled", true);
////						break;
////					}
//				}
//			}
//			sshList.add(jsonObject);
//		});
//		return sshList;
//	}
//
//
//}
