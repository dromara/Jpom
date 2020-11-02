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
      <a-form-item :wrapper-col="{span: 24}">
        <a-row>
          <a-col :span="14">
            <a-input v-decorator="['code', { rules: [{ required: true, message: 'Please input your code!' }] }]" placeholder="Code"/>
          </a-col>
          <a-col :span="10">
            <img :src="randCode" @click="changeCode" class="rand-code"/>
          </a-col>
        </a-row>
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
      randCode: 'randCode.png'
    }
  },
  created() {
  },
  methods: {
    // change Code
    changeCode() {
      this.randCode = 'randCode.png?r=' + new Date().getTime()
    },
    // login
    handleLogin(e) {
      e.preventDefault();
      this.loginForm.validateFields((err, values) => {
        if (!err) {
          console.log('Received values of form: ', values);
        }
        login(values).then(res => {
          // 登录不成功，更新验证码
          if(res.code !== 200) {
            this.changeCode();
          }
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
.rand-code {
  height: 38px;
}
</style>