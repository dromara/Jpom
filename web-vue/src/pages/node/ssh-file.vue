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
      <a-table :data-source="fileList" :loading="loading" :columns="columns" :pagination="false" bordered
        :style="{'max-height': tableHeight + 'px' }" :scroll="{x: 790, y: tableHeight - 60}" :rowKey="(record, index) => index">
        <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <a-tooltip slot="dir" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text ? '目录' : '文件' }}</span>
        </a-tooltip>
        <a-tooltip slot="size" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <template slot="operation" slot-scope="text, record">
          <a-button type="primary" @click="handlePreview(record)">查看</a-button>
          <a-button type="primary" @click="handleDownload(record)">下载</a-button>
          <a-button type="danger" @click="handleDelete(record)">删除</a-button>
        </template>
      </a-table>
      <!-- 上传文件 -->
      <a-modal v-model="uploadFileVisible" width="300px" title="上传文件" :footer="null" :maskClosable="true">
        <a-upload :file-list="uploadFileList" :remove="handleRemove" :before-upload="beforeUpload" multiple>
          <a-button><a-icon type="upload" />选择文件</a-button>
        </a-upload>
        <br/>
        <a-button type="primary" :disabled="uploadFileList.length === 0" @click="startUpload">开始上传</a-button>
      </a-modal>
      <!-- Terminal -->
      <a-modal v-model="terminalVisible" width="50%" title="Terminal" :footer="null" :maskClosable="false">
        <terminal v-if="terminalVisible" :sshId="ssh.id" :nodeId="ssh.nodeModel.id" :tail="temp.path + temp.parentDir"/>
      </a-modal>
    </a-layout-content>
  </a-layout>
</template>
<script>
import { getRootFileList, getFileList, downloadFile, deleteFile, uploadFile } from '../../api/ssh';
import Terminal from './terminal';
export default {
  props: {
    ssh: {
      type: Object
    }
  },
  components: {
    Terminal
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
      terminalVisible: false,
      tableHeight: '80vh',
      replaceFields: {
        children: 'children',
        title: 'title',
        key: 'key'
      },
      columns: [
        {title: '文件名称', dataIndex: 'title', width: 100, ellipsis: true, scopedSlots: {customRender: 'name'}},
        {title: '文件类型', dataIndex: 'dir', width: 100, ellipsis: true, scopedSlots: {customRender: 'dir'}},
        {title: '文件大小', dataIndex: 'size', width: 120, ellipsis: true, scopedSlots: {customRender: 'size'}},
        {title: '修改时间', dataIndex: 'modifyTime', width: 180, ellipsis: true},
        {title: '操作', dataIndex: 'operation', scopedSlots: {customRender: 'operation'}, width: 280}
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
      this.tableHeight = window.innerHeight - this.$refs['filter'].clientHeight - 135;
    },
    // 加载数据
    loadData() {
      this.loading = true;
      getRootFileList(this.ssh.id).then(res => {
        if (res.code === 200) {
          let tempList = [];
          res.data.forEach(element => {
            tempList.push({
              key: element.path,
              title: element.path,
              path: element.path,
              parentDir: '/',
              isLeaf: false,
              disabled: element.error ? true : false
            })
          });
          this.treeList = tempList;
        }
        this.loading = false;
      })
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
    handleRemove(file) {
      const index = this.uploadFileList.indexOf(file);
      const newFileList = this.uploadFileList.slice();
      newFileList.splice(index, 1);
      this.uploadFileList = newFileList;
    },
    beforeUpload(file) {
      this.uploadFileList = [...this.uploadFileList, file];
      return false;
    },
    // 开始上传文件
    startUpload() {
      this.uploadFileList.forEach(file => {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('id', this.ssh.id);
        formData.append('name', this.tempNode.parentDir);
        formData.append('path', this.tempNode.path);
        // 上传文件
        uploadFile(formData).then(res => {
          if (res.code === 200) {
            this.$notification.success({
              message: res.msg,
              duration: 2
            });
          }
        })
      })
      setTimeout(() => {
        this.loadFileList();
      }, 1000 * 3);
      this.uploadFileList = [];
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
          id: this.ssh.id,
          path: node.dataRef.path,
          children: node.dataRef.parentDir
        }
        this.fileList = [];
        this.loading = true;
        // 加载文件
        getFileList(params).then(res => {
          if (res.code === 200) {
            let children = [];
            // 区分目录和文件
            res.data.forEach(element => {
              if (element.dir) {
                children.push({
                  key: element.id,
                  title: element.title,
                  name: element.name,
                  path: node.dataRef.path,
                  parentDir: element.parentDir,
                  isLeaf: element.dir ? false : true,
                  disabled: element.error ? true : false
                })
              } else {
                // 设置文件表格
                this.fileList.push({
                  path: node.dataRef.path,
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
        id: this.ssh.id,
        path: this.tempNode.path,
        children: this.tempNode.parentDir
      }
      this.fileList = [];
      this.loading = true;
      // 加载文件
      getFileList(params).then(res => {
        if (res.code === 200) {
          // 区分目录和文件
          res.data.forEach(element => {
            if (!element.dir) {
              // 设置文件表格
              this.fileList.push({
                path: this.tempNode.path,
                ...element
              });
            }
          })
        }
        this.loading = false;
      })
    },
    // 查看
    handlePreview(record) {
      this.temp = Object.assign(record);
      this.terminalVisible = true;
    },
    // 下载
    handleDownload(record) {
      // 请求参数
      const params = {
        id: this.ssh.id,
        path: record.path,
        name: record.parentDir
      }
      // 请求接口拿到 blob
      downloadFile(params).then(blob => {
        const url = window.URL.createObjectURL(blob);
        let link = document.createElement('a');
        link.style.display = 'none';
        link.href = url;
        link.setAttribute('download', record.name);
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
            id: this.ssh.id,
            path: record.path,
            name: record.parentDir
          }
          // 删除
          deleteFile(params).then((res) => {
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