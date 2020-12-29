<template>
  <a-card class="login-card" hoverable style="width: 400px">
    <a-card-meta title="Init Jpom Dashboard" description=""/>
    <br/>
    <a-form :form="loginForm" :label-col="{ span: 0 }" @submit="handleLogin">
      <a-form-item :wrapper-col="{span: 24}">
        <a-input v-decorator="['userName', { rules: [{ required: true, message: 'Please input your name!' }] }]" placeholder="User Name"/>
      </a-form-item>
      <a-form-item :wrapper-col="{span: 24}">
        <a-input-password v-decorator="['userPwd', { rules: [{ required: true, message: 'Please input your password!' }] }]" placeholder="Password"/>
      </a-form-item>
      <a-form-item :wrapper-col="{ span: 24 }">
        <a-button type="primary" html-type="submit">
          提交并登录
        </a-button>
      </a-form-item>
    </a-form>
  </a-card>
</template>
<script>
import { initInstall } from '../../api/install';
import sha1 from 'sha1';
export default {
  data() {
    return {
      loginForm: this.$form.createForm(this, { name: 'login-form' })
    }
  },
  created() {
  },
  methods: {
    // login
    handleLogin(e) {
      e.preventDefault();
      this.loginForm.validateFields((err, values) => {
        if (!err) {
          const params = {
            ...values,
            userPwd: sha1(values.userPwd)
          }
          initInstall(params).then(res => {
            // 登录不成功，更新验证码
            if(res.code === 200) {
              this.$notification.success({
                message: res.msg,
                duration: 2
              });
              // 调用 store action 存储当前登录的用户名和 token
              this.$store.dispatch('login', {token: res.data.token}).then(() => {
                // 跳转主页面
                this.$router.push({ path: '/' });
              })
            }
          })
        }
      });
    }
  }
}
</script>
<style scoped>
.login-card {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%,-50%);
  text-align: center;
}
</style>