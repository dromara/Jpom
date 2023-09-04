<template>
  <!-- 布局 -->
  <div>
    <a-alert :message=$t('node.node_layout.nginx.list.alert') banner />
    <a-layout class="file-layout node-full-content">
      <!-- 目录树 -->
      <a-layout-sider theme="light" class="nginx-sider" width="25%">
        <a-empty v-if="treeList.length === 0" />
        <a-directory-tree :treeData="treeList" :replace-fields="defaultProps" @select="nodeClick"></a-directory-tree>
      </a-layout-sider>
      <!-- 表格 -->
      <a-layout-content class="file-content">
        <a-table size="middle" :data-source="fileList" :loading="loading" :columns="columns" :pagination="false" bordered :rowKey="(record, index) => index">
          <template slot="title">
            <a-space>
              <div>
                {{$t('common.query')}}
                <a-switch v-model="listData.showAll" :checked-children=$t('common.showAll') :un-checked-children=$t('common.showNorm') />
              </div>
              <a-button size="small" type="primary" @click="handleFilter">{{ $t('common.refresh') }}</a-button>
              <a-button size="small" type="primary" @click="handleAdd">{{ $t('common.addConfig') }}</a-button>
              ｜
              <a-switch v-model="nginxData.status" :checked-children=$t('common.able') :un-checked-children=$t('common.disable') disabled />
              <a-dropdown>
                <a class="ant-dropdown-link" @click="(e) => e.preventDefault()">
                  {{$t('common.moreOper')}}
                  <a-icon type="down" />
                </a>
                <a-menu slot="overlay">
                  <a-menu-item>
                    <a-button type="primary" @click="handleEditNginx">{{$t('node.node_layout.nginx.list.editNginx')}}</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button :disabled="nginxData.status" type="primary" @click="handleNginxCommand('open')">{{$t('node.node_layout.nginx.list.openNginx')}}</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button :disabled="!nginxData.status" type="danger" @click="handleNginxCommand('reload')">{{$t('node.node_layout.nginx.list.reloadNginx')}}</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button :disabled="!nginxData.status" type="danger" @click="handleNginxCommand('close')">{{$t('node.node_layout.nginx.list.closeNginx')}}</a-button>
                  </a-menu-item>
                </a-menu>
              </a-dropdown>
              <a-tooltip>
                <template slot="title">
                  <div>{{$t('node.node_layout.nginx.list.nginxManage')}}</div>

                  <div>
                    <ul>
                      <li>{{$t('node.node_layout.nginx.list.li_1')}}</li>
                      <li>{{$t('node.node_layout.nginx.list.li_2')}} <b>/usr/local/nginx/sbin/nginx</b></li>
                      <li>{{$t('node.node_layout.nginx.list.li_3')}}</li>
                    </ul>
                  </div>
                </template>
                <a-icon type="question-circle" theme="filled" />
              </a-tooltip>
            </a-space>
          </template>
          <a-tooltip slot="name" slot-scope="text, record" placement="topLeft" :title=$t('node.node_layout.nginx.list.handleEdit')>
            <div @click="handleEdit(record)" :style="`${record.name.endsWith('.conf') ? 'color: blue' : ''}`">
              <a-icon v-if="record.name.endsWith('.conf')" type="edit" />
              <a-icon v-else type="eye" />
              <span>{{ text }}</span>
            </div>
          </a-tooltip>
          <!-- <a-tooltip slot="isDirectory" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text ? "目录" : "文件" }}</span>
        </a-tooltip> -->
          <a-tooltip slot="serverName" slot-scope="text, record" placement="topLeft" :title="record.serverName || record.server_name || ''">
            <span>{{ record.serverName || record.server_name || "" }}</span>
          </a-tooltip>
          <a-tooltip slot="location" slot-scope="text, record" placement="topLeft" :title="record.location">
            <span>{{ record.location }}</span>
          </a-tooltip>
          <a-tooltip slot="time" slot-scope="text" placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
          <template slot="operation" slot-scope="text, record">
            <!-- <a-button type="primary" @click="handleEdit(record)">编辑</a-button> -->
            <a-popover :title=$t('common.deleteConfirm') v-if="record.name.endsWith('.conf')">
              <template slot="content">
                <p><a-button size="small" type="danger" @click="handleDelete(record, 'temp', 'none')">{{ $t('common.tempDelete') }}</a-button></p>
                <p><a-button size="small" type="danger" @click="handleDelete(record, 'real', 'none')">{{ $t('common.permDelete')}}</a-button></p>
              </template>
              <a-button size="small" type="danger">{{$t('common.delete')}}</a-button>
            </a-popover>
            <a-popover :title=$t('common.restore') v-else>
              <template slot="content">
                <p><a-button size="small" type="danger" @click="handleDelete(record, 'temp', 'back')">{{ $t('common.restoreConfig')}}</a-button></p>
                <p><a-button size="small" type="danger" @click="handleDelete(record, 'real', 'back')">{{ $t('common.permDelete')}}</a-button></p>
              </template>
              <a-button size="small" type="danger">{{$t('common.restore')}}</a-button>
            </a-popover>
          </template>
        </a-table>
        <!-- 编辑区 -->
        <a-modal destroyOnClose v-model="editNginxVisible" :title=$t('node.node_layout.nginx.list.editNginxConfig') @ok="handleEditNginxOk" :maskClosable="false" width="70vw">
          <a-form-model ref="editNginxForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 18 }">
            <a-form-model-item :label=$t('node.node_layout.nginx.list.whitePath') prop="whitePath">
              <a-select v-model="temp.whitePath" :placeholder=$t('node.node_layout.nginx.list.chooseWhitePath')>
                <a-select-option v-for="element in whiteList" :key="element">{{ element }}</a-select-option>
              </a-select>
            </a-form-model-item>
            <a-form-model-item :label=$t('common.fileName') prop="name">
              <a-input v-model="temp.name" :placeholder=$t('node.node_layout.nginx.list.fileNameMessage') />
            </a-form-model-item>
            <a-form-model-item :label=$t('common.configContent') prop="context">
              <code-editor v-model="temp.context" :options="{ mode: 'nginx' }" style="resize: none; height: 40vh"></code-editor>
              <!-- <a-input v-model="temp.context" type="textarea" :rows="10" style="resize: none; height: 40vh" placeholder="配置内容" /> -->
            </a-form-model-item>
          </a-form-model>
        </a-modal>
        <!-- 编辑 Nginx 服务名 -->
        <a-modal destroyOnClose v-model="editNginxNameVisible" :title=$t('node.node_layout.nginx.list.editMessage') @ok="handleEditNginxNameOk" :maskClosable="false" width="500px">
          <a-form-model ref="editNginxNameForm" :rules="rules" :model="nginxData" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
            <a-form-model-item :label=$t('common.serviceName') prop="name">
              <a-input v-model="nginxData.name" :placeholder=$t('node.node_layout.nginx.list.serviceNameMessage') />
            </a-form-model-item>
          </a-form-model>
        </a-modal>
      </a-layout-content>
    </a-layout>
  </div>
</template>
<script>
import { deleteNginxConfig, doNginxCommand, editNginxConfig, editNginxServerName, getNginxDirectoryList, getNginxFileList, loadNginxConfig, loadNginxData, loadNginxWhiteList } from "@/api/node-nginx";

import codeEditor from "@/components/codeEditor";

export default {
  components: {
    codeEditor,
  },
  props: {
    node: {
      type: Object,
    },
  },
  data() {
    return {
      loading: false,
      whiteList: [],
      treeList: [],
      fileList: [],
      nginxData: {},
      temp: {},
      tempNode: {},
      listData: {
        showAll: false,
      },
      // tableHeight: "80vh",
      editNginxVisible: false,
      editNginxNameVisible: false,
      defaultProps: {
        // children: "children",
        title: "title",
      },
      columns: [
        { title: this.$t('common.fileName'), dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "name" } },
        // { title: "文件类型", dataIndex: "isDirectory", width: 100, ellipsis: true, scopedSlots: { customRender: "isDirectory" } },
        // { title: "数量", dataIndex: "serverCount", width: 80, ellipsis: true },
        { title: this.$t('common.domain'), dataIndex: "serverName", width: 140, ellipsis: true, scopedSlots: { customRender: "serverName" } },
        { title: "location", dataIndex: "location", width: 140, ellipsis: true, scopedSlots: { customRender: "location" } },
        { title: this.$t('common.modifyTime'), dataIndex: "time", width: 140, ellipsis: true, scopedSlots: { customRender: "time" } },
        { title: this.$t('common.operation'), dataIndex: "operation", scopedSlots: { customRender: "operation" }, width: 60 },
      ],
      rules: {
        name: [{ required: true, message: "Please input name", trigger: "blur" }],
        whitePath: [{ required: true, message: "Please select item", trigger: "blur" }],
        context: [{ required: true, message: "Please input context", trigger: "blur" }],
      },
    };
  },
  mounted() {
    // this.calcTableHeight();
    this.handleFilter();
  },
  methods: {
    // // 计算表格高度
    // calcTableHeight() {
    //   this.tableHeight = window.innerHeight - this.$refs["filter"].clientHeight - 175;
    // },
    // 加载 Nginx 数据
    loadNginxData() {
      const params = {
        nodeId: this.node.id,
      };
      loadNginxData(params).then((res) => {
        if (res.code === 200) {
          this.nginxData = res.data;
        }
      });
    },
    // 加载目录数据
    loadDirectoryList() {
      this.treeList = [];
      this.loading = true;
      // 请求参数
      const params = {
        nodeId: this.node.id,
      };
      getNginxDirectoryList(params).then((res) => {
        if (res.code === 200) {
          // // 添加一个唯一标识
          // const list = res.data.map((item, i) => {
          //   return item;
          // });
          this.treeList = res.data;
        }
        // 取出第一个默认选中
        this.$nextTick(() => {
          const node = this.treeList[0];
          if (node) {
            this.tempNode = node;
            this.loadFileList();
          }
        });
        this.loading = false;
      });
    },
    // 点击树节点
    nodeClick(selectedKeys, { node }) {
      if (node.dataRef) {
        this.tempNode = node.dataRef;
        this.loadFileList();
      }
    },
    // 刷新
    handleFilter() {
      this.loadDirectoryList();
      this.loadNginxData();
      this.loadNginxWhiteList();
    },
    // 加载文件列表
    loadFileList() {
      this.fileList = [];
      this.loading = true;
      // 请求参数
      const params = {
        nodeId: this.node.id,
        whitePath: this.tempNode.whitePath,
        name: this.tempNode.path,
        showAll: this.listData.showAll,
      };
      // 加载文件
      getNginxFileList(params).then((res) => {
        if (res.code === 200) {
          // 区分目录和文件
          res.data.forEach((element) => {
            if (!element.isDirectory) {
              // 设置文件表格
              this.fileList.push({
                ...element,
              });
            }
          });
        }
        this.loading = false;
      });
    },
    // 加载 Nginx 白名单列表
    loadNginxWhiteList() {
      const params = {
        nodeId: this.node.id,
      };
      loadNginxWhiteList(params).then((res) => {
        if (res.code === 200) {
          this.whiteList = res.data;
        }
      });
    },
    // 添加
    handleAdd() {
      this.temp = {};
      this.editNginxVisible = true;
    },
    // 修改
    handleEdit(record) {
      const params = {
        nodeId: this.node.id,
        path: record.path,
        name: record.relativePath,
      };
      loadNginxConfig(params).then((res) => {
        if (res.code === 200) {
          this.temp = res.data;
          this.editNginxVisible = true;
        }
      });
    },
    // 提交 Nginx 数据
    handleEditNginxOk() {
      // 检验表单
      this.$refs["editNginxForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        this.temp.nodeId = this.node.id;
        // 提交数据
        editNginxConfig(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });
            this.$refs["editNginxForm"].resetFields();
            this.editNginxVisible = false;
            //this.loadDirectoryList();
            this.loadFileList();
          }
        });
      });
    },
    // 删除
    handleDelete(record, type, from) {
      let msg;
      if (from === "back") {
        msg = this.$t('node.node_layout.nginx.list.reallyWant') + (type === "real" ? this.$t('node.node_layout.nginx.list.ifPermDelete') : this.$t('node.node_layout.nginx.list.ifRestoreConfig'));
      } else {
        msg = this.$t('node.node_layout.nginx.list.reallyWant') + (type === "real" ? this.$t('common.perm') : this.$t('common.temp')) + this.$t('node.node_layout.nginx.list.ifDelete');
      }
      this.$confirm({
        title: this.$t('common.systemPrompt'),
        content: msg,
        okText: this.$t('common.confirm'),
        cancelText: this.$t('common.cancel'),
        onOk: () => {
          // 请求参数
          const params = {
            nodeId: this.node.id,
            path: record.path,
            name: record.relativePath,
            type: type,
            from: from,
          };
          // 删除
          deleteNginxConfig(params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              //this.loadDirectoryList();
              this.loadFileList();
            }
          });
        },
      });
    },
    // 加载编辑 Nginx 服务名称窗口
    handleEditNginx() {
      this.editNginxNameVisible = true;
    },
    // 编辑 Nginx 服务名称
    handleEditNginxNameOk() {
      // 检验表单
      this.$refs["editNginxNameForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        const params = {
          nodeId: this.node.id,
          name: this.nginxData.name,
        };
        editNginxServerName(params).then((res) => {
          if (res.code === 200) {
            this.$notification.success({
              message: res.msg,
            });
            this.$refs["editNginxNameForm"].resetFields();
            this.editNginxNameVisible = false;
            this.loadNginxData();
          }
        });
      });
    },
    // 执行 Nginx 命令
    handleNginxCommand(command) {
      const params = {
        nodeId: this.node.id,
        command,
      };
      doNginxCommand(params).then((res) => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
          });
          this.loadNginxData();
        }
      });
    },
  },
};
</script>
<style scoped>
.file-layout {
  padding: 0;
  margin: 0;
}
.nginx-sider {
  border: 1px solid #e2e2e2;
  /* height: calc(100vh - 130px); */
  /* overflow-y: auto; */
  overflow-x: auto;
}
.file-content {
  /* height: calc(100vh - 150px); */
  /* overflow-y: hidden; */
  /* margin: 10px 10px 0; */
  padding: 10px;
  background-color: #fff;
}
</style>
