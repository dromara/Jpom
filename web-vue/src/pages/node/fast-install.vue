<template>
  <div>
    <div v-if="fastInstallInfo">
      <a-collapse v-model:activeKey="fastInstallActiveKey">
        <a-collapse-panel key="1" :header="$t('pages.node.fast-install.d5e02e8a')">
          <a-alert message="" type="warning" show-icon>
            <template #description>
              <ul>
                <li>
                  {{ $t('pages.node.fast-install.32fdd87c') }}<b>{{ $t('pages.node.fast-install.10b5da87') }}</b
                  >,<b>{{ $t('pages.node.fast-install.c80b813d') }}</b
                  >{{ $t('pages.node.fast-install.c390a968') }}
                </li>
                <li>{{ $t('pages.node.fast-install.72ca71a6') }}<b>2123</b></li>
                <li>
                  {{ $t('pages.node.fast-install.39badef3') }}<b>{{ $t('pages.node.fast-install.d6480653') }}</b>
                </li>
                <li>{{ $t('pages.node.fast-install.3f82fca3') }}</li>
                <li style="color: red">{{ $t('pages.node.fast-install.bea26ef8') }}</li>
                <li>
                  {{ $t('pages.node.fast-install.ba5049eb') }}<b>{{ $t('pages.node.fast-install.8716288a') }}</b
                  >,{{ $t('pages.node.fast-install.4e2553dc') }}
                </li>
                <li>
                  {{ $t('pages.node.fast-install.5056e934') }}<b>{{ $t('pages.node.fast-install.6703099') }}</b
                  >,{{ $t('pages.node.fast-install.fa70b3c9') }}
                </li>
                <li>
                  {{ $t('pages.node.fast-install.5b7c9ba') }}<b>networkName</b
                  >{{
                    $t('pages.node.fast-install.32ef99d')
                  }}://192.168.31.175:2122/api/node/receive_push?token=xxx&workspaceId=xxx&networkName=en0
                </li>
              </ul>
            </template>
          </a-alert>
        </a-collapse-panel>
        <a-collapse-panel key="2" :header="$t('pages.node.fast-install.fa50cb40')">
          <a-tabs :default-active-key="0">
            <a-tab-pane v-for="(item, index) in fastInstallInfo.shUrls" :key="index" :tab="item.name">
              <div>
                <a-alert type="info" :message="`${$t('pages.node.fast-install.f0d88095')}`">
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
        <a-collapse-panel key="3" :header="$t('pages.node.fast-install.7e4f29f1')">
          <a-alert
            type="info"
            :message="`${$t('pages.node.fast-install.f0d88095')}(${$t('pages.node.fast-install.bce891f2')})`"
          >
            <template #description>
              <a-typography-paragraph :copyable="{ tooltip: false, text: fastInstallInfo.bindCommand }">
                <span>{{ fastInstallInfo.bindCommand }} </span>
              </a-typography-paragraph>
            </template>
          </a-alert>
        </a-collapse-panel>
        <a-collapse-panel key="4" :header="$t('pages.node.fast-install.89283f33')">
          <div v-if="!pullFastInstallResultData || !pullFastInstallResultData.length">
            {{ $t('pages.node.fast-install.b01ed3b7') }}
          </div>
          <a-alert
            v-for="(item, index) in pullFastInstallResultData"
            :key="`${index}-${new Date().getTime()}`"
            :message="`${$t('pages.node.fast-install.a3aba05c')} ${index + 1} ${$t(
              'pages.node.fast-install.d6f154c5'
            )}`"
            :type="`${item.type === 'success' ? 'success' : item.type === 'exists' ? 'error' : 'warning'}`"
            closable
            @close="clearPullFastInstallResult(item.id)"
          >
            <template #description>
              <a-space direction="vertical" style="width: 100%">
                <div v-if="item.type === 'canUseIpEmpty'">
                  <a-tag color="orange">{{ $t('pages.node.fast-install.3d731c66') }}</a-tag>
                </div>
                <div v-if="item.type === 'multiIp'">
                  <a-tag color="green">{{ $t('pages.node.fast-install.ece41c2a') }}</a-tag>
                </div>
                <div v-if="item.type === 'exists'">
                  <a-tag color="orange">{{ $t('pages.node.fast-install.1d8067db') }}</a-tag>
                </div>
                <div v-if="item.type === 'success'">
                  <a-tag color="orange">{{ $t('pages.node.fast-install.9c1bff84') }}</a-tag>
                </div>
                <div>
                  {{ $t('pages.node.fast-install.2112ed0f')
                  }}<a-tag v-for="(itemIp, indexIp) in item.allIp" :key="indexIp">{{ itemIp }}:{{ item.port }}</a-tag>
                </div>
                <div v-if="item.type === 'multiIp'">
                  {{ $t('pages.node.fast-install.a38eb00c') }}({{ $t('pages.node.fast-install.a7e9d227') }}):
                  <a-tag
                    v-for="(itemIp, indexIp) in item.canUseIp"
                    :key="indexIp"
                    @click="confirmFastInstall(item.id, itemIp, item.port)"
                    >{{ itemIp }}:{{ item.port }}<ApiOutlined
                  /></a-tag>
                </div>
                <div v-if="item.type === 'success' || item.type === 'exists'">
                  {{ $t('pages.node.fast-install.f48a6e61') }}:
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
