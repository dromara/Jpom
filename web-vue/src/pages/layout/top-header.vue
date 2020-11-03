<template>
  <div class="right-header">
    <a-dropdown>
      <a-avatar
        shape="square"
        size="large"
        :style="{ backgroundColor: '#f56a00', verticalAlign: 'middle' }">
        <a-tooltip placement="left" :title="getUserName">
          {{ avatarName }}
        </a-tooltip>
      </a-avatar>
      <a-menu slot="overlay">
        <a-menu-item>
          <a href="javascript:;">修改密码</a>
        </a-menu-item>
        <a-menu-item>
          <a href="javascript:;">修改昵称</a>
        </a-menu-item>
        <a-menu-item>
          <a href="javascript:;">用户资料</a>
        </a-menu-item>
        <a-menu-item>
          <a href="javascript:;" @click="logOut">退出登录</a>
        </a-menu-item>
      </a-menu>
    </a-dropdown>
  </div>
</template>
<script>
import { mapGetters } from 'vuex';
export default {
  data() {
    return {}
  },
  computed: {
    ...mapGetters([
      'getToken',
      'getUserName'
    ]),
    avatarName() {
      const reg = new RegExp("[\u4E00-\u9FA5]+");
      if (reg.test(this.getUserName)) {
        return this.getUserName.substring(0, 3);
      } else {
        return this.getUserName.substring(0, 4);
      }
    }
  },
  methods: {
    // 退出登录
    logOut() {
      this.$confirm({
        title: '系统提示',
        content: '真的要退出系统么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          return new Promise((resolve) => {
            // 退出登录
            this.$store.dispatch('logOut').then(() => {
              this.$notification.success({
                message: '退出登录成功',
                duration: 2
              });
              this.$router.push('/login');
              resolve();
            })
          })
        }
      });
    }
  }
}
</script>
<style scoped>
.right-header {
  margin-right: 20px;
  float: right;
  cursor: pointer;
}
</style>