<template>
  <div>
    <a-collapse v-model:activeKey="activeKey">
      <a-collapse-panel key="1">
        <template #header>
          {{ $t('i18n_0e44ae17ae') }}
          <a-tooltip>
            <template #title>
              <ul>
                <li>A{{ $t('i18n_8be868ba1b') }}.0.0.0-10.255.255.255</li>
                <li>B{{ $t('i18n_a66644ff47') }}.16.0.0-172.31.255.255</li>
                <li>C{{ $t('i18n_768e843a3e') }}.168.0.0-192.168.255.255</li>
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
                  <a-tag v-if="item.virtual">{{ $t('i18n_b60352bc4f') }}</a-tag>
                  <a-tag v-if="item.loopback">{{ $t('i18n_4393b5e25b') }}</a-tag>
                </div>
              </template>
            </a-list>
          </template>
        </a-space>
      </a-collapse-panel>
      <a-collapse-panel key="ping" :header="$t('i18n_bc4b0fd88a')">
        <a-form
          ref="form"
          :model="pingData"
          :rules="pingRules"
          :label-col="{ span: 4 }"
          :wrapper-col="{ span: 18 }"
          @finish="onPingSubmit"
        >
          <a-form-item :label="$t('i18n_02d9819dda')" name="">
            <a-alert :message="$t('i18n_1dc9514548')" banner />
          </a-form-item>
          <a-form-item label="HOST" name="host">
            <a-input
              v-model:value="pingData.host"
              :placeholder="$t('i18n_49d569f255')"
              @change="
                () => {
                  pingReulst = {}
                }
              "
            />
            <template #help>{{ $t('i18n_d373338541') }} </template>
          </a-form-item>
          <a-form-item :label="$t('i18n_84b28944b7')" name="timeout">
            <a-input-number
              v-model:value="pingData.timeout"
              :min="1"
              :placeholder="$t('i18n_6be30eaad7')"
              style="width: 100%"
            />
          </a-form-item>

          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" html-type="submit" :loading="pingLoading">
              {{ $t('i18n_db06c78d1e') }}
            </a-button>
          </a-form-item>
          <template v-if="Object.keys(pingReulst).length">
            <a-form-item :label="$t('i18n_5ad7f5a8b2')" name="result">
              <a-tag v-if="pingReulst.ping" color="green">{{ $t('i18n_330363dfc5') }}</a-tag>
              <a-tag v-else color="red">{{ $t('i18n_acd5cb847a') }}</a-tag>
            </a-form-item>
            <a-form-item :label="$t('i18n_226b091218')" name="labels">
              <a-tag v-for="(item, index) in pingReulst.labels" :key="index">{{ item }}</a-tag>
            </a-form-item>
            <a-form-item v-if="pingReulst.originalIP" :label="$t('i18n_1b5266365f')" name="originalIP">
              {{ pingReulst.originalIP }}
            </a-form-item>
          </template>
        </a-form>
      </a-collapse-panel>
      <a-collapse-panel key="telnet" :header="$t('i18n_97d08b02e7')">
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
              :placeholder="$t('i18n_49d569f255')"
              @change="
                () => {
                  telnetReulst = {}
                }
              "
            />
            <template #help>{{ $t('i18n_d373338541') }} </template>
          </a-form-item>
          <a-form-item :label="$t('i18n_c76cfefe72')" name="port">
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
                :placeholder="$t('i18n_82416714a8')"
                :controls="false"
              />
              <template #option="item"> {{ item.title }} {{ item.value }} </template>
              <template #clearIcon>1</template>
            </a-auto-complete>
          </a-form-item>
          <a-form-item :label="$t('i18n_84b28944b7')" name="timeout">
            <a-input-number
              v-model:value="telnetData.timeout"
              :min="1"
              :placeholder="$t('i18n_6be30eaad7')"
              style="width: 100%"
            />
          </a-form-item>

          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" html-type="submit" :loading="telnetLoading">
              {{ $t('i18n_db06c78d1e') }}
            </a-button>
          </a-form-item>
          <template v-if="Object.keys(telnetReulst).length">
            <a-form-item :label="$t('i18n_5ad7f5a8b2')" name="result">
              <a-tag v-if="telnetReulst.open" color="green">{{ $t('i18n_330363dfc5') }}</a-tag>
              <a-tag v-else color="red">{{ $t('i18n_acd5cb847a') }}</a-tag>
            </a-form-item>
            <a-form-item :label="$t('i18n_226b091218')" name="labels">
              <a-tag v-for="(item, index) in telnetReulst.labels" :key="index">{{ item }}</a-tag>
            </a-form-item>
            <a-form-item v-if="telnetReulst.originalIP" :label="$t('i18n_1b5266365f')" name="originalIP">
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

import { useI18n } from 'vue-i18n'
const { t: $t } = useI18n()
const ipListArray = ref([])
const activeKey = ref(['ping', 'telnet'])

const UniversalPort = ref([
  {
    title: $t('i18n_6a922e0fb6'),
    value: '2123'
  },
  {
    title: $t('i18n_9af372557e'),
    value: '2122'
  },
  {
    title: `SSH${$t('i18n_4722bc0c56')}`,
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
      message: $t('i18n_49d569f255'),
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
      message: $t('i18n_49d569f255'),
      trigger: 'blur'
    }
  ],

  port: [
    {
      required: true,
      message: $t('i18n_9302bc7838'),
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
