<template>
  <div class="init-wrapper">
    <svg width="100%" height="100%" viewBox="0 0 1440 500" stroke="none" stroke-width="1" fill="none" fill-rule="evenodd">
      <g>
        <circle stroke="#13C2C2" cx="500" cy="-20" r="6"></circle>
        <circle fill-opacity="0.4" fill="#9EE6E6" cx="166" cy="76" r="8"></circle>
        <circle stroke="#13C2C2" cx="1165" cy="240" r="5"></circle>
        <circle stroke="#CED4D9" cx="1300" cy="10" r="8"></circle>
        <circle stroke="#ffffff" cx="1326.5" cy="180" r="6"></circle>
        <circle fill-opacity="0.4" fill="#9EE6E6" cx="944" cy="250" r="5"></circle>
      </g>
      <g>
        <path
          d="M1182.79367,448.230356 L1186.00213,453.787581 C1186.55442,454.744166 1186.22667,455.967347 1185.27008,456.519632 C1184.96604,456.695168 1184.62116,456.787581 1184.27008,456.787581 L1177.85315,456.787581 C1176.74858,456.787581 1175.85315,455.89215 1175.85315,454.787581 C1175.85315,454.436507 1175.94556,454.091619 1176.1211,453.787581 L1179.32957,448.230356 C1179.88185,447.273771 1181.10503,446.946021 1182.06162,447.498305 C1182.36566,447.673842 1182.61813,447.926318 1182.79367,448.230356 Z"
          stroke="#CED4D9"
        ></path>
        <path
          d="M1376.79367,204.230356 L1380.00213,209.787581 C1380.55442,210.744166 1380.22667,211.967347 1379.27008,212.519632 C1378.96604,212.695168 1378.62116,212.787581 1378.27008,212.787581 L1371.85315,212.787581 C1370.74858,212.787581 1369.85315,211.89215 1369.85315,210.787581 C1369.85315,210.436507 1369.94556,210.091619 1370.1211,209.787581 L1373.32957,204.230356 C1373.88185,203.273771 1375.10503,202.946021 1376.06162,203.498305 C1376.36566,203.673842 1376.61813,203.926318 1376.79367,204.230356 Z"
          stroke="#2F54EB"
        ></path>
      </g>
      <g>
        <rect stroke="#13C2C2" stroke-opacity="0.6" x="120" y="322" width="12" height="12" rx="1"></rect>
        <rect stroke="#CED4D9" x="108" y="1" width="9" height="9" rx="1"></rect>
      </g>
    </svg>
    <a-card class="login-card" hoverable>
      <a-card-meta title="初始化JPOM账户" style="textalign: center" description="您需要创建一个账户用以后续登录管理JPOM" />
      <br />
      <a-form :form="loginForm" :label-col="{ span: 0 }" @submit="handleLogin" class="init-form">
        <a-form-item :wrapper-col="{ span: 24 }" class="init-user-name">
          <a-input v-decorator="['userName', { rules: [{ required: true, message: '请输入账户名' }] }]" placeholder="账户名称" />
        </a-form-item>
        <a-form-item :wrapper-col="{ span: 24 }" class="init-user-password">
          <a-input-password
            v-decorator="[
              'userPwd',
              {
                rules: [
                  { required: true, message: '请输入密码' },
                  { pattern: /^(?![\d]+$)(?![a-zA-Z]+$)(?![^\da-zA-Z]+$).{6,18}$/, message: '密码必须包含数字，字母，字符，且大于6位' },
                ],
              },
            ]"
            placeholder="密码（6-18位数字、字母、符号组合）"
          />
        </a-form-item>

        <a-button type="primary" html-type="submit" class="btn"> 创建并登录 </a-button>
      </a-form>
    </a-card>
  </div>
</template>
<script>
import { mapGetters } from "vuex";
import { initInstall } from "../../api/install";
import sha1 from "sha1";
export default {
  data() {
    return {
      loginForm: this.$form.createForm(this, { name: "login-form" }),
    };
  },
  computed: {
    ...mapGetters(["getGuideFlag"]),
  },
  watch: {
    getGuideFlag() {
      this.introGuide();
    },
  },
  created() {
    this.$nextTick(() => {
      setTimeout(() => {
        this.introGuide();
      }, 500);
    });
  },
  methods: {
    // 页面引导
    introGuide() {
      if (this.getGuideFlag) {
        this.$introJs()
          .setOptions({
            hidePrev: true,
            steps: [
              {
                title: "Jpom 导航助手",
                intro: "不要慌，出现这个页面表示您没有设置系统管理员信息，或者需要重置管理员信息",
              },
              {
                title: "Jpom 导航助手",
                element: document.querySelector(".login-card"),
                intro: "此处需要填写的信息是用以管理 Jpom 系统的系统管理员的账户密码，一定要记住哦，它是登录 jpom 的唯一凭证",
              },
              {
                title: "Jpom 导航助手",
                element: document.querySelector(".init-user-password"),
                intro: "为了您的账户安全，设定的密码需要包含字母、数字、字符，且长度于6-18位之间",
              },
            ],
          })
          .start();
        return false;
      }
      this.$introJs().exit();
    },
    // login
    handleLogin(e) {
      e.preventDefault();
      this.loginForm.validateFields((err, values) => {
        if (!err) {
          //  密码强度
          if (!this.checkPasswordStrong(values.userPwd)) {
            this.$notification.error({
              message: "系统管理员密码强度太低",
            });
            return false;
          }
          const params = {
            ...values,
            userPwd: sha1(values.userPwd),
          };
          initInstall(params).then((res) => {
            // 登录不成功，更新验证码
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              // 调用 store action 存储当前登录的用户名和 token
              this.$store.dispatch("login", { token: res.data.token, longTermToken: res.data.longTermToken }).then(() => {
                // 跳转主页面
                this.$router.push({ path: "/" });
              });
            }
          });
        }
      });
    },
    // /^.*(?=.{6,})(?=.*\d)(?=.*[A-Z])(?=.*[a-z])(?=.*[!@#$%^&*? +]).*$/
    // 验证密码安全强度
    checkPasswordStrong(fieldValue) {
      function checkStrong(sPW) {
        let Modes = 0;
        for (let i = 0; i < sPW.length; i++) {
          // 测试每一个字符的类别并统计一共有多少种模式.
          Modes |= CharMode(sPW.charCodeAt(i));
        }
        return bitTotal(Modes);
      }

      //判断字符类型
      function CharMode(iN) {
        if (iN >= 48 && iN <= 57)
          //数字
          return 1;
        if (iN >= 65 && iN <= 90)
          //大写字母
          return 2;
        if (iN >= 97 && iN <= 122)
          //小写
          return 4;
        else return 8; //特殊字符
      }

      //统计字符类型
      function bitTotal(num) {
        var modes = 0;
        for (let i = 0; i < 4; i++) {
          if (num & 1) modes++;
          num >>>= 1;
        }
        return modes;
      }

      if (!fieldValue || fieldValue == "") {
        return false;
      }
      //
      //    密码强度等级说明，字符包括：小写字母、大写字母、数字、特殊字符
      //
      //  1-- - 密码包含其中之一
      //
      // 2-- - 密码包含其中之二
      //
      // # 3-- - 密码包含其中之三
      // #
      // # 4-- - 密码包含其中之四
      return checkStrong(fieldValue) >= 3;
    },
  },
};
</script>
<style scoped>
.init-wrapper {
  width: 100vw;
  height: 100vh;
  background: #f6f6f6;
}
.login-card {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  text-align: left;
  min-width: 380px;
  border-radius: 6px;
  padding: 4px;
}
.btn {
  width: 100%;
  margin-top: 20px;
}
</style>
