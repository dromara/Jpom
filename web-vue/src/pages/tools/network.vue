<template>
  <div>
    <a-collapse v-model:activeKey="activeKey">
      <a-collapse-panel key="1">
        <template #header>
          {{ $tl('p.serverNetwork') }}
          <a-tooltip>
            <template #title>
              <ul>
                <li>A{{ $tl('p.class10') }}.0.0.0-10.255.255.255</li>
                <li>B{{ $tl('p.class172') }}.16.0.0-172.31.255.255</li>
                <li>C{{ $tl('p.class192') }}.168.0.0-192.168.255.255</li>
              </ul>
            </template>
            <QuestionCircleOutlined />
          </a-tooltip>
        </template>
        <a-space direction="vertical" style="width: 100%">
          <template v-for="(item, index) in ipListArray" :key="index">
            <a-list size="small" bordered :data-source="item.ips">
              <template #renderItem="{ item }">
                <a-list-item
                  >{{ item.ip }}
                  <a-tag v-for="(item, idx) in item.labels" :key="idx">{{ label }}</a-tag>
                </a-list-item>
              </template>
              <template #header>
                <div>
                  {{ item.name }} <a-tag>{{ item.displayName }}</a-tag>
                  <a-tag v-if="item.virtual">{{ $tl('p.virtual') }}</a-tag>
                  <a-tag v-if="item.loopback">{{ $tl('p.loopback') }}</a-tag>
                </div>
              </template>
            </a-list>
          </template>
        </a-space>
      </a-collapse-panel>
      <a-collapse-panel key="ping" :header="$tl('p.networkReachableTest')">
        <a-form
          ref="form"
          :model="pingData"
          :rules="pingRules"
          :label-col="{ span: 4 }"
          :wrapper-col="{ span: 18 }"
          @finish="onPingSubmit"
        >
          <a-form-item :label="$tl('p.hint')" name="">
            <a-alert :message="$tl('p.note')" banner />
          </a-form-item>
          <a-form-item label="HOST" name="host">
            <a-input
              v-model:value="pingData.host"
              :placeholder="$tl('c.host')"
              @change="
                () => {
                  pingReulst = {}
                }
              "
            />
            <template #help>{{ $tl('c.hostType') }} </template>
          </a-form-item>
          <a-form-item :label="$tl('c.timeoutSeconds')" name="timeout">
            <a-input-number
              v-model:value="pingData.timeout"
              :min="1"
              :placeholder="$tl('c.inputTimeout')"
              style="width: 100%"
            />
          </a-form-item>

          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" html-type="submit" :loading="pingLoading"> {{ $tl('c.test') }} </a-button>
          </a-form-item>
          <template v-if="Object.keys(pingReulst).length">
            <a-form-item :label="$tl('c.result')" name="result">
              <a-tag v-if="pingReulst.ping" color="green">{{ $tl('c.success') }}</a-tag>
              <a-tag v-else color="red">{{ $tl('c.failure') }}</a-tag>
            </a-form-item>
            <a-form-item :label="$tl('c.type')" name="labels">
              <a-tag v-for="(item, index) in pingReulst.labels" :key="index">{{ item }}</a-tag>
            </a-form-item>
            <a-form-item v-if="pingReulst.originalIP" :label="$tl('c.originalIp')" name="originalIP">
              {{ pingReulst.originalIP }}
            </a-form-item>
          </template>
        </a-form>
      </a-collapse-panel>
      <a-collapse-panel key="telnet" :header="$tl('p.networkPortTest')">
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
              :placeholder="$tl('c.host')"
              @change="
                () => {
                  telnetReulst = {}
                }
              "
            />
            <template #help>{{ $tl('c.hostType') }} </template>
          </a-form-item>
          <a-form-item :label="$tl('p.port')" name="port">
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
                :placeholder="$tl('p.testPort')"
                :controls="false"
              />
              <template #option="item"> {{ item.title }} {{ item.value }} </template>
              <template #clearIcon>1</template>
            </a-auto-complete>
          </a-form-item>
          <a-form-item :label="$tl('c.timeoutSeconds')" name="timeout">
            <a-input-number
              v-model:value="telnetData.timeout"
              :min="1"
              :placeholder="$tl('c.inputTimeout')"
              style="width: 100%"
            />
          </a-form-item>

          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" html-type="submit" :loading="telnetLoading"> {{ $tl('c.test') }} </a-button>
          </a-form-item>
          <template v-if="Object.keys(telnetReulst).length">
            <a-form-item :label="$tl('c.result')" name="result">
              <a-tag v-if="telnetReulst.open" color="green">{{ $tl('c.success') }}</a-tag>
              <a-tag v-else color="red">{{ $tl('c.failure') }}</a-tag>
            </a-form-item>
            <a-form-item :label="$tl('c.type')" name="labels">
              <a-tag v-for="(item, index) in telnetReulst.labels" :key="index">{{ item }}</a-tag>
            </a-form-item>
            <a-form-item v-if="telnetReulst.originalIP" :label="$tl('c.originalIp')" name="originalIP">
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

import { useI18nPage } from '@/i18n/hooks/useI18nPage'
const { $tl } = useI18nPage('pages.tools.network')
const ipListArray = ref([])
const activeKey = ref(['ping', 'telnet'])

const UniversalPort = ref([
  {
    title: $tl('p.pluginPort'),
    value: '2123'
  },
  {
    title: $tl('p.serverPort'),
    value: '2122'
  },
  {
    title: `SSH${$tl('p.terminal')}`,
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
      message: $tl('c.host'),
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
      message: $tl('c.host'),
      trigger: 'blur'
    }
  ],
  port: [
    {
      required: true,
      message: $tl('p.inputPort'),
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
