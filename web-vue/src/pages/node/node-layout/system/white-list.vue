<template>
  <div class="node-full-content">
    <template>
      <a-alert style="margin-bottom: 20px" message="路径需要配置绝对路径,不支持软链" type="info" />
    </template>
    <a-form-model ref="editForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
      <a-form-model-item label="项目路径" prop="project">
        <a-input v-model="temp.project" type="textarea" :rows="5" style="resize: none" placeholder="请输入项目存放路径白名单，回车支持输入多个路径，系统会自动过滤 ../ 路径、不允许输入根路径" />
      </a-form-model-item>
      <a-form-model-item label="证书路径" prop="certificate">
        <a-input v-model="temp.certificate" type="textarea" :rows="5" style="resize: none" placeholder="请输入证书存放路径白名单，回车支持输入多个路径，系统会自动过滤 ../ 路径、不允许输入根路径" />
      </a-form-model-item>
      <a-form-model-item label="Nginx 白名单路径" prop="nginx">
        <a-input v-model="temp.nginx" type="textarea" :rows="5" style="resize: none" placeholder="请输入 nginx 存放路径白名单，回车支持输入多个路径，系统会自动过滤 ../ 路径、不允许输入根路径" />
      </a-form-model-item>
      <a-form-model-item label="Nginx 安装路径" prop="nginxPath">
        <a-input v-model="temp.nginxPath" placeholder="请输入 nginx安装路径,一般情况下无需配置，windows 下建议配置 nginx 安装的绝对路径,用于能 reload nginx " />
      </a-form-model-item>
      <a-form-model-item label="远程下载安全HOST" prop="allowRemoteDownloadHost">
        <a-input v-model="temp.allowRemoteDownloadHost" type="textarea" :rows="5" style="resize: none" placeholder="请输入远程下载安全HOST，回车支持输入多个路径，示例 https://www.test.com 等" />
      </a-form-model-item>
      <a-form-model-item label="文件后缀" prop="allowEditSuffix">
        <a-input
          v-model="temp.allowEditSuffix"
          type="textarea"
          :rows="5"
          style="resize: none"
          placeholder="请输入允许编辑文件的后缀及文件编码，不设置编码则默认取系统编码，示例：设置编码：txt@utf-8， 不设置编码：txt"
        />
      </a-form-model-item>
      <a-form-model-item :wrapper-col="{ span: 14, offset: 4 }">
        <a-button type="primary" :disabled="submitAble" @click="onSubmit">提交</a-button>
      </a-form-model-item>
    </a-form-model>
  </div>
</template>
<script>
import {editWhiteList, getWhiteList} from "@/api/node-system";

export default {
  props: {
    node: {
      type: Object,
    },
  },
  data() {
    return {
      temp: {},
      submitAble: false,
    };
  },
  mounted() {
    this.loadData();
  },
  methods: {
    // load data
    loadData() {
      getWhiteList(this.node.id).then((res) => {
        if (res.code === 200) {
          this.temp = res.data;
        }
      });
    },
    // submit
    onSubmit() {
      // disabled submit button
      this.submitAble = true;
      this.temp.nodeId = this.node.id;
      editWhiteList(this.temp).then((res) => {
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
  },
};
</script>
