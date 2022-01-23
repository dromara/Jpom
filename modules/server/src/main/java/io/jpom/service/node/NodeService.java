package io.jpom.service.node;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.common.BaseServerController;
import io.jpom.common.Const;
import io.jpom.common.JpomManifest;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.SshModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.data.WorkspaceModel;
import io.jpom.service.h2db.BaseGroupService;
import io.jpom.service.node.ssh.SshService;
import io.jpom.service.system.WorkspaceService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2021/12/4
 */
@Service
public class NodeService extends BaseGroupService<NodeModel> {

	private final SshService sshService;
	private final WorkspaceService workspaceService;

	public NodeService(SshService sshService,
					   WorkspaceService workspaceService) {
		this.sshService = sshService;
		this.workspaceService = workspaceService;
	}

	@Override
	protected void fillSelectResult(NodeModel data) {
		if (data != null) {
			data.setLoginPwd(null);
		}
	}

	/**
	 * 测试节点是否可以访问
	 *
	 * @param nodeModel 节点信息
	 */
	public void testNode(NodeModel nodeModel) {
		//
		int timeOut = ObjectUtil.defaultIfNull(nodeModel.getTimeOut(), 0);
		// 检查是否可用默认为5秒，避免太长时间无法连接一直等待
		nodeModel.setTimeOut(5);
		//
		JsonMessage<Object> objectJsonMessage = NodeForward.requestBySys(nodeModel, NodeUrl.Info, "nodeId", nodeModel.getId());
		JpomManifest jpomManifest = objectJsonMessage.getData(JpomManifest.class);
		Assert.notNull(jpomManifest, "节点连接失败，请检查节点是否在线");
		//
		nodeModel.setTimeOut(timeOut);
	}

	public boolean existsByUrl(String url, String workspaceId, String id) {
		//		Entity entity = Entity.create();
		//		entity.set("url", nodeModel.getUrl());
		//		entity.set("workspaceId", workspaceId);
		//		if (StrUtil.isNotEmpty(id)) {
		//			entity.set("id", StrUtil.format(" <> {}", id));
		//		}
		//		boolean exists = super.exists(entity);
		//		Assert.state(!exists, "对应的节点已经存在啦");
		// 可能出现错误
		NodeModel nodeModel1 = new NodeModel();
		nodeModel1.setUrl(url);
		nodeModel1.setWorkspaceId(workspaceId);
		List<NodeModel> nodeModels = ObjectUtil.defaultIfNull(super.listByBean(nodeModel1), Collections.EMPTY_LIST);
		Optional<NodeModel> any = nodeModels.stream().filter(nodeModel2 -> !StrUtil.equals(id, nodeModel2.getId())).findAny();
		return any.isPresent();
	}

	/**
	 * 修改 节点
	 *
	 * @param request 请求对象
	 */
	public void update(HttpServletRequest request, boolean autoReg) {
		String type = request.getParameter("type");
		boolean create = "add".equalsIgnoreCase(type);
		// 创建对象
		NodeModel nodeModel = ServletUtil.toBean(request, NodeModel.class, true);
		String id = nodeModel.getId();
		if (StrUtil.isNotEmpty(id)) {
			String checkId = StrUtil.replace(id, StrUtil.DASHED, StrUtil.UNDERLINE);
			Validator.validateGeneral(checkId, 2, Const.ID_MAX_LEN, "节点id不能为空并且2-50（英文字母 、数字和下划线）");
		}
		Assert.hasText(nodeModel.getName(), "节点名称 不能为空");
		NodeModel existsNode = super.getByKey(id);
		String workspaceId;
		if (autoReg) {
			if (create) {
				Assert.isNull(existsNode, "对应的节点 id 已经存在啦");
				// 绑定到默认工作空间
				workspaceId = Const.WORKSPACE_DEFAULT_ID;
			} else {
				Assert.notNull(existsNode, "对应的节点不存在");
				workspaceId = existsNode.getWorkspaceId();
			}
		} else {
			workspaceId = this.getCheckUserWorkspace(request);
		}
		nodeModel.setWorkspaceId(workspaceId);

		//nodeModel.setProtocol(StrUtil.emptyToDefault(nodeModel.getProtocol(), "http"));
		{// 节点地址 重复
			boolean exists = this.existsByUrl(nodeModel.getUrl(), nodeModel.getWorkspaceId(), id);
			Assert.state(!exists, "对应的节点已经存在啦");
		}
		// 判断 ssh
		String sshId = nodeModel.getSshId();
		if (StrUtil.isNotEmpty(sshId)) {
			SshModel byKey = sshService.getByKey(sshId, request);
			Assert.notNull(byKey, "对应的 SSH 不存在");
			List<NodeModel> nodeBySshId = this.getNodeBySshId(sshId);
			nodeBySshId = ObjectUtil.defaultIfNull(nodeBySshId, Collections.EMPTY_LIST);
			Optional<NodeModel> any = nodeBySshId.stream().filter(nodeModel2 -> !StrUtil.equals(id, nodeModel2.getId())).findAny();
			Assert.state(!any.isPresent(), "对应的SSH已经被其他节点绑定啦");
		}
		if (nodeModel.isOpenStatus()) {
			//
			this.checkLockType(existsNode);
			this.testNode(nodeModel);
		}
		try {
			if (autoReg) {
				BaseServerController.resetInfo(UserModel.EMPTY);
			}
			if (create) {
				if (autoReg) {
					// 自动注册节点默认关闭
					nodeModel.setOpenStatus(0);
					// 默认锁定 (原因未分配工作空间)
					nodeModel.setUnLockType("unassignedWorkspace");
				}
				this.insert(nodeModel);
				// 同步项目
				ProjectInfoCacheService projectInfoCacheService = SpringUtil.getBean(ProjectInfoCacheService.class);
				projectInfoCacheService.syncNode(nodeModel);
			} else {
				this.update(nodeModel);
			}
		} finally {
			if (autoReg) {
				BaseServerController.removeEmpty();
			}
		}
	}

	/**
	 * 解锁分配工作空间
	 *
	 * @param id          节点ID
	 * @param workspaceId 工作空间
	 */
	public void unLock(String id, String workspaceId) {
		NodeModel nodeModel = super.getByKey(id);
		Assert.notNull(nodeModel, "没有对应对节点");
		//
		WorkspaceModel workspaceModel = workspaceService.getByKey(workspaceId);
		Assert.notNull(workspaceModel, "没有对应对工作空间");

		NodeModel nodeModel1 = new NodeModel();
		nodeModel1.setId(id);
		nodeModel1.setWorkspaceId(workspaceId);
		nodeModel1.setUnLockType(StrUtil.EMPTY);
		nodeModel1.setOpenStatus(1);
		super.update(nodeModel1);
	}

	private void checkLockType(NodeModel nodeModel) {
		if (nodeModel == null) {
			return;
		}
		// 判断锁定类型
		if (StrUtil.isNotEmpty(nodeModel.getUnLockType())) {
			//
			Assert.state(!StrUtil.equals(nodeModel.getUnLockType(), "unassignedWorkspace"), "当前节点还未分配工作空间,请分配");
		}
	}

	@Override
	public void insert(NodeModel nodeModel) {
		this.fillNodeInfo(nodeModel);
		super.insert(nodeModel);
		this.updateDuplicateNode(nodeModel);
	}

	@Override
	public void insertNotFill(NodeModel nodeModel) {
		nodeModel.setWorkspaceId(Const.WORKSPACE_DEFAULT_ID);
		this.fillNodeInfo(nodeModel);
		super.insertNotFill(nodeModel);
		this.updateDuplicateNode(nodeModel);
	}

	/**
	 * 填充默认字段
	 *
	 * @param nodeModel 节点
	 */
	private void fillNodeInfo(NodeModel nodeModel) {
		nodeModel.setProtocol(StrUtil.emptyToDefault(nodeModel.getProtocol(), "http"));
		nodeModel.setCycle(0);
		nodeModel.setOpenStatus(ObjectUtil.defaultIfNull(nodeModel.getOpenStatus(), 0));
	}

	@Override
	public int delByKey(String keyValue) {
		return super.delByKey(keyValue);
	}

	@Override
	public int del(Entity where) {
		return super.del(where);
	}

	@Override
	public int updateById(NodeModel info) {
		int updateById = super.updateById(info);
		if (updateById > 0) {
			this.updateDuplicateNode(info);
		}
		return updateById;
	}

	/**
	 * 更新相同节点对 授权信息
	 *
	 * @param info 节点信息
	 */
	private void updateDuplicateNode(NodeModel info) {
		if (StrUtil.hasEmpty(info.getUrl(), info.getLoginName(), info.getLoginPwd())) {
			return;
		}
		NodeModel update = new NodeModel();
		update.setLoginName(info.getLoginName());
		update.setLoginPwd(info.getLoginPwd());
		//
		NodeModel where = new NodeModel();
		where.setUrl(info.getUrl());
		int updateCount = super.update(super.dataBeanToEntity(update), super.dataBeanToEntity(where));
		if (updateCount > 1) {
			DefaultSystemLog.getLog().debug("update duplicate node {} {}", info.getUrl(), updateCount);
		}
	}

	/**
	 * 根据 url 去重
	 *
	 * @return list
	 */
	public List<NodeModel> listDeDuplicationByUrl() {
		String sql = "select url,max(loginName) as loginName,max(loginPwd) as loginPwd,max(protocol) as protocol from " + super.getTableName() + "  group  by url";
		List<Entity> query = this.query(sql);
		if (query != null) {
			return query.stream().map((entity -> this.entityToBean(entity, this.tClass))).collect(Collectors.toList());
		}
		return null;
	}

	public List<NodeModel> getNodeBySshId(String sshId) {
		NodeModel nodeModel = new NodeModel();
		nodeModel.setSshId(sshId);
		return super.listByBean(nodeModel);
	}
}
