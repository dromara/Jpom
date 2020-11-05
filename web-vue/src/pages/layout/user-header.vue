<template>
  <div class="user-header">
    <a-dropdown>
      <a-avatar
        shape="square"
        size="large"
        :style="{ backgroundColor: '#f56a00', verticalAlign: 'middle' }">
        <a-tooltip placement="left" :title="getUserName">
          {{ avatarName }}
        </a-tooltip>
      </a-avatar>
      <a-menu slot="overlay">
        <a-menu-item>
          <a href="javascript:;" @click="handleUpdatePwd">修改密码</a>
        </a-menu-item>
        <!-- <a-menu-item>
          <a href="javascript:;" @click="handleUpdateName">修改昵称</a>
        </a-menu-item> -->
        <a-menu-item>
          <a href="javascript:;">用户资料</a>
        </a-menu-item>
        <a-menu-item>
          <a href="javascript:;" @click="logOut">退出登录</a>
        </a-menu-item>
      </a-menu>
    </a-dropdown>
    <!-- 编辑区 -->
    <a-modal v-model="updateNamevisible" title="修改昵称" @ok="handleUpdatePwdOk" :maskClosable="false">
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
  </div>
</template>
<script>
import { mapGetters } from 'vuex';
import { updatePwd } from '../../api/user';
import sha1 from 'sha1';
export default {
  data() {
    return {
      collapsed: false,
      // 修改密码框
      updateNamevisible: false,
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
      }
    }
  },
  computed: {
    ...mapGetters([
      'getToken',
      'getUserName'
    ]),
    // 处理展示的名称 中文 3 个字 其他 4 个字符
    avatarName() {
      const reg = new RegExp("[\u4E00-\u9FA5]+");
      if (reg.test(this.getUserName)) {
        return this.getUserName.substring(0, 3);
      } else {
        return this.getUserName.substring(0, 4);
      }
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
      this.updateNamevisible = true;
      this.$nextTick(() => {
        this.$refs['pwdForm'].resetFields();
      })
    },
    // 修改密码
    handleUpdatePwdOk() {
      // 检验表单
      this.$refs['pwdForm'].validate((err) => {
        if (!err) {
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
                this.updateNamevisible = false;
                this.$router.push('/login');
              })
            }
          })
        }
      })
    }
  }
}
</script>
<style scoped>
.user-header {
  margin-right: 20px;
  cursor: pointer;
}
</style>