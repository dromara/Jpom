<template>
  <div>
    <a-space direction="vertical" style="width: 100%">
      <a-alert :message="$t('i18n_c8c6e37071')" type="info" show-icon>
        <template #description>
          <ul>
            <li>{{ $t('i18n_cf38e8f9fd') }}</li>
            <li>{{ $t('i18n_a4f629041c') }}</li>
          </ul>
        </template>
      </a-alert>
      <!-- <a-alert message=",不支持软链" type="info" /> -->

      <a-form ref="editForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }" @finish="onSubmit">
        <a-form-item :label="$t('i18n_28e1eec677')" name="outGiving">
          <template #help>{{ $t('i18n_5b1f0fd370') }}</template>
          <a-textarea
            v-model:value="temp.outGiving"
            :rows="5"
            style="resize: none"
            :placeholder="$t('i18n_9b78491b25')"
          />
        </a-form-item>
        <a-form-item :label="$t('i18n_6f7ee71e77')" name="staticDir">
          <template #help>{{ $t('i18n_3f8cedd1d7') }}</template>
          <a-textarea
            v-model:value="temp.staticDir"
            :rows="5"
            style="resize: none"
            :placeholder="$t('i18n_ec7ef29bdf')"
          />
        </a-form-item>
        <a-form-item :label="$t('i18n_95dbee0207')" name="allowRemoteDownloadHost">
          <template #help>{{ $t('i18n_aadf9d7028') }}</template>
          <a-textarea
            v-model:value="temp.allowRemoteDownloadHost"
            :rows="5"
            style="resize: none"
            :placeholder="$t('i18n_c32e7adb20')"
          />
        </a-form-item>
        <a-form-item :wrapper-col="{ span: 14, offset: 6 }">
          <a-button type="primary" html-type="submit" :disabled="submitAble">{{ $t('i18n_939d5345ad') }}</a-button>
        </a-form-item>
      </a-form>
    </a-space>
  </div>
</template>
<script>
import { getDispatchWhiteList, editDispatchWhiteList } from '@/api/dispatch'
export default {
  props: {
    workspaceId: {
      type: String,
      default: ''
    }
  },
  emits: ['cancel'],
  data() {
    return {
      temp: {},
      submitAble: true
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    // load data
    loadData() {
      this.loading = true
      getDispatchWhiteList({ workspaceId: this.workspaceId }).then((res) => {
        if (res.code === 200) {
          this.temp = res.data
          this.submitAble = false
        }
      })
    },
    // submit
    onSubmit() {
      // disabled submit button
      this.submitAble = true
      editDispatchWhiteList({
        ...this.temp,
        workspaceId: this.workspaceId
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
