<template>
  <div>
    <a-space direction="vertical" style="width: 100%">
      <a-alert :message="$tl('p.warmReminder')" type="info" show-icon>
        <template #description>
          <ul>
            <li>{{ $tl('p.distributionAuthorizationPathConfig') }}</li>
            <li>{{ $tl('p.absolutePathRequired') }}</li>
          </ul>
        </template>
      </a-alert>
      <!-- <a-alert message=",不支持软链" type="info" /> -->

      <a-form ref="editForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }" @finish="onSubmit">
        <a-form-item :label="$tl('p.authorizationPath')" name="outGiving">
          <template #help>{{ $tl('p.usageForCreatingDistributionProjects') }}</template>
          <a-textarea
            v-model:value="temp.outGiving"
            :rows="5"
            style="resize: none"
            :placeholder="$tl('p.inputAuthorizationPath')"
          />
        </a-form-item>
        <a-form-item :label="$tl('p.staticDirectory')" name="staticDir">
          <template #help>{{ $tl('p.usageForStaticFileBindingAndReading') }}</template>
          <a-textarea
            v-model:value="temp.staticDir"
            :rows="5"
            style="resize: none"
            :placeholder="$tl('p.inputStaticPaths')"
          />
        </a-form-item>
        <a-form-item :label="$tl('p.remoteDownloadSecureHost')" name="allowRemoteDownloadHost">
          <template #help>{{ $tl('p.usageForDownloadingRemoteFiles') }}</template>
          <a-textarea
            v-model:value="temp.allowRemoteDownloadHost"
            :rows="5"
            style="resize: none"
            :placeholder="`${$tl('p.inputRemoteDownloadSecureHosts')}://www.test.com 等`"
          />
        </a-form-item>
        <a-form-item :wrapper-col="{ span: 14, offset: 6 }">
          <a-button type="primary" html-type="submit" :disabled="submitAble">{{ $tl('p.submit') }}</a-button>
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
    $tl(key, ...args) {
      return this.$t(`pages.dispatch.whiteList.${key}`, ...args)
    },
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
