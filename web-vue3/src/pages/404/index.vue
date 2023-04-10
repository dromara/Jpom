<template>
  <a-card hoverable class="card">
    <a-card-meta>
      <template slot="description">
        <a-result status="404" title="404" sub-title="Sorry, the page you visited does not exist.">
          <a-button type="primary" @click="goBack">
            <a-icon type="left"/>
            <span>Go Back</span>
          </a-button>
        </a-result>
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