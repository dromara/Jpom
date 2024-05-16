<template>
  <div>
    <div v-if="fastInstallInfo">
      <a-collapse v-model:activeKey="fastInstallActiveKey">
        <a-collapse-panel key="1" :header="$tl('p.warmTip')">
          <a-alert message="" type="warning" show-icon>
            <template #description>
              <ul>
                <li>
                  {{ $tl('p.copyCommand') }}<b>{{ $tl('p.firewallPort') }}</b
                  >,<b>{{ $tl('p.securityGroupRule') }}</b
                  >{{ $tl('p.networkPortRestriction') }}
                </li>
                <li>{{ $tl('p.defaultPluginPort') }}<b>2123</b></li>
                <li>
                  {{ $tl('p.checkAddressAccess') }}<b>{{ $tl('p.usePingCheck') }}</b>
                </li>
                <li>{{ $tl('p.reportNodeInfo') }}</li>
                <li style="color: red">{{ $tl('p.confirmMultipleIps') }}</li>
                <li>
                  {{ $tl('p.autoBindWorkspace') }}<b>{{ $tl('p.bindToCurrentWorkspace') }}</b
                  >,{{ $tl('p.switchWorkspace') }}
                </li>
                <li>
                  {{ $tl('p.commandExpireAfterRestart') }}<b>{{ $tl('p.commandInvalidateAfterRestart') }}</b
                  >,{{ $tl('p.regainCommandAfterRestart') }}
                </li>
                <li>
                  {{ $tl('p.supportSpecifyNetworkCard') }}<b>networkName</b
                  >{{
                    $tl('p.networkCardExample')
                  }}://192.168.31.175:2122/api/node/receive_push?token=xxx&workspaceId=xxx&networkName=en0
                </li>
              </ul>
            </template>
          </a-alert>
        </a-collapse-panel>
        <a-collapse-panel key="2" :header="$tl('p.quickInstall')">
          <a-tabs :default-active-key="0">
            <a-tab-pane v-for="(item, index) in fastInstallInfo.shUrls" :key="index" :tab="item.name">
              <div>
                <a-alert type="info" :message="`${$tl('p.commandContent')}`">
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
        <a-collapse-panel key="3" :header="$tl('p.quickBind')">
          <a-alert type="info" :message="`${$tl('p.commandContent')}(${$tl('p.commandPathNote')})`">
            <template #description>
              <a-typography-paragraph :copyable="{ tooltip: false, text: fastInstallInfo.bindCommand }">
                <span>{{ fastInstallInfo.bindCommand }} </span>
              </a-typography-paragraph>
            </template>
          </a-alert>
        </a-collapse-panel>
        <a-collapse-panel key="4" :header="$tl('p.executionResult')">
          <div v-if="!pullFastInstallResultData || !pullFastInstallResultData.length">{{ $tl('p.noResultYet') }}</div>
          <a-alert
            v-for="(item, index) in pullFastInstallResultData"
            :key="`${index}-${new Date().getTime()}`"
            :message="`${$tl('p.resultIndex')} ${index + 1} ${$tl('p.resultOrder')}`"
            :type="`${item.type === 'success' ? 'success' : item.type === 'exists' ? 'error' : 'warning'}`"
            closable
            @close="clearPullFastInstallResult(item.id)"
          >
            <template #description>
              <a-space direction="vertical" style="width: 100%">
                <div v-if="item.type === 'canUseIpEmpty'">
                  <a-tag color="orange">{{ $tl('p.cantCommunicateWithNode') }}</a-tag>
                </div>
                <div v-if="item.type === 'multiIp'">
                  <a-tag color="green">{{ $tl('p.multipleIpsAvailable') }}</a-tag>
                </div>
                <div v-if="item.type === 'exists'">
                  <a-tag color="orange">{{ $tl('p.nodeAlreadyExists') }}</a-tag>
                </div>
                <div v-if="item.type === 'success'">
                  <a-tag color="orange">{{ $tl('p.bindSuccess') }}</a-tag>
                </div>
                <div>
                  {{ $tl('p.allIps')
                  }}<a-tag v-for="(itemIp, indexIp) in item.allIp" :key="indexIp">{{ itemIp }}:{{ item.port }}</a-tag>
                </div>
                <div v-if="item.type === 'multiIp'">
                  {{ $tl('p.communicableIps') }}({{ $tl('p.manualConfirmationRequired') }}):
                  <a-tag
                    v-for="(itemIp, indexIp) in item.canUseIp"
                    :key="indexIp"
                    @click="confirmFastInstall(item.id, itemIp, item.port)"
                    >{{ itemIp }}:{{ item.port }}<ApiOutlined
                  /></a-tag>
                </div>
                <div v-if="item.type === 'success' || item.type === 'exists'">
                  {{ $tl('p.nodeIp') }}:
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
    $tl(key, ...args) {
      return this.$t(`pages.node.fastInstall.${key}`, ...args)
    },
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

          this.fastInstallInfo.host = `${location.protocol}//${location.host}${res.data.url}?token=${res.data.token}\\&workspaceId=${this.getWorkspaceId()}`
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
