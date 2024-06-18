<template>
  <div>
    <a-alert
      style="margin-bottom: 20px"
      :message="$t('i18n.020f31f535')"
      type="info"
    />

    <a-form ref="editForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
      <a-form-item :label="$t('i18n.aabdc3b7c0')" name="project">
        <a-textarea
          v-model:value="temp.project"
          :rows="5"
          style="resize: none"
          :placeholder="$t('i18n.631d5b88ab')"
        />
      </a-form-item>

      <a-form-item :label="$t('i18n.649231bdee')" name="allowEditSuffix">
        <a-textarea
          v-model:value="temp.allowEditSuffix"
          :rows="5"
          style="resize: none"
          :placeholder="$t('i18n.afa8980495')"
        />
      </a-form-item>
      <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
        <a-button type="primary" :disabled="submitAble" @click="onSubmit">{{
          $t('i18n.939d5345ad')
        }}</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>
<script>
import { editWhiteList, getWhiteList } from '@/api/node-system'

export default {
  props: {
    machineId: {
      type: String,
      default: ''
    },
    nodeId: {
      type: String,
      default: ''
    }
  },
  emits: ['cancel'],
  data() {
    return {
      temp: {},
      submitAble: false
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    // load data
    loadData() {
      getWhiteList({
        machineId: this.machineId,
        nodeId: this.nodeId
      }).then((res) => {
        if (res.code === 200) {
          this.temp = res.data
        }
      })
    },
    // submit
    onSubmit() {
      // disabled submit button
      this.submitAble = true

      editWhiteList({
        ...this.temp,
        machineId: this.machineId,
        nodeId: this.nodeId
      })
        .then((res) => {
          if (res.code === 200) {
            // 成功
            $notification.success({
              message: res.msg
            })
            this.$emit('cancel')
          }
        })
        .finally(() => {
          // button recover
          this.submitAble = false
        })
    }
  }
}
</script>
