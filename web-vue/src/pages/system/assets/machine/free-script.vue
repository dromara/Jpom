<template>
  <div>
    <a-row :gutter="[16, 0]">
      <a-col :span="10">
        <a-form :model="temp">
          <a-form-item>
            <code-editor
              v-model:content="temp.content"
              height="calc(100vh - 50px - 30px - 100px)"
              :options="{ mode: 'shell', tabSize: 2 }"
              :show-tool="true"
            >
              <template #tool_before>
                <a-tooltip>
                  <template #title>{{ $t('pages.system.assets.machine.free-script.69ed9ab7') }}</template>
                  {{ $t('pages.system.assets.machine.free-script.edd2beb7') }}
                  <QuestionCircleOutlined />
                </a-tooltip>
              </template>
            </code-editor>
          </a-form-item>
          <a-form-item :label="$t('pages.system.assets.machine.free-script.243b7015')" name="path">
            <a-input v-model:value="temp.path" :placeholder="$t('pages.system.assets.machine.free-script.8eec4f88')" />
          </a-form-item>
          <!-- <a-form-item :wrapper-col="{ span: 14, offset: 2 }">
            <a-space>
              <a-button type="primary" danger :disabled="submitAble" @click="onSubmit(true)">保存并重启</a-button>
            </a-space>
          </a-form-item> -->
        </a-form>
      </a-col>
      <a-col :span="14">
        <log-view2 ref="logView" height="calc(100vh - 50px - 30px)">
          <template #before>
            <a-space>
              <a-button
                type="primary"
                size="small"
                :loading="loading"
                :disabled="!temp.content"
                @click="onSubmit(false)"
                >{{ $t('pages.system.assets.machine.free-script.985968bf') }}</a-button
              >
              <a-switch
                v-model:checked="temp.appendTemplate"
                :checked-children="$t('pages.system.assets.machine.free-script.f18e273b')"
                :un-checked-children="$t('pages.system.assets.machine.free-script.d47b3f96')"
              />
            </a-space>
          </template>
        </log-view2>
      </a-col>
    </a-row>
  </div>
</template>
<script lang="ts" setup>
import codeEditor from '@/components/codeEditor'
import LogView2 from '@/components/logView/index2'
import { getWebSocketUrl } from '@/api/config'

import { useI18n } from 'vue-i18n'
const { t: $t } = useI18n()
const props = defineProps({
  machineId: {
    type: String,
    required: true
  }
})

const socket = ref(null)
const loading = ref(false)
const logView = ref()
const userStore_ = userStore()
const temp = ref({
  appendTemplate: true,
  content: ''
})

const socketUrl = computed(() => {
  return getWebSocketUrl(
    '/socket/free_script',
    `userId=${userStore_.getLongTermToken()}&machineId=${props.machineId}&nodeId=system&type=freeScript`
  )
})

const conentScript = () => {
  socket.vlaue?.close()
  socket.vlaue = null
  const socket_ = new WebSocket(socketUrl.value)
  logView.value.clearLogCache()
  // 连接成功后
  socket_.onopen = () => {
    loading.value = true
    socket_.send(JSON.stringify(temp.value))
  }
  socket_.onmessage = (msg) => {
    logView.value.appendLine(msg.data)
  }
  socket_.onerror = (err) => {
    console.error(err)
    $notification.error({
      message: `web socket ${
        ($t('pages.system.assets.machine.free-script.d75d207f'), $t('pages.system.assets.machine.free-script.763330b'))
      }`
    })
  }
  socket_.onclose = (err) => {
    //当客户端收到服务端发送的关闭连接请求时，触发onclose事件
    console.error(err)
    loading.value = false
    $message.warning($t('pages.system.assets.machine.free-script.8a2aae09'))
    // clearInterval(this.heart);
  }
  socket.value = socket_
}

// console.log(socketUrl)

// const socketUrl = com

const onSubmit = () => {
  conentScript()
}
const close = () => {
  socket.vlaue?.close()
  socket.vlaue = null
}

onMounted(() => {
  // 监听窗口关闭事件，当窗口关闭时，主动去关闭websocket连接，防止连接还没断开就关闭窗口，server端会抛异常。
  window.onbeforeunload = () => {
    close()
  }
})

onBeforeUnmount(() => {
  close()
})
</script>
