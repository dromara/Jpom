<template>
  <a-form :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
    <a-form-item :label="$t('i18n_3fea7ca76c')" name="enabled">
      <a-switch
        v-model:checked="temp.enabled"
        :checked-children="$t('i18n_cc42dd3170')"
        :un-checked-children="$t('i18n_b15d91274e')"
      />
    </a-form-item>
    <a-form-item :label="$t('i18n_32c65d8d74')" name="title">
      <a-input v-model:value="temp.title" type="text" :placeholder="$t('i18n_0728fee230')" />
      <template #help> {{ $t('i18n_d263a9207f') }}</template>
    </a-form-item>
    <a-form-item :label="$t('i18n_2d711b09bd')" name="content">
      <a-textarea v-model:value="temp.content" type="text" :placeholder="$t('i18n_cca4454cf8')" />
      <template #help> {{ $t('i18n_d263a9207f') }}</template>
    </a-form-item>
    <a-form-item :label="$t('i18n_b15d91274e')" name="closable">
      <a-switch
        v-model:checked="temp.closable"
        :checked-children="$t('i18n_faaa995a8b')"
        :un-checked-children="$t('i18n_0bf9f55e9d')"
      />
    </a-form-item>
    <a-form-item :label="$t('i18n_e78e4b2dc4')" name="enabled">
      <a-radio-group v-model:value="temp.level" name="radioGroup">
        <a-radio value="info">{{ $t('i18n_4b027f3979') }}</a-radio>
        <a-radio value="warning">{{ $t('i18n_900c70fa5f') }}</a-radio>
        <a-radio value="error">{{ $t('i18n_7030ff6470') }}</a-radio>
      </a-radio-group>
    </a-form-item>
    <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
      <a-button type="primary" class="btn" @click="onSubmit()">{{ $t('i18n_be5fbbe34c') }}</a-button>
    </a-form-item>
  </a-form>
</template>
<script lang="ts" setup>
import { UserNotificationType, getUserNotification, saveUserNotification } from '@/api/user/user-notification'

import { useI18n } from 'vue-i18n'
const { t: $t } = useI18n()
const defaultValue = {
  level: 'info',
  closable: true,
  title: $t('i18n_1432c7fcdb'),
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
