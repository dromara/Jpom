<template>
  <div>
    <div v-if="fastInstallInfo">
      <a-collapse v-model:activeKey="fastInstallActiveKey">
        <a-collapse-panel key="1" :header="$t('i18n_947d983961')">
          <a-alert message="" type="warning" show-icon>
            <template #description>
              <ul>
                <li>
                  {{ $t('i18n_e2be9bab6b') }}<b>{{ $t('i18n_0f59fe5338') }}</b
                  >,<b>{{ $t('i18n_baafe06808') }}</b
                  >{{ $t('i18n_c5c69827c5') }}
                </li>
                <li>{{ $t('i18n_b4e2b132cf') }}<b>2123</b></li>
                <li>
                  {{ $t('i18n_e222f4b9ad') }}<b>{{ $t('i18n_57cadc4cf3') }}</b>
                </li>
                <li>{{ $t('i18n_d19bae9fe0') }}</li>
                <li style="color: red">{{ $t('i18n_282c8cda1f') }}</li>
                <li>
                  {{ $t('i18n_88c85a2506') }}<b>{{ $t('i18n_310c809904') }}</b
                  >,{{ $t('i18n_f7596f3159') }}
                </li>
                <li>
                  {{ $t('i18n_38da533413') }}<b>{{ $t('i18n_0bac3db71c') }}</b
                  >,{{ $t('i18n_0e052223a4') }}
                </li>
                <li>
                  {{ $t('i18n_a50fbc5a52') }}<b>networkName</b
                  >{{
                    $t('i18n_9971192b6a')
                  }}://192.168.31.175:2122/api/node/receive_push?token=xxx&workspaceId=xxx&networkName=en0
                </li>
              </ul>
            </template>
          </a-alert>
        </a-collapse-panel>
        <a-collapse-panel key="2" :header="$t('i18n_70b5b45591')">
          <a-tabs :default-active-key="0">
            <a-tab-pane v-for="(item, index) in fastInstallInfo.shUrls" :key="index" :tab="item.name">
              <div>
                <a-alert type="info" :message="`${$t('i18n_ccb91317c5')}`">
                  <template #description>
                    <a-typography-paragraph :copyable="{ tooltip: false, text: item.allText }">
                      <span>{{ item.allText }} </span>
                    </a-typography-paragraph>
                  </template>
                </a-alert>
              </div>
            </a-tab-pane>
          </a-tabs>
        </a-collapse-panel>
        <a-collapse-panel key="3" :header="$t('i18n_dab864ab72')">
          <a-alert type="info" :message="`${$t('i18n_ccb91317c5')}(${$t('i18n_a8920fbfad')})`">
            <template #description>
              <a-typography-paragraph :copyable="{ tooltip: false, text: fastInstallInfo.bindCommand }">
                <span>{{ fastInstallInfo.bindCommand }} </span>
              </a-typography-paragraph>
            </template>
          </a-alert>
        </a-collapse-panel>
        <a-collapse-panel key="4" :header="$t('i18n_adaf94c06b')">
          <div v-if="!pullFastInstallResultData || !pullFastInstallResultData.length">
            {{ $t('i18n_f4fb0cbecf') }}
          </div>
          <a-alert
            v-for="(item, index) in pullFastInstallResultData"
            :key="`${index}-${new Date().getTime()}`"
            :message="`${$t('i18n_ac00774608')} ${index + 1} ${$t('i18n_dbc0b66ca4')}`"
            :type="`${item.type === 'success' ? 'success' : item.type === 'exists' ? 'error' : 'warning'}`"
            closable
            @close="clearPullFastInstallResult(item.id)"
          >
            <template #description>
              <a-space direction="vertical" style="width: 100%">
                <div v-if="item.type === 'canUseIpEmpty'">
                  <a-tag color="orange">{{ $t('i18n_5d803afb8d') }}</a-tag>
                </div>
                <div v-if="item.type === 'multiIp'">
                  <a-tag color="green">{{ $t('i18n_d9569a5d3b') }}</a-tag>
                </div>
                <div v-if="item.type === 'exists'">
                  <a-tag color="orange">{{ $t('i18n_9bd451c4e9') }}</a-tag>
                </div>
                <div v-if="item.type === 'success'">
                  <a-tag color="orange">{{ $t('i18n_1974fe5349') }}</a-tag>
                </div>
                <div>
                  {{ $t('i18n_8cd628f495')
                  }}<a-tag v-for="(itemIp, indexIp) in item.allIp" :key="indexIp">{{ itemIp }}:{{ item.port }}</a-tag>
                </div>
                <div v-if="item.type === 'multiIp'">
                  {{ $t('i18n_78a4b837e3') }}({{ $t('i18n_c5de93f9c7') }}):
                  <a-tag
                    v-for="(itemIp, indexIp) in item.canUseIp"
                    :key="indexIp"
                    @click="confirmFastInstall(item.id, itemIp, item.port)"
                    >{{ itemIp }}:{{ item.port }}<ApiOutlined
                  /></a-tag>
                </div>
                <div v-if="item.type === 'success' || item.type === 'exists'">
                  {{ $t('i18n_69c743de70') }}:
                  <a-tag v-for="(itemIp, indexIp) in item.canUseIp" :key="indexIp">{{ itemIp }}:{{ item.port }}</a-tag>
                </div>
              </a-space>
            </template>
          </a-alert>
        </a-collapse-panel>
      </a-collapse>
    </div>
    <div v-else>loading....</div>
  </div>
</template>
<script>
import { mapState } from 'pinia'

import { useAppStore } from '@/stores/app'

import { confirmFastInstall, fastInstall, pullFastInstallResult } from '@/api/node'
export default {
  data() {
    return {
      fastInstallActiveKey: ['1', '2', '4'],
      fastInstallInfo: null,

      pullFastInstallResultTime: null,
      pullFastInstallResultData: [],
      fastInstallNode: false
    }
  },
  computed: {
    ...mapState(useAppStore, ['getWorkspaceId'])
  },
  beforeUnmount() {
    this.cancelFastInstall()
  },
  unmounted() {
    this.cancelFastInstall()
  },
  mounted() {
    this.fastInstall()
  },
  methods: {
    // 关闭弹窗,关闭定时轮询
    cancelFastInstall() {
      if (this.pullFastInstallResultTime) {
        clearInterval(this.pullFastInstallResultTime)
        this.pullFastInstallResultTime = null
      }
      //this.loadData();
    },
    // 快速安装弹窗
    fastInstall() {
      fastInstall().then((res) => {
        if (res.code === 200) {
          this.fastInstallNode = true
          this.fastInstallInfo = res.data

          this.fastInstallInfo.host = `${location.protocol}//${location.host}${res.data.url}?token=${
            res.data.token
          }\\&workspaceId=${this.getWorkspaceId()}`
          this.fastInstallInfo.shUrls = res.data.shUrls.map((item) => {
            item.allText = `${item.url} ${this.fastInstallInfo.key} \\'${this.fastInstallInfo.host}\\'`
            return item
          })
          this.fastInstallInfo.bindCommand = `sh ./bin/Agent.sh restart -s ${this.fastInstallInfo.key} \\'${this.fastInstallInfo.host}\\' && tail -f ./logs/agent.log`
          // 轮询 结果
          this.pullFastInstallResultTime = setInterval(() => {
            pullFastInstallResult().then((res) => {
              if (res.code === 200) {
                this.pullFastInstallResultData = res.data
              }
            })
          }, 2000)
        }
      })
    },
    // 清除快速安装节点缓存
    clearPullFastInstallResult(id) {
      pullFastInstallResult({
        removeId: id
      }).then((res) => {
        if (res.code === 200) {
          this.pullFastInstallResultData = res.data
        }
      })
    },
    // 安装确认
    confirmFastInstall(id, ip, port) {
      confirmFastInstall({
        id: id,
        ip: ip,
        port: port
      }).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
          this.pullFastInstallResultData = res.data
        }
      })
    }
  }
}
</script>
