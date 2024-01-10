<template>
  <div class="full-content">
    <a-form-model ref="editForm" :model="temp" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
      <a-form-model-item label="SMTP 服务器" prop="host">
        <a-auto-complete v-model="temp.host" placeholder="SMTP 服务器域名" option-label-prop="value">
          <template slot="dataSource">
            <a-select-opt-group v-for="group in hostDataSource" :key="group.title">
              <span slot="label">
                {{ group.title }}
              </span>
              <a-select-option v-for="opt in group.children" :key="opt.title" :value="opt.value"> {{ opt.title }} {{ opt.value }} </a-select-option>
            </a-select-opt-group>
          </template>
        </a-auto-complete>
      </a-form-model-item>
      <a-form-model-item label="SMTP 端口" prop="port">
        <a-auto-complete v-model="temp.port" placeholder="SMTP 服务器端口" option-label-prop="value">
          <template slot="dataSource">
            <a-select-opt-group v-for="group in portDataSource" :key="group.title">
              <span slot="label">
                {{ group.title }}
              </span>
              <a-select-option v-for="opt in group.children" :key="opt.title" :value="opt.value"> {{ opt.title }} {{ opt.value }} </a-select-option>
            </a-select-opt-group>
          </template>
        </a-auto-complete>
      </a-form-model-item>
      <a-form-model-item label="用户名" prop="user">
        <a-input v-model="temp.user" type="text" placeholder="发件人名称" />
      </a-form-model-item>
      <a-form-model-item label="密码" :prop="`${this.temp.type === 'add' ? 'pass' : 'pass-update'}`">
        <a-input-password v-model="temp.pass" type="text" placeholder="邮箱密码或者授权码" />
      </a-form-model-item>
      <a-form-model-item label="邮箱账号" prop="from">
        <!-- <a-input v-model="temp.from" type="text" placeholder="发送方邮箱账号" /> -->
        <a-tooltip>
          <template slot="title">
            支持配置发送方：遵循RFC-822标准 发件人可以是以下形式：
            <ul>
              <li>1. user@xxx.xx</li>
              <li>2. name &lt;user@xxx.xx&gt;</li>
            </ul>
          </template>
          <a-auto-complete v-model="temp.from" placeholder="发送方邮箱账号" option-label-prop="value" @search="handleFromSearch">
            <template slot="dataSource">
              <a-select-option v-for="email in fromResult" :key="email">
                {{ email }}
              </a-select-option>
            </template>
          </a-auto-complete>
        </a-tooltip>
      </a-form-model-item>
      <a-form-model-item label="SSL 连接" prop="sslEnable">
        <a-switch v-model="temp.sslEnable" checked-children="启用" un-checked-children="停用" />
        <!-- <a-input v-show="temp.sslEnable" v-model="temp.socketFactoryPort" type="text" placeholder="SSL 端口" /> -->
      </a-form-model-item>
      <a-form-model-item label="超时时间" prop="timeout">
        <a-input-number style="width: 100%" :min="3" v-model="temp.timeout" type="text" placeholder="单位秒，默认 10 秒,最小 3 秒" />
      </a-form-model-item>

      <a-form-model-item :wrapper-col="{ span: 14, offset: 4 }">
        <a-button type="primary" class="btn" :disabled="submitAble" @click="onSubmit">提交</a-button>
      </a-form-model-item>
    </a-form-model>
    <a-alert message="阿里云企业邮箱配置" description="SMTP 地址：smtp.mxhichina.com，端口使用 465 并且开启 SSL，用户名需要和邮件发送人一致，密码为邮箱的登录密码" type="info" show-icon />
    <br />
    <a-alert message="QQ 邮箱配置" type="info" description="SMTP 地址：【smtp.qq.com】，用户名一般是QQ号码，密码是邮箱授权码，端口默认 587/465" show-icon />
    <br />
    <a-alert message="163 邮箱配置" description="SMTP 地址：【smtp.163.com, smtp.126.com...】，密码是邮箱授权码，端口默认 25，SSL 端口 465" type="info" show-icon />
    <br />
    <a-alert message="Gmail 邮箱配置" description="待完善" type="info" show-icon />
  </div>
</template>
<script>
import { getMailConfigData, editMailConfig } from "@/api/system";

export default {
  data() {
    return {
      temp: {},
      submitAble: false,
      rules: {
        host: [{ required: true, message: "Please input SMTP host", trigger: "blur" }],
        pass: [{ required: true, message: "Please input password", trigger: "blur" }],
        user: [{ required: true, message: "Please input user name", trigger: "blur" }],
        from: [{ required: true, message: "Please input email account", trigger: "blur" }],
      },
      fromResult: [],
      hostDataSource: [
        {
          title: "参考数据",
          children: [
            {
              title: "QQ邮箱",
              value: "smtp.qq.com",
            },
            {
              title: "163邮箱",
              value: "smtp.163.com",
            },
            {
              title: "126邮箱",
              value: "smtp.126.com",
            },
            {
              title: "阿里云企业邮箱",
              value: "smtp.mxhichina.com",
            },
            {
              title: "gmail邮箱",
              value: "smtp.gmail.com",
            },
          ],
        },
      ],
      portDataSource: [
        {
          title: "QQ邮箱",
          children: [
            {
              title: "QQ邮箱",
              value: "587",
            },
            {
              title: "QQ邮箱 SSL",
              value: "465",
            },
          ],
        },
        {
          title: "163邮箱",
          children: [
            {
              title: "163邮箱",
              value: "25",
            },
            {
              title: "163邮箱 SSL",
              value: "465",
            },
          ],
        },
        {
          title: "阿里云企业邮箱",
          children: [
            {
              title: "阿里云企业邮箱 SSL",
              value: "465",
            },
          ],
        },
        {
          title: "通用邮箱",
          children: [
            {
              title: "通用邮箱 SSL",
              value: "465",
            },
          ],
        },
      ],
    };
  },
  mounted() {
    this.loadData();
  },
  methods: {
    // load data
    loadData() {
      getMailConfigData().then((res) => {
        if (res.code === 200) {
          this.temp = res.data || { type: "add" };
          if (this.temp.port) {
            this.temp.port = this.temp.port + "";
          }
        }
      });
    },
    // submit
    onSubmit() {
      // disabled submit button
      this.submitAble = true;
      // if (this.temp.sslsslEnable === false) {
      //   this.temp.socketFactoryPort = "";
      // }
      editMailConfig(this.temp).then((res) => {
        if (res.code === 200) {
          // 成功
          this.$notification.success({
            message: res.msg,
          });
        }
        // button recover
        this.submitAble = false;
      });
    },
    handleFromSearch(value) {
      let result;
      if (!value || value.indexOf("@") >= 0) {
        result = [];
      } else {
        result = ["gmail.com", "163.com", "qq.com"].map((domain) => `${value}@${domain}`);
      }
      this.fromResult = result;
    },
  },
};
</script>
<style scoped>
.btn {
  margin-left: 20px;
}
</style>
