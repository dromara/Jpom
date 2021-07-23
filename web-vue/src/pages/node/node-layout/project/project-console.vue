<template>
	<div>
		<div ref="filter" class="filter">
			<template v-if="copyId">
				<a-button :disabled="replica.status" type="primary" @click="start">启动</a-button>
				<a-button :disabled="!replica.status" type="danger" @click="restart">重启</a-button>
				<a-button :disabled="!replica.status" type="danger" @click="stop">停止</a-button>
				<a-button type="primary" @click="handleDownload">导出日志</a-button>
				<a-tag color="#87d068">文件大小: {{ project.logSize }}</a-tag>
			</template>
			<template v-else>
				<a-button :disabled="project.status" type="primary" @click="start">启动</a-button>
				<a-button :disabled="!project.status" type="danger" @click="restart">重启</a-button>
				<a-button :disabled="!project.status" type="danger" @click="stop">停止</a-button>
				<a-button type="primary" @click="handleDownload">导出日志</a-button>
				<a-button type="primary" @click="handleLogBack">备份列表</a-button>
				<a-button type="primary" @click="goFile">文件管理</a-button>
				<a-tag color="#87d068">文件大小: {{ project.logSize }}</a-tag>
			</template>
		</div>
		<!-- console -->
		<div>
			<a-input class="console" id="project-console" v-model="logContext" readOnly type="textarea" style="resize: none;"/>
		</div>
		<!-- 日志备份 -->
		<a-modal v-model="lobbackVisible" title="日志备份列表" width="850px" :footer="null" :maskClosable="false">
			<div ref="model-filter" class="filter">
				<a-tag color="orange">控制台日志路径: {{ project.log }}</a-tag>
				<a-tag color="orange">控制台日志备份路径: {{ project.logBack }}</a-tag>
			</div>
			<!-- 数据表格 -->
			<a-table :data-source="logBackList" :loading="loading" :columns="columns" :scroll="{y: 400}" :pagination="false" bordered :rowKey="(record, index) => index">
				<a-tooltip slot="filename" slot-scope="text" placement="topLeft" :title="text">
					<span>{{ text }}</span>
				</a-tooltip>
				<a-tooltip slot="fileSize" slot-scope="text" placement="topLeft" :title="text">
					<span>{{ text }}</span>
				</a-tooltip>
				<template slot="operation" slot-scope="text, record">
					<a-button type="primary" @click="handleDownloadLogback(record)">下载</a-button>
					<a-button type="danger" @click="handleDelete(record)">删除</a-button>
				</template>
			</a-table>
		</a-modal>
	</div>
</template>
<script>
import {
	getProjectData,
	getProjectLogSize,
	downloadProjectLogFile,
	getLogBackList,
	downloadProjectLogBackFile,
	deleteProjectLogBackFile
} from '../../../../api/node-project';
import {mapGetters} from 'vuex';

export default {
	props: {
		nodeId: {
			type: String
		},
		projectId: {
			type: String
		},
		replica: {
			type: Object
		},
		copyId: {
			type: String
		}
	},
	data() {
		return {
			project: {},
			loading: false,
			socket: null,
			// 日志内容
			logContext: 'choose file loading context...',
			lobbackVisible: false,
			logBackList: [],
			columns: [
				{title: '文件名称', dataIndex: 'filename', width: 150, ellipsis: true, scopedSlots: {customRender: 'filename'}},
				{title: '修改时间', dataIndex: 'modifyTime', width: 150, ellipsis: true, scopedSlots: {customRender: 'modifyTime'}},
				{title: '文件大小', dataIndex: 'fileSize', width: 100, ellipsis: true, scopedSlots: {customRender: 'fileSize'}},
				{title: '操作', dataIndex: 'operation', scopedSlots: {customRender: 'operation'}, width: 130}
			],
			heart: null
		}
	},
	computed: {
		...mapGetters([
			'getLongTermToken'
		]),
		socketUrl() {
			const protocol = location.protocol === 'https:' ? 'wss://' : 'ws://';
			const domain = window.routerBase;
			const url = (domain + '/console').replace(new RegExp('//', 'gm'), '/');
			return `${protocol}${location.host}${url}?userId=${this.getLongTermToken}&projectId=${this.projectId}&nodeId=${this.nodeId}&type=console&copyId=${this.copyId}`;
		}
	},
	mounted() {
		this.loadProject();
		this.initWebSocket();
	},
	beforeDestroy() {
		if (this.socket) {
			this.socket.close();
		}
		clearInterval(this.heart);
	},
	methods: {
		// 加载项目
		loadProject() {
			const params = {
				id: this.projectId,
				nodeId: this.nodeId
			}
			getProjectData(params).then(res => {
				if (res.code === 200) {
					this.project = res.data;
					// 加载日志文件大小
					this.loadFileSize();
				}
			})
		},
		// 初始化
		initWebSocket() {
			this.logContext = '';
			if (!this.socket || this.socket.readyState !== this.socket.OPEN || this.socket.readyState !== this.socket.CONNECTING) {
				this.socket = new WebSocket(this.socketUrl);
			}
			// 连接成功后
			this.socket.onopen = () => {
				this.sendMsg('status');
				this.sendMsg('showlog');
			}
			this.socket.onmessage = (msg) => {
				if (msg.data.indexOf('code') > -1 && msg.data.indexOf('msg') > -1) {
					const res = JSON.parse(msg.data);
					if (res.code === 200) {
						this.$notification.success({
							message: res.msg,
							duration: 3
						});
						// 如果操作是启动或者停止
						if (res.op === 'stop') {
							if (this.copyId) {
								this.replica.status = false;
							} else {
								this.project.status = false;
							}
						}
						if (res.op === 'start') {
							if (this.copyId) {
								this.replica.status = true;
							} else {
								this.project.status = true;
							}
						}
						// 如果是 status
						if (res.op === 'status') {
							if (this.copyId) {
								this.replica.status = true;
							} else {
								this.project.status = true;
							}
						}
					} else {
						this.$notification.error({
							message: res.msg,
							duration: 5
						});
						// 设置未启动
						if (this.copyId) {
							this.replica.status = false;
						} else {
							this.project.status = false;
						}
					}
				}
				this.logContext += `${msg.data}\r\n`;

				// 自动滚动到底部
				this.$nextTick(() => {
					setTimeout(() => {
						const projectConsole = document.getElementById('project-console');
						projectConsole.scrollTop = projectConsole.scrollHeight;
					}, 100);
				});
				clearInterval(this.heart);
				// 创建心跳，防止掉线
				this.heart = setInterval(() => {
					this.sendMsg('heart');
					this.loadFileSize();
				}, 5000);
			}
		},
		// 发送消息
		sendMsg(op) {
			const data = {
				op: op,
				projectId: this.projectId,
				copyId: this.copyId
			}
			this.socket.send(JSON.stringify(data));
		},
		// 加载日志文件大小
		loadFileSize() {
			const params = {
				nodeId: this.nodeId,
				id: this.projectId,
				copyId: this.copyId
			}
			getProjectLogSize(params).then(res => {
				if (res.code === 200) {
					this.project.logSize = res.data.logSize;
				}
			})
		},
		// 启动
		start() {
			this.sendMsg('start');
		},
		// 重启
		restart() {
			this.$confirm({
				title: '系统提示',
				content: '真的要重启项目么？',
				okText: '确认',
				cancelText: '取消',
				onOk: () => {
					this.sendMsg('restart');
				}
			});
		},
		// 停止
		stop() {
			this.$confirm({
				title: '系统提示',
				content: '真的要停止项目么？',
				okText: '确认',
				cancelText: '取消',
				onOk: () => {
					this.sendMsg('stop');
				}
			});
		},
		// 下载日志文件
		handleDownload() {
			this.$notification.info({
				message: '正在下载，请稍等...',
				duration: 5
			});
			// 请求参数
			const params = {
				nodeId: this.nodeId,
				id: this.projectId,
				copyId: this.copyId
			}
			// 请求接口拿到 blob
			downloadProjectLogFile(params).then(blob => {
				const url = window.URL.createObjectURL(blob);
				let link = document.createElement('a');
				link.style.display = 'none';
				link.href = url;
				if (this.copyId) {
					link.setAttribute('download', `${this.copyId}.log`);
				} else {
					link.setAttribute('download', this.project.log);
				}
				document.body.appendChild(link);
				link.click();
			})
		},
		// 日志备份列表
		handleLogBack() {
			this.loading = true;
			// 设置显示的数据
			this.detailData = [];
			this.lobbackVisible = true;
			const params = {
				nodeId: this.nodeId,
				id: this.projectId
			}
			getLogBackList(params).then(res => {
				if (res.code === 200) {
					this.logBackList = res.data.array;
				}
				this.loading = false;
			})
		},
		// 下载日志备份文件
		handleDownloadLogback(record) {
			this.$notification.info({
				message: '正在下载，请稍等...',
				duration: 5
			});
			// 请求参数
			const params = {
				nodeId: this.nodeId,
				id: this.projectId,
				copyId: this.copyId,
				key: record.filename
			}
			// 请求接口拿到 blob
			downloadProjectLogBackFile(params).then(blob => {
				const url = window.URL.createObjectURL(blob);
				let link = document.createElement('a');
				link.style.display = 'none';
				link.href = url;
				link.setAttribute('download', record.filename);
				document.body.appendChild(link);
				link.click();
			})
		},
		// 删除日志备份文件
		handleDelete(record) {
			this.$confirm({
				title: '系统提示',
				content: '真的要删除文件么？',
				okText: '确认',
				cancelText: '取消',
				onOk: () => {
					// 请求参数
					const params = {
						nodeId: this.nodeId,
						id: this.projectId,
						copyId: this.copyId,
						name: record.filename
					}
					// 删除
					deleteProjectLogBackFile(params).then((res) => {
						if (res.code === 200) {
							this.$notification.success({
								message: res.msg,
								duration: 2
							});
							this.handleLogBack();
						}
					})
				}
			});
		},
		goFile() {
			this.$emit("goFile");
		}
	}
}
</script>
<style scoped>
.filter {
	margin: 0 0 10px;
}

.ant-btn {
	margin-right: 10px;
}

.console {
	padding: 5px;
	color: #fff;
	font-size: 14px;
	background-color: black;
	width: 100%;
	height: calc(100vh - 120px);
	overflow-y: auto;
	border: 1px solid #e2e2e2;
	border-radius: 5px 5px;
}
</style>
