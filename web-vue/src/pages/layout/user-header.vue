<template>
  <div class="user-header">
    <a-tooltip placement="left" title="切换工作空间">
      <a-select v-model="selectWorkspace" class="workspace jpom-workspace" option-filter-prop="children" show-search placeholder="工作空间" @change="handleChange">
        <a-icon slot="suffixIcon" type="swap" />
        <a-select-option v-for="item in myWorkspaceList" :key="item.id">
          <a-tooltip placement="left" :title="item.name">
            {{ item.name }}
          </a-tooltip>
        </a-select-option>
      </a-select>
    </a-tooltip>
    <a-dropdown>
      <!--      <a-avatar-->
      <!--        shape="square"-->
      <!--        size="large"-->
      <!--        :style="{ backgroundColor: '#f56a00', verticalAlign: 'middle' ,fontSize:'40px'}">-->
      <!--        -->
      <!--      </a-avatar>-->
      <a-tooltip placement="left" :title="this.getUserInfo.name">
        <a-button
          class="ant-dropdown-link jpom-user-operation"
          :style="{ backgroundColor: '#1890ff', color: '#fff', verticalAlign: 'middle', marginRight: 0 }"
          @click="(e) => e.preventDefault()"
          :title="getUserInfo.name"
        >
          {{ avatarName }} <a-icon type="down" />
        </a-button>
      </a-tooltip>
      <a-menu slot="overlay">
        <a-menu-item @click="handleUpdatePwd">
          <a-button type="link">
            <a-space><a-icon type="lock" />安全管理</a-space>
          </a-button>
        </a-menu-item>
        <!-- <a-menu-item>
          <a href="javascript:;" @click="handleUpdateName">修改昵称</a>
        </a-menu-item> -->
        <a-menu-item @click="handleUpdateUser">
          <a-button type="link">
            <a-space><a-icon type="profile" />用户资料</a-space>
          </a-button>
        </a-menu-item>
        <!-- <a-menu-item>
          <a-button type="link" @click="toggleGuide" :disabled="this.getDisabledGuide">
            <a-space> <a-icon :type="`${this.guideStatus ? 'question-circle' : 'issues-close'}`" /> {{ this.guideStatus ? "开启导航" : "关闭导航" }} </a-space>
          </a-button>
        </a-menu-item> -->
        <!-- <a-menu-item>
          <a-button @click="restGuide" type="link" :disabled="this.getDisabledGuide">
            <a-space><a-icon type="rest" /> 重置导航</a-space>
          </a-button>
        </a-menu-item> -->
        <a-menu-item>
          <a-button @click="customize" type="link">
            <a-space><a-icon type="skin" /> 个性配置</a-space>
          </a-button>
        </a-menu-item>
        <a-menu-item @click="logOut">
          <a-button type="link" icon="logout"> 退出登录 </a-button>
        </a-menu-item>
      </a-menu>
    </a-dropdown>
    <!-- 修改密码区 -->
    <a-modal v-model="updateNameVisible" :width="'60vw'" title="安全管理" :footer="null" :maskClosable="false">
      <a-tabs v-model="temp.tabActiveKey" @change="tabChange">
        <a-tab-pane :key="1" tab="修改密码">
          <a-form-model ref="pwdForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
            <a-form-model-item label="原密码" prop="oldPwd">
              <a-input-password v-model="temp.oldPwd" placeholder="请输入原密码" />
            </a-form-model-item>
            <a-form-model-item label="新密码" prop="newPwd">
              <a-input-password v-model="temp.newPwd" placeholder="请输入新密码" />
            </a-form-model-item>
            <a-form-model-item label="确认密码" prop="confirmPwd">
              <a-input-password v-model="temp.confirmPwd" placeholder="请输入确认密码" />
            </a-form-model-item>
            <a-form-model-item>
              <a-row type="flex" justify="center">
                <a-col :span="2">
                  <a-button type="primary" @click="handleUpdatePwdOk">确认重置</a-button>
                </a-col>
              </a-row>
            </a-form-model-item>
          </a-form-model>
        </a-tab-pane>
        <a-tab-pane :key="2" tab="两步验证">
          <a-row>
            <a-alert type="warning" v-if="temp.needVerify">
              <template slot="message"> 提示 </template>
              <template slot="description">
                <ul style="color: red">
                  <li>绑定成功后将不再显示,强烈建议保存此二维码或者下面的 MFA key</li>
                  <li>请使用应用扫码绑定令牌,然后输入验证码确认绑定才生效</li>
                </ul>
              </template>
            </a-alert>
            <a-col :span="12">
              <a-form-model ref="mfaForm" :model="temp" :rules="rules" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
                <a-form-model-item label="当前状态" prop="status">
                  <a-switch checked-children="开启中" disabled un-checked-children="关闭中" v-model="temp.status" />
                </a-form-model-item>
                <template v-if="temp.needVerify">
                  <a-form-model-item label="二维码">
                    <a-row>
                      <a-col :span="14">
                        <div class="qrcode" ref="qrCodeUrl" id="qrCodeUrl"></div>
                      </a-col>
                    </a-row>
                  </a-form-model-item>
                  <a-form-model-item label="MFA key">
                    <a-input
                      v-clipboard:copy="temp.mfaKey"
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
                      v-model="temp.mfaKey"
                    >
                      <a-icon slot="prefix" type="copy" />
                    </a-input>
                  </a-form-model-item>
                </template>
                <!-- 不能使用  template 包裹 否则验证不能正常启用 -->
                <a-form-model-item v-if="temp.needVerify" label="验证码" prop="twoCode">
                  <a-input v-model="temp.twoCode" ref="twoCode" placeholder="两步验证码" />
                </a-form-model-item>
                <a-form-model-item v-if="temp.needVerify">
                  <a-row type="flex" justify="center">
                    <a-col :span="2">
                      <a-button type="primary" html-type="submit" @click="handleBindMfa">确认绑定</a-button>
                    </a-col>
                  </a-row>
                </a-form-model-item>
                <!-- 不能使用  template 包裹 否则验证不能正常启用 -->
                <a-form-model-item v-if="!temp.needVerify && temp.status" label="验证码" prop="twoCode">
                  <a-input v-model="temp.twoCode" ref="twoCode" placeholder="两步验证码" />
                </a-form-model-item>
                <a-form-model-item v-if="!temp.needVerify && temp.status">
                  <a-row type="flex" justify="center">
                    <a-col :span="2">
                      <a-button type="primary" html-type="submit" @click="closeMfaFn">确认关闭</a-button>
                    </a-col>
                  </a-row>
                </a-form-model-item>

                <a-form-model-item v-if="!temp.needVerify && !temp.status">
                  <a-row type="flex" justify="center">
                    <a-col :span="2">
                      <a-button type="primary" @click="openMfaFn">开启 MFA</a-button>
                    </a-col>
                  </a-row>
                </a-form-model-item>
              </a-form-model>
            </a-col>
            <a-col :span="12">
              <h3 id="两步验证应用">两步验证应用</h3>
              <p v-for="(html, index) in MFA_APP_TIP_ARRAY" :key="index" v-html="html" />
            </a-col>
          </a-row>
        </a-tab-pane>
      </a-tabs>
    </a-modal>
    <!-- 修改用户资料区 -->
    <a-modal v-model="updateUserVisible" title="修改用户资料" @ok="handleUpdateUserOk" :maskClosable="false">
      <a-form-model ref="userForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-model-item label="临时token" prop="token">
          <a-input disabled v-model="temp.token" placeholder="Token">
            <a-tooltip
              slot="suffix"
              title="复制"
              v-clipboard:copy="temp.token"
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
            >
              <a-icon type="copy" />
            </a-tooltip>
          </a-input>
        </a-form-model-item>
        <a-form-model-item label="长期token" prop="md5Token">
          <a-input disabled v-model="temp.md5Token" placeholder="Token">
            <a-tooltip
              slot="suffix"
              title="复制"
              v-clipboard:copy="temp.md5Token"
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
            >
              <a-icon type="copy" />
            </a-tooltip>
          </a-input>
        </a-form-model-item>
        <a-form-model-item label="邮箱地址" prop="email">
          <a-input v-model="temp.email" placeholder="邮箱地址" />
        </a-form-model-item>
        <a-form-model-item v-show="showCode" label="邮箱验证码" prop="code">
          <a-row :gutter="8">
            <a-col :span="15">
              <a-input v-model="temp.code" placeholder="邮箱验证码" />
            </a-col>
            <a-col :span="4">
              <a-button type="primary" :disabled="!temp.email" @click="sendEmailCode">发送验证码</a-button>
            </a-col>
          </a-row>
        </a-form-model-item>
        <a-form-model-item label="钉钉通知地址" prop="dingDing">
          <a-input v-model="temp.dingDing" placeholder="钉钉通知地址" />
        </a-form-model-item>
        <a-form-model-item label="企业微信通知地址" prop="workWx">
          <a-input v-model="temp.workWx" placeholder="企业微信通知地址" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 个性配置区 -->
    <a-modal v-model="customizeVisible" title="个性配置区" :footer="null" :maskClosable="false">
      <a-form-model :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-alert banner>
          <template slot="message"> 下列配置信息仅在当前浏览器生效,清空浏览器缓存配置将恢复默认 </template>
        </a-alert>
        <a-form-model-item label="页面导航" prop="token">
          <a-space>
            <a-switch checked-children="开" @click="toggleGuide" :checked="!this.guideStatus" :disabled="this.getDisabledGuide" un-checked-children="关" />

            <div v-if="!this.guideStatus">
              重置导航
              <a-icon type="rest" @click="restGuide" />
            </div>
          </a-space>
        </a-form-model-item>
        <a-form-model-item label="菜单配置" prop="token">
          <a-space>
            同时展开多个：
            <a-switch checked-children="是" @click="toggleMenuMultiple" :checked="this.menuMultipleFlag" un-checked-children="否" />
          </a-space>
        </a-form-model-item>
        <a-form-model-item label="页面配置" prop="token">
          <a-space>
            自动撑开：
            <a-switch checked-children="是" @click="toggleFullScreenFlag" :checked="this.fullScreenFlag" un-checked-children="否" />
          </a-space>
        </a-form-model-item>
        <a-form-model-item label="滚动条显示" prop="token">
          <a-space>
            全局配置：
            <a-switch checked-children="显示" @click="toggleScrollbarFlag" :checked="this.scrollbarFlag" un-checked-children="不显示" />
          </a-space>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
import {mapGetters} from "vuex";
import {bindMfa, closeMfa, editUserInfo, generateMfa, getUserInfo, myWorkspace, sendEmailCode, updatePwd} from "@/api/user";
import QRCode from "qrcodejs2";
import sha1 from "sha1";
import Vue from "vue";
import {MFA_APP_TIP_ARRAY} from "@/utils/const";

export default {
  data() {
    return {
      collapsed: false,
      // 修改密码框
      updateNameVisible: false,
      updateUserVisible: false,
      temp: {},
      tempVue: null,
      myWorkspaceList: [],
      selectWorkspace: "",
      customizeVisible: false,
      // 表单校验规则
      rules: {
        oldPwd: [
          { required: true, message: "请输入原密码", trigger: "blur" },
          { max: 20, message: "密码长度为6-20", trigger: "blur" },
          { min: 6, message: "密码长度为6-20", trigger: "blur" },
        ],
        newPwd: [
          { required: true, message: "请输入新密码", trigger: "blur" },
          { max: 20, message: "密码长度为6-20", trigger: "blur" },
          { min: 6, message: "密码长度为6-20", trigger: "blur" },
        ],
        confirmPwd: [
          { required: true, message: "请输入确认密码", trigger: "blur" },
          { max: 20, message: "密码长度为6-20", trigger: "blur" },
          { min: 6, message: "密码长度为6-20", trigger: "blur" },
        ],
        email: [{ required: true, message: "请输入邮箱", trigger: "blur" }],
        twoCode: [
          { required: true, message: "请输入两步验证码", trigger: ["change", "blur"] },
          { pattern: /^\d{6}$/, message: "验证码 6 为纯数字", trigger: ["change", "blur"] },
        ],
      },
      MFA_APP_TIP_ARRAY,
    };
  },
  computed: {
    ...mapGetters(["getToken", "getUserInfo", "getWorkspaceId", "getGuideCache", "getDisabledGuide"]),
    // 处理展示的名称 中文 3 个字 其他 4 个字符
    avatarName() {
      const reg = new RegExp("[\u4E00-\u9FA5]+");
      if (reg.test(this.getUserInfo.name)) {
        return this.getUserInfo.name.substring(0, 3);
      } else {
        return this.getUserInfo.name.substring(0, 4);
      }
    },
    showCode() {
      return this.getUserInfo.email !== this.temp.email;
    },
    guideStatus() {
      return this.getGuideCache.close;
    },
    menuMultipleFlag() {
      return this.getGuideCache.menuMultipleFlag === undefined ? true : this.getGuideCache.menuMultipleFlag;
    },
    fullScreenFlag() {
      return this.getGuideCache.fullScreenFlag === undefined ? true : this.getGuideCache.fullScreenFlag;
    },
    scrollbarFlag() {
      return this.getGuideCache.scrollbarFlag === undefined ? true : this.getGuideCache.scrollbarFlag;
    },
  },
  inject: ["reload"],
  created() {
    this.init();
  },
  methods: {
    customize() {
      this.customizeVisible = true;
    },
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
    init() {
      myWorkspace().then((res) => {
        this.myWorkspaceList = res.data;
        let wid = this.$route.query.wid;
        this.selectWorkspace = wid ? wid : this.getWorkspaceId;
        if (!this.selectWorkspace) {
          this.handleChange(res.data[0]?.id);
        } else {
          this.$router.push({
            query: { ...this.$route.query, wid: this.selectWorkspace },
          });
        }
      });
    },
    // 切换引导
    toggleGuide() {
      this.$store.dispatch("toggleGuideFlag").then((flag) => {
        if (flag) {
          this.$notification.success({
            message: "关闭页面操作引导、导航",
          });
        } else {
          this.$notification.success({
            message: "开启页面操作引导、导航",
          });
        }
      });
    },
    // 切换菜单打开
    toggleMenuMultiple() {
      this.$store.dispatch("toggleMenuFlag").then((flag) => {
        if (flag) {
          this.$notification.success({
            message: "可以同时展开多个菜单",
          });
        } else {
          this.$notification.success({
            message: "同时只能展开一个菜单",
          });
        }
      });
    },
    // 页面全屏
    toggleFullScreenFlag() {
      this.$store.dispatch("toggleFullScreenFlag").then((flag) => {
        if (flag) {
          this.$notification.success({
            message: "页面内容自动撑开出现屏幕滚动条",
          });
        } else {
          this.$notification.success({
            message: "页面全屏，高度 100%。局部区域可以滚动",
          });
        }
      });
    },
    // 切换滚动条是否显示
    toggleScrollbarFlag() {
      this.$store.dispatch("toggleScrollbarFlag").then((flag) => {
        if (flag) {
          this.$notification.success({
            message: "页面内容会出现滚动条",
          });
        } else {
          this.$notification.success({
            message: "隐藏滚动条。纵向滚动方式提醒：滚轮，横行滚动方式：Shift+滚轮",
          });
        }
      });
    },
    restGuide() {
      this.$store.dispatch("restGuide").then(() => {
        this.$notification.success({
          message: "重置页面操作引导、导航成功",
        });
      });
    },
    // 退出登录
    logOut() {
      this.$confirm({
        title: "系统提示",
        content: "真的要退出系统么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          return new Promise((resolve) => {
            // 退出登录
            this.$store.dispatch("logOut").then(() => {
              this.$notification.success({
                message: "退出登录成功",
              });
              this.$router.push("/login");
              resolve();
            });
          });
        },
      });
    },
    // 加载修改密码对话框
    handleUpdatePwd() {
      this.temp = {
        tabActiveKey: 1,
      };
      this.updateNameVisible = true;
      this.$refs["pwdForm"] && this.$refs["pwdForm"].resetFields();
    },
    // 修改密码
    handleUpdatePwdOk() {
      // 检验表单
      this.$refs["pwdForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        // 判断两次新密码是否一致
        if (this.temp.newPwd !== this.temp.confirmPwd) {
          this.$notification.error({
            message: "两次密码不一致...",
          });
          return;
        }
        // 提交修改
        const params = {
          oldPwd: sha1(this.temp.oldPwd),
          newPwd: sha1(this.temp.newPwd),
        };
        updatePwd(params).then((res) => {
          // 修改成功
          if (res.code === 200) {
            // 退出登录
            this.$store.dispatch("logOut").then(() => {
              this.$notification.success({
                message: res.msg,
              });
              this.$refs["pwdForm"].resetFields();
              this.updateNameVisible = false;
              this.$router.push("/login");
            });
          }
        });
      });
    },
    // 加载修改用户资料对话框
    handleUpdateUser() {
      getUserInfo().then((res) => {
        if (res.code === 200) {
          this.temp = res.data;
          this.temp.token = this.getToken;
          //this.temp.md5Token = res.data.md5Token;
          this.updateUserVisible = true;
          this.tempVue = Vue;
        }
      });
    },
    // 发送邮箱验证码
    sendEmailCode() {
      if (!this.temp.email) {
        this.$notification.error({
          message: "请输入邮箱地址",
        });
        return;
      }
      sendEmailCode(this.temp.email).then((res) => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
          });
        }
      });
    },
    // 修改用户资料
    handleUpdateUserOk() {
      // 检验表单
      this.$refs["userForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        const tempData = Object.assign({}, this.temp);
        delete tempData.token, delete tempData.md5Token;
        editUserInfo(tempData).then((res) => {
          // 修改成功
          if (res.code === 200) {
            this.$notification.success({
              message: res.msg,
            });
            // 清空表单校验
            this.$refs["userForm"].resetFields();
            this.updateUserVisible = false;
          }
        });
      });
    },
    handleChange(value) {
      this.$store.dispatch("changeWorkspace", value);
      this.$router
        .push({
          query: { ...this.$route.query, wid: value },
        })
        .then(() => {
          this.reload();
        });
    },
    tabChange(key) {
      if (key === 1) {
        this.temp = { tabActiveKey: key };
      } else if (key == 2) {
        this.temp = { tabActiveKey: key };
        getUserInfo().then((res) => {
          if (res.code === 200) {
            this.temp = { ...this.temp, status: res.data.bindMfa };
            this.$nextTick(() => {
              this.$refs?.twoCode?.focus();
            });
          }
        });
      }
    },
    showQrCode() {
      // console.log(this.temp);
      if (!this.temp.status) {
        return;
      }
      this.tempVue = Vue;
      this.$nextTick(() => {
        const qrCodeDom = document.getElementById("qrCodeUrl");
        qrCodeDom.innerHTML = "";
        this.creatQrCode(qrCodeDom, this.temp.url);
        this.$nextTick(function () {
          this.$refs.twoCode.focus();
        });
      });
    },
    // 关闭 mfa
    closeMfaFn() {
      //console.log(this.$refs["mfaForm"]);
      this.$refs["mfaForm"].validate((valid) => {
        // console.log(valid);
        if (!valid) {
          return false;
        }
        this.$confirm({
          title: "系统提示",
          content: "确定要关闭两步验证吗？关闭后账号安全性将受到影响,关闭后已经存在的 mfa key 将失效",
          okText: "确认",
          cancelText: "取消",
          onOk: () => {
            //
            closeMfa({
              code: this.temp.twoCode,
            }).then((res) => {
              if (res.code === 200) {
                this.$notification.success({
                  message: res.msg,
                });
                this.temp = { ...this.temp, needVerify: false, status: false };
              }
            });
          },
        });
      });
    },
    // mfa 状态切换
    openMfaFn() {
      //
      generateMfa().then((res) => {
        if (res.code === 200) {
          Object.assign(this.temp, {
            status: true,
            mfaKey: res.data.mfaKey,
            url: res.data.url,
            needVerify: true,
            showSaveTip: true,
            twoCode: "",
          });
          this.temp = { ...this.temp };
          this.showQrCode();
          this.$notification.info({
            // placement: "",
            message: "需要输入验证码,确认绑定后才生效奥",
          });
        }
      });
    },
    handleBindMfa() {
      this.$refs["mfaForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        bindMfa({
          mfa: this.temp.mfaKey,
          twoCode: this.temp.twoCode,
        }).then((res) => {
          if (res.code === 200) {
            this.$notification.success({
              message: res.msg,
            });
            this.temp = { ...this.temp, needVerify: false, twoCode: "" };
          }
        });
      });
    },
  },
};
</script>
<style scoped>
.workspace {
  width: 100px;
  margin-right: 10px;
}
</style>
