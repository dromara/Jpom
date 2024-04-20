<template>
  <a-form :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
    <a-form-item label="状态" name="enabled">
      <a-switch v-model:checked="temp.enabled" checked-children="开启" un-checked-children="关闭" />
    </a-form-item>
    <a-form-item label="标题" name="title">
      <a-input v-model:value="temp.title" type="text" placeholder="请输入公告标题" />
      <template #help> 支持 html 格式</template>
    </a-form-item>
    <a-form-item label="内容" name="content">
      <a-textarea v-model:value="temp.content" type="text" placeholder="请输入公告内容" />
      <template #help> 支持 html 格式</template>
    </a-form-item>
    <a-form-item label="关闭" name="closable">
      <a-switch v-model:checked="temp.closable" checked-children="可以关闭" un-checked-children="不能关闭" />
    </a-form-item>
    <a-form-item label="级别" name="enabled">
      <a-radio-group v-model:value="temp.level" name="radioGroup">
        <a-radio value="info">提醒</a-radio>
        <a-radio value="warning">警告</a-radio>
        <a-radio value="error">错误</a-radio>
      </a-radio-group>
    </a-form-item>
    <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
      <a-button type="primary" class="btn" @click="onSubmit()">保存</a-button>
    </a-form-item>
  </a-form>
</template>
<script setup lang="ts">
import { UserNotificationType, getUserNotification, saveUserNotification } from '@/api/user/user-notification'
const defaultValue = <UserNotificationType>{
  level: 'info',
  closable: true,
  title: '系统公告',
  enabled: false
}

const temp = ref<UserNotificationType>(defaultValue)

onMounted(() => {
  getUserNotification().then((res) => {
    if (res.code === 200) {
      temp.value = res.data || defaultValue
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
