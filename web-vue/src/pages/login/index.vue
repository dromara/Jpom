<template>
  <a-card class="login-card" hoverable style="width: 400px">
    <a-card-meta title="Jpom Dashboard" description=""/>
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
          登录
        </a-button>
      </a-form-item>
    </a-form>
  </a-card>
</template>
<script>
import { login } from '../../api/user'
export default {
  data() {
    return {
      loginForm: this.$form.createForm(this, { name: 'login-form' }),
    }
  },
  methods: {
    handleLogin(e) {
      e.preventDefault();
      this.loginForm.validateFields((err, values) => {
        if (!err) {
          console.log('Received values of form: ', values);
        }
        login(values).then(res => {
          console.log(res);
        })
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