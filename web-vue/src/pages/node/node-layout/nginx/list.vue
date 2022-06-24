<template>
  <!-- 布局 -->

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
              查询：
              <a-switch v-model="listData.showAll" checked-children="显示所有" un-checked-children="显示正常" />
            </div>
            <a-button size="small" type="primary" @click="handleFilter">刷新</a-button>
            <a-button size="small" type="primary" @click="handleAdd">新增配置</a-button>
            ｜
            <a-switch v-model="nginxData.status" checked-children="运行中" un-checked-children="未运行" disabled />
            <a-dropdown>
              <a class="ant-dropdown-link" @click="(e) => e.preventDefault()">
                更多操作
                <a-icon type="down" />
              </a>
              <a-menu slot="overlay">
                <a-menu-item>
                  <a-button type="primary" @click="handleEditNginx">编辑 Nginx 服务</a-button>
                </a-menu-item>
                <a-menu-item>
                  <a-button :disabled="nginxData.status" type="primary" @click="handleNginxCommand('open')">启动 Nginx</a-button>
                </a-menu-item>
                <a-menu-item>
                  <a-button :disabled="!nginxData.status" type="danger" @click="handleNginxCommand('reload')">重新加载 Nginx</a-button>
                </a-menu-item>
                <a-menu-item>
                  <a-button :disabled="!nginxData.status" type="danger" @click="handleNginxCommand('close')">停止 Nginx</a-button>
                </a-menu-item>
              </a-menu>
            </a-dropdown>
            <a-tooltip>
              <template slot="title">
                <div>nginx 管理是指在想编辑配置文件，并自动重新加载(reload)</div>

                <div>
                  <ul>
                    <li>linux 服务器默认执行 nginx -s reload 、service xxxx start、service xxxx top</li>
                    <li>linux 服务器如果为编译安装则需要将 nginx 服务名称配置到 nginx执行文件的绝对路径，如 <b>/usr/local/nginx/sbin/nginx</b></li>
                    <li>windows 服务器是需要提前安装 nginx 并配置服务,默认执行 net start xxxx、net stop xxxx、net、sc query xxxx</li>
                  </ul>
                </div>
              </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </a-space>
        </template>
        <a-tooltip slot="name" slot-scope="text, record" placement="topLeft" :title="`名称：${text}  server 节点数 ${record.serverCount}`">
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
          <a-popover title="删除确认" v-if="record.name.endsWith('.conf')">
            <template slot="content">
              <p><a-button size="small" type="danger" @click="handleDelete(record, 'temp', 'none')">临时删除</a-button></p>
              <p><a-button size="small" type="danger" @click="handleDelete(record, 'real', 'none')">永久删除</a-button></p>
            </template>
            <a-button size="small" type="danger">删除</a-button>
          </a-popover>
          <a-popover title="还原" v-else>
            <template slot="content">
              <p><a-button size="small" type="danger" @click="handleDelete(record, 'temp', 'back')">还原配置</a-button></p>
              <p><a-button size="small" type="danger" @click="handleDelete(record, 'real', 'back')">永久删除</a-button></p>
            </template>
            <a-button size="small" type="danger">还原</a-button>
          </a-popover>
        </template>
      </a-table>
      <!-- 编辑区 -->
      <a-modal v-model="editNginxVisible" title="编辑 Nginx 配置文件" @ok="handleEditNginxOk" :maskClosable="false" width="70vw">
        <a-form-model ref="editNginxForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 18 }">
          <a-form-model-item label="白名单路径" prop="whitePath">
            <a-select
              :getPopupContainer="
                (triggerNode) => {
                  return triggerNode.parentNode || document.body;
                }
              "
              v-model="temp.whitePath"
              placeholder="请选择白名单路径"
            >
              <a-select-option v-for="element in whiteList" :key="element">{{ element }}</a-select-option>
            </a-select>
          </a-form-model-item>
          <a-form-model-item label="文件名称" prop="name">
            <a-input v-model="temp.name" placeholder="需要以 .conf 结尾" />
          </a-form-model-item>
          <a-form-model-item label="配置内容" prop="context">
            <code-editor v-model="temp.context" :options="{ mode: 'nginx' }" style="resize: none; height: 40vh"></code-editor>
            <!-- <a-input v-model="temp.context" type="textarea" :rows="10" style="resize: none; height: 40vh" placeholder="配置内容" /> -->
          </a-form-model-item>
        </a-form-model>
      </a-modal>
      <!-- 编辑 Nginx 服务名 -->
      <a-modal v-model="editNginxNameVisible" title="编辑 Nginx 服务名称" @ok="handleEditNginxNameOk" :maskClosable="false" width="500px">
        <a-form-model ref="editNginxNameForm" :rules="rules" :model="nginxData" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-form-model-item label="服务名称" prop="name">
            <a-input v-model="nginxData.name" placeholder="请输入 Nginx 服务名称" />
          </a-form-model-item>
        </a-form-model>
      </a-modal>
    </a-layout-content>
  </a-layout>
</template>
<script>
import {deleteNginxConfig, doNginxCommand, editNginxConfig, editNginxServerName, getNginxDirectoryList, getNginxFileList, loadNginxConfig, loadNginxData, loadNginxWhiteList} from "@/api/node-nginx";

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
        { title: "文件名称", dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "name" } },
        // { title: "文件类型", dataIndex: "isDirectory", width: 100, ellipsis: true, scopedSlots: { customRender: "isDirectory" } },
        // { title: "数量", dataIndex: "serverCount", width: 80, ellipsis: true },
        { title: "域名", dataIndex: "serverName", width: 140, ellipsis: true, scopedSlots: { customRender: "serverName" } },
        { title: "location", dataIndex: "location", width: 140, ellipsis: true, scopedSlots: { customRender: "location" } },
        { title: "修改时间", dataIndex: "time", width: 140, ellipsis: true, scopedSlots: { customRender: "time" } },
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, width: 60 },
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
        msg = "真的要" + (type === "real" ? "永久删除文件么？" : "还原配置文件么？");
      } else {
        msg = "真的要" + (type === "real" ? "永久" : "临时") + "删除文件么？";
      }
      this.$confirm({
        title: "系统提示",
        content: msg,
        okText: "确认",
        cancelText: "取消",
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
