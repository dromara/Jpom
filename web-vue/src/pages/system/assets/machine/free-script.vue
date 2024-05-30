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
                  <template #title>{{ $tl('p.freeScriptDescription') }}</template>
                  {{ $tl('p.help') }}
                  <QuestionCircleOutlined />
                </a-tooltip>
              </template>
            </code-editor>
          </a-form-item>
          <a-form-item :label="$tl('p.executionPath')" name="path">
            <a-input v-model:value="temp.path" :placeholder="$tl('p.scriptPath')" />
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
                >{{ $tl('p.execute') }}</a-button
              >
              <a-switch
                v-model:checked="temp.appendTemplate"
                :checked-children="$tl('p.appendScriptTemplate')"
                :un-checked-children="`不${$tl('p.appendScriptTemplate')}`"
              />
            </a-space>
          </template>
        </log-view2>
      </a-col>
    </a-row>
  </div>
</template>
<script setup lang="ts">
import codeEditor from '@/components/codeEditor'
import LogView2 from '@/components/logView/index2'
import { getWebSocketUrl } from '@/api/config'

import { useI18nPage } from '@/i18n/hooks/useI18nPage'
const { $tl } = useI18nPage('pages.system.assets.machine.freeScript')

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
      message: `web socket ${($tl('p.error'), $tl('p.checkWsProxy'))}`
    })
  }
  socket_.onclose = (err) => {
    //当客户端收到服务端发送的关闭连接请求时，触发onclose事件
    console.error(err)
    loading.value = false
    $message.warning($tl('p.sessionClosed'))
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
