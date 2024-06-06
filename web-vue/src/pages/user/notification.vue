<template>
  <a-form :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
    <a-form-item :label="$t('pages.user.notification.969229b3')" name="enabled">
      <a-switch
        v-model:checked="temp.enabled"
        :checked-children="$t('pages.user.notification.e27f3b33')"
        :un-checked-children="$t('pages.user.notification.9c32c887')"
      />
    </a-form-item>
    <a-form-item :label="$t('pages.user.notification.a1b6e465')" name="title">
      <a-input v-model:value="temp.title" type="text" :placeholder="$t('pages.user.notification.67e93c8b')" />
      <template #help> {{ $t('pages.user.notification.a988be3f') }}</template>
    </a-form-item>
    <a-form-item :label="$t('pages.user.notification.99ff48c8')" name="content">
      <a-textarea v-model:value="temp.content" type="text" :placeholder="$t('pages.user.notification.8037cba5')" />
      <template #help> {{ $t('pages.user.notification.a988be3f') }}</template>
    </a-form-item>
    <a-form-item :label="$t('pages.user.notification.9c32c887')" name="closable">
      <a-switch
        v-model:checked="temp.closable"
        :checked-children="$t('pages.user.notification.a496321')"
        :un-checked-children="$t('pages.user.notification.1bfc6f2e')"
      />
    </a-form-item>
    <a-form-item :label="$t('pages.user.notification.106a501d')" name="enabled">
      <a-radio-group v-model:value="temp.level" name="radioGroup">
        <a-radio value="info">{{ $t('pages.user.notification.7ae524f6') }}</a-radio>
        <a-radio value="warning">{{ $t('pages.user.notification.2774e4a7') }}</a-radio>
        <a-radio value="error">{{ $t('pages.user.notification.d75d207f') }}</a-radio>
      </a-radio-group>
    </a-form-item>
    <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
      <a-button type="primary" class="btn" @click="onSubmit()">{{ $t('pages.user.notification.b033d8c5') }}</a-button>
    </a-form-item>
  </a-form>
</template>
<script setup lang="ts">
import { UserNotificationType, getUserNotification, saveUserNotification } from '@/api/user/user-notification'

import { useI18n } from 'vue-i18n'
const { t: $t } = useI18n()
const defaultValue = {
  level: 'info',
  closable: true,
  title: $t('pages.user.notification.98cf7c5a'),
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
