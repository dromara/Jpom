<template>
  <div>
    <CustomDrawer
      destroy-on-close
      :open="true"
      width="80vw"
      :title="$t('i18n_585ae8592f')"
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
        :message="$t('i18n_a35740ae41')"
        type="warning"
        show-icon
      >
        <template #description>
          {{ $t('i18n_77688e95af') }}
          <div>
            <b style="color: red">{{ $t('i18n_c3490e81bf') }}</b
            >,{{ $t('i18n_765592aa05') }}
          </div>
          <div>
            <b>{{ $t('i18n_706333387b') }}</b>
          </div>
        </template>
      </a-alert>
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
        <a-form-item :label="$t('i18n_42a93314b4')" name="name">
          <a-row>
            <a-col :span="10">
              <a-input v-model:value="temp.image" disabled placeholder="" />
            </a-col>
            <a-col :span="4" style="text-align: right">{{ $t('i18n_0c256f73b8') }}</a-col>
            <a-col :span="10">
              <a-form-item-rest>
                <a-input v-model:value="temp.name" :placeholder="$t('i18n_a51cd0898f')" />
              </a-form-item-rest>
            </a-col>
          </a-row>
        </a-form-item>

        <a-form-item :label="$t('i18n_c76cfefe72')">
          <a-form-item-rest>
            <a-space direction="vertical" style="width: 100%">
              <a-row v-for="(item, index) in temp.exposedPorts" :key="index">
                <a-col :span="21">
                  <a-space>
                    <a-input-group>
                      <a-row>
                        <a-col :span="8">
                          <a-input v-model:value="item.ip" addon-before="IP" :placeholder="$t('i18n_8e89763d95')">
                          </a-input>
                        </a-col>
                        <a-col :span="6" :offset="1">
                          <a-input
                            v-model:value="item.publicPort"
                            :addon-before="$t('i18n_c76cfefe72')"
                            :placeholder="$t('i18n_c76cfefe72')"
                          >
                          </a-input>
                        </a-col>
                        <a-col :span="8" :offset="1">
                          <a-input
                            v-model:value="item.port"
                            :addon-before="$t('i18n_22c799040a')"
                            :disabled="item.disabled"
                            :placeholder="$t('i18n_ceee1db95a')"
                          >
                            <template #addonAfter>
                              <a-select
                                v-model:value="item.scheme"
                                :disabled="item.disabled"
                                :placeholder="$t('i18n_0739b9551d')"
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

        <a-form-item :label="$t('i18n_640374b7ae')">
          <a-form-item-rest>
            <a-space direction="vertical" style="width: 100%">
              <a-row v-for="(item, index) in temp.volumes" :key="index">
                <a-col :span="10">
                  <a-input
                    v-model:value="item.host"
                    :addon-before="$t('i18n_ad4b4a5b3b')"
                    :placeholder="$t('i18n_90eac06e61')"
                  />
                </a-col>
                <a-col :span="10" :offset="1">
                  <a-input
                    v-model:value="item.container"
                    :addon-before="$t('i18n_22c799040a')"
                    :disabled="item.disabled"
                    :placeholder="$t('i18n_b3401c3657')"
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
        <a-form-item :label="$t('i18n_3867e350eb')">
          <a-form-item-rest>
            <a-space direction="vertical" style="width: 100%">
              <a-row v-for="(item, index) in temp.env" :key="index">
                <a-col :span="10">
                  <a-input v-model:value="item.key" :placeholder="$t('i18n_a25657422b')" />
                </a-col>
                <a-col :span="10" :offset="1">
                  <a-input v-model:value="item.value" :placeholder="$t('i18n_9a2ee7044f')" />
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
        <a-form-item :label="$t('i18n_ddf7d2a5ce')">
          <a-form-item-rest>
            <a-space direction="vertical" style="width: 100%">
              <a-row v-for="(item, index) in temp.commands" :key="index">
                <a-col :span="20">
                  <a-input
                    v-model:value="item.value"
                    :addon-before="$t('i18n_579a6d0d92')"
                    :placeholder="$t('i18n_2a6a516f9d')"
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
          <a-input v-model:value="temp.hostname" :placeholder="$t('i18n_f9361945f3')" />
        </a-form-item>
        <a-form-item :label="$t('i18n_7ddbe15c84')">
          <a-auto-complete
            v-model:value="temp.networkMode"
            :placeholder="$t('i18n_abd9ee868a')"
            :options="[
              {
                title: $t('i18n_c36ab9a223'),
                value: 'bridge'
              },
              {
                title: $t('i18n_3d6acaa5ca'),
                value: 'none'
              },
              {
                title: $t('i18n_fcaef5b17a'),
                value: 'container:<name|id>'
              },
              {
                title: $t('i18n_ff39c45fbc'),
                value: 'host'
              }
            ]"
          >
            <template #option="item">
              {{ item.title }}
            </template>
          </a-auto-complete>
        </a-form-item>
        <a-form-item :label="$t('i18n_5805998e42')">
          <a-auto-complete
            v-model:value="temp.restartPolicy"
            :placeholder="$t('i18n_32dcc6f36e')"
            :options="[
              {
                title: $t('i18n_0c1f1cd79b'),
                value: 'no'
              },
              {
                title: $t('i18n_7006410585'),
                value: 'always'
              },
              {
                title: $t('i18n_2456d2c0f8'),
                value: 'on-failure:1'
              },
              {
                title: $t('i18n_c97e6e823a'),
                value: 'unless-stopped'
              }
            ]"
          >
            <template #option="item">
              {{ item.title }}
            </template>
          </a-auto-complete>
        </a-form-item>
        <a-form-item :label="$t('i18n_3c586b2cc0')">
          <a-form-item-rest>
            <a-space direction="vertical" style="width: 100%">
              <a-row v-for="(item, index) in temp.extraHosts" :key="index">
                <a-col :span="20">
                  <a-input v-model:value="temp.extraHosts[index]" :placeholder="$t('i18n_5bca8cf7ee')" />
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
        <a-form-item :label="$t('i18n_64933b1012')">
          <a-form-item-rest>
            <a-space direction="vertical" style="width: 100%">
              <a-row v-for="(item, index) in temp.storageOpt" :key="index">
                <a-col :span="10">
                  <a-input v-model:value="item.key" :placeholder="$t('i18n_930fdcdf90')" />
                </a-col>
                <a-col :span="10" :offset="1">
                  <a-input v-model:value="item.value" :placeholder="$t('i18n_e84b981eb4')" />
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
          <a-input v-model:value="temp.runtime" :placeholder="$t('i18n_96e6f43118')" />
        </a-form-item>
        <a-form-item :label="$t('i18n_a823cfa70c')">
          <parameter-widget v-model:value="temp.labels"></parameter-widget>
          <!-- <a-input v-model:value="temp.labels" :placeholder="$t('i18n_4b404646f4')" /> -->
        </a-form-item>
        <a-form-item :label="$t('i18n_7307ca1021')">
          <a-form-item-rest>
            <a-row>
              <a-col :span="4"
                ><a-switch
                  v-model:checked="temp.autorun"
                  :checked-children="$t('i18n_8e54ddfe24')"
                  :un-checked-children="$t('i18n_f4baf7c6c0')"
              /></a-col>
              <a-col :span="4" style="text-align: right">
                <a-tooltip>
                  <template #title>
                    <p>--privileged</p>
                    <ul>
                      privileged=true|false
                      {{
                        $t('i18n_61a3ec6656')
                      }}
                      <li>true container{{ $t('i18n_9c2f1d3f39') }}</li>
                      <li>false container{{ $t('i18n_211354a780') }}</li>
                      <li>privileged{{ $t('i18n_ffaf95f0ef') }}</li>
                    </ul>
                  </template>

                  <QuestionCircleOutlined v-if="!temp.id" />
                  {{ $t('i18n_059ac641c0') }}
                </a-tooltip>
              </a-col>
              <a-col :span="4">
                <a-switch
                  v-model:checked="temp.privileged"
                  :checked-children="$t('i18n_0a60ac8f02')"
                  :un-checked-children="$t('i18n_c9744f45e7')"
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
            {{ $t('i18n_625fb26b4b') }}
          </a-button>
          <a-button type="primary" :loading="loading" @click="handleBuildOk">
            {{ $t('i18n_e83a256e4f') }}
          </a-button>
        </a-space>
        <!-- </div> -->
      </template>
    </CustomDrawer>
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
          { required: true, message: this.$t('i18n_5c502af799'), trigger: 'blur' },
          {
            pattern: /[a-zA-Z0-9][a-zA-Z0-9_.-]$/,
            message: this.$t('i18n_8d5c1335b6'),
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
      const _ports = ports?.map((item) => {
        item.disabled = item.privatePort !== null
        item.port = item.privatePort
        return item
      })
      return _ports?.length > 0 ? _ports : null
    },
    getPortsFromExposedPorts(exposedPorts) {
      const _ports = exposedPorts?.map((item) => {
        item.disabled = item.port !== null
        item.ip = '0.0.0.0'
        item.scheme = item.scheme || 'tcp'
        return item
      })
      return _ports?.length > 0 ? _ports : null
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
        const extraHosts = res.data?.hostConfig?.extraHosts || ['']
        if (!extraHosts.length) {
          extraHosts.push('')
        }

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
          extraHosts: extraHosts,
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
        temp.exposedPorts = (this.temp?.exposedPorts || [])
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
