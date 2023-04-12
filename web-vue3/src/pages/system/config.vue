<template>
  <a-tabs default-active-key="1" @change="tabChange">
    <a-tab-pane key="1">
      <span slot="tab">
        <a-icon type="setting" />
        服务端系统配置
      </span>
      <a-alert
        v-if="temp.file"
        :message="`配置文件路径:${temp.file}`"
        style="margin-top: 10px; margin-bottom: 20px"
        banner
      />
      <a-form ref="editForm" :model="temp">
        <a-form-item class="config-editor">
          <code-editor v-model="temp.content" :options="{ mode: 'yaml', tabSize: 2 }"></code-editor>
        </a-form-item>
        <a-form-item :wrapper-col="{ span: 14, offset: 2 }">
          <a-space>
            <a-button type="primary" class="btn" @click="onSubmit(false)">保存</a-button>
            <a-button type="danger" class="btn" @click="onSubmit(true)">保存并重启</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-tab-pane>
    <a-tab-pane key="2" class="ip-config-panel">
      <span slot="tab">
        <a-icon type="lock" />
        服务端IP白名单配置
      </span>
      <a-alert :message="`当前访问IP：${ipTemp.ip}`" type="success" />
      <a-alert
        message="请仔细确认后配置，ip配置后立即生效。配置时需要保证当前ip能访问！127.0.0.1 该IP不受访问限制.支持配置IP段 192.168.1.1/192.168.1.254,192.168.1.0/24"
        style="margin-top: 10px"
        banner
      />
      <a-alert
        message="如果配置错误需要重启服务端并添加命令行参数 --rest:ip_config 将恢复默认配置"
        style="margin-top: 10px"
        banner
      />
      <a-form
        style="margin-top: 10px"
        ref="editIpConfigForm"
        :model="temp"
        :label-col="{ span: 2 }"
        :wrapper-col="{ span: 20 }"
      >
        <a-form-item prop="content">
          <template slot="label">
            <a-space align="center">
              <a-tooltip>
                <template slot="title">禁止访问的 IP 地址 </template>
                <a-icon type="stop" theme="twoTone" />
                IP黑名单
              </a-tooltip>
            </a-space>
          </template>
          <a-input
            v-model="ipTemp.prohibited"
            type="textarea"
            :rows="8"
            class="ip-list-config"
            placeholder="请输入IP黑名单,多个使用换行,支持配置IP段 192.168.1.1/192.168.1.254,192.168.1.0/24"
          />
        </a-form-item>
        <a-form-item prop="content">
          <template slot="label">
            <a-space align="center">
              <a-tooltip>
                <template slot="title"> 只允许访问的 IP 地址 </template>
                <a-icon type="check-circle" theme="twoTone" />
                IP白名单
              </a-tooltip>
            </a-space>
          </template>
          <a-input
            v-model="ipTemp.allowed"
            type="textarea"
            :rows="8"
            class="ip-list-config"
            placeholder="请输入IP白名单,多个使用换行,0.0.0.0 是开放所有IP,支持配置IP段 192.168.1.1/192.168.1.254,192.168.1.0/24"
          />
        </a-form-item>

        <a-form-item :wrapper-col="{ offset: 10 }" class="ip-config-button">
          <a-button type="primary" class="btn" @click="onSubmitIp()">保存</a-button>
        </a-form-item>
      </a-form>
    </a-tab-pane>

    <!-- 全局代理 -->
    <a-tab-pane key="6">
      <span slot="tab">
        <a-icon type="api" />
        全局代理
      </span>
      <a-alert
        :message="`全局代理配置后将对服务端的网络生效，代理实现方式：ProxySelector`"
        style="margin-top: 10px; margin-bottom: 20px"
        banner
      />
      <a-row justify="center" type="flex">
        <a-form layout="inline" ref="editProxyForm" :model="proxyConfigData">
          <a-row v-for="(item, index) in proxyConfigData.globalProxy" :key="index">
            <a-form-item label="通配符" prop="pattern">
              <a-input
                style="width: 30vw"
                :maxLength="200"
                v-model="item.pattern"
                placeholder="地址通配符,* 表示所有地址都将使用代理"
              >
              </a-input>
            </a-form-item>
            <a-form-item label="代理" prop="httpProxy">
              <a-input style="width: 30vw" v-model="item.proxyAddress" placeholder="代理地址 (127.0.0.1:8888)">
                <a-select slot="addonBefore" v-model="item.proxyType" style="width: 100px">
                  <a-select-option value="HTTP">HTTP</a-select-option>
                  <a-select-option value="SOCKS">SOCKS</a-select-option>
                  <a-select-option value="DIRECT">DIRECT</a-select-option>
                </a-select>
              </a-input>
            </a-form-item>
            <a-form-item>
              <a-button
                type="danger"
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
          </a-row>
          <a-row type="flex" justify="center">
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
                  >添加</a-button
                >
                <a-button type="primary" @click="saveProxyConfigHannder">保存</a-button>
              </a-space>
            </a-form-item>
          </a-row>
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
    // 加载 ip 白名单配置
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
        title: '系统提示',
        content: '真的要保存当前配置吗？如果配置有误,可能无法启动服务需要手动还原奥！！！',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          this.temp.restart = restart
          editConfig(this.temp).then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              if (this.temp.restart) {
                this.startCheckRestartStatus(res.msg)
              }
              //
            }
          })
        }
      })
    },
    startCheckRestartStatus(msg) {
      this.checkCount = 0
      this.$setLoading({
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
                this.$setLoading(false)
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
                  this.$setLoading(false)
                  clearInterval(this.timer)
                }
              }
            })
            .catch((error) => {
              console.error(error)
              if (this.checkCount > RESTART_UPGRADE_WAIT_TIME_COUNT) {
                this.$setLoading(false)
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
      $confirm({
        title: '系统提示',
        content:
          '真的要保存当前配置吗？IP 白名单请慎重配置奥( 白名单是指只允许访问的 IP ),配置后立马生效 如果配置错误将出现无法访问的情况,需要手动恢复奥！！！',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          editIpConfig(this.ipTemp).then((res) => {
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
<style scoped>
textarea {
  resize: none;
}
.config-editor {
  height: calc(100vh - 300px);
  width: 100%;
  overflow-y: scroll;
  border: 1px solid #d9d9d9;
}
</style>
