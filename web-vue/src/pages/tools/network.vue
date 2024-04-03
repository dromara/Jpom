<template>
  <div>
    <a-collapse v-model:activeKey="activeKey">
      <a-collapse-panel key="1">
        <template #header>
          服务端机器网络
          <a-tooltip>
            <template #title>
              <ul>
                <li>A类 10.0.0.0-10.255.255.255</li>
                <li>B类 172.16.0.0-172.31.255.255</li>
                <li>C类 192.168.0.0-192.168.255.255</li>
              </ul>
            </template>
            <QuestionCircleOutlined />
          </a-tooltip>
        </template>
        <a-space direction="vertical" style="width: 100%">
          <template v-for="item in ipListArray">
            <a-list size="small" bordered :data-source="item.ips">
              <template #renderItem="{ item }">
                <a-list-item
                  >{{ item.ip }}
                  <a-tag v-for="label in item.labels">{{ label }}</a-tag>
                </a-list-item>
              </template>
              <template #header>
                <div>
                  {{ item.name }} <a-tag>{{ item.displayName }}</a-tag>
                  <a-tag v-if="item.virtual">虚拟</a-tag>
                  <a-tag v-if="item.loopback">环回</a-tag>
                </div>
              </template>
            </a-list>
          </template>
        </a-space>
      </a-collapse-panel>
      <a-collapse-panel key="ping" header="网络 Reachable 测试">
        <a-form
          ref="form"
          :model="pingData"
          :rules="pingRules"
          :label-col="{ span: 4 }"
          :wrapper-col="{ span: 18 }"
          @finish="onPingSubmit"
        >
          <a-form-item label="提示" name="">
            <a-alert message="不等同于 PING 测试，此处测试成功表示网络一定通畅，此处测试失败网络不一定不通畅" banner />
          </a-form-item>
          <a-form-item label="HOST" name="host">
            <a-input
              v-model:value="pingData.host"
              placeholder="请输入要检查的 host"
              @change="
                () => {
                  pingReulst = {}
                }
              "
            />
            <template #help>支持IP 地址、域名、主机名 </template>
          </a-form-item>
          <a-form-item label="超时时间(S)" name="timeout">
            <a-input-number
              v-model:value="pingData.timeout"
              :min="1"
              placeholder="请输入超时时间"
              style="width: 100%"
            />
          </a-form-item>

          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" html-type="submit" :loading="pingLoading"> 测试 </a-button>
          </a-form-item>
          <template v-if="Object.keys(pingReulst).length">
            <a-form-item label="结果" name="result">
              <a-tag color="green" v-if="pingReulst.ping">成功</a-tag>
              <a-tag color="red" v-else>失败</a-tag>
            </a-form-item>
            <a-form-item label="类型" name="labels">
              <a-tag v-for="item in pingReulst.labels">{{ item }}</a-tag>
            </a-form-item>
            <a-form-item label="原始IP" name="originalIP" v-if="pingReulst.originalIP">
              {{ pingReulst.originalIP }}
            </a-form-item>
          </template>
        </a-form>
      </a-collapse-panel>
      <a-collapse-panel key="telnet" header="网络端口测试">
        <a-form
          ref="form"
          :model="telnetData"
          :rules="telnetRules"
          :label-col="{ span: 4 }"
          :wrapper-col="{ span: 18 }"
          @finish="onTelnetSubmit"
        >
          <a-form-item label="HOST" name="host">
            <a-input
              v-model:value="telnetData.host"
              placeholder="请输入要检查的 host"
              @change="
                () => {
                  telnetReulst = {}
                }
              "
            />
            <template #help>支持IP 地址、域名、主机名 </template>
          </a-form-item>
          <a-form-item label="端口" name="port">
            <a-auto-complete
              v-model:value="telnetData.port"
              :options="UniversalPort"
              @change="
                () => {
                  telnetReulst = {}
                }
              "
            >
              <a-input-number
                v-model:value="telnetData.port"
                :min="0"
                :max="65535"
                placeholder="需要测试的端口"
                :controls="false"
              />
              <template #option="item"> {{ item.title }} {{ item.value }} </template>
              <template #clearIcon>1</template>
            </a-auto-complete>
          </a-form-item>
          <a-form-item label="超时时间(S)" name="timeout">
            <a-input-number
              v-model:value="telnetData.timeout"
              :min="1"
              placeholder="请输入超时时间"
              style="width: 100%"
            />
          </a-form-item>

          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" html-type="submit" :loading="telnetLoading"> 测试 </a-button>
          </a-form-item>
          <template v-if="Object.keys(telnetReulst).length">
            <a-form-item label="结果" name="result">
              <a-tag color="green" v-if="telnetReulst.open">成功</a-tag>
              <a-tag color="red" v-else>失败</a-tag>
            </a-form-item>
            <a-form-item label="类型" name="labels">
              <a-tag v-for="item in telnetReulst.labels">{{ item }}</a-tag>
            </a-form-item>
            <a-form-item label="原始IP" name="originalIP" v-if="telnetReulst.originalIP">
              {{ telnetReulst.originalIP }}
            </a-form-item>
          </template>
        </a-form>
      </a-collapse-panel>
    </a-collapse>
  </div>
</template>
<script setup>
import { ipList, netPing, netTelnet } from '@/api/tools'
const ipListArray = ref([])
const activeKey = ref(['ping', 'telnet'])

const UniversalPort = ref([
  {
    title: '插件端端口',
    value: '2123'
  },
  {
    title: '服务端端口',
    value: '2122'
  },
  {
    title: 'SSH终端',
    value: '22'
  },
  {
    title: 'Docker HTTP',
    value: '2375'
  },

  {
    title: 'HTTP',
    value: '80'
  },
  {
    title: 'HTTPS',
    value: '443'
  }
])

const pingData = ref({
  timeout: 1
})
const pingRules = ref({
  host: [
    {
      required: true,
      message: '请输入要检查的 host',
      trigger: 'blur'
    }
  ]
})
const pingLoading = ref(false)
const pingReulst = ref({})

const onPingSubmit = (value) => {
  pingLoading.value = true
  pingReulst.value = {}
  netPing(value)
    .then((res) => {
      if (res.code !== 200) {
        $notification.warn({
          message: res.msg
        })
        return
      }
      pingReulst.value = res.data
    })
    .finally(() => {
      pingLoading.value = false
    })
}

//
const telnetData = ref({
  timeout: 1
})
const telnetRules = ref({
  host: [
    {
      required: true,
      message: '请输入要检查的 host',
      trigger: 'blur'
    }
  ],
  port: [
    {
      required: true,
      message: '请输入要检查的端口',
      trigger: 'blur'
    }
  ]
})
const telnetLoading = ref(false)
const telnetReulst = ref({})

const onTelnetSubmit = (value) => {
  telnetLoading.value = true
  telnetReulst.value = {}
  netTelnet(value)
    .then((res) => {
      if (res.code !== 200) {
        $notification.warn({
          message: res.msg
        })
        return
      }
      telnetReulst.value = res.data
    })
    .finally(() => {
      telnetLoading.value = false
    })
}

onMounted(() => {
  ipList().then((res) => {
    ipListArray.value = res.data || []
  })
})
</script>
