<template>
  <div class="mail-config">
    <a-form-model ref="editForm" :model="temp" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
      <a-form-model-item label="SMTP 服务器" prop="host">
        <a-input v-model="temp.host" type="text" placeholder="SMTP 服务器域名"/>
      </a-form-model-item>
      <a-form-model-item label="SMTP 端口" prop="port">
        <a-input v-model="temp.port" type="text" placeholder="SMTP 服务器端口"/>
      </a-form-model-item>
      <a-form-model-item label="用户名" prop="user">
        <a-input v-model="temp.user" type="text" placeholder="发件人名称"/>
      </a-form-model-item>
      <a-form-model-item label="密码" prop="pass">
        <a-input-password v-model="temp.pass" type="text" placeholder="邮箱密码或者授权码"/>
      </a-form-model-item>
      <a-form-model-item label="邮箱账号" prop="from">
        <a-input v-model="temp.from" type="text" placeholder="发送方邮箱账号"/>
      </a-form-model-item>
      <a-form-model-item label="SSL 连接" prop="sslEnable">
        <a-switch v-model="temp.sslEnable" checked-children="启用" un-checked-children="停用" />
        <a-input v-show="temp.sslEnable" v-model="temp.socketFactoryPort" type="text" placeholder="SSL 端口"/>
      </a-form-model-item>
      <a-form-model-item :wrapper-col="{ span: 14, offset: 4 }">
        <a-button type="primary" class="btn" :disabled="submitAble" @click="onSubmit">提交</a-button>
      </a-form-model-item>
    </a-form-model>
    <a-alert
      message="阿里云企业邮箱配置"
      description="SMTP 地址：smtp.mxhichina.com，端口使用 465 并且开启 SSL，用户名需要和邮件发送人一致，密码为邮箱的登录密码"
      type="info"
      show-icon/>
    <br/>
    <a-alert
      message="QQ 邮箱配置"
      description="待完善"
      type="info"
      show-icon/>
    <br/>
    <a-alert
      message="163 邮箱配置"
      description="SMTP 地址：【smtp.163.com, smtp.126.com...】，密码是邮箱授权码，端口默认 25，SSL 端口 465"
      type="info"
      show-icon/>
    <br/>
    <a-alert
      message="Gmail 邮箱配置"
      description="待完善"
      type="info"
      show-icon/>
  </div>
</template>
<script>
import { getMailConfigData, editMailConfig } from '../../api/system';
export default {
  data() {
    return {
      temp: {},
      submitAble: false,
      rules: {
        host: [
          { required: true, message: 'Please input SMTP host', trigger: 'blur' }
        ],
        pass: [
          { required: true, message: 'Please input password', trigger: 'blur' }
        ],
        user: [
          { required: true, message: 'Please input user name', trigger: 'blur' }
        ],
        from: [
          { required: true, message: 'Please input email account', trigger: 'blur' }
        ]
      }
    }
  },
  mounted() {
    this.loadData();
  },
  methods: {
    // load data
    loadData() {
      getMailConfigData().then(res => {
        if (res.code === 200) {
          this.temp = res.data || {};
        }
      })
    },
    // submit
    onSubmit() {
      // disabled submit button
      this.submitAble = true;
      if (this.temp.sslsslEnable === false) {
        this.temp.socketFactoryPort = '';
      }
      editMailConfig(this.temp).then(res => {
        if (res.code === 200) {
          // 成功
          this.$notification.success({
            message: res.msg,
            duration: 2
          });
        }
        // button recover
        this.submitAble = false;
      })
    }
  }
}
</script>
<style scoped>
.mail-config {
  height: calc(100vh - 115px);
  overflow-y: scroll;
}
.btn {
  margin-left: 20px;
}
</style>