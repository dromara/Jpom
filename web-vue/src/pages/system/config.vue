<template>
  <a-tabs default-active-key="1" @change="tabChange">
    <a-tab-pane key="1">
      <template #tab>
        <span>
          <SettingOutlined />
          {{ $t('pages.system.config.16c8a2eb') }}
        </span>
      </template>

      <a-form ref="editForm" :model="temp">
        <a-form-item>
          <code-editor
            v-model:content="temp.content"
            file-suffix="conf.yml"
            :options="{ mode: 'yaml', tabSize: 2 }"
            :show-tool="true"
            height="calc(100vh - 200px)"
          >
            <template #tool_before>
              <a-alert v-if="temp.file" show-icon :message="`${$t('pages.system.config.b6bc55e9')}:${temp.file}`" />
            </template>
          </code-editor>
        </a-form-item>
        <a-form-item :wrapper-col="{ span: 14, offset: 2 }">
          <a-space>
            <a-button type="primary" class="btn" @click="onSubmit(false)">{{
              $t('pages.system.config.3571a8f0')
            }}</a-button>
            <a-button type="primary" danger class="btn" @click="onSubmit(true)">{{
              $t('pages.system.config.fda40980')
            }}</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-tab-pane>
    <a-tab-pane key="2" class="ip-config-panel">
      <template #tab>
        <span>
          <LockOutlined />
          {{ $t('pages.system.config.12fec1a7') }}
        </span>
      </template>
      <a-alert :message="`${$t('pages.system.config.280a47a7')}${ipTemp.ip}`" type="success" />
      <a-alert :message="$t('pages.system.config.f86e70bd')" style="margin-top: 10px" banner />
      <a-alert :message="$t('pages.system.config.f16607cf')" style="margin-top: 10px" banner />
      <a-form
        ref="editIpConfigForm"
        style="margin-top: 10px"
        :model="ipTemp"
        :label-col="{ span: 2 }"
        :wrapper-col="{ span: 20 }"
      >
        <a-form-item name="prohibited">
          <template #label>
            <a-space align="center">
              <a-tooltip>
                <template #title>{{ $t('pages.system.config.4e608c6f') }} </template>
                <StopFilled />
                IP{{ $t('pages.system.config.170303f') }}
              </a-tooltip>
            </a-space>
          </template>
          <a-textarea
            v-model:value="ipTemp.prohibited"
            :rows="8"
            class="ip-list-config"
            :placeholder="$t('pages.system.config.6c07b8a7')"
          />
        </a-form-item>
        <a-form-item name="allowed">
          <template #label>
            <a-space align="center">
              <a-tooltip>
                <template #title> {{ $t('pages.system.config.c4b3b613') }} </template>
                <CheckCircleFilled />
                IP{{ $t('pages.system.config.e243dfef') }}
              </a-tooltip>
            </a-space>
          </template>
          <a-textarea
            v-model:value="ipTemp.allowed"
            :rows="8"
            class="ip-list-config"
            :placeholder="$t('pages.system.config.1f774cad')"
          />
        </a-form-item>

        <a-form-item :wrapper-col="{ offset: 10 }" class="ip-config-button">
          <a-button type="primary" class="btn" @click="onSubmitIp()">{{ $t('pages.system.config.3571a8f0') }}</a-button>
        </a-form-item>
      </a-form>
    </a-tab-pane>

    <!-- 全局代理 -->
    <a-tab-pane key="6">
      <template #tab>
        <span>
          <ApiOutlined />
          {{ $t('pages.system.config.c1c625c1') }}
        </span>
      </template>
      <a-alert
        :message="`${$t('pages.system.config.2a338cec')}`"
        style="margin-top: 10px; margin-bottom: 20px"
        banner
      />
      <a-row justify="center" type="flex">
        <a-form ref="editProxyForm" :model="proxyConfigData">
          <a-row v-for="(item, index) in proxyConfigData.globalProxy" :key="index">
            <a-space>
              <a-form-item :label="$t('pages.system.config.c39f7c03')" name="pattern">
                <a-input
                  v-model:value="item.pattern"
                  style="width: 30vw"
                  :max-length="200"
                  :placeholder="$t('pages.system.config.62a3d2f9')"
                >
                </a-input>
              </a-form-item>
              <a-form-item :label="$t('pages.system.config.f9f255b0')">
                <a-input
                  v-model:value="item.proxyAddress"
                  style="width: 30vw"
                  :placeholder="$t('pages.system.config.775fdae9')"
                >
                  <template #addonBefore>
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
                  size="small"
                  :disabled="proxyConfigData.globalProxy && proxyConfigData.globalProxy.length <= 1"
                  @click="
                    () => {
                      proxyConfigData.globalProxy && proxyConfigData.globalProxy.splice(index, 1)
                    }
                  "
                >
                  {{ $t('pages.system.config.dd20d11c') }}
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
                >{{ $t('pages.system.config.7d46652a') }}</a-button
              >
              <a-button type="primary" @click="saveProxyConfigHannder">{{
                $t('pages.system.config.3571a8f0')
              }}</a-button>
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
  components: {
    codeEditor
  },
  inject: ['globalLoading'],
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
      $confirm({
        title: this.$t('pages.system.config.a8fe4c17'),
        zIndex: 1009,
        content: this.$t('pages.system.config.b812712c'),
        okText: this.$t('pages.system.config.7da4a591'),
        cancelText: this.$t('pages.system.config.43105e21'),
        onOk: () => {
          this.temp.restart = restart
          return editConfig(this.temp).then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              if (this.temp.restart) {
                this.startCheckRestartStatus(res.msg)
              }
            }
          })
        }
      })
    },
    startCheckRestartStatus(msg) {
      this.checkCount = 0

      this.globalLoading({
        spinning: true,
        tip:
          (msg || this.$t('pages.system.config.9e8c0aa9')) +
          `,${this.$t('pages.system.config.4098e2c9')},${this.$t('pages.system.config.c4521fd3')}`
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
                  message: this.$t('pages.system.config.a6229050')
                })

                setTimeout(() => {
                  location.reload()
                }, 1000)
              } else {
                if (this.checkCount > RESTART_UPGRADE_WAIT_TIME_COUNT) {
                  $notification.warning({
                    message: this.$t('pages.system.config.b0e4c704') + (res.msg || '')
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
                  message: this.$t('pages.system.config.48633a70')
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
      $confirm({
        title: this.$t('pages.system.config.a8fe4c17'),
        zIndex: 1009,
        content: this.$t('pages.system.config.eed9d363'),
        okText: this.$t('pages.system.config.7da4a591'),
        cancelText: this.$t('pages.system.config.43105e21'),
        onOk: () => {
          return editIpConfig(this.ipTemp).then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
            }
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
