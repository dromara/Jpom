<template>
  <div>
    <div ref="filter" class="filter">
      <a-select v-model="listQuery.group" allowClear placeholder="请选择分组"
        class="filter-item" @change="handleFilter">
        <a-select-option v-for="group in groupList" :key="group">{{ group }}</a-select-option>
      </a-select>
      <a-button type="primary" @click="handleFilter">搜索</a-button>
      <a-button type="primary" @click="handleAdd">新增</a-button>
      <a-button type="primary" @click="loadData">刷新</a-button>
    </div>
  </div>
</template>
<script>
import {mapGetters} from 'vuex';
import {
    clearBuid,
    deleteBuild,
    editBuild,
    getBranchList,
    getBuildGroupList,
    getBuildList,
    getTriggerUrl,
    releaseMethodMap,
    resetTrigger,
    startBuild,
    stopBuild
} from '../../api/build';
import {getDishPatchList} from '../../api/dispatch';
import {getNodeProjectList} from '../../api/node'
import {getSshList} from '../../api/ssh'
import {parseTime} from '../../utils/time';

export default {
    components: {
    },
    data() {
        return {
            releaseMethodMap: releaseMethodMap,
            loading: false,
            listQuery: {},
            tableHeight: '70vh',
            groupList: [],
            list: [],
            branchList: [],
            dispatchList: [],
            cascaderList: [],
            sshList: [],
            temp: {},
            editBuildVisible: false,
            addGroupvisible: false,
            triggerVisible: false,
            buildLogVisible: false,
            afterOptList: [
                {title: '不做任何操作', value: 0},
                {title: '并发重启', value: 1},
                {title: '完整顺序重启(有重启失败将结束本次)', value: 2},
                {title: '顺序重启(有重启失败将继续)', value: 3},
            ],
            columns: [
                {title: '名称', dataIndex: 'name', width: 150, ellipsis: true, scopedSlots: {customRender: 'name'}},
                {title: '分组', dataIndex: 'group', width: 150, ellipsis: true, scopedSlots: {customRender: 'group'}},
                {
                    title: '分支',
                    dataIndex: 'branchName',
                    width: 100,
                    ellipsis: true,
                    scopedSlots: {customRender: 'branchName'}
                },
                {title: '状态', dataIndex: 'status', width: 100, ellipsis: true, scopedSlots: {customRender: 'status'}},
                {
                    title: '构建 ID',
                    dataIndex: 'buildIdStr',
                    width: 80,
                    ellipsis: true,
                    scopedSlots: {customRender: 'buildIdStr'}
                },
                {
                    title: '修改人',
                    dataIndex: 'modifyUser',
                    width: 150,
                    ellipsis: true,
                    scopedSlots: {customRender: 'modifyUser'}
                },
                {
                    title: '修改时间', dataIndex: 'modifyTime', customRender: (text) => {
                        if (!text) {
                            return '';
                        }
                        return parseTime(text);
                    }, width: 180
                },
                {
                    title: '发布方式',
                    dataIndex: 'releaseMethod',
                    width: 100,
                    ellipsis: true,
                    scopedSlots: {customRender: 'releaseMethod'}
                },
                {
                    title: '产物目录',
                    dataIndex: 'resultDirFile',
                    ellipsis: true,
                    scopedSlots: {customRender: 'resultDirFile'}
                },
                {title: '构建命令', dataIndex: 'script', ellipsis: true, scopedSlots: {customRender: 'script'}},
                {
                    title: '操作',
                    dataIndex: 'operation',
                    width: 240,
                    scopedSlots: {customRender: 'operation'},
                    align: 'left',
                    fixed: 'right'
                }
            ],
            rules: {
                name: [
                    {required: true, message: 'Please input build name', trigger: 'blur'}
                ],
                script: [
                    {required: true, message: 'Please input build script', trigger: 'blur'}
                ],
                resultDirFile: [
                    {required: true, message: 'Please input build target path', trigger: 'blur'}
                ],
                releasePath: [
                    {required: true, message: 'Please input release path', trigger: 'blur'}
                ]
            }
        }
    },
    computed: {
        ...mapGetters([
            'getGuideFlag'
        ])
    },
    watch: {
        getGuideFlag() {
            this.introGuide();
        }
    },
    created() {
        this.calcTableHeight();
        this.loadGroupList();
        this.handleFilter();
    },
    methods: {
        // 页面引导
        introGuide() {
            if (this.getGuideFlag) {
                this.$introJs().setOptions({
                    hidePrev: true,
                    steps: [{
                        title: 'Jpom 导航助手',
                        element: document.querySelector('.jpom-target-dir'),
                        intro: '可以理解为项目打包的目录。如 Jpom 项目执行 <b>mvn clean package</b> 构建命令，构建产物相对路径为：<b>modules/server/target/server-2.4.2-release</b>'
                    }]
                }).start();
                return false;
            }
            this.$introJs().exit();
        },
        // 计算表格高度
        calcTableHeight() {
            this.$nextTick(() => {
                this.tableHeight = window.innerHeight - this.$refs['filter'].clientHeight - 135;
            })
        },
        // 分组列表
        loadGroupList() {
            getBuildGroupList().then(res => {
                if (res.code === 200) {
                    this.groupList = res.data;
                }
            })
        },
        // 加载数据
        loadData() {
            this.list = [];
            this.loading = true;
            getBuildList(this.listQuery).then(res => {
                if (res.code === 200) {
                    this.list = res.data;
                }
                this.loading = false;
            })
        },
        // 加载节点分发列表
        loadDispatchList() {
            this.dispatchList = [];
            getDishPatchList().then(res => {
                if (res.code === 200) {
                    this.dispatchList = res.data;
                }
            })
        },
        // 加载节点项目列表
        loadNodeProjectList() {
            this.cascaderList = [];
            getNodeProjectList().then(res => {
                if (res.code === 200) {
                    res.data.forEach(node => {
                        const nodeItem = {
                            label: node.name,
                            value: node.id,
                            children: []
                        }
                        node.projects.forEach(project => {
                            const projectItem = {
                                label: project.name,
                                value: project.id
                            }
                            nodeItem.children.push(projectItem);
                        })
                        this.cascaderList.push(nodeItem)
                    })
                }
            })
        },
        // 加载 SSH 列表
        loadSshList() {
            this.sshList = [];
            getSshList().then(res => {
                if (res.code === 200) {
                    this.sshList = res.data;
                }
            })
        },
        // 筛选
        handleFilter() {
            this.loadData();
        },
        // 添加
        handleAdd() {
            this.temp = {};
            this.branchList = [];
            this.loadDispatchList();
            this.loadNodeProjectList();
            this.loadSshList();
            this.editBuildVisible = true;
            this.$nextTick(() => {
                setTimeout(() => {
                    this.introGuide();
                }, 500);
            })
        },
        // 修改
        handleEdit(record) {
            this.temp = Object.assign(record);
            this.temp.tempGroup = '';
            // 设置发布方式的数据
            if (record.releaseMethodDataId) {
                if (record.releaseMethod === 1) {
                    this.temp.releaseMethodDataId_1 = record.releaseMethodDataId;
                }
                if (record.releaseMethod === 2) {
                    this.temp.releaseMethodDataIdList = record.releaseMethodDataId.split(':');
                }
                if (record.releaseMethod === 3) {
                    this.temp.releaseMethodDataId_3 = record.releaseMethodDataId;
                }
            }
            this.loadBranchList();
            this.loadDispatchList();
            this.loadNodeProjectList();
            this.loadSshList();
            this.editBuildVisible = true;
        },
        // 添加分组
        handleAddGroup() {
            if (!this.temp.tempGroup || this.temp.tempGroup.length === 0) {
                this.$notification.warning({
                    message: '分组名称不能为空',
                    duration: 2
                });
                return false;
            }
            // 添加到分组列表
            if (this.groupList.indexOf(this.temp.tempGroup) === -1) {
                this.groupList.push(this.temp.tempGroup);
            }
            this.temp.tempGroup = '';
            this.$notification.success({
                message: '添加成功',
                duration: 2
            });
            this.addGroupvisible = false;
        },
        // 获取仓库分支
        loadBranchList() {
            const loading = this.$loading.service({
                lock: true,
                text: '正在加载项目分支',
                spinner: 'el-icon-loading',
                background: 'rgba(0, 0, 0, 0.7)'
            });
            this.branchList = [];
            const params = {
                url: this.temp.gitUrl,
                userName: this.temp.userName,
                userPwd: this.temp.password
            }
            getBranchList(params).then(res => {
                if (res.code === 200) {
                    this.branchList = res.data;
                }
                loading.close();
            })
        },
        // 提交节点数据
        handleEditBuildOk() {
            // 检验表单
            this.$refs['editBuildForm'].validate((valid) => {
                if (!valid) {
                    return false;
                }
                // 设置参数
                if (this.temp.releaseMethod === 2) {
                    if (this.temp.releaseMethodDataIdList.length < 2) {
                        this.$notification.warn({
                            message: '请选择节点项目',
                            duration: 2
                        });
                        return false;
                    }
                    this.temp.releaseMethodDataId_2_node = this.temp.releaseMethodDataIdList[0];
                    this.temp.releaseMethodDataId_2_project = this.temp.releaseMethodDataIdList[1];
                }
                // 提交数据
                editBuild(this.temp).then(res => {
                    if (res.code === 200) {
                        // 成功
                        this.$notification.success({
                            message: res.msg,
                            duration: 2
                        });
                        this.$refs['editBuildForm'].resetFields();
                        this.editBuildVisible = false;
                        this.handleFilter();
                        this.loadGroupList();
                    }
                })
            })
        },
        // 删除
        handleDelete(record) {
            this.$confirm({
                title: '系统提示',
                content: '真的要删除构建信息么？',
                okText: '确认',
                cancelText: '取消',
                onOk: () => {
                    // 删除
                    deleteBuild(record.id).then((res) => {
                        if (res.code === 200) {
                            this.$notification.success({
                                message: res.msg,
                                duration: 2
                            });
                            this.loadData();
                        }
                    })
                }
            });
        },
        // 触发器
        handleTrigger(record) {
            this.temp = Object.assign(record);
            getTriggerUrl(record.id).then(res => {
                if (res.code === 200) {
                    this.temp.triggerBuildUrl = `${location.protocol}${location.host}${res.data.triggerBuildUrl}`;
                    this.triggerVisible = true;
                }
            })
        },
        // 重置触发器
        resetTrigger() {
            resetTrigger(this.temp.id).then(res => {
                if (res.code === 200) {
                    this.$notification.success({
                        message: res.msg,
                        duration: 2
                    });
                    this.triggerVisible = false;
                    this.handleTrigger(this.temp);
                }
            })
        },
        // 清除构建
        handleClear(record) {
            this.$confirm({
                title: '系统提示',
                content: '真的要清除构建信息么？',
                okText: '确认',
                cancelText: '取消',
                onOk: () => {
                    clearBuid(record.id).then((res) => {
                        if (res.code === 200) {
                            this.$notification.success({
                                message: res.msg,
                                duration: 2
                            });
                            this.loadData();
                        }
                    })
                }
            });
        },
        // 开始构建
        handleStartBuild(record) {
            this.temp = Object.assign(record);
            startBuild(this.temp.id).then(res => {
                if (res.code === 200) {
                    this.$notification.success({
                        message: res.msg,
                        duration: 2
                    });
                    this.handleFilter();
                    // 自动打开构建日志
                    this.handleBuildLog({
                        id: this.temp.id,
                        buildId: res.data
                    })
                }
            })
        },
        // 停止构建
        handleStopBuild(record) {
            this.temp = Object.assign(record);
            stopBuild(this.temp.id).then(res => {
                if (res.code === 200) {
                    this.$notification.success({
                        message: res.msg,
                        duration: 2
                    });
                    this.handleFilter();
                }
            })
        },
        // 查看构建日志
        handleBuildLog(record) {
            this.temp = {
                id: record.id,
                buildId: record.buildId
            }
            this.buildLogVisible = true;
        },
        // 关闭日志对话框
        closeBuildLogModel() {
            this.handleFilter();
        }
    }
}
</script>
<style scoped>
.filter {
    margin-bottom: 10px;
}

.ant-btn {
    margin-right: 10px;
}

.filter-item {
    width: 150px;
    margin-right: 10px;
}

.btn-add {
    margin-left: 10px;
    margin-right: 0;
}
</style>
