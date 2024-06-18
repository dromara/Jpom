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
                  <template #title>{{ $t('i18n_3a94281b91') }}</template>
                  {{ $t('i18n_92e3a830ae') }}
                  <QuestionCircleOutlined />
                </a-tooltip>
              </template>
            </code-editor>
          </a-form-item>
          <a-form-item :label="$t('i18n_938dd62952')" name="path">
            <a-input v-model:value="temp.path" :placeholder="$t('i18n_622d00a119')" />
          </a-form-item>
        </a-form>
      </a-col>
      <a-col :span="14">
        <log-view2 ref="logView" height="calc(100vh - 50px - 30px)">
          <template #before>
            <a-space>
              <a-button type="primary" size="small" :loading="loading" :disabled="!temp.content" @click="onSubmit()">{{
                $t('i18n_1a6aa24e76')
              }}</a-button>
              <a-switch
                v-model:checked="temp.appendTemplate"
                :checked-children="$t('i18n_87e2f5bf75')"
                :un-checked-children="$t('i18n_e3ee3ca673')"
              />
            </a-space>
          </template>
        </log-view2>
      </a-col>
    </a-row>
  </div>
</template>
<script lang="ts" setup>
import codeEditor from '@/components/codeEditor/index.vue'
import LogView2 from '@/components/logView/index2.vue'
import { getWebSocketUrl } from '@/api/config'

import { useI18n } from 'vue-i18n'
const { t: $t } = useI18n()
const props = defineProps({
  machineId: {
    type: String,
    required: true
  }
})

const socketRef = ref<WebSocket>()
const loading = ref(false)
const logView = ref()
const userStore_ = userStore()
const temp = ref({
  appendTemplate: true,
  content: '',
  path: ''
})

const socketUrl = computed(() => {
  return getWebSocketUrl(
    '/socket/free_script',
    `userId=${userStore_.getLongTermToken()}&machineId=${props.machineId}&nodeId=system&type=freeScript`
  )
})

const conentScript = () => {
  close()
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
      message: `web socket ${($t('i18n_7030ff6470'), $t('i18n_226a6f9cdd'))}`
    })
  }
  socket_.onclose = (err) => {
    //当客户端收到服务端发送的关闭连接请求时，触发onclose事件
    console.error(err)
    loading.value = false
    $message.warning($t('i18n_a53d137403'))
    // clearInterval(this.heart);
  }
  socketRef.value = socket_
}

// console.log(socketUrl)

// const socketUrl = com

const onSubmit = () => {
  conentScript()
}
const close = () => {
  if (socketRef.value != null) {
    socketRef.value?.close()
  }
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
