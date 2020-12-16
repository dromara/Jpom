<template>
  <!-- 布局 -->
  <a-layout class="file-layout">
    <!-- 目录树 -->
    <a-layout-sider theme="light" class="sider" width="25%">
      <a-empty v-if="treeList.length === 0" />
      <a-directory-tree :treeData="treeList" :replaceFields="replaceFields" @select="onSelect">
      </a-directory-tree>
    </a-layout-sider>
    <!-- 表格 -->
    <a-layout-content class="file-content">
      <div ref="filter" class="filter">
        <a-button type="primary" @click="handleUpload">上传文件</a-button>
        <a-button type="primary" @click="loadFileList()">刷新</a-button>
      </div>
      <a-table :data-source="fileList" :loading="loading" :columns="columns" :scroll="{y: tableHeight}" :pagination="false" bordered :rowKey="(record, index) => index">
        <a-tooltip slot="filename" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <a-tooltip slot="isDirectory" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text ? '目录' : '文件' }}</span>
        </a-tooltip>
        <a-tooltip slot="fileSize" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <template slot="operation" slot-scope="text, record">
          <a-button type="primary" @click="handleDownload(record)">下载</a-button>
          <a-button type="danger" @click="handleDelete(record)">删除</a-button>
        </template>
      </a-table>
    </a-layout-content>
  </a-layout>
</template>
<script>
import { getFileList, downloadProjectFile, deleteProjectFile } from '../../../../api/node-project';
export default {
  props: {
    node: {
      type: Object
    },
    project: {
      type: Object
    }
  },
  data() {
    return {
      loading: false,
      treeList: [],
      fileList: [],
      uploadFileList: [],
      tempNode: {},
      temp: {},
      uploadFileVisible: false,
      tableHeight: '80vh',
      replaceFields: {
        children: 'children',
        title: 'title',
        key: 'key'
      },
      columns: [
        {title: '文件名称', dataIndex: 'filename', width: 100, ellipsis: true, scopedSlots: {customRender: 'filename'}},
        {title: '文件类型', dataIndex: 'isDirectory', width: 100, ellipsis: true, scopedSlots: {customRender: 'isDirectory'}},
        {title: '文件大小', dataIndex: 'fileSize', width: 120, ellipsis: true, scopedSlots: {customRender: 'fileSize'}},
        {title: '修改时间', dataIndex: 'modifyTime', width: 170, ellipsis: true},
        {title: '操作', dataIndex: 'operation', scopedSlots: {customRender: 'operation'}, width: 120}
      ]
    }
  },
  mounted() {
    this.calcTableHeight()
    this.loadData();
  },
  methods: {
    // 计算表格高度
    calcTableHeight() {
      this.tableHeight = window.innerHeight - this.$refs['filter'].clientHeight - 185;
    },
    // 加载数据
    loadData() {
      this.loading = true;
      this.treeList.push({
        key: '1',
        title: '项目根目录',
        path: ''
      })
      this.loading = false;
    },
    // 上传文件
    handleUpload() {
      if (Object.keys(this.tempNode).length === 0) {
        this.$notification.error({
          message: '请选择一个节点',
          duration: 2
        });
        return;
      }
      this.uploadFileVisible = true;
    },
    // 选中目录
    onSelect(selectedKeys, {node}) {
      return new Promise(resolve => {
        this.tempNode = node.dataRef;
        if (node.dataRef.disabled) {
          resolve();
          return;
        }
        // 请求参数
        const params = {
          nodeId: this.node.id,
          id: this.project.id,
          path: node.dataRef.path
        }
        this.fileList = [];
        this.loading = true;
        // 加载文件
        getFileList(params).then(res => {
          if (res.code === 200) {
            let children = [];
            // 区分目录和文件
            res.data.forEach(element => {
              if (element.isDirectory) {
                children.push({
                  key: element.id,
                  title: element.filename,
                  path: `${node.dataRef.path}/${element.filename}`,
                  isLeaf: element.isDirectory ? false : true
                })
              } else {
                // 设置文件表格
                this.fileList.push({
                  ...element
                });
              }
            })
            // 设置目录树
            node.dataRef.children = children;
            this.treeList = [...this.treeList];
          }
          this.loading = false;
        })
        resolve();
      });
    },
    // 加载文件列表
    loadFileList() {
      if (Object.keys(this.tempNode).length === 0) {
        this.$notification.warn({
          message: '请选择一个节点',
          duration: 2
        });
        return false;
      }
      // 请求参数
      const params = {
        nodeId: this.node.id,
        id: this.project.id,
        path: this.tempNode.path
      }
      this.fileList = [];
      this.loading = true;
      // 加载文件
      getFileList(params).then(res => {
        if (res.code === 200) {
          // 区分目录和文件
          res.data.forEach(element => {
            if (!element.isDirectory) {
              // 设置文件表格
              this.fileList.push({
                ...element
              });
            }
          })
        }
        this.loading = false;
      })
    },
    // 下载
    handleDownload(record) {
      this.$notification.info({
        message: '正在下载，请稍等...',
        duration: 5
      });
      // 请求参数
      const params = {
        nodeId: this.node.id,
        id: this.project.id,
        levelName: record.levelName,
        filename: record.filename
      }
      // 请求接口拿到 blob
      downloadProjectFile(params).then(blob => {
        const url = window.URL.createObjectURL(blob);
        let link = document.createElement('a');
        link.style.display = 'none';
        link.href = url;
        link.setAttribute('download', record.filename);
        document.body.appendChild(link);
        link.click();
      })
    },
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: '系统提示',
        content: '真的要删除文件么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 请求参数
          const params = {
            nodeId: this.node.id,
            id: this.project.id,
            levelName: record.levelName,
            filename: record.filename
          }
          // 删除
          deleteProjectFile(params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
                duration: 2
              });
              this.loadFileList();
            }
          })
        }
      });
    }
  }
}
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
.file-content {
  height: calc(100vh - 100px);
  overflow-y: auto;
  margin: 10px 10px 0;
  padding: 10px;
  background-color: #fff;
}
.filter {
  margin: 0 0 10px;
}
.ant-btn {
  margin-right: 10px;
}
</style>