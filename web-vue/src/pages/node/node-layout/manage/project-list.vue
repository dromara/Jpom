<template>
  <div>
    <div ref="filter" class="filter">
      <a-button type="primary" @click="handleAdd">新增</a-button>
      <a-button type="primary" @click="loadData">刷新</a-button>
    </div>
  </div>
</template>
<script>
import { getProjectList } from '../../../../api/node-manage';
export default {
  props: {
    node: {
      type: Object
    }
  },
  data() {
    return {
      loading: false,
      list: []
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    // 加载数据
    loadData() {
      this.loading = true;
      const params = {
        nodeId: this.node.id
      }
      getProjectList(params).then(res => {
        if (res.code === 200) {
          this.list = res.data;
        }
        this.loading = false;
      })
    },
    // 添加
    handleAdd() {
    },
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
</style>