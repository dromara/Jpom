<template>
  <div>
    <div v-show="viewList">
      <a-table size="middle" :data-source="backupListData.list" :loading="backupListLoading" :columns="columns" :pagination="false" bordered :rowKey="(record, index) => index">
        <template v-if="backupListData.path" #title> 备份文件存储目录：{{ backupListData.path }} </template>
        <a-tooltip slot="filename" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <a-tooltip slot="fileSize" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <template slot="operation" slot-scope="text, record">
          <a-space>
            <a-button size="small" type="primary" @click="handleBackupFile(record)">详情</a-button>
            <a-button size="small" type="danger" @click="handlBackupeDelete(record)">删除</a-button>
          </a-space>
        </template>
      </a-table>
    </div>
    <!-- 布局 -->
    <a-layout v-show="!viewList" class="file-layout node-full-content">
      <!-- 目录树 -->
      <a-layout-sider theme="light" class="sider" width="25%">
        <div class="dir-container">
          <a-space>
            <a-button
              size="small"
              type="primary"
              @click="
                () => {
                  viewList = true;
                }
              "
              >返回列表
            </a-button>
            <a-button size="small" type="primary" @click="loadData">刷新目录</a-button>
          </a-space>
        </div>

        <a-directory-tree :replace-fields="treeReplaceFields" @select="nodeClick" :loadData="onTreeData" :treeData="treeList"></a-directory-tree>
      </a-layout-sider>
      <!-- 表格 -->
      <a-layout-content class="file-content">
        <a-table :data-source="fileList" size="middle" :loading="loading" :columns="fileColumns" :pagination="false" bordered :rowKey="(record, index) => index">
          <template slot="title">
            <a-popconfirm
              :title="`${uploadPath ? '将还原【' + uploadPath + '】目录,' : ''} 请选择还原方式,清空还原将会先删除项目目录中的文件再将对应备份文件恢复至当前目录`"
              okText="覆盖还原"
              cancelText="清空还原"
              @confirm="recoverNet('', uploadPath)"
              @cancel="recoverNet('clear', uploadPath)"
            >
              <a-icon slot="icon" type="question-circle-o" style="color: red" />
              <!-- @click="recoverPath(uploadPath)" -->
              <a-button size="small" type="primary">还原</a-button>
            </a-popconfirm>

            <a-space>
              <a-tag color="#2db7f5" v-if="uploadPath">当前目录: {{ uploadPath || "" }}</a-tag>
            </a-space>
          </template>
          <a-tooltip slot="filename" slot-scope="text" placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
          <a-tooltip slot="isDirectory" slot-scope="text" placement="topLeft" :title="text">
            <span>{{ text ? "目录" : "文件" }}</span>
          </a-tooltip>
          <a-tooltip slot="fileSize" slot-scope="text" placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
          <template slot="operation" slot-scope="text, record">
            <a-space>
              <template v-if="record.isDirectory">
                <a-tooltip title="不能下载目录">
                  <a-button size="small" type="primary" :disabled="true">下载</a-button>
                </a-tooltip>
              </template>
              <template v-else>
                <a-button size="small" type="primary" @click="handleDownload(record)">下载</a-button>
              </template>
              <template v-if="record.isDirectory">
                <!-- record.filename -->
                <a-popconfirm
                  :title="`${record.filename ? '将还原【' + record.filename + '】目录,' : ''} 请选择还原方式,清空还原将会先删除项目目录中的文件再将对应备份文件恢复至当前目录`"
                  okText="覆盖还原"
                  cancelText="清空还原"
                  @confirm="recoverNet('', record.filename)"
                  @cancel="recoverNet('clear', record.filename)"
                >
                  <a-icon slot="icon" type="question-circle-o" style="color: red" />
                  <a-button size="small" type="primary">还原</a-button>
                </a-popconfirm>
              </template>
              <template v-else>
                <a-button size="small" type="primary" @click="recover(record)">还原</a-button>
              </template>

              <a-button size="small" type="danger" @click="handleDelete(record)">删除</a-button>
            </a-space>
          </template>
        </a-table>
      </a-layout-content>
    </a-layout>
  </div>
</template>
<script>
import {backupDeleteProjectFile, backupDownloadProjectFile, backupFileList, backupRecoverProjectFile, listBackup} from "@/api/node-project-backup";

export default {
  components: {},
  props: {
    nodeId: {
      type: String,
    },
    projectId: {
      type: String,
    },
  },
  data() {
    return {
      viewList: true,
      loading: false,
      treeList: [],
      fileList: [],
      backupListData: {},
      backupListLoading: false,
      tempNode: {},
      temp: {},
      treeReplaceFields: {
        title: "filename",
        isLeaf: "isDirectory",
      },

      defaultProps: {
        children: "children",
        label: "filename",
      },

      columns: [
        { title: "文件名称", dataIndex: "filename", ellipsis: true, scopedSlots: { customRender: "filename" } },

        { title: "文件大小", dataIndex: "fileSize", width: 120, ellipsis: true, scopedSlots: { customRender: "fileSize" } },
        { title: "修改时间", dataIndex: "modifyTime", width: 180, ellipsis: true },
        { title: "操作", dataIndex: "operation", width: 180, align: "center", scopedSlots: { customRender: "operation" } },
      ],
      fileColumns: [
        { title: "文件名称", dataIndex: "filename", ellipsis: true, scopedSlots: { customRender: "filename" } },
        { title: "文件类型", dataIndex: "isDirectory", width: 100, ellipsis: true, scopedSlots: { customRender: "isDirectory" } },
        { title: "文件大小", dataIndex: "fileSize", width: 120, ellipsis: true, scopedSlots: { customRender: "fileSize" } },
        { title: "修改时间", dataIndex: "modifyTime", width: 180, ellipsis: true },
        { title: "操作", dataIndex: "operation", width: 180, align: "center", scopedSlots: { customRender: "operation" } },
      ],
    };
  },
  computed: {
    uploadPath() {
      if (!Object.keys(this.tempNode).length) {
        return "";
      }
      if (this.tempNode.level === 1) {
        return "";
      } else {
        return (this.tempNode.levelName || "") + "/" + this.tempNode.filename;
      }
    },
  },
  mounted() {
    this.loadBackupList();
  },
  methods: {
    onTreeData(treeNode) {
      return new Promise((resolve) => {
        if (treeNode.dataRef.children || !treeNode.dataRef.isDirectory) {
          resolve();
          return;
        }
        this.loadNode(treeNode.dataRef, resolve);
      });
    },
    // 查询备份列表
    loadBackupList() {
      listBackup({
        nodeId: this.nodeId,
        id: this.projectId,
      }).then((res) => {
        if (res.code === 200) {
          this.backupListData = res.data;
        }
        this.backupListLoading = false;
      });
    },
    // 加载数据
    loadData() {
      const key = "root-" + new Date().getTime();
      this.treeList = [
        {
          filename: "目录：" + (this.temp.filename || ""),
          level: 1,
          isDirectory: true,
          key: key,
          isLeaf: false,
        },
      ];
      // 设置默认展开第一个
      setTimeout(() => {
        const node = this.treeList[0];
        this.tempNode = node;
        this.expandKeys = [key];
        this.loadFileList();
      }, 1000);
    },
    // 加载子节点
    loadNode(data, resolve) {
      this.tempNode = data;
      // 如果是目录
      if (data.isDirectory) {
        setTimeout(() => {
          // 请求参数
          const params = {
            nodeId: this.nodeId,
            id: this.projectId,
            path: this.uploadPath,
            backupId: this.temp.filename,
          };
          // if (node.level === 1) {
          //   params.path = ''
          // } else {
          //   params.path = data.levelName || '' + '/' + data.filename
          // }
          // 加载文件
          backupFileList(params).then((res) => {
            if (res.code === 200) {
              const treeData = res.data
                .filter((ele) => {
                  return ele.isDirectory;
                })
                .map((ele) => {
                  ele.isLeaf = !ele.isDirectory;
                  ele.key = ele.filename + "-" + new Date().getTime();
                  return ele;
                });
              data.children = treeData;

              this.treeList = [...this.treeList];
              resolve();
            } else {
              resolve();
            }
          });
        }, 500);
      } else {
        resolve();
      }
    },

    // 点击树节点
    nodeClick(selectedKeys, { node }) {
      if (node.dataRef.isDirectory) {
        this.tempNode = node.dataRef;
        this.loadFileList();
      }
    },

    // 加载文件列表
    loadFileList() {
      if (Object.keys(this.tempNode).length === 0) {
        this.$notification.warn({
          message: "请选择一个节点",
        });
        return false;
      }
      // 请求参数
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        path: this.uploadPath,
        backupId: this.temp.filename,
      };
      this.fileList = [];
      this.loading = true;
      // 加载文件
      backupFileList(params).then((res) => {
        if (res.code === 200) {
          // 区分目录和文件
          res.data.forEach((element) => {
            // if (!element.isDirectory) {
            // 设置文件表格
            this.fileList.push({
              ...element,
            });
            // }
          });
        }
        this.loading = false;
      });
    },

    // 下载
    handleDownload(record) {
      this.$notification.info({
        message: "正在下载，请稍等...",
      });
      // 请求参数
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        levelName: record.levelName,
        filename: record.filename,
        backupId: this.temp.filename,
      };
      // 请求接口拿到 blob
      backupDownloadProjectFile(params).then((blob) => {
        const url = window.URL.createObjectURL(blob);
        let link = document.createElement("a");
        link.style.display = "none";
        link.href = url;
        link.setAttribute("download", record.filename);
        document.body.appendChild(link);
        link.click();
      });
    },
    // 删除
    handleDelete(record) {
      const msg = record.isDirectory ? "真的要删除【" + record.filename + "】文件夹么？" : "真的要删除【" + record.filename + "】文件么？";
      this.$confirm({
        title: "系统提示",
        content: msg,
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 请求参数
          const params = {
            nodeId: this.nodeId,
            id: this.projectId,
            levelName: record.levelName,
            filename: record.filename,
            backupId: this.temp.filename,
          };
          // 删除
          backupDeleteProjectFile(params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.loadData();
              this.loadFileList();
            }
          });
        },
      });
    },
    // 删除备份
    handlBackupeDelete(record) {
      const msg = "真的要删除【" + record.filename + "】备份文件夹么？";
      this.$confirm({
        title: "系统提示",
        content: msg,
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 请求参数
          const params = {
            nodeId: this.nodeId,
            id: this.projectId,
            levelName: "/",
            filename: "/",
            backupId: record.filename,
          };
          // 删除
          backupDeleteProjectFile(params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.loadBackupList();
            }
          });
        },
      });
    },
    handleBackupFile(record) {
      this.viewList = false;
      this.temp = Object.assign({}, record);
      this.loadData();
    },
    // recoverPath(filename) {
    //   // const msg = ;
    //   this.$confirm({
    //     title: "系统提示",
    //     // content: ,
    //     okText: "覆盖还原",
    //     cancelText: "清空还原",
    //     closable: false,
    //     maskClosable: true,
    //     onOk: () => {
    //       // // 请求参数
    //       this.recoverNet("", filename);
    //     },
    //     onCancel: () => {
    //       this.recoverNet("clear", filename);
    //     },
    //   });
    // },
    //
    recover(record) {
      if (record.isDirectory) {
        this.recoverPath(record.filename);
      } else {
        this.$confirm({
          title: "系统提示",
          content: "是否将【" + record.filename + "】此文件还原到项目目录？",
          okText: "确认",
          cancelText: "取消",
          onOk: () => {
            // // 请求参数
            this.recoverNet("", record.filename);
          },
        });
      }
    },
    recoverNet(type, filename) {
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        type,
        levelName: this.uploadPath,
        filename,
        backupId: this.temp.filename,
      };
      // 删除
      backupRecoverProjectFile(params).then((res) => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
          });
        }
      });
    },
  },
};
</script>

<style scoped>
.file-layout {
  padding: 0;
}
.sider {
  border: 1px solid #e2e2e2;
  height: calc(100vh - 80px);
  overflow-y: auto;
}
.dir-container {
  padding: 10px;
  border-bottom: 1px solid #eee;
}
.file-content {
  height: calc(100vh - 100px);
  overflow-y: auto;
  margin: 10px 10px 0;
  padding: 10px;
  background-color: #fff;
}
.successTag {
  height: 32px;
  line-height: 30px;
}
</style>
