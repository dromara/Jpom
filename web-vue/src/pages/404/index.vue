<template>
  <a-card hoverable class="card">
    <a-card-meta title="404">
      <template slot="description">
        <p>Page Not Found</p>
        <a-button type="primary" @click="goBack">
          <a-icon type="left"/>
          <span>Go Back</span>
        </a-button>
      </template>
    </a-card-meta>
  </a-card>
</template>
<script>
export default {
  data() {
    return {
      
    }
  },
  methods: {
    // 返回
    goBack() {
      this.$router.back(-1);
    }
  }
}
</script>
<style scoped>
.card {
  width: 400px;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%,-50%);
  text-align: center;
}
</style>