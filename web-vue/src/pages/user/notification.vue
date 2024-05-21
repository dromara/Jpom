<template>
  <a-form :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
    <a-form-item :label="$tl('p.statusValue')" name="enabled">
      <a-switch
        v-model:checked="temp.enabled"
        :checked-children="$tl('p.isOpen')"
        :un-checked-children="$tl('p.status')"
      />
    </a-form-item>
    <a-form-item :label="$tl('p.title')" name="title">
      <a-input v-model:value="temp.title" type="text" :placeholder="$tl('p.titleInput')" />
      <template #help> {{ $tl('p.htmlSupport') }}</template>
    </a-form-item>
    <a-form-item :label="$tl('p.content')" name="content">
      <a-textarea v-model:value="temp.content" type="text" :placeholder="$tl('p.contentInput')" />
      <template #help> {{ $tl('p.htmlSupport') }}</template>
    </a-form-item>
    <a-form-item :label="$tl('p.status')" name="closable">
      <a-switch
        v-model:checked="temp.closable"
        :checked-children="$tl('p.canClose')"
        :un-checked-children="$tl('p.cannotClose')"
      />
    </a-form-item>
    <a-form-item :label="$tl('p.level')" name="enabled">
      <a-radio-group v-model:value="temp.level" name="radioGroup">
        <a-radio value="info">{{ $tl('p.reminder') }}</a-radio>
        <a-radio value="warning">{{ $tl('p.warning') }}</a-radio>
        <a-radio value="error">{{ $tl('p.error') }}</a-radio>
      </a-radio-group>
    </a-form-item>
    <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
      <a-button type="primary" class="btn" @click="onSubmit()">{{ $tl('p.save') }}</a-button>
    </a-form-item>
  </a-form>
</template>
<script setup lang="ts">
import { UserNotificationType, getUserNotification, saveUserNotification } from '@/api/user/user-notification'

import { useI18nPage } from '@/i18n/hooks/useI18nPage'
const { $tl } = useI18nPage('pages.user.notification')
const defaultValue = {
  level: 'info',
  closable: true,
  title: $tl('p.systemNotice'),
  enabled: false
} as UserNotificationType

const temp = ref<UserNotificationType>(defaultValue)

onMounted(() => {
  getUserNotification().then((res) => {
    if (res.code === 200) {
      if (Object.keys(res.data).length) {
        temp.value = res.data || defaultValue
      } else {
        temp.value = defaultValue
      }
    }
  })
})

const onSubmit = () => {
  saveUserNotification(temp.value).then((res) => {
    if (res.code === 200) {
      $notification.success({
        message: res.msg
      })
    }
  })
}
</script>
