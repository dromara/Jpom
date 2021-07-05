<template>
  <div class="user-header">
    <a-tooltip placement="left" >
      <a-button title="只保留当前的 Tab" :disabled="getTabList.length <= 1" class="close-all jpom-close-tabs" @click="closeTabs">关闭 Tab</a-button>
      <a-button title="回到旧版 UI" class="close-all jpom-old-version" @click="toOldIndex">旧版</a-button>
    </a-tooltip>
    <a-dropdown>
<!--      <a-avatar-->
<!--        shape="square"-->
<!--        size="large"-->
<!--        :style="{ backgroundColor: '#f56a00', verticalAlign: 'middle' ,fontSize:'40px'}">-->
<!--        -->
<!--      </a-avatar>-->
      <a-button class="ant-dropdown-link jpom-user-operation" :style="{ backgroundColor: '#f56a00', color: '#fff', verticalAlign: 'middle'}" @click="e => e.preventDefault()"  :title="getUserInfo.name">
        {{ avatarName }} <a-icon type="down" />
      </a-button>
      <a-menu slot="overlay">
        <a-menu-item>
          <a href="javascript:;" @click="handleUpdatePwd">修改密码</a>
        </a-menu-item>
        <!-- <a-menu-item>
          <a href="javascript:;" @click="handleUpdateName">修改昵称</a>
        </a-menu-item> -->
        <a-menu-item>
          <a href="javascript:;" @click="handleUpdateUser">用户资料</a>
        </a-menu-item>
        <a-menu-item>
          <a href="javascript:;" @click="logOut">退出登录</a>
        </a-menu-item>
      </a-menu>
    </a-dropdown>
    <!-- 修改密码区 -->
    <a-modal v-model="updateNameVisible" title="修改密码" @ok="handleUpdatePwdOk" :maskClosable="false">
      <a-form-model ref="pwdForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-model-item label="原密码" prop="oldPwd">
          <a-input-password v-model="temp.oldPwd" placeholder="Old password"/>
        </a-form-model-item>
        <a-form-model-item label="新密码" prop="newPwd">
          <a-input-password v-model="temp.newPwd" placeholder="New password"/>
        </a-form-model-item>
        <a-form-model-item label="确认密码" prop="confirmPwd">
          <a-input-password v-model="temp.confirmPwd" placeholder="Confirm password"/>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 修改用户资料区 -->
    <a-modal v-model="updateUserVisible" title="修改用户资料" @ok="handleUpdateUserOk" :maskClosable="false">
      <a-form-model ref="userForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-model-item label="Token" prop="token">
          <a-input v-model="temp.token" placeholder="Token"/>
        </a-form-model-item>
        <a-form-model-item label="邮箱地址" prop="email">
          <a-input v-model="temp.email" placeholder="邮箱地址"/>
        </a-form-model-item>
        <a-form-model-item v-show="showCode" label="邮箱验证码" prop="code">
          <a-row :gutter="8">
            <a-col :span="15">
              <a-input v-model="temp.code" placeholder="邮箱验证码"/>
            </a-col>
            <a-col :span="4">
              <a-button type="primary" :disabled="!temp.email" @click="sendEmailCode">发送验证码</a-button>
            </a-col>
          </a-row>
        </a-form-model-item>
        <a-form-model-item label="钉钉通知地址" prop="dingDing">
          <a-input v-model="temp.dingDing" placeholder="钉钉通知地址"/>
        </a-form-model-item>
        <a-form-model-item label="企业微信通知地址" prop="workWx">
          <a-input v-model="temp.workWx" placeholder="企业微信通知地址"/>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
import { mapGetters } from 'vuex';
import { updatePwd, sendEmailCode, editUserInfo } from '../../api/user';
import sha1 from 'sha1';
export default {
  data() {
    return {
      collapsed: false,
      // 修改密码框
      updateNameVisible: false,
      updateUserVisible: false,
      temp: {},
      // 表单校验规则
      rules: {
        oldPwd: [
          { required: true, message: 'Please input old password', trigger: 'blur' }
        ],
        newPwd: [
          { required: true, message: 'Please input new password', trigger: 'blur' }
        ],
        confirmPwd: [
          { required: true, message: 'Please input confirmPwd password', trigger: 'blur' }
        ],
        email: [
          { required: true, message: 'Please input email', trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    ...mapGetters([
      'getToken',
      'getUserInfo',
      'getTabList'
    ]),
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
    }
  },
  methods: {
    // 退出登录
    logOut() {
      this.$confirm({
        title: '系统提示',
        content: '真的要退出系统么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          return new Promise((resolve) => {
            // 退出登录
            this.$store.dispatch('logOut').then(() => {
              this.$notification.success({
                message: '退出登录成功',
                duration: 2
              });
              this.$router.push('/login');
              resolve();
            })
          })
        }
      });
    },
    // 加载修改密码对话框
    handleUpdatePwd() {
      this.temp = {};
      this.updateNameVisible = true;
    },
    // 修改密码
    handleUpdatePwdOk() {
      // 检验表单
      this.$refs['pwdForm'].validate(valid => {
        if (!valid) {
          return false;
        }
        // 判断两次新密码是否一致
        if (this.temp.newPwd !== this.temp.confirmPwd) {
          this.$notification.error({
            message: '两次密码不一致...',
            duration: 2
          });
          return;
        }
        // 提交修改
        const params = {
          oldPwd: sha1(this.temp.oldPwd),
          newPwd: sha1(this.temp.newPwd)
        }
        updatePwd(params).then(res => {
          // 修改成功
          if (res.code === 200) {
            // 退出登录
            this.$store.dispatch('logOut').then(() => {
              this.$notification.success({
                message: res.msg,
                duration: 2
              });
              this.$refs['pwdForm'].resetFields();
              this.updateNameVisible = false;
              this.$router.push('/login');
            })
          }
        })
      })
    },
    // 加载修改用户资料对话框
    handleUpdateUser() {
      this.temp = {...this.getUserInfo};
      this.temp.token = this.getToken;
      this.updateUserVisible = true;
    },
    // 发送邮箱验证码
    sendEmailCode() {
      if (!this.temp.email) {
        this.$notification.error({
          message: '请输入邮箱地址',
          duration: 2
        });
        return;
      }
      sendEmailCode(this.temp.email).then(res => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
            duration: 2
          });
        }
      })
    },
    // 修改用户资料
    handleUpdateUserOk() {
      // 检验表单
      this.$refs['userForm'].validate(valid => {
        if (!valid) {
          return false;
        }
        editUserInfo(this.temp).then(res => {
          // 修改成功
          if (res.code === 200) {
            this.$notification.success({
              message: res.msg,
              duration: 2
            });
            // 清空表单校验
            this.$refs['userForm'].resetFields();
            this.updateUserVisible = false;
          }
        })
      })
    },
    // 关闭 tabs
    closeTabs() {
      this.$notification.success({
        message: '操作成功',
        top: '100px',
        duration: 1
      });
      this.$store.dispatch('clearTabs');
    },
    toOldIndex() {
      window.location.href = '/old.html'
    }
  }
}
</script>
<style scoped>
.user-header {
  display: inline-table;
  width: 300px;
  text-align: right;
  margin-right: 20px;
  cursor: pointer;
}
.close-all {
  margin-right: 10px;
}
</style>