<template>
  <div>
    <a-form-model ref="editForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
      <a-form-model-item label="白名单路径" prop="id">
        <a-input v-model="temp.whiteList" type="textarea" :rows="5" style="resize: none" placeholder="请输入白名单路径白名单，回车支持输入多个路径，系统会自动过滤 ../ 路径、不允许输入根路径"/>
      </a-form-model-item>
      <a-form-model-item :wrapper-col="{ span: 14, offset: 6 }">
        <a-button type="primary" :disabled="submitAble" @click="onSubmit">提交</a-button>
      </a-form-model-item>
    </a-form-model>
  </div>
</template>
<script>
import { getDispatchWhiteList, editDispatchWhiteList } from '../../api/dispatch';
export default {
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
      getDispatchWhiteList().then(res => {
        if (res.code === 200) {
          this.temp = res.data;
        }
      })
    },
    // submit
    onSubmit() {
      // disabled submit button
      this.submitAble = true;
      const params = {
        data: this.temp.whiteList
      }
      editDispatchWhiteList(params).then(res => {
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