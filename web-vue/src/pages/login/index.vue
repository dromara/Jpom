<template>
  <!-- <div class="wrapper"> -->
    <a-card class="login-card" hoverable style="width: 450px">
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
          <a-button type="primary" html-type="submit" class="btn-login">
            登录
          </a-button>
        </a-form-item>
      </a-form>
    </a-card>
  <!-- </div> -->
</template>
<script>
import { login } from '../../api/user';
import { checkSystem } from '../../api/install';
import sha1 from 'sha1';
export default {
  data() {
    return {
      loginForm: this.$form.createForm(this, { name: 'login-form' }),
      randCode: 'randCode.png'
    }
  },
  beforeCreate() {
    document.querySelector('body').setAttribute('style', 'background-color: #fbefdf')
  }, //
  beforeDestroy() {
    document.querySelector('body').removeAttribute('style')
  },
  created() {
    this.checkSystem();
  },
  methods: {
    // 检查是否需要初始化
    checkSystem() {
      checkSystem().then(res => {
        if(res.code === 900){
          //
          this.$router.push('/system/ipAccess');
        }else if (res.code !== 200) {
          this.$notification.warn({
            message: res.msg,
            duration: 2
          });
          this.$router.push('/install');
        }
      })
    },
    // change Code
    changeCode() {
      this.randCode = 'randCode.png?r=' + new Date().getTime()
    },
    // login
    handleLogin(e) {
      e.preventDefault();
      this.loginForm.validateFields((err, values) => {
        if (!err) {
          const params = {
            ...values,
            userPwd: sha1(values.userPwd)
          }
          login(params).then(res => {
            // 登录不成功，更新验证码
            if(res.code !== 200) {
              this.changeCode();
            } else {
              this.$notification.success({
                message: res.msg,
                duration: 2
              });
              // 调用 store action 存储当前登录的用户名和 token
              this.$store.dispatch('login', {token: res.data.token,longTermToken: res.data.longTermToken}).then(() => {
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
  border-radius: 10px;
}
.rand-code {
  height: 38px;
}
.btn-login {
  width: 120px;
}
/*body {*/
/*  background-color: #fbefdf;*/
/*}*/
</style>
<style>

.ant-card-meta-title {
  font-size: 30px;
}
</style>