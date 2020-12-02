<template>
  <a-layout class="log-layout">
    <!-- 侧边栏 文件树 -->
    <a-layout-sider theme="light" class="sider" width="20%">
      <a-empty v-if="list.length === 0" />
      <a-directory-tree :treeData="list" :replaceFields="replaceFields" @select="select"
        @rightClick="rightClick" default-expand-all>
      </a-directory-tree>
    </a-layout-sider>
    <!-- 单个文件内容 -->
    <a-layout-content class="log-content">
      <div class="filter">
        <a-button type="primary" @click="loadData">刷新</a-button>
      </div>
      <div>......</div>
    </a-layout-content>
    <!-- 对话框 -->
    <a-modal v-model="visible" title="系统提示" :footer="null">
      <div class="op-btn">
        <a-button type="danger" @click="deleteLog">删除日志文件</a-button>
        <a-button type="primary" @click="downloadLog">下载日志文件</a-button>
        <a-button @click="visible = false">取消</a-button>
      </div>
    </a-modal>
  </a-layout>
</template>
<script>
import { getLogList, downloadFile, deleteLog } from '../../../../api/system';
export default {
  props: {
    node: {
      type: Object
    }
  },
  data() {
    return {
      list: [],
      replaceFields: {
        children: 'children',
        title: 'title',
        key: 'path'
      },
      visible: false,
      temp: {}
    }
  },
  created() {
    this.loadData();
  },
  methods: {
    // 加载数据
    loadData() {
      this.list = [];
      getLogList().then(res => {
        if (res.code === 200) {
          res.data.forEach(element => {
            if (element.children) {
              this.calcTreeNode(element.children);
            }
            // 组装数据
            this.list.push({
              ...element,
              isLeaf: !element.children ? true : false
            });
          });
        }
      })
    },
    // 递归处理节点
    calcTreeNode(list) {
      list.forEach(element => {
        if (element.children) {
          this.calcTreeNode(element.children);
        } else {
          // 叶子节点
          element.isLeaf = true;
        }
      })
    },
    // 选择节点
    select(selectedKeys, {node}) {
      console.log(node)
    },
    // 右键点击
    rightClick({node}) {
      this.temp = node.dataRef;
      // 弹出提示 下载还是删除
      this.visible = true;
    },
    // 下载文件
    downloadLog() {
      // 请求参数
      const params = {
        nodeId: this.node.id,
        path: this.temp.path
      }
      // 请求接口拿到 blob
      downloadFile(params).then(blob => {
        const url = window.URL.createObjectURL(blob);
        let link = document.createElement('a');
        link.style.display = 'none';
        link.href = url;
        link.setAttribute('download', this.temp.title);
        document.body.appendChild(link);
        link.click();
        // 关闭弹窗
        this.visible = false;
      })
    },
    // 删除文件
    deleteLog() {
       this.$confirm({
        title: '系统提示',
        content: '真的要删除日志文件么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          const params = {
            nodeId: this.node.id,
            path: this.temp.path
          }
          // 删除日志
          deleteLog(params).then(res => {
            if(res.code === 200) {
              this.$notification.success({
                message: res.msg,
                duration: 2
              });
              this.visible = false;
              this.loadData();
            }
          })
        }
      })
    }
  }
}
</script>
<style scoped>
.log-layout {
  padding: 0;
  margin: 0;
}
.sider {
  border: 1px solid #e2e2e2;
  height: calc(100vh - 130px);
  overflow-y: auto;
}
.log-content {
  margin: 0;
  padding-left: 15px;
  background: #fff;
  height: calc(100vh - 130px);
  overflow-y: auto;
}
.op-btn {
  text-align: right;
}
.ant-btn {
  margin: 10px;
}
</style>