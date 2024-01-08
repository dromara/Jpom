<template>
  <div>
    <div v-if="fastInstallInfo">
      <a-collapse v-model:activeKey="fastInstallActiveKey">
        <a-collapse-panel key="1" header="温馨提示">
          <a-alert message="温馨提示" type="warning" show-icon>
            <template v-slot:description>
              <ul>
                <li>
                  复制下面任意一条命令到还未安装插件端的服务器中去执行,执行前需要放行<b>防火墙端口</b>,<b>安全组规则</b>等网络端口限制
                </li>
                <li>插件端运行端口默认使用：<b>2123</b></li>
                <li>
                  执行前需要检查命令中的地址在对应的服务器中是否可以访问,如果无法访问将不能自动绑定节点,<b
                    >会使用 PING 检查</b
                  >
                </li>
                <li>插件端安装并启动成功后将主动上报节点信息,如果上报的 IP+PROP 能正常通讯将添加节点信息</li>
                <li style="color: red">如果上报的节点信息包含多个 IP 地址需要用户确认使用具体的 IP 地址信息</li>
                <li>添加的节点(插件端)将自动<b>绑定到当前工作空间</b>,如果需要在其他工作空间需要提前切换生成命令</li>
                <li>下面命令将在<b>重启服务端后失效</b>,重启服务端需要重新获取</li>
                <li>
                  支持指定网卡名称来绑定：<b>networkName</b>。如：http://192.168.31.175:2122/api/node/receive_push?token=xxx&workspaceId=xxx&networkName=en0
                </li>
              </ul>
            </template>
          </a-alert>
        </a-collapse-panel>
        <a-collapse-panel key="2" header="快速安装">
          <a-tabs :default-active-key="0">
            <a-tab-pane v-for="(item, index) in fastInstallInfo.shUrls" :tab="item.name" :key="index">
              <div>
                <a-alert type="info" :message="`命令内容`">
                  <template v-slot:description>
                    <a-typography-paragraph :copyable="{ tooltip: false, text: item.allText }">
                      <span>{{ item.allText }} </span>
                    </a-typography-paragraph>
                  </template>
                </a-alert>
              </div>
            </a-tab-pane>
          </a-tabs>
        </a-collapse-panel>
        <a-collapse-panel key="3" header="快速绑定">
          <a-alert type="info" :message="`命令内容(命令路径请修改为您的服务器中的实际路径)`">
            <template v-slot:description>
              <a-typography-paragraph :copyable="{ tooltip: false, text: fastInstallInfo.bindCommand }">
                <span>{{ fastInstallInfo.bindCommand }} </span>
              </a-typography-paragraph>
            </template>
          </a-alert>
        </a-collapse-panel>
        <a-collapse-panel key="4" header="执行结果">
          <div v-if="!pullFastInstallResultData || !pullFastInstallResultData.length">还没有任何结果</div>
          <a-alert
            :message="`第 ${index + 1} 个结果`"
            :type="`${item.type === 'success' ? 'success' : item.type === 'exists' ? 'error' : 'warning'}`"
            v-for="(item, index) in pullFastInstallResultData"
            :key="`${index}-${new Date().getTime()}`"
            closable
            @close="clearPullFastInstallResult(item.id)"
          >
            <template v-slot:description>
              <a-space direction="vertical">
                <div v-if="item.type === 'canUseIpEmpty'">
                  <a-tag color="orange">不能和节点正常通讯</a-tag>
                </div>
                <div v-if="item.type === 'multiIp'">
                  <a-tag color="green">多IP可以使用</a-tag>
                </div>
                <div v-if="item.type === 'exists'">
                  <a-tag color="orange">节点已经存在</a-tag>
                </div>
                <div v-if="item.type === 'success'">
                  <a-tag color="orange">绑定成功</a-tag>
                </div>
                <div>
                  所有的IP：<a-tag v-for="(itemIp, indexIp) in item.allIp" :key="indexIp"
                    >{{ itemIp }}:{{ item.port }}</a-tag
                  >
                </div>
                <div v-if="item.type === 'multiIp'">
                  能通讯的IP:
                  <a-tag
                    @click="confirmFastInstall(item.id, itemIp, item.port)"
                    v-for="(itemIp, indexIp) in item.canUseIp"
                    :key="indexIp"
                    >{{ itemIp }}:{{ item.port }}<ApiOutlined
                  /></a-tag>
                </div>
                <div v-if="item.type === 'success' || item.type === 'exists'">
                  节点的IP:
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
  computed: {
    ...mapState(useAppStore, ['getWorkspaceId'])
  },
  data() {
    return {
      fastInstallActiveKey: ['1', '2', '4'],
      fastInstallInfo: null,

      pullFastInstallResultTime: null,
      pullFastInstallResultData: [],
      fastInstallNode: false
    }
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
