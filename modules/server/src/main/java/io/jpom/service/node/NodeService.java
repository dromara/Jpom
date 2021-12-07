package io.jpom.service.node;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.extra.servlet.ServletUtil;
import io.jpom.common.JpomManifest;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.Cycle;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.SshModel;
import io.jpom.model.data.WorkspaceModel;
import io.jpom.monitor.NodeMonitor;
import io.jpom.service.h2db.BaseWorkspaceService;
import io.jpom.service.node.ssh.SshService;
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

	public NodeService(SshService sshService) {
		this.sshService = sshService;
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
	public void update(HttpServletRequest request) {
		String type = request.getParameter("type");
		boolean create = "add".equalsIgnoreCase(type);
		// 创建对象
		NodeModel nodeModel = ServletUtil.toBean(request, NodeModel.class, true);
		String id = nodeModel.getId();
		if (StrUtil.isNotEmpty(id)) {
			boolean general = StringUtil.isGeneral(id, 2, 20);
			Assert.state(general, "节点id不能为空并且2-20（英文字母 、数字和下划线）");
		}
		Assert.hasText(nodeModel.getName(), "节点名称 不能为空");
		// 节点地址 重复
		String workspaceId = this.getCheckUserWorkspace(request);
		//		Entity entity = Entity.create();
		//		entity.set("url", nodeModel.getUrl());
		//		entity.set("workspaceId", workspaceId);
		//		if (StrUtil.isNotEmpty(id)) {
		//			entity.set("id", StrUtil.format(" <> {}", id));
		//		}
		//		boolean exists = super.exists(entity);
		//		Assert.state(!exists, "对应的节点已经存在啦");
		{
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
		}
		if (nodeModel.isOpenStatus()) {
			int timeOut = nodeModel.getTimeOut();
			// 检查是否可用默认为5秒，避免太长时间无法连接一直等待
			nodeModel.setTimeOut(5);
			JpomManifest jpomManifest = NodeForward.requestData(nodeModel, NodeUrl.Info, request, JpomManifest.class);
			Assert.notNull(jpomManifest, "节点连接失败，请检查节点是否在线");
			nodeModel.setTimeOut(timeOut);
		}
		if (create) {
			this.insert(nodeModel);
		} else {
			this.update(nodeModel);
		}
	}

	@Override
	public void insert(NodeModel nodeModel) {
		super.insert(nodeModel);
		if (nodeModel.isOpenStatus() && nodeModel.getCycle() != Cycle.none.getCode()) {
			NodeMonitor.start();
		}
	}

	@Override
	public void insertNotFill(NodeModel nodeModel) {
		nodeModel.setWorkspaceId(WorkspaceModel.DEFAULT_ID);
		super.insertNotFill(nodeModel);
		if (nodeModel.isOpenStatus() && nodeModel.getCycle() != Cycle.none.getCode()) {
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
}
