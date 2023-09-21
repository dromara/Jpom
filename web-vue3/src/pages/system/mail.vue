<template>
  <div class="full-content">
    <a-form
      ref="editForm"
      :model="temp"
      :rules="rules"
      :label-col="{ span: 4 }"
      :wrapper-col="{ span: 16 }"
    >
      <a-form-item label="SMTP 服务器" prop="host">
        <a-auto-complete
          v-model:value="temp.host"
          placeholder="SMTP 服务器域名"
          class="certain-category-search"
          option-label-prop="value"
          :options="hostDataSource"
        >
          <template #option="item">
            <template v-if="item.options">
          <span>
            {{ item.title }}
          </span>
            </template>

            <template v-else>
              <div style="display: flex; justify-content: space-between">
                {{ item.title }} {{ item.value }}
              </div>
            </template>
          </template>
        </a-auto-complete>
      </a-form-item>
      <a-form-item label="SMTP 端口" prop="port">
        <a-auto-complete
          v-model:value="temp.port"
          placeholder="SMTP 服务器端口"
          option-label-prop="value"
          :options="portDataSource"
        >
          <template #option="item">
            <template v-if="item.options">
              <span>
                {{ item.title }}
              </span>
            </template>
            <template v-else>
              <div style="display: flex; justify-content: space-between">
                {{ item.title }} {{ item.value }}
              </div>
            </template>
          </template>
        </a-auto-complete>
      </a-form-item>
      <a-form-item label="用户名" prop="user">
        <a-input v-model:value="temp.user" type="text" placeholder="发件人名称"/>
      </a-form-item>
      <a-form-item :label="temp.type === 'add' ? '密码' : '密码更新'" prop="pass">
        <a-input-password v-model:value="temp.pass" type="password" placeholder="邮箱密码或者授权码"/>
      </a-form-item>
      <a-form-item label="邮箱账号" prop="from">
        <a-tooltip>
          <template #title>
            支持配置发送方：遵循 RFC-822 标准 发件人可以是以下形式：
            <ul>
              <li>1. user@xxx.xx</li>
              <li>2. name &lt;user@xxx.xx&gt;</li>
            </ul>
          </template>
          <a-auto-complete
            v-model:value="temp.from"
            placeholder="发送方邮箱账号"
            option-label-prop="value"
            @search="handleFromSearch"
          >
            <template #dataSource>
              <a-select-option v-for="email in fromResult" :key="email">
                {{ email }}
              </a-select-option>
            </template>
          </a-auto-complete>
        </a-tooltip>
      </a-form-item>
      <a-form-item label="SSL 连接" prop="sslEnable">
        <a-switch v-model:checked="temp.sslEnable" checked-children="启用" un-checked-children="停用"/>
      </a-form-item>
      <a-form-item label="超时时间" prop="timeout">
        <a-input-number style="width: 100%" :min="3" v-model:value="temp.timeout" type="number" placeholder="单位秒，默认 10 秒,最小 3 秒"/>
      </a-form-item>

      <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
        <a-button type="primary" class="btn" :disabled="submitAble" @click="onSubmit">提交</a-button>
      </a-form-item>
    </a-form>
    <a-alert message="阿里云企业邮箱配置" description="SMTP 地址：smtp.mxhichina.com，端口使用 465 并且开启 SSL，用户名需要和邮件发送人一致，密码为邮箱的登录密码" type="info" show-icon/>
    <br/>
    <a-alert message="QQ 邮箱配置" type="info" description="SMTP 地址：【smtp.qq.com】，用户名一般是 QQ 号码，密码是邮箱授权码，端口默认 587/465" show-icon/>
    <br/>
    <a-alert message="163 邮箱配置" description="SMTP 地址：【smtp.163.com, smtp.126.com...】，密码是邮箱授权码，端口默认 25，SSL 端口 465" type="info" show-icon/>
    <br/>
    <a-alert message="Gmail 邮箱配置" description="待完善" type="info" show-icon/>
  </div>
</template>

<script lang="ts" setup>
import {ref, onMounted} from 'vue';
import {getMailConfigData, editMailConfig} from "@/api/system";
import {message} from "ant-design-vue";

const temp = ref({});
const submitAble = ref(false);
const fromResult = ref<string[]>([]);
const hostDataSource = [
  {
    title: "参考数据",
    options: [
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
];
const portDataSource = ref([
  {
    title: "QQ邮箱",
    options: [
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
    options: [
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
    options: [
      {
        title: "阿里云企业邮箱 SSL",
        value: "465",
      },
    ],
  },
  {
    title: "通用邮箱",
    options: [
      {
        title: "通用邮箱 SSL",
        value: "465",
      },
    ],
  },
]);

const rules = {
  host: [{required: true, message: "Please input SMTP host", trigger: "blur"}],
  pass: [{required: true, message: "Please input password", trigger: "blur"}],
  user: [{required: true, message: "Please input user name", trigger: "blur"}],
  from: [{required: true, message: "Please input email account", trigger: "blur"}],
};

// load data
const loadData = () => {
  getMailConfigData().then((res) => {
    if (res.code === 200) {
      temp.value = res.data || {type: "add"};
      if (temp.value.port) {
        temp.value.port = temp.value.port + "";
      }
    }
  });
};

// submit
const onSubmit = () => {
  // disabled submit button
  submitAble.value = true;
  editMailConfig(temp.value).then((res) => {
    if (res.code === 200) {
      // 成功
      message.success(res.msg);
    }
    // button recover
    submitAble.value = false;
  });
};

const handleFromSearch = (value: string) => {
  let result;
  if (!value || value.indexOf("@") >= 0) {
    result = [];
  } else {
    result = ["gmail.com", "163.com", "qq.com"].map((domain) => `${value}@${domain}`);
  }
  fromResult.value = result;
};

onMounted(() => {
  loadData();
});
</script>

<style scoped>
.btn {
  margin-left: 20px;
}
</style>
