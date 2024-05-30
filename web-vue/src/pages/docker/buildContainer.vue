<template>
  <div>
    <a-drawer
      destroy-on-close
      :open="true"
      width="80vw"
      :title="$tl('p.rebuildContainer')"
      :mask-closable="false"
      :footer-style="{ textAlign: 'right' }"
      @close="
        () => {
          $emit('cancelBtnClick')
        }
      "
    >
      <a-alert
        v-if="containerData && Object.keys(containerData).length"
        style="margin-bottom: 10px"
        :message="$tl('p.operationPrompt')"
        type="warning"
        show-icon
      >
        <template #description>
          {{ $tl('p.containerRebuildDesc') }}
          <div>
            <b style="color: red">{{ $tl('p.containerRestartDeleteBefore') }}</b
            >,{{ $tl('p.containerDataBackupWarning') }}
          </div>
          <div>
            <b>{{ $tl('p.newContainerParamNotGuaranteed') }}</b>
          </div>
        </template>
      </a-alert>
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
        <a-form-item :label="$tl('p.baseImage')" name="name">
          <a-row>
            <a-col :span="10">
              <a-input v-model:value="temp.image" disabled placeholder="" />
            </a-col>
            <a-col :span="4" style="text-align: right">{{ $tl('p.containerName') }}</a-col>
            <a-col :span="10">
              <a-form-item-rest>
                <a-input v-model:value="temp.name" placeholder="容器名称" />
              </a-form-item-rest>
            </a-col>
          </a-row>
        </a-form-item>

        <a-form-item :label="$tl('p.port')">
          <a-form-item-rest>
            <a-space direction="vertical" style="width: 100%">
              <a-row v-for="(item, index) in temp.exposedPorts" :key="index">
                <a-col :span="21">
                  <a-space>
                    <a-input-group>
                      <a-row>
                        <a-col :span="8">
                          <a-input v-model:value="item.ip" addon-before="IP" :placeholder="$tl('p.hostIp')"> </a-input>
                        </a-col>
                        <a-col :span="6" :offset="1">
                          <a-input
                            v-model:value="item.publicPort"
                            :addon-before="$tl('p.port')"
                            :placeholder="$tl('p.port')"
                          >
                          </a-input>
                        </a-col>
                        <a-col :span="8" :offset="1">
                          <a-input
                            v-model:value="item.port"
                            :addon-before="$tl('p.container')"
                            :disabled="item.disabled"
                            :placeholder="$tl('p.containerPort')"
                          >
                            <template #addonAfter>
                              <a-select
                                v-model:value="item.scheme"
                                :disabled="item.disabled"
                                :placeholder="$tl('p.portProtocol')"
                              >
                                <a-select-option value="tcp">tcp</a-select-option>
                                <a-select-option value="udp">udp</a-select-option>
                                <a-select-option value="sctp">sctp</a-select-option>
                              </a-select>
                            </template>
                          </a-input>
                        </a-col>
                      </a-row>
                    </a-input-group>
                  </a-space>
                </a-col>
                <a-col :span="2" :offset="1">
                  <a-space>
                    <MinusCircleOutlined
                      v-if="temp.exposedPorts && temp.exposedPorts.length > 1"
                      @click="
                        () => {
                          temp.exposedPorts.splice(index, 1)
                        }
                      "
                    />

                    <PlusSquareOutlined
                      @click="
                        () => {
                          temp.exposedPorts.push({
                            scheme: 'tcp',
                            ip: '0.0.0.0'
                          })
                        }
                      "
                    />
                  </a-space>
                </a-col>
              </a-row>
            </a-space>
          </a-form-item-rest>
        </a-form-item>

        <a-form-item :label="$tl('p.mountedVolume')">
          <a-form-item-rest>
            <a-space direction="vertical" style="width: 100%">
              <a-row v-for="(item, index) in temp.volumes" :key="index">
                <a-col :span="10">
                  <a-input
                    v-model:value="item.host"
                    :addon-before="$tl('p.host')"
                    :placeholder="`${$tl('p.host')}机目录`"
                  />
                </a-col>
                <a-col :span="10" :offset="1">
                  <a-input
                    v-model:value="item.container"
                    :addon-before="$tl('p.container')"
                    :disabled="item.disabled"
                    :placeholder="$tl('p.containerDirectory')"
                  />
                </a-col>
                <a-col :span="2" :offset="1">
                  <a-space>
                    <MinusCircleOutlined
                      v-if="temp.volumes && temp.volumes.length > 1"
                      @click="
                        () => {
                          temp.volumes.splice(index, 1)
                        }
                      "
                    />

                    <PlusSquareOutlined
                      @click="
                        () => {
                          temp.volumes.push({})
                        }
                      "
                    />
                  </a-space>
                </a-col>
              </a-row>
            </a-space>
          </a-form-item-rest>
        </a-form-item>
        <a-form-item :label="$tl('p.environmentVariables')">
          <a-form-item-rest>
            <a-space direction="vertical" style="width: 100%">
              <a-row v-for="(item, index) in temp.env" :key="index">
                <a-col :span="10">
                  <a-input v-model:value="item.key" :placeholder="$tl('p.variableName')" />
                </a-col>
                <a-col :span="10" :offset="1">
                  <a-input v-model:value="item.value" :placeholder="$tl('p.variableValue')" />
                </a-col>
                <a-col :span="2" :offset="1">
                  <a-space>
                    <MinusCircleOutlined
                      v-if="temp.env && temp.env.length > 1"
                      @click="
                        () => {
                          temp.env.splice(index, 1)
                        }
                      "
                    />

                    <PlusSquareOutlined
                      @click="
                        () => {
                          temp.env.push({})
                        }
                      "
                    />
                  </a-space>
                </a-col>
              </a-row>
            </a-space>
          </a-form-item-rest>
        </a-form-item>
        <a-form-item :label="$tl('p.command')">
          <a-form-item-rest>
            <a-space direction="vertical" style="width: 100%">
              <a-row v-for="(item, index) in temp.commands" :key="index">
                <a-col :span="20">
                  <a-input
                    v-model:value="item.value"
                    :addon-before="$tl('p.commandValue')"
                    :placeholder="$tl('p.fillRunningCommand')"
                  />
                </a-col>

                <a-col :span="2" :offset="1">
                  <a-space>
                    <MinusCircleOutlined
                      v-if="temp.commands && temp.commands.length > 1"
                      @click="
                        () => {
                          temp.commands.splice(index, 1)
                        }
                      "
                    />
                    <PlusSquareOutlined
                      @click="
                        () => {
                          temp.commands.push({})
                        }
                      "
                    />
                  </a-space>
                </a-col>
              </a-row>
            </a-space>
          </a-form-item-rest>
        </a-form-item>
        <a-form-item label="hostname" name="hostname">
          <a-input v-model:value="temp.hostname" :placeholder="$tl('p.hostname')" />
        </a-form-item>
        <a-form-item :label="$tl('p.network')">
          <a-auto-complete
            v-model:value="temp.networkMode"
            :placeholder="$tl('p.networkMode')"
            :options="[
              {
                title: $tl('p.createNetworkStack'),
                value: 'bridge'
              },
              {
                title: $tl('p.containerNoNetwork'),
                value: 'none'
              },
              {
                title: $tl('p.reuseAnotherContainerNetworkStack'),
                value: 'container:<name|id>'
              },
              {
                title: $tl('p.useHostNetworkStack'),
                value: 'host'
              }
            ]"
          >
            <template #option="item">
              {{ item.title }}
            </template>
          </a-auto-complete>
        </a-form-item>
        <a-form-item :label="$tl('p.restartPolicy')">
          <a-auto-complete
            v-model:value="temp.restartPolicy"
            :placeholder="$tl('p.restartPolicyOptions')"
            :options="[
              {
                title: $tl('p.noAutoRestart'),
                value: 'no'
              },
              {
                title: $tl('p.alwaysRestart'),
                value: 'always'
              },
              {
                title: $tl('p.restartOnFailure'),
                value: 'on-failure:1'
              },
              {
                title: $tl('p.restartUnlessStopped'),
                value: 'unless-stopped'
              }
            ]"
          >
            <template #option="item">
              {{ item.title }}
            </template>
          </a-auto-complete>
        </a-form-item>
        <a-form-item label="自定义 host">
          <a-form-item-rest>
            <a-space direction="vertical" style="width: 100%">
              <a-row v-for="(item, index) in temp.extraHosts" :key="index">
                <a-col :span="20">
                  <a-input v-model:value="temp.extraHosts[index]" placeholder="自定义host, xxx:192.168.0.x" />
                </a-col>

                <a-col :span="2" :offset="1">
                  <a-space>
                    <MinusCircleOutlined
                      v-if="temp.extraHosts && temp.extraHosts.length > 1"
                      @click="
                        () => {
                          temp.extraHosts.splice(index, 1)
                        }
                      "
                    />
                    <PlusSquareOutlined
                      @click="
                        () => {
                          temp.extraHosts.push('')
                        }
                      "
                    />
                  </a-space>
                </a-col>
              </a-row>
            </a-space>
          </a-form-item-rest>
        </a-form-item>
        <a-form-item :label="$tl('p.storageOptions')">
          <a-form-item-rest>
            <a-space direction="vertical" style="width: 100%">
              <a-row v-for="(item, index) in temp.storageOpt" :key="index">
                <a-col :span="10">
                  <a-input v-model:value="item.key" :placeholder="$tl('p.configName')" />
                </a-col>
                <a-col :span="10" :offset="1">
                  <a-input v-model:value="item.value" :placeholder="$tl('p.configValue')" />
                </a-col>
                <a-col :span="2" :offset="1">
                  <a-space>
                    <MinusCircleOutlined
                      v-if="temp.storageOpt && temp.storageOpt.length > 1"
                      @click="
                        () => {
                          temp.storageOpt.splice(index, 1)
                        }
                      "
                    />

                    <PlusSquareOutlined
                      @click="
                        () => {
                          temp.storageOpt.push({})
                        }
                      "
                    />
                  </a-space>
                </a-col>
              </a-row>
            </a-space>
          </a-form-item-rest>
        </a-form-item>
        <a-form-item label="runtime">
          <a-input v-model:value="temp.runtime" :placeholder="$tl('p.containerRuntime')" />
        </a-form-item>
        <a-form-item :label="$tl('p.containerLabels')">
          <a-input v-model:value="temp.labels" :placeholder="$tl('p.containerLabelExample')" />
        </a-form-item>
        <a-form-item :label="$tl('p.autoStart')">
          <a-form-item-rest>
            <a-row>
              <a-col :span="4"
                ><a-switch
                  v-model:checked="temp.autorun"
                  :checked-children="$tl('p.start')"
                  :un-checked-children="$tl('p.noStart')"
              /></a-col>
              <a-col :span="4" style="text-align: right">
                <a-tooltip>
                  <template #title>
                    <p>--privileged</p>
                    <ul>
                      privileged=true|false
                      {{
                        $tl('p.introduction')
                      }}
                      <li>true container{{ $tl('p.rootHasTrueRootPermission') }}</li>
                      <li>false container{{ $tl('p.rootIsJustAnOrdinaryUser') }}</li>
                      <li>privileged{{ $tl('p.containerCanSeeManyDevices') }}</li>
                    </ul>
                  </template>

                  <QuestionCircleOutlined v-if="!temp.id" />
                  {{ $tl('p.privilege') }}
                </a-tooltip>
              </a-col>
              <a-col :span="4">
                <a-switch
                  v-model:checked="temp.privileged"
                  :checked-children="$tl('p.yes')"
                  :un-checked-children="$tl('p.no')"
                />
              </a-col>
            </a-row>
          </a-form-item-rest>
        </a-form-item>
      </a-form>
      <template #footer>
        <!-- <div
          :style="{
            position: 'absolute',
            right: 0,
            bottom: 0,
            width: '100%',
            borderTop: '1px solid #e9e9e9',
            padding: '10px 16px',
            background: '#fff',
            textAlign: 'right',
            zIndex: 1
          }"
        > -->
        <a-space>
          <a-button
            @click="
              () => {
                $emit('cancelBtnClick')
              }
            "
          >
            {{ $tl('p.cancel') }}
          </a-button>
          <a-button type="primary" :loading="loading" @click="handleBuildOk"> {{ $tl('p.confirm') }} </a-button>
        </a-space>
        <!-- </div> -->
      </template>
    </a-drawer>
  </div>
</template>

<script>
import {
  dockerImageCreateContainer,
  dockerImageInspect,
  dockerInspectContainer,
  dockerContainerRebuildContainer
} from '@/api/docker-api'
export default {
  props: {
    id: {
      type: String,
      default: ''
    },
    imageId: {
      type: String,
      default: ''
    },
    urlPrefix: {
      type: String,
      default: ''
    },
    machineDockerId: {
      type: String,
      default: ''
    },
    containerId: {
      type: String,
      default: ''
    },
    containerData: {
      type: Object,
      default: () => ({})
    }
  },
  emits: ['cancelBtnClick', 'confirmBtnClick'],
  data() {
    return {
      temp: {},
      rules: {
        name: [
          { required: true, message: this.$tl('p.containerNameIsRequired'), trigger: 'blur' },
          {
            pattern: /[a-zA-Z0-9][a-zA-Z0-9_.-]$/,
            message: this.$tl('p.containerNameAlphanumeric'),
            trigger: 'blur'
          }
        ]
      },
      loading: false
    }
  },
  computed: {
    reqDataId() {
      return this.id || this.machineDockerId
    },
    getLabels() {
      if (!this.containerData.labels) {
        return ''
      }
      let labels = ''
      Object.keys(this.containerData.labels).map((key) => {
        labels += `${key}=${this.containerData.labels[key]}&`
      })
      return labels.slice(0, -1)
    }
  },
  mounted() {
    this.createContainer()
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.docker.buildContainer.${key}`, ...args)
    },
    // 构建镜像
    createContainer() {
      // 判断 containerId
      if (this.containerId) {
        // form container
        this.inspectContainer()
      } else {
        // form image
        this.inspectImage()
      }
    },
    getPortsFromPorts(ports) {
      const _ports = ports.map((item) => {
        item.disabled = item.privatePort !== null
        item.port = item.privatePort
        return item
      })
      return _ports.length > 0 ? _ports : null
    },
    getPortsFromExposedPorts(exposedPorts) {
      const _ports = exposedPorts.map((item) => {
        item.disabled = item.port !== null
        item.ip = '0.0.0.0'
        item.scheme = item.scheme || 'tcp'
        return item
      })
      return _ports.length > 0 ? _ports : null
    },
    getVolumesFromMounts(mounts) {
      const _mounts = mounts.map((item) => {
        item.disabled = item.destination !== null
        item.host = item.source
        item.container = item.destination?.path
        return item
      })
      return _mounts.length > 0 ? _mounts : null
    },
    getRestartPolicy(restartPolicy) {
      if (!restartPolicy) {
        return ''
      }
      const name = restartPolicy.name
      if (restartPolicy.maximumRetryCount) {
        return name + ':' + restartPolicy.maximumRetryCount
      }
      return name
    },
    // inspect container
    inspectContainer() {
      // 单独获取 image 信息
      this.temp = {}
      dockerImageInspect(this.urlPrefix, {
        id: this.reqDataId,
        imageId: this.imageId
      }).then((res) => {
        this.temp = { ...this.temp, image: (res.data.repoTags || []).join(',') }
      })

      dockerInspectContainer(this.urlPrefix, {
        id: this.reqDataId,
        containerId: this.containerId
      }).then((res) => {
        this.buildVisible = true
        const storageOpt = res.data.hostConfig?.storageOpt || { '': '' }

        this.temp = {
          name: res.data?.name,
          labels: this.getLabels,
          volumes: this.getVolumesFromMounts(res.data?.mounts) || [{}],
          exposedPorts: this.getPortsFromPorts(this.containerData.ports) ||
            this.getPortsFromExposedPorts(res.data.config.exposedPorts) || [{}],
          autorun: true,
          imageId: this.imageId,
          env: (res.data?.config?.env || ['']).map((item) => {
            const i = item.indexOf('=')
            if (i == -1) {
              return {}
            }
            return {
              key: item.substring(0, i),
              value: item.substring(i + 1, item.length)
            }
          }) || [{}],
          storageOpt: Object.keys(storageOpt).map((item) => {
            return {
              key: item,
              value: storageOpt[item]
            }
          }),
          commands: (res.data?.config?.cmd || ['']).map((item) => {
            return {
              value: item || ''
            }
          }) || [{}],
          hostname: res.data?.config?.hostName,
          restartPolicy: this.getRestartPolicy(res.data?.hostConfig?.restartPolicy),
          networkMode: res.data?.hostConfig?.networkMode,
          runtime: res.data?.hostConfig?.runtime,
          privileged: res.data.hostConfig?.privileged || false,
          extraHosts: res.data?.hostConfig?.extraHosts || [''],
          ...this.temp
        }
        this.$refs['editForm']?.resetFields()
      })
    },
    // inspect image
    inspectImage() {
      dockerImageInspect(this.urlPrefix, {
        id: this.reqDataId,
        imageId: this.imageId
      }).then((res) => {
        this.buildVisible = true
        this.temp = {
          name: (res.data?.repoTags[0] || '').split(':')[0] || '',
          volumes: [{}],
          exposedPorts: (res.data?.config?.exposedPorts || [{}]).map((item) => {
            item.disabled = item.port !== null
            item.ip = '0.0.0.0'
            item.scheme = item.scheme || 'tcp'
            return item
          }),
          image: (res.data?.repoTags || []).join(','),
          autorun: true,
          imageId: this.imageId,
          env: [{}],
          storageOpt: [{}],
          commands: [{}],
          extraHosts: ['']
        }
        this.$refs['editForm']?.resetFields()
      })
    },
    // 创建容器
    handleBuildOk() {
      this.$refs['editForm'].validate().then(() => {
        const temp = {
          id: this.reqDataId,
          autorun: this.temp.autorun,
          imageId: this.temp.imageId,
          name: this.temp.name,
          env: {},
          commands: [],
          networkMode: this.temp.networkMode,
          privileged: this.temp.privileged,
          restartPolicy: this.temp.restartPolicy,
          labels: this.temp.labels,
          runtime: this.temp.runtime,
          hostname: this.temp.hostname,
          storageOpt: {},
          extraHosts: this.temp.extraHosts
        }
        temp.volumes = (this.temp.volumes || [])
          .filter((item) => {
            return item.host
          })
          .map((item) => {
            return item.host + ':' + item.container
          })
          .join(',')
        // 处理端口
        temp.exposedPorts = (this.temp.exposedPorts || [])
          .filter((item) => {
            return item.publicPort && item.ip
          })
          .map((item) => {
            return item.ip + ':' + item.publicPort + ':' + item.port
          })
          .join(',')
        // 环境变量
        this.temp.env.forEach((item) => {
          if (item.key && item.key) {
            temp.env[item.key] = item.value
          }
        })
        this.temp.storageOpt.forEach((item) => {
          if (item.key && item.key) {
            temp.storageOpt[item.key] = item.value
          }
        })
        //
        temp.commands = (this.temp.commands || []).map((item) => {
          return item.value || ''
        })
        // 判断 containerId
        if (this.containerId) {
          temp.containerId = this.containerId
          this.loading = true
          dockerContainerRebuildContainer(this.urlPrefix, temp)
            .then((res) => {
              if (res.code === 200) {
                $notification.success({
                  message: res.msg
                })

                // 通知父组件关闭弹窗
                this.$emit('confirmBtnClick')
              }
            })
            .finally(() => {
              this.loading = false
            })
        } else {
          this.loading = true
          dockerImageCreateContainer(this.urlPrefix, temp)
            .then((res) => {
              if (res.code === 200) {
                $notification.success({
                  message: res.msg
                })

                // 通知父组件关闭弹窗
                this.$emit('confirmBtnClick')
              }
            })
            .finally(() => {
              this.loading = false
            })
        }
      })
    }
  }
}
</script>
