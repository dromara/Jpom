<template>
  <div class="wrapper" :style="backgroundImage">
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
    <div class="switch" @click="handleToggleBg">{{ dynamicBg ? "关闭动态背景" : "开启动态背景" }}</div>
    <a-card class="login-card" hoverable>
      <a-card-meta :title="`${loginTitle}`" style="text-align: center" description="" />
      <br />
      <template v-if="this.action === 'login'">
        <a-form-model ref="loginForm" :label-col="{ span: 0 }" :model="loginForm" :rules="rules" @submit="handleLogin">
          <a-form-model-item :wrapper-col="{ span: 24 }" prop="userName">
            <a-input v-model="loginForm.userName" placeholder="用户名" />
          </a-form-model-item>
          <a-form-model-item :wrapper-col="{ span: 24 }" prop="userPwd">
            <a-input-password v-model="loginForm.userPwd" placeholder="密码" />
          </a-form-model-item>
          <a-form-model-item v-if="!this.disabledCaptcha" :wrapper-col="{ span: 24 }" prop="code">
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
      </template>
      <template v-if="this.action === 'mfa'">
        <a-form-model ref="mfaDataForm" :label-col="{ span: 0 }" :model="mfaData" :rules="rules" @submit="handleMfa">
          <a-form-model-item label="验证码" :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }" prop="mfaCode">
            <a-input v-model="mfaData.mfaCode" placeholder="mfa 验证码" />
          </a-form-model-item>

          <a-button type="primary" html-type="submit" class="btn-login"> 确认 </a-button>
        </a-form-model>
      </template>
    </a-card>
  </div>
</template>
<script>
import { login, demoInfo, mfaVerify } from "@/api/user";
import { checkSystem } from "@/api/install";
import sha1 from "sha1";

import { mapGetters } from "vuex";
export default {
  data() {
    return {
      loginForm: {
        userName: "",
        userPwd: "",
        code: "",
      },
      mfaData: {},
      action: "login",
      randCode: "randCode.png",
      dynamicBg: localStorage.getItem("dynamicBg") === "true",
      loginTitle: "登录JPOM",
      rules: {
        userName: [{ required: true, message: "请输入用户名" }],
        userPwd: [{ required: true, message: "请输入密码" }],
        code: [{ required: true, message: "请输入验证码" }],
        mfaCode: [
          { required: true, message: "请输入两步验证码" },
          { pattern: /^\d{6}$/, message: "验证码 6 为纯数字" },
        ],
      },
      disabledCaptcha: false,
    };
  },
  created() {
    this.checkSystem();
    //this.getBg();
    this.changeCode();
    this.getDemoInfo();
  },
  computed: {
    ...mapGetters(["getWorkspaceId"]),
    backgroundImage: function () {
      if (this.dynamicBg) {
        return {
          backgroundImage: `url(https://picsum.photos/${screen.width}/${screen.height}/?random)`,
        };
      }
      return {};
    },
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
        this.disabledCaptcha = res.data.disabledCaptcha;
      });
    },
    // Controls the background display or hiding
    handleToggleBg() {
      this.dynamicBg = !this.dynamicBg;
      localStorage.setItem("dynamicBg", this.dynamicBg);
      //this.getBg();
    },
    // Get background pic
    // getBg() {},
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
          if (res.code === 201) {
            //
            this.action = "mfa";
            this.mfaData.token = res.data.tempToken;
            return;
          }
          // 登录不成功，更新验证码
          if (res.code !== 200) {
            this.changeCode();
          } else {
            this.startDispatchLogin(res);
          }
        });
      });
    },
    // 验证 验证码
    handleMfa(e) {
      e.preventDefault();
      this.$refs["mfaDataForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        mfaVerify({
          token: this.mfaData.token,
          code: this.mfaData.mfaCode,
        }).then((res) => {
          if (res.code === 201) {
            // 过期需要重新登录
            this.action = "login";
            this.mfaData = {};
            return;
          } else if (res.code === 200) {
            this.startDispatchLogin(res);
          }
        });
      });
    },
    startDispatchLogin(res) {
      this.$notification.success({
        message: res.msg,
      });
      if (!this.getWorkspaceId) {
        // 还没有选择工作空间，默认选中第一个 用户加载菜单
        let firstWorkspace = res.data.bindWorkspaceModels[0];
        this.$store.dispatch("changeWorkspace", firstWorkspace.id).then(() => {
          this.dispatchLogin(res.data);
        });
      } else {
        this.dispatchLogin(res.data);
      }
    },
    dispatchLogin(data) {
      // 调用 store action 存储当前登录的用户名和 token
      this.$store.dispatch("login", { token: data.token, longTermToken: data.longTermToken }).then(() => {
        // 跳转主页面
        this.$router.push({ path: "/" });
      });
    },
  },
};
</script>
<style scoped>
.wrapper {
  width: 100vw;
  height: 100vh;
  /* background-color: #fbefdf; */
  background: linear-gradient(#1890ff, #66a9c9);
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
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
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
