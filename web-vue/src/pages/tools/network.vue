<template>
  <div>
    <a-collapse v-model:activeKey="activeKey">
      <a-collapse-panel key="1">
        <template #header>
          {{ $t('pages.tools.network.f772d2f4') }}
          <a-tooltip>
            <template #title>
              <ul>
                <li>A{{ $t('pages.tools.network.54d58ede') }}.0.0.0-10.255.255.255</li>
                <li>B{{ $t('pages.tools.network.34ab0297') }}.16.0.0-172.31.255.255</li>
                <li>C{{ $t('pages.tools.network.aa282f19') }}.168.0.0-192.168.255.255</li>
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
                  <a-tag v-if="item.virtual">{{ $t('pages.tools.network.578245d6') }}</a-tag>
                  <a-tag v-if="item.loopback">{{ $t('pages.tools.network.e9b07e4c') }}</a-tag>
                </div>
              </template>
            </a-list>
          </template>
        </a-space>
      </a-collapse-panel>
      <a-collapse-panel key="ping" :header="$t('pages.tools.network.da1383cd')">
        <a-form
          ref="form"
          :model="pingData"
          :rules="pingRules"
          :label-col="{ span: 4 }"
          :wrapper-col="{ span: 18 }"
          @finish="onPingSubmit"
        >
          <a-form-item :label="$t('pages.tools.network.d193e148')" name="">
            <a-alert :message="$t('pages.tools.network.2ae8180f')" banner />
          </a-form-item>
          <a-form-item label="HOST" name="host">
            <a-input
              v-model:value="pingData.host"
              :placeholder="$t('pages.tools.network.af3081d3')"
              @change="
                () => {
                  pingReulst = {}
                }
              "
            />
            <template #help>{{ $t('pages.tools.network.61d5175d') }} </template>
          </a-form-item>
          <a-form-item :label="$t('pages.tools.network.bb9a46b6')" name="timeout">
            <a-input-number
              v-model:value="pingData.timeout"
              :min="1"
              :placeholder="$t('pages.tools.network.683170cf')"
              style="width: 100%"
            />
          </a-form-item>

          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" html-type="submit" :loading="pingLoading">
              {{ $t('pages.tools.network.b868ec22') }}
            </a-button>
          </a-form-item>
          <template v-if="Object.keys(pingReulst).length">
            <a-form-item :label="$t('pages.tools.network.66c5a40')" name="result">
              <a-tag v-if="pingReulst.ping" color="green">{{ $t('pages.tools.network.9d77d967') }}</a-tag>
              <a-tag v-else color="red">{{ $t('pages.tools.network.d3ec0514') }}</a-tag>
            </a-form-item>
            <a-form-item :label="$t('pages.tools.network.ecc9c507')" name="labels">
              <a-tag v-for="(item, index) in pingReulst.labels" :key="index">{{ item }}</a-tag>
            </a-form-item>
            <a-form-item v-if="pingReulst.originalIP" :label="$t('pages.tools.network.35ef11ff')" name="originalIP">
              {{ pingReulst.originalIP }}
            </a-form-item>
          </template>
        </a-form>
      </a-collapse-panel>
      <a-collapse-panel key="telnet" :header="$t('pages.tools.network.c38573a0')">
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
              :placeholder="$t('pages.tools.network.af3081d3')"
              @change="
                () => {
                  telnetReulst = {}
                }
              "
            />
            <template #help>{{ $t('pages.tools.network.61d5175d') }} </template>
          </a-form-item>
          <a-form-item :label="$t('pages.tools.network.a6c4bfd7')" name="port">
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
                :placeholder="$t('pages.tools.network.ed675c79')"
                :controls="false"
              />
              <template #option="item"> {{ item.title }} {{ item.value }} </template>
              <template #clearIcon>1</template>
            </a-auto-complete>
          </a-form-item>
          <a-form-item :label="$t('pages.tools.network.bb9a46b6')" name="timeout">
            <a-input-number
              v-model:value="telnetData.timeout"
              :min="1"
              :placeholder="$t('pages.tools.network.683170cf')"
              style="width: 100%"
            />
          </a-form-item>

          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" html-type="submit" :loading="telnetLoading">
              {{ $t('pages.tools.network.b868ec22') }}
            </a-button>
          </a-form-item>
          <template v-if="Object.keys(telnetReulst).length">
            <a-form-item :label="$t('pages.tools.network.66c5a40')" name="result">
              <a-tag v-if="telnetReulst.open" color="green">{{ $t('pages.tools.network.9d77d967') }}</a-tag>
              <a-tag v-else color="red">{{ $t('pages.tools.network.d3ec0514') }}</a-tag>
            </a-form-item>
            <a-form-item :label="$t('pages.tools.network.ecc9c507')" name="labels">
              <a-tag v-for="(item, index) in telnetReulst.labels" :key="index">{{ item }}</a-tag>
            </a-form-item>
            <a-form-item v-if="telnetReulst.originalIP" :label="$t('pages.tools.network.35ef11ff')" name="originalIP">
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
    title: $t('pages.tools.network.a6d443ad'),
    value: '2123'
  },
  {
    title: $t('pages.tools.network.f8fe6a6e'),
    value: '2122'
  },
  {
    title: `SSH${$t('pages.tools.network.b5a97ef7')}`,
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
      message: $t('pages.tools.network.af3081d3'),
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
      message: $t('pages.tools.network.af3081d3'),
      trigger: 'blur'
    }
  ],
  port: [
    {
      required: true,
      message: $t('pages.tools.network.88a272b2'),
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
