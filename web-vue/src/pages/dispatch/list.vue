<template>
  <div>
    <div ref="filter" class="filter">
      <a-button type="primary" @click="handleLink">添加关联项目</a-button>
      <a-button type="primary" @click="handleAdd">创建分发项目</a-button>
      <a-button type="primary" @click="loadData">刷新</a-button>
    </div>
  </div>
</template>
<script>
export default {
  data() {
    return {
      temp: {},
      editDispatchVisible: false,
    }
  },
  created() {

  },
  methods: {
    // 加载数据
    loadData() {

    },
    // 筛选
    handleFilter() {
      this.loadData();
    },
    // 关联
    handleLink() {

    },
    // 添加
    handleAdd() {
      this.temp = {};
      this.editDispatchVisible = true;
    },
  }
}
</script>