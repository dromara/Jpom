<template>
  <div>
    <a-alert
      style="margin-bottom: 20px"
      :message="$t('pages.node.node-layout.system.white-list.ef5f6164')"
      type="info"
    />

    <a-form ref="editForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
      <a-form-item :label="$t('pages.node.node-layout.system.white-list.5b716424')" name="project">
        <a-textarea
          v-model:value="temp.project"
          :rows="5"
          style="resize: none"
          :placeholder="$t('pages.node.node-layout.system.white-list.e0f3b8cd')"
        />
      </a-form-item>

      <a-form-item :label="$t('pages.node.node-layout.system.white-list.5ad2c7b8')" name="allowEditSuffix">
        <a-textarea
          v-model:value="temp.allowEditSuffix"
          :rows="5"
          style="resize: none"
          :placeholder="$t('pages.node.node-layout.system.white-list.3d39570')"
        />
      </a-form-item>
      <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
        <a-button type="primary" :disabled="submitAble" @click="onSubmit">{{
          $t('pages.node.node-layout.system.white-list.d8031ed8')
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
