<template>
  <a-tabs default-active-key="1" @change="tabChange">
    <a-tab-pane key="1">
      <template v-slot:tab>
        <span>
          <SettingOutlined />
          服务端系统配置
        </span>
      </template>

      <a-form ref="editForm" :model="temp">
        <a-form-item>
          <code-editor
            v-model:content="temp.content"
            fileSuffix="conf.yml"
            :options="{ mode: 'yaml', tabSize: 2 }"
            :showTool="true"
            height="calc(100vh - 200px)"
          >
            <template #tool_before>
              <a-alert show-icon v-if="temp.file" :message="`配置文件路径:${temp.file}`" />
            </template>
          </code-editor>
        </a-form-item>
        <a-form-item :wrapper-col="{ span: 14, offset: 2 }">
          <a-space>
            <a-button type="primary" class="btn" @click="onSubmit(false)">保存</a-button>
            <a-button type="primary" danger class="btn" @click="onSubmit(true)">保存并重启</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-tab-pane>
    <a-tab-pane key="2" class="ip-config-panel">
      <template #tab>
        <span>
          <LockOutlined />
          服务端IP授权配置
        </span>
      </template>
      <a-alert :message="`当前访问IP：${ipTemp.ip}`" type="success" />
      <a-alert
        message="请仔细确认后配置，ip配置后立即生效。配置时需要保证当前ip能访问！127.0.0.1 该IP不受访问限制.支持配置IP段 192.168.1.1/192.168.1.254,192.168.1.0/24"
        style="margin-top: 10px"
        banner
      />
      <a-alert
        message="如果配置错误需要重启服务端并新增命令行参数 --rest:ip_config 将恢复默认配置"
        style="margin-top: 10px"
        banner
      />
      <a-form
        style="margin-top: 10px"
        ref="editIpConfigForm"
        :model="ipTemp"
        :label-col="{ span: 2 }"
        :wrapper-col="{ span: 20 }"
      >
        <a-form-item name="prohibited">
          <template #label>
            <a-space align="center">
              <a-tooltip>
                <template #title>禁止访问的 IP 地址 </template>
                <StopFilled />
                IP禁止
              </a-tooltip>
            </a-space>
          </template>
          <a-textarea
            v-model:value="ipTemp.prohibited"
            :rows="8"
            class="ip-list-config"
            placeholder="请输入IP禁止,多个使用换行,支持配置IP段 192.168.1.1/192.168.1.254,192.168.1.0/24"
          />
        </a-form-item>
        <a-form-item name="allowed">
          <template #label>
            <a-space align="center">
              <a-tooltip>
                <template v-slot:title> 只允许访问的 IP 地址 </template>
                <CheckCircleFilled />
                IP授权
              </a-tooltip>
            </a-space>
          </template>
          <a-textarea
            v-model:value="ipTemp.allowed"
            :rows="8"
            class="ip-list-config"
            placeholder="请输入IP授权,多个使用换行,0.0.0.0 是开放所有IP,支持配置IP段 192.168.1.1/192.168.1.254,192.168.1.0/24"
          />
        </a-form-item>

        <a-form-item :wrapper-col="{ offset: 10 }" class="ip-config-button">
          <a-button type="primary" class="btn" @click="onSubmitIp()">保存</a-button>
        </a-form-item>
      </a-form>
    </a-tab-pane>

    <!-- 全局代理 -->
    <a-tab-pane key="6">
      <template v-slot:tab>
        <span>
          <ApiOutlined />
          全局代理
        </span>
      </template>
      <a-alert
        :message="`全局代理配置后将对服务端的网络生效，代理实现方式：ProxySelector`"
        style="margin-top: 10px; margin-bottom: 20px"
        banner
      />
      <a-row justify="center" type="flex">
        <a-form ref="editProxyForm" :model="proxyConfigData">
          <a-row v-for="(item, index) in proxyConfigData.globalProxy" :key="index">
            <a-space>
              <a-form-item label="通配符" name="pattern">
                <a-input
                  style="width: 30vw"
                  :maxLength="200"
                  v-model:value="item.pattern"
                  placeholder="地址通配符,* 表示所有地址都将使用代理"
                >
                </a-input>
              </a-form-item>
              <a-form-item label="代理">
                <a-input style="width: 30vw" v-model:value="item.proxyAddress" placeholder="代理地址 (127.0.0.1:8888)">
                  <template v-slot:addonBefore>
                    <a-select v-model:value="item.proxyType" style="width: 100px">
                      <a-select-option value="HTTP">HTTP</a-select-option>
                      <a-select-option value="SOCKS">SOCKS</a-select-option>
                      <a-select-option value="DIRECT">DIRECT</a-select-option>
                    </a-select>
                  </template>
                </a-input>
              </a-form-item>
              <a-form-item>
                <a-button
                  type="primary"
                  danger
                  @click="
                    () => {
                      proxyConfigData.globalProxy && proxyConfigData.globalProxy.splice(index, 1)
                    }
                  "
                  size="small"
                  :disabled="proxyConfigData.globalProxy && proxyConfigData.globalProxy.length <= 1"
                >
                  删除
                </a-button>
              </a-form-item>
            </a-space>
          </a-row>

          <a-form-item>
            <a-space>
              <a-button
                type="primary"
                @click="
                  () => {
                    proxyConfigData = {
                      ...proxyConfigData,
                      globalProxy: [
                        ...proxyConfigData.globalProxy,
                        {
                          proxyType: 'HTTP'
                        }
                      ]
                    }
                  }
                "
                >新增</a-button
              >
              <a-button type="primary" @click="saveProxyConfigHannder">保存</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </a-row>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
import {
  editConfig,
  editIpConfig,
  getConfigData,
  getIpConfigData,
  getProxyConfig,
  saveProxyConfig,
  systemInfo
} from '@/api/system'
import codeEditor from '@/components/codeEditor'
import { RESTART_UPGRADE_WAIT_TIME_COUNT } from '@/utils/const'

export default {
  inject: ['globalLoading'],
  components: {
    codeEditor
  },
  data() {
    return {
      temp: {
        content: ''
      },
      ipTemp: {
        allowed: '',
        prohibited: ''
      },

      checkCount: 0,

      proxyConfigData: {
        globalProxy: [
          {
            proxyType: 'HTTP'
          }
        ]
      }
    }
  },
  mounted() {
    this.tabChange('1')
  },
  methods: {
    // load data
    loadConfitData() {
      getConfigData().then((res) => {
        if (res.code === 200) {
          this.temp.content = res.data.content
          this.temp.file = res.data.file
        }
      })
    },
    // 加载 ip 授权配置
    loadIpConfigData() {
      getIpConfigData().then((res) => {
        if (res.code === 200) {
          if (res.data) {
            this.ipTemp = res.data
          }
        }
      })
    },

    // submit
    onSubmit(restart) {
      const that = this
      $confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '真的要保存当前配置吗？如果配置有误,可能无法启动服务需要手动还原奥！！！',
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            that.temp.restart = restart
            editConfig(that.temp)
              .then((res) => {
                if (res.code === 200) {
                  // 成功
                  $notification.success({
                    message: res.msg
                  })
                  if (that.temp.restart) {
                    that.startCheckRestartStatus(res.msg)
                  }
                  //
                }
                resolve()
              })
              .catch(reject)
          })
        }
      })
    },
    startCheckRestartStatus(msg) {
      this.checkCount = 0

      this.globalLoading({
        spinning: true,
        tip: (msg || '重启中，请稍候...') + ',请耐心等待暂时不用刷新页面,重启成功后会自动刷新'
      })
      setTimeout(() => {
        //
        this.timer = setInterval(() => {
          systemInfo()
            .then((res) => {
              if (res.code === 200) {
                clearInterval(this.timer)
                this.globalLoading({
                  spinning: false
                })
                $notification.success({
                  message: '重启成功'
                })

                setTimeout(() => {
                  location.reload()
                }, 1000)
              } else {
                if (this.checkCount > RESTART_UPGRADE_WAIT_TIME_COUNT) {
                  $notification.warning({
                    message: '未重启成功：' + (res.msg || '')
                  })
                  this.globalLoading({
                    spinning: false
                  })
                  clearInterval(this.timer)
                }
              }
            })
            .catch((error) => {
              console.error(error)
              if (this.checkCount > RESTART_UPGRADE_WAIT_TIME_COUNT) {
                this.globalLoading({
                  spinning: false
                })
                $notification.error({
                  message: '重启超时,请去服务器查看控制台日志排查问题'
                })
                clearInterval(this.timer)
              }
            })
          this.checkCount = this.checkCount + 1
        }, 2000)
      }, 6000)
    },
    // submit ip config
    onSubmitIp() {
      const that = this
      $confirm({
        title: '系统提示',
        zIndex: 1009,
        content:
          '真的要保存当前配置吗？IP 授权请慎重配置奥( 授权是指只允许访问的 IP ),配置后立马生效 如果配置错误将出现无法访问的情况,需要手动恢复奥！！！',
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            editIpConfig(that.ipTemp)
              .then((res) => {
                if (res.code === 200) {
                  // 成功
                  $notification.success({
                    message: res.msg
                  })
                }
                resolve()
              })
              .catch(reject)
          })
        }
      })
    },

    // 加载
    loadProxyConfig() {
      getProxyConfig().then((res) => {
        if (res.data && res.data.length) {
          this.proxyConfigData = { globalProxy: res.data }
        }
      })
    },
    // 保存
    saveProxyConfigHannder() {
      saveProxyConfig(this.proxyConfigData.globalProxy).then((res) => {
        if (res.code === 200) {
          // 成功
          $notification.success({
            message: res.msg
          })
        }
      })
    },
    // 切换
    tabChange(activeKey) {
      if (activeKey === '1') {
        this.loadConfitData()
      } else if (activeKey === '2') {
        this.loadIpConfigData()
      } else if (activeKey === '6') {
        //
        this.loadProxyConfig()
      }
    }
  }
}
</script>

<style scoped></style>
