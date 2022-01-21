<template>
  <div class="wrapper" :style="{ backgroundImage: `url(${bg})` }">
    <div class="switch" @click="handleToggleBg">{{ dynamicBg ? "关闭动态背景" : "开启动态背景" }}</div>
    <a-card class="login-card" hoverable>
      <a-card-meta :title="`${loginTitle}`" style="text-align: center" description="" />
      <br />
      <a-form-model ref="loginForm" :label-col="{ span: 0 }" :model="loginForm" :rules="rules" @submit="handleLogin">
        <a-form-model-item :wrapper-col="{ span: 24 }" prop="userName">
          <a-input v-model="loginForm.userName" placeholder="用户名" />
        </a-form-model-item>
        <a-form-model-item :wrapper-col="{ span: 24 }" prop="userPwd">
          <a-input-password v-model="loginForm.userPwd" placeholder="密码" />
        </a-form-model-item>
        <a-form-model-item :wrapper-col="{ span: 24 }" prop="code">
          <a-row>
            <a-col :span="14">
              <a-input v-model="loginForm.code" placeholder="验证码" />
            </a-col>
            <a-col :offset="2" :span="8">
              <div class="rand-code">
                <img :src="randCode" @click="changeCode" />
              </div>
            </a-col>
          </a-row>
        </a-form-model-item>
        <a-button type="primary" html-type="submit" class="btn-login"> 登录 </a-button>
      </a-form-model>
    </a-card>
  </div>
</template>
<script>
import { login, demoInfo } from "@/api/user";
import { checkSystem } from "@/api/install";
import sha1 from "sha1";
import defaultBg from "../../assets/images/bg.jpeg";
export default {
  data() {
    return {
      loginForm: {
        userName: "",
        userPwd: "",
        code: "",
      },
      randCode: "randCode.png",
      bg: defaultBg,
      dynamicBg: localStorage.getItem("dynamicBg") === "false" ? false : true,
      loginTitle: "登录JPOM",
      rules: {
        userName: [{ required: true, message: "请输入用户名" }],
        userPwd: [{ required: true, message: "请输入密码" }],
        code: [{ required: true, message: "请输入验证码" }],
      },
    };
  },
  created() {
    this.checkSystem();
    this.getBg();
    this.changeCode();
    this.getDemoInfo();
  },
  methods: {
    // 检查是否需要初始化
    checkSystem() {
      checkSystem().then((res) => {
        if (res.code !== 200) {
          this.$notification.warn({
            message: res.msg,
          });
        }
        if (res.code === 999) {
          this.$router.push("/system/ipAccess");
        } else if (res.code === 222) {
          this.$router.push("/install");
        }
        if (res.data?.loginTitle) {
          this.loginTitle = res.data.loginTitle;
        }
      });
    },
    // Controls the background display or hiding
    handleToggleBg() {
      this.dynamicBg = !this.dynamicBg;
      localStorage.setItem("dynamicBg", this.dynamicBg);
      this.getBg();
    },
    // Get background pic
    getBg() {
      if (this.dynamicBg) {
        this.bg = `https://picsum.photos/${screen.width}/${screen.height}/?random`;
      } else {
        this.bg = defaultBg;
      }
    },
    getDemoInfo() {
      demoInfo().then((res) => {
        if (res.data && res.data.user) {
          const h = this.$createElement;
          this.$notification.info({
            message: "温馨提示",
            description: h("div", null, [h("p", { domProps: { innerHTML: res.msg } }, null)]),
          });
          this.loginForm.userName = res.data.user;
        }
      });
    },
    // change Code
    changeCode() {
      this.randCode = "randCode.png?r=" + new Date().getTime();
    },
    // login
    handleLogin(e) {
      e.preventDefault();
      this.$refs["loginForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        const params = {
          ...this.loginForm,
          userPwd: sha1(this.loginForm.userPwd),
        };
        login(params).then((res) => {
          // 登录不成功，更新验证码
          if (res.code !== 200) {
            this.changeCode();
          } else {
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
      });
    },
  },
};
</script>
<style scoped>
.wrapper {
  width: 100vw;
  height: 100vh;
  background-color: #fbefdf;
  background-size: cover;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  overflow: hidden;
}
.switch {
  width: 128px;
  height: 38px;
  position: absolute;
  top: 49%;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  padding: 0 10px 0 18px;
  cursor: pointer;
  transform: translateX(105px);
  transition: all 0.3s ease-in-out;
  border-top-left-radius: 6px;
  border-bottom-left-radius: 6px;
}
.switch:hover {
  transform: translateX(0);
}
.switch::before {
  content: "";
  position: absolute;
  left: 10px;
  top: 13px;
  width: 10px;
  height: 10px;
  border-right: 1px solid #333;
  border-bottom: 1px solid #333;
  transform: rotate(135deg);
}

.login-card {
  min-width: 380px;
  border-radius: 8px;
}
.rand-code {
  width: 100%;
  height: 36px;
}
.rand-code img {
  width: 100%;
  height: 100%;
  display: inherit;
}
.btn-login {
  width: 100%;
  margin: 10px 0;
}
</style>
<style>
.ant-card-meta-title {
  font-size: 30px;
}
.ant-card-body {
  padding: 30px;
}
</style>
