package io.jpom.service.node;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.common.BaseServerController;
import io.jpom.common.Const;
import io.jpom.common.JpomManifest;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.Cycle;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.SshModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.data.WorkspaceModel;
import io.jpom.monitor.NodeMonitor;
import io.jpom.service.h2db.BaseWorkspaceService;
import io.jpom.service.node.ssh.SshService;
import io.jpom.service.system.WorkspaceService;
import io.jpom.util.StringUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author bwcx_jzy
 * @since 2021/12/4
 */
@Service
public class NodeService extends BaseWorkspaceService<NodeModel> {

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
			boolean general = StringUtil.isGeneral(id, 2, Const.ID_MAX_LEN);
			Assert.state(general, "节点id不能为空并且2-20（英文字母 、数字和下划线）");
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
		//		Entity entity = Entity.create();
		//		entity.set("url", nodeModel.getUrl());
		//		entity.set("workspaceId", workspaceId);
		//		if (StrUtil.isNotEmpty(id)) {
		//			entity.set("id", StrUtil.format(" <> {}", id));
		//		}
		//		boolean exists = super.exists(entity);
		//		Assert.state(!exists, "对应的节点已经存在啦");
		nodeModel.setProtocol(StrUtil.emptyToDefault(nodeModel.getProtocol(), "http"));
		{// 节点地址 重复
			NodeModel nodeModel1 = new NodeModel();
			nodeModel1.setUrl(nodeModel.getUrl());
			nodeModel1.setWorkspaceId(workspaceId);
			List<NodeModel> nodeModels = ObjectUtil.defaultIfNull(super.listByBean(nodeModel1), Collections.EMPTY_LIST);
			Optional<NodeModel> any = nodeModels.stream().filter(nodeModel2 -> !StrUtil.equals(id, nodeModel2.getId())).findAny();
			Assert.state(!any.isPresent(), "对应的节点已经存在啦");
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
			//
			int timeOut = ObjectUtil.defaultIfNull(nodeModel.getTimeOut(), 0);
			// 检查是否可用默认为5秒，避免太长时间无法连接一直等待
			nodeModel.setTimeOut(5);
			JpomManifest jpomManifest = NodeForward.requestData(nodeModel, NodeUrl.Info, request, JpomManifest.class);
			Assert.notNull(jpomManifest, "节点连接失败，请检查节点是否在线");
			nodeModel.setTimeOut(timeOut);
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
				BaseServerController.remove();
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
		super.insert(nodeModel);
		Integer cycle = nodeModel.getCycle();
		if (nodeModel.isOpenStatus() && cycle != null && cycle != Cycle.none.getCode()) {
			NodeMonitor.start();
		}
	}

	@Override
	public void insertNotFill(NodeModel nodeModel) {
		nodeModel.setWorkspaceId(Const.WORKSPACE_DEFAULT_ID);
		super.insertNotFill(nodeModel);
		Integer cycle = nodeModel.getCycle();
		if (nodeModel.isOpenStatus() && cycle != null && cycle != Cycle.none.getCode()) {
			NodeMonitor.start();
		}
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
	public int update(NodeModel nodeModel) {
		int update = super.update(nodeModel);
		this.checkCronStatus();
		return update;
	}

	@Override
	public int updateById(NodeModel info) {
		int updateById = super.updateById(info);
		this.checkCronStatus();
		return updateById;
	}

	public boolean checkCronStatus() {
		// 关闭监听
		Entity entity = Entity.create();
		entity.set("openStatus", 1);
		entity.set("cycle", StrUtil.format(" <> {}", Cycle.none.getCode()));
		long count = super.count(entity);
		if (count <= 0) {
			NodeMonitor.stop();
			return false;
		}
		NodeMonitor.start();
		return true;
	}

	/**
	 * 根据周期获取list
	 *
	 * @param cycle 周期
	 * @return list
	 */
	public List<NodeModel> listByCycle(Cycle cycle) {
		NodeModel nodeModel = new NodeModel();
		nodeModel.setCycle(cycle.getCode());
		nodeModel.setOpenStatus(1);
		List<NodeModel> list = this.listByBean(nodeModel);
		if (list == null) {
			return new ArrayList<>();
		}
		return list;
	}

	public List<NodeModel> getNodeBySshId(String sshId) {
		NodeModel nodeModel = new NodeModel();
		nodeModel.setSshId(sshId);
		return super.listByBean(nodeModel);
	}
}
