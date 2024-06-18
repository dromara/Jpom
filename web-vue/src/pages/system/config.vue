<template>
  <a-tabs default-active-key="1" @change="tabChange">
    <a-tab-pane key="1">
      <template #tab>
        <span>
          <SettingOutlined />
          {{ $t('i18n_3181790b4b') }}
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
              <a-alert v-if="temp.file" show-icon :message="`${$t('i18n_37c1eb9b23')}:${temp.file}`" />
            </template>
          </code-editor>
        </a-form-item>
        <a-form-item :wrapper-col="{ span: 14, offset: 2 }">
          <a-space>
            <a-button type="primary" class="btn" @click="onSubmit(false)">{{ $t('i18n_be5fbbe34c') }}</a-button>
            <a-button type="primary" danger class="btn" @click="onSubmit(true)">{{ $t('i18n_6aab88d6a3') }}</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-tab-pane>
    <a-tab-pane key="2" class="ip-config-panel">
      <template #tab>
        <span>
          <LockOutlined />
          {{ $t('i18n_decef97c7c') }}
        </span>
      </template>
      <a-alert :message="`${$t('i18n_22cf31df5d')}${ipTemp.ip}`" type="success" />
      <a-alert :message="$t('i18n_578adf7a12')" style="margin-top: 10px" banner />
      <a-alert :message="$t('i18n_49645e398b')" style="margin-top: 10px" banner />
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
                <template #title>{{ $t('i18n_02db59c146') }} </template>
                <StopFilled />
                IP{{ $t('i18n_ff1fda9e47') }}
              </a-tooltip>
            </a-space>
          </template>
          <a-textarea
            v-model:value="ipTemp.prohibited"
            :rows="8"
            class="ip-list-config"
            :placeholder="$t('i18n_5569a840c8')"
          />
        </a-form-item>
        <a-form-item name="allowed">
          <template #label>
            <a-space align="center">
              <a-tooltip>
                <template #title> {{ $t('i18n_8e331a52de') }} </template>
                <CheckCircleFilled />
                IP{{ $t('i18n_98a315c0fc') }}
              </a-tooltip>
            </a-space>
          </template>
          <a-textarea
            v-model:value="ipTemp.allowed"
            :rows="8"
            class="ip-list-config"
            :placeholder="$t('i18n_847afa1ff2')"
          />
        </a-form-item>

        <a-form-item :wrapper-col="{ offset: 10 }" class="ip-config-button">
          <a-button type="primary" class="btn" @click="onSubmitIp()">{{ $t('i18n_be5fbbe34c') }}</a-button>
        </a-form-item>
      </a-form>
    </a-tab-pane>

    <!-- 全局代理 -->
    <a-tab-pane key="6">
      <template #tab>
        <span>
          <ApiOutlined />
          {{ $t('i18n_a0d0ebc519') }}
        </span>
      </template>
      <a-alert :message="`${$t('i18n_c9b79a2b4f')}`" style="margin-top: 10px; margin-bottom: 20px" banner />
      <a-row justify="center" type="flex">
        <a-form ref="editProxyForm" :model="proxyConfigData">
          <a-row v-for="(item, index) in proxyConfigData.globalProxy" :key="index">
            <a-space>
              <a-form-item :label="$t('i18n_3ac34faf6d')" name="pattern">
                <a-input
                  v-model:value="item.pattern"
                  style="width: 30vw"
                  :max-length="200"
                  :placeholder="$t('i18n_0a9634edf2')"
                >
                </a-input>
              </a-form-item>
              <a-form-item :label="$t('i18n_fc954d25ec')">
                <a-input v-model:value="item.proxyAddress" style="width: 30vw" :placeholder="$t('i18n_dcf14deb0e')">
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
                  {{ $t('i18n_2f4aaddde3') }}
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
                >{{ $t('i18n_66ab5e9f24') }}</a-button
              >
              <a-button type="primary" @click="saveProxyConfigHannder">{{ $t('i18n_be5fbbe34c') }}</a-button>
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
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_863a95c914'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
        tip: (msg || this.$t('i18n_85da2e5bb1')) + `,${this.$t('i18n_809b12d6a0')},${this.$t('i18n_af013dd9dc')}`
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
                  message: this.$t('i18n_906f6102a7')
                })

                setTimeout(() => {
                  location.reload()
                }, 1000)
              } else {
                if (this.checkCount > RESTART_UPGRADE_WAIT_TIME_COUNT) {
                  $notification.warning({
                    message: this.$t('i18n_953ec2172b') + (res.msg || '')
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
                  message: this.$t('i18n_0e502fed63')
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
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_a2e62165dc'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
