<template>
  <div>
    <a-space direction="vertical" align="center" style="display: block">
      <a-alert message="节点分发的白名单路径配置" type="info" />
      <a-alert message="路径需要配置绝对路径,不支持软链" type="info" />

      <a-form-model ref="editForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-model-item label="白名单路径" prop="id">
          <template #help>用于创建节点分发项目、文件中心发布文件</template>
          <a-input v-model="temp.outGiving" type="textarea" :rows="5" style="resize: none" placeholder="请输入白名单路径，回车支持输入多个路径，系统会自动过滤 ../ 路径、不允许输入根路径" />
        </a-form-model-item>
        <a-form-model-item label="远程下载安全HOST" prop="name">
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
import { getDispatchWhiteList, editDispatchWhiteList, editDispatchWhiteList2 } from "@/api/dispatch";
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
      const api = this.workspaceId ? editDispatchWhiteList2 : editDispatchWhiteList;
      api({ ...this.temp, workspaceId: this.workspaceId }).then((res) => {
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
