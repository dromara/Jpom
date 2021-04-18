<template>
  <a-card class="login-card" hoverable style="width: 400px">
    <a-card-meta title="Init Jpom Dashboard" description=""/>
    <br/>
    <a-form :form="loginForm" :label-col="{ span: 0 }" @submit="handleLogin" class="init-form">
      <a-form-item :wrapper-col="{span: 24}" class="init-user-name">
        <a-input v-decorator="['userName', { rules: [{ required: true, message: 'Please input your name!' }] }]" placeholder="User Name"/>
      </a-form-item>
      <a-form-item :wrapper-col="{span: 24}" class="init-user-password">
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
import { mapGetters } from 'vuex';
import { initInstall } from '../../api/install';
import sha1 from 'sha1';
export default {
  data() {
    return {
      loginForm: this.$form.createForm(this, { name: 'login-form' })
    }
  },
  computed: {
    ...mapGetters([
      'getGuideFlag'
    ])
  },
  watch: {
    getGuideFlag() {
      this.introGuide();
    }
  },
  created() {
    this.$nextTick(() => {
      setTimeout(() => {
        this.introGuide();
      }, 500);
    })
  },
  methods: {
    // 页面引导
    introGuide() {
      if (this.getGuideFlag) {
        this.$introJs().setOptions({
          steps: [ {
            title: 'Jpom 导航助手',
            intro: '不要慌，出现这个页面表示你没有设置系统管理员信息，或者需要重置管理员信息'
          }, {
            title: 'Jpom 导航助手',
            element: document.querySelector('.init-form'),
            intro: '这里填写的账号密码是设置 Jpom 系统的系统管理员的账号密码'
          }, {
            title: 'Jpom 导航助手',
            intro: '系统管理员的账号密码一定要记住哦，是登录 jpom 的唯一凭证'
          }, {
            title: 'Jpom 导航助手',
            element: document.querySelector('.init-user-password'),
            intro: '系统管理员密码的强度有要求数字+字母+符号'
          }]
        }).start();
        return false;
      }
      this.$introJs().exit();
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
          initInstall(params).then(res => {
            // 登录不成功，更新验证码
            if(res.code === 200) {
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
}
</style>