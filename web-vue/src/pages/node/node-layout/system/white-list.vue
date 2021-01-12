<template>
  <div>
    <a-form-model ref="editForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
      <a-form-model-item label="项目路径" prop="id">
        <a-input v-model="temp.project" type="textarea" :rows="5" style="resize: none" placeholder="请输入项目存放路径白名单，回车支持输入多个路径，系统会自动过滤 ../ 路径、不允许输入根路径"/>
      </a-form-model-item>
      <a-form-model-item label="证书路径" prop="name">
        <a-input v-model="temp.certificate" type="textarea" :rows="5" style="resize: none" placeholder="请输入证书存放路径白名单，回车支持输入多个路径，系统会自动过滤 ../ 路径、不允许输入根路径"/>
      </a-form-model-item>
      <a-form-model-item label="Nginx 路径" prop="name">
        <a-input v-model="temp.nginx" type="textarea" :rows="5" style="resize: none" placeholder="请输入 nginx 存放路径白名单，回车支持输入多个路径，系统会自动过滤 ../ 路径、不允许输入根路径"/>
      </a-form-model-item>
      <a-form-model-item :wrapper-col="{ span: 14, offset: 4 }">
        <a-button type="primary" :disabled="submitAble" @click="onSubmit">提交</a-button>
      </a-form-model-item>
    </a-form-model>
  </div>
</template>
<script>
import { getWhiteList, editWhiteList } from '../../../../api/node-system';
export default {
  props: {
    node: {
      type: Object
    }
  },
  data() {
    return {
      temp: {},
      submitAble: false
    }
  },
  mounted() {
    this.loadData();
  },
  methods: {
    // load data
    loadData() {
      getWhiteList(this.node.id).then(res => {
        if (res.code === 200) {
          this.temp = res.data;
        }
      })
    },
    // submit
    onSubmit() {
      // disabled submit button
      this.submitAble = true;
      this.temp.nodeId = this.node.id;
      editWhiteList(this.temp).then(res => {
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