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
export default {
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
  }
}
</script>
<style scoped>
.log-layout {
  padding: 0;
}
.sider {
  height: calc(100vh - 100px);
  overflow-y: auto;
}
.log-content {
  height: calc(100vh - 125px);
  overflow-y: auto;
}
.op-btn {
  text-align: right;
}
.ant-btn {
  margin: 10px;
}
</style>