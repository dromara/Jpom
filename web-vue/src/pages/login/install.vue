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
    <a-card class="card-box" :style="`${setpCurrent === 1 ? 'width: 60vw' : 'width: 550px'}`" hoverable :bodyStyle="{ padding: '24px 0' }">
      <template slot="title">
        <a-steps :current="setpCurrent">
          <a-step title="初始化系统" status="process" description="设置一个超级管理员账号">
            <a-icon slot="icon" type="user" />
          </a-step>
          <a-step title="启用两步验证" description="开启两步验证使账号更安全">
            <a-icon slot="icon" type="file-protect" />
          </a-step>
        </a-steps>
      </template>

      <a-row type="flex" justify="center">
        <a-col :span="16" v-if="setpCurrent === 0">
          <a-card-meta title="初始化系统账户" style="textalign: center" description="您需要创建一个账户用以后续登录管理系统,请牢记超级管理员账号密码" />
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
            <a-form-item>
              <a-row type="flex" justify="center">
                <a-col :span="4">
                  <a-button type="primary" html-type="submit" class="btn"> 创建账号 </a-button>
                </a-col>
              </a-row>
            </a-form-item>
          </a-form>
        </a-col>
        <a-col :span="22" v-if="setpCurrent === 1">
          <a-alert banner>
            <template slot="message">
              <ul style="color: red">
                <li>为了考虑系统安全我们强烈建议超级管理员开启两步验证来确保账号的安全性</li>
                <li>绑定成功后将不再显示,强烈建议保存此二维码或者下面的 MFA key</li>
                <li>请使用应用扫码绑定令牌,然后输入验证码确认绑定才生效</li>
              </ul>
            </template>
          </a-alert>
          <br />
          <a-row>
            <a-col :span="12">
              <a-form-item>
                <h3 id="两步验证应用">两步验证应用</h3>
                <p v-for="(html, index) in MFA_APP_TIP_ARRAY" :key="index" v-html="html" />
              </a-form-item>
            </a-col>
            <a-divider type="vertical" />
            <a-col :span="12">
              <a-form :form="bindMfaForm" :label-col="{ span: 0 }" @submit="handleMfaSure" class="init-form">
                <a-form-item label="二维码" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }" style="margin-bottom: 5px">
                  <a-row>
                    <a-col :span="14">
                      <div class="qrcode" ref="qrCodeUrl" id="qrCodeUrl"></div>
                    </a-col>
                  </a-row>
                </a-form-item>
                <a-form-item label="MFA key" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
                  <a-input
                    v-clipboard:copy="mfaData.key"
                    v-clipboard:success="
                      () => {
                        tempVue.prototype.$notification.success({ message: '复制成功' });
                      }
                    "
                    v-clipboard:error="
                      () => {
                        tempVue.prototype.$notification.error({ message: '复制失败' });
                      }
                    "
                    readOnly
                    disabled
                    v-model="mfaData.key"
                  >
                    <a-icon slot="prefix" type="copy" />
                  </a-input>
                </a-form-item>

                <a-form-item label="验证码" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
                  <a-input
                    v-decorator="[
                      'twoCode',
                      {
                        rules: [
                          { required: true, message: '请输入两步验证码' },
                          { pattern: /^\d{6}$/, message: '验证码 6 为纯数字' },
                        ],
                      },
                    ]"
                    placeholder="两步验证码"
                  />
                </a-form-item>

                <a-form-item>
                  <a-row type="flex" justify="center">
                    <a-col :span="10">
                      <a-space>
                        <a-button type="primary" html-type="submit" class="btn"> 确认绑定 </a-button>
                        <a-button type="dashed" @click="ignoreBindMfa"> 忽略 </a-button>
                      </a-space>
                    </a-col>
                  </a-row>
                </a-form-item>
              </a-form>
            </a-col>
          </a-row>
        </a-col>
      </a-row>
    </a-card>
  </div>
</template>
<script>
import { initInstall } from "@/api/install";
import { bindMfa } from "@/api/user";
import { MFA_APP_TIP_ARRAY } from "@/utils/const";
import QRCode from "qrcodejs2";
import sha1 from "sha1";
import Vue from "vue";
export default {
  data() {
    return {
      loginForm: this.$form.createForm(this, { name: "login-form" }),
      bindMfaForm: this.$form.createForm(this, { name: "bind-mfa-form" }),
      setpCurrent: 0,
      mfaData: {},
      tempVue: Vue,
      MFA_APP_TIP_ARRAY,
    };
  },
  computed: {},
  watch: {},
  created() {
    this.$nextTick(() => {
      setTimeout(() => {
        this.introGuide();
      }, 500);
      // this.creatQrCode();
    });
  },
  methods: {
    creatQrCode(qrCodeDom, text) {
      // console.log(qrCodeDom);
      new QRCode(qrCodeDom, {
        text: text || "xxxx",
        width: 120,
        height: 120,
        colorDark: "#000000",
        colorLight: "#ffffff",
        correctLevel: QRCode.CorrectLevel.H,
      });
    },
    // 页面引导
    introGuide() {
      this.$store.dispatch("tryOpenGuide", {
        key: "install",
        options: {
          hidePrev: true,
          steps: [
            {
              title: "导航助手",
              intro: "不要慌，出现这个页面表示您没有设置系统管理员信息，或者需要重置管理员信息",
            },
            {
              title: "导航助手",
              element: document.querySelector(".login-card"),
              intro: "此处需要填写的信息是用以管理系统的系统管理员的账户密码，一定要记住哦，它是登录的唯一凭证",
            },
            {
              title: "导航助手",
              element: document.querySelector(".init-user-password"),
              intro: "为了您的账户安全，设定的密码需要包含字母、数字、字符，且长度于6-18位之间",
            },
          ],
        },
      });
    },
    // login
    handleLogin(e) {
      e.preventDefault();
      this.loginForm.validateFields((err, values) => {
        if (!err) {
          //  密码强度
          // if (!this.checkPasswordStrong(values.userPwd)) {
          //   this.$notification.error({
          //     message: "系统管理员密码强度太低",
          //   });
          //   return false;
          // }
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
              const tokenData = res.data.tokenData;
              this.mfaData = {
                key: res.data.mfaKey,
                url: res.data.url,
              };
              this.setpCurrent = 1;
              this.$nextTick(() => {
                const qrCodeDom = document.getElementById("qrCodeUrl");
                this.creatQrCode(qrCodeDom, this.mfaData.url);
              });

              // // 调用 store action 存储当前登录的用户名和 token
              this.$store.dispatch("login", { token: tokenData.token, longTermToken: tokenData.longTermToken }).then(() => {
                // 跳转主页面
                //  this.$router.push({ path: "/" });
              });
              const firstWorkspace = tokenData.bindWorkspaceModels[0];
              this.$store.dispatch("changeWorkspace", firstWorkspace.id).then(() => {});
            }
          });
        }
      });
    },
    handleMfaSure(e) {
      e.preventDefault();
      this.bindMfaForm.validateFields((err, values) => {
        if (!err) {
          const params = {
            ...values,
            mfa: this.mfaData.key,
          };

          bindMfa(params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              // 跳转主页面;
              this.$router.push({ path: "/" });
            }
          });
        }
      });
    },
    // 忽略 mfa
    ignoreBindMfa() {
      this.$confirm({
        title: "系统提示",
        content: "确定要忽略绑定两步验证吗？强烈建议超级管理员开启两步验证来保证账号安全性",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          this.$router.push({ path: "/" });
        },
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
  /* background: #f6f6f6; */
  /* height: 100vh; */
  background: linear-gradient(#1890ff, #66a9c9);
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 16px;
  /* color: #03e9f4; */
}
.card-box {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  text-align: left;
  /* width: 550px; */
  border-radius: 6px;
  padding: 4px;
}
p {
  margin-bottom: 0;
  line-height: 30px;
}
.card-content {
  /* width: 300px; */
  /* text-align: center; */
}
.qrcode {
  margin-left: 15px;
}
/* .btn {
  width: 100%;
  margin-top: 20px;
} */
</style>
