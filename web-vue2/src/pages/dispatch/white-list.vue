<template>
  <div>
    <a-space direction="vertical" align="center" style="display: block">
      <a-alert message="节点分发的授权路径配置" type="info" />
      <a-alert message="路径需要配置绝对路径,不支持软链" type="info" />

      <a-form-model ref="editForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-model-item label="授权路径" prop="outGiving">
          <template #help>用于创建节点分发项目、文件中心发布文件</template>
          <a-input v-model="temp.outGiving" type="textarea" :rows="5" style="resize: none" placeholder="请输入授权路径，回车支持输入多个路径，系统会自动过滤 ../ 路径、不允许输入根路径" />
        </a-form-model-item>
        <a-form-model-item label="静态目录" prop="staticDir">
          <template #help>用于静态文件绑定和读取(不建议配置大目录，避免扫描消耗过多资源)</template>
          <a-input v-model="temp.staticDir" type="textarea" :rows="5" style="resize: none" placeholder="请输入静态，回车支持输入多个路径，系统会自动过滤 ../ 路径、不允许输入根路径" />
        </a-form-model-item>
        <a-form-model-item label="远程下载安全HOST" prop="allowRemoteDownloadHost">
          <template #help>用于下载远程文件来进行节点分发和文件上传</template>
          <a-input v-model="temp.allowRemoteDownloadHost" type="textarea" :rows="5" style="resize: none" placeholder="请输入远程下载安全HOST，回车支持输入多个路径，示例 https://www.test.com 等" />
        </a-form-model-item>
        <a-form-model-item :wrapper-col="{ span: 14, offset: 6 }">
          <a-button type="primary" :disabled="submitAble" @click="onSubmit">提交</a-button>
        </a-form-model-item>
      </a-form-model>
    </a-space>
  </div>
</template>
<script>
import { getDispatchWhiteList, editDispatchWhiteList } from "@/api/dispatch";
export default {
  props: {
    workspaceId: {
      type: String,
      default: "",
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
      getDispatchWhiteList({ workspaceId: this.workspaceId }).then((res) => {
        if (res.code === 200) {
          this.temp = res.data;
        }
      });
    },
    // submit
    onSubmit() {
      // disabled submit button
      this.submitAble = true;
      editDispatchWhiteList({ ...this.temp, workspaceId: this.workspaceId }).then((res) => {
        if (res.code === 200) {
          // 成功
          this.$notification.success({
            message: res.msg,
          });
          this.$emit("cancel");
        }
        // button recover
        this.submitAble = false;
      });
    },
  },
};
</script>
