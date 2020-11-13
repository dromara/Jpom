<template>
  <div>
    <div class="filter">
      <a-button type="primary" @click="loadData">刷新</a-button>
    </div>
  </div>
</template>
<script>
export default {
  data() {
    return {
      loadding: false,
    }
  },
  created() {
    this.loadData();
  },
  methods: {
    // 加载数据
    loadData() {

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
</style>