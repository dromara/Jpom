<template>
  <div>
    <a-space direction="vertical" style="width: 100%">
      <a-alert :message="$t('pages.dispatch.white-list.30376d6d')" type="info" show-icon>
        <template #description>
          <ul>
            <li>{{ $t('pages.dispatch.white-list.82002fa4') }}</li>
            <li>{{ $t('pages.dispatch.white-list.468a7b4d') }}</li>
          </ul>
        </template>
      </a-alert>
      <!-- <a-alert message=",不支持软链" type="info" /> -->

      <a-form ref="editForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }" @finish="onSubmit">
        <a-form-item :label="$t('pages.dispatch.white-list.87db3fab')" name="outGiving">
          <template #help>{{ $t('pages.dispatch.white-list.ce6ec671') }}</template>
          <a-textarea
            v-model:value="temp.outGiving"
            :rows="5"
            style="resize: none"
            :placeholder="$t('pages.dispatch.white-list.b3f9e000')"
          />
        </a-form-item>
        <a-form-item :label="$t('pages.dispatch.white-list.b9e442da')" name="staticDir">
          <template #help>{{ $t('pages.dispatch.white-list.e6655bef') }}</template>
          <a-textarea
            v-model:value="temp.staticDir"
            :rows="5"
            style="resize: none"
            :placeholder="$t('pages.dispatch.white-list.8190377f')"
          />
        </a-form-item>
        <a-form-item :label="$t('pages.dispatch.white-list.b850dd8a')" name="allowRemoteDownloadHost">
          <template #help>{{ $t('pages.dispatch.white-list.cb53f096') }}</template>
          <a-textarea
            v-model:value="temp.allowRemoteDownloadHost"
            :rows="5"
            style="resize: none"
            :placeholder="`${$t('pages.dispatch.white-list.a2b2c7e5')}://www.test.com 等`"
          />
        </a-form-item>
        <a-form-item :wrapper-col="{ span: 14, offset: 6 }">
          <a-button type="primary" html-type="submit" :disabled="submitAble">{{
            $t('pages.dispatch.white-list.d8031ed8')
          }}</a-button>
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
