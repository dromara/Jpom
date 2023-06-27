<template>
  <div>
    <a-form-model ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
      <a-form-model-item label="基础镜像" prop="name">
        <a-row>
          <a-col :span="10">
            <a-input v-model="temp.image" disabled placeholder="" />
          </a-col>
          <a-col :span="4" style="text-align: right">容器名称：</a-col>
          <a-col :span="10">
            <a-input v-model="temp.name" placeholder="容器名称" />
          </a-col>
        </a-row>
      </a-form-model-item>

      <a-form-model-item label="端口">
        <a-row v-for="(item, index) in temp.exposedPorts" :key="index">
          <a-col :span="21">
            <a-space>
              <a-input-group>
                <a-row>
                  <a-col :span="8">
                    <a-input addon-before="IP" placeholder="宿主机ip" v-model="item.ip"> </a-input>
                  </a-col>
                  <a-col :span="6" :offset="1">
                    <a-input addon-before="端口" placeholder="端口" v-model="item.publicPort"> </a-input>
                  </a-col>
                  <a-col :span="8" :offset="1">
                    <a-input addon-before="容器" :disabled="item.disabled" v-model="item.port" placeholder="容器端口">
                      <a-select slot="addonAfter" :disabled="item.disabled" v-model="item.scheme" placeholder="端口协议">
                        <a-select-option value="tcp">tcp</a-select-option>
                        <a-select-option value="udp">udp</a-select-option>
                        <a-select-option value="sctp">sctp</a-select-option>
                      </a-select>
                    </a-input>
                  </a-col>
                </a-row>
              </a-input-group>
            </a-space>
          </a-col>
          <a-col :span="2" :offset="1">
            <a-space>
              <a-icon
                type="minus-circle"
                v-if="temp.exposedPorts && temp.exposedPorts.length > 1"
                @click="
                  () => {
                    temp.exposedPorts.splice(index, 1);
                  }
                "
              />

              <a-icon
                type="plus-square"
                @click="
                  () => {
                    temp.exposedPorts.push({
                      scheme: 'tcp',
                      ip: '0.0.0.0',
                    });
                  }
                "
              />
            </a-space>
          </a-col>
        </a-row>
      </a-form-model-item>

      <a-form-model-item label="挂载卷">
        <a-row v-for="(item, index) in temp.volumes" :key="index">
          <a-col :span="10">
            <a-input addon-before="宿主" v-model="item.host" placeholder="宿主机目录" />
          </a-col>
          <a-col :span="10" :offset="1">
            <a-input addon-before="容器" :disabled="item.disabled" v-model="item.container" placeholder="容器目录" />
          </a-col>
          <a-col :span="2" :offset="1">
            <a-space>
              <a-icon
                type="minus-circle"
                v-if="temp.volumes && temp.volumes.length > 1"
                @click="
                  () => {
                    temp.volumes.splice(index, 1);
                  }
                "
              />

              <a-icon
                type="plus-square"
                @click="
                  () => {
                    temp.volumes.push({});
                  }
                "
              />
            </a-space>
          </a-col>
        </a-row>
      </a-form-model-item>
      <a-form-model-item label="环境变量">
        <a-row v-for="(item, index) in temp.env" :key="index">
          <a-col :span="10">
            <a-input v-model="item.key" placeholder="变量名" />
          </a-col>
          <a-col :span="10" :offset="1">
            <a-input v-model="item.value" placeholder="变量值" />
          </a-col>
          <a-col :span="2" :offset="1">
            <a-space>
              <a-icon
                type="minus-circle"
                v-if="temp.env && temp.env.length > 1"
                @click="
                  () => {
                    temp.env.splice(index, 1);
                  }
                "
              />

              <a-icon
                type="plus-square"
                @click="
                  () => {
                    temp.env.push({});
                  }
                "
              />
            </a-space>
          </a-col>
        </a-row>
      </a-form-model-item>
      <a-form-model-item label="命令">
        <a-row v-for="(item, index) in temp.commands" :key="index">
          <a-col :span="20">
            <a-input addon-before="命令值" v-model="item.value" placeholder="填写运行命令" />
          </a-col>

          <a-col :span="2" :offset="1">
            <a-space>
              <a-icon
                type="minus-circle"
                v-if="temp.commands && temp.commands.length > 1"
                @click="
                  () => {
                    temp.commands.splice(index, 1);
                  }
                "
              />
              <a-icon
                type="plus-square"
                @click="
                  () => {
                    temp.commands.push({});
                  }
                "
              />
            </a-space>
          </a-col>
        </a-row>
      </a-form-model-item>
      <a-form-model-item label="hostname" prop="hostname">
        <a-input v-model="temp.hostname" placeholder="主机名 hostname" />
      </a-form-model-item>
      <a-form-model-item label="网络">
        <a-auto-complete v-model="temp.networkMode" placeholder="网络模式：bridge、container:<name|id>、host、container、none" option-label-prop="value">
          <template slot="dataSource">
            <a-select-option
              v-for="dataItem in [
                {
                  title: '为 docker bridge 上的容器创建一个新的网络堆栈',
                  value: 'bridge',
                },
                {
                  title: '这个容器没有网络',
                  value: 'none',
                },
                {
                  title: '重用另一个容器网络堆栈',
                  value: 'container:<name|id>',
                },
                {
                  title: '使用容器内的主机网络堆栈。 注意：主机模式赋予容器对本地系统服务（如 D-bus）的完全访问权限，因此被认为是不安全的。',
                  value: 'host',
                },
              ]"
              :key="dataItem.value"
            >
              {{ dataItem.title }}
            </a-select-option>
          </template>
        </a-auto-complete>
      </a-form-model-item>
      <a-form-model-item label="重启策略">
        <a-auto-complete v-model="temp.restartPolicy" placeholder="重启策略：no、always、unless-stopped、on-failure" option-label-prop="value">
          <template slot="dataSource">
            <a-select-option
              v-for="dataItem in [
                {
                  title: '不自动重启',
                  value: 'no',
                },
                {
                  title: '无论返回什么退出代码，始终重新启动容器。',
                  value: 'always',
                },
                {
                  title: '如果容器以非零退出代码退出，则重新启动容器。可以指定次数：on-failure:2',
                  value: 'on-failure:1',
                },
                {
                  title: '重新启动容器，除非它已被停止',
                  value: 'unless-stopped',
                },
              ]"
              :key="dataItem.value"
            >
              {{ dataItem.title }}
            </a-select-option>
          </template>
        </a-auto-complete>
      </a-form-model-item>
      <a-form-model-item label="存储选项">
        <a-row v-for="(item, index) in temp.storageOpt" :key="index">
          <a-col :span="10">
            <a-input v-model="item.key" placeholder="配置名 （如：size）" />
          </a-col>
          <a-col :span="10" :offset="1">
            <a-input v-model="item.value" placeholder="配置值 （如：5g）" />
          </a-col>
          <a-col :span="2" :offset="1">
            <a-space>
              <a-icon
                type="minus-circle"
                v-if="temp.storageOpt && temp.storageOpt.length > 1"
                @click="
                  () => {
                    temp.storageOpt.splice(index, 1);
                  }
                "
              />

              <a-icon
                type="plus-square"
                @click="
                  () => {
                    temp.storageOpt.push({});
                  }
                "
              />
            </a-space>
          </a-col>
        </a-row>
      </a-form-model-item>
      <a-form-model-item label="runtime"> <a-input v-model="temp.runtime" placeholder="容器 runtime" /> </a-form-model-item>
      <a-form-model-item label="容器标签"> <a-input v-model="temp.labels" placeholder="容器标签,如：key1=values1&keyvalue2" /> </a-form-model-item>
      <a-form-model-item label="自动启动">
        <a-row>
          <a-col :span="4"><a-switch v-model="temp.autorun" checked-children="启动" un-checked-children="不启动" /></a-col>
          <a-col :span="4" style="text-align: right">
            <a-tooltip>
              <template slot="title">
                <p>--privileged</p>
                <ul>
                  privileged=true|false 介绍
                  <li>true container内的root拥有真正的root权限。</li>
                  <li>false container内的root只是外部的一个普通用户权限。默认false</li>
                  <li>privileged启动的容器 可以看到很多host上的设备 可以执行mount。 可以在docker容器中启动docker容器。</li>
                </ul>
              </template>

              <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
              特权：
            </a-tooltip>
          </a-col>
          <a-col :span="4">
            <a-switch v-model="temp.privileged" checked-children="是" un-checked-children="否" />
          </a-col>
        </a-row>
      </a-form-model-item>
      <div style="padding: 40px">
        <div
          :style="{
            position: 'absolute',
            right: 0,
            bottom: 0,
            width: '100%',
            borderTop: '1px solid #e9e9e9',
            padding: '10px 16px',
            background: '#fff',
            textAlign: 'right',
            zIndex: 1,
          }"
        >
          <a-space>
            <a-button
              @click="
                () => {
                  this.$emit('cancelBtnClick');
                }
              "
            >
              取消
            </a-button>
            <a-button type="primary" @click="handleBuildOk"> 确认 </a-button>
          </a-space>
        </div>
      </div>
    </a-form-model>
  </div>
</template>
<script>
import {
  dockerImageCreateContainer, 
  dockerImageInspect, 
  dockerInspectContainer,
  dockerContainerRebuildContainer
} from "@/api/docker-api";
export default {
  props: {
    id: {
      type: String,
      default: "",
    },
    imageId: {
      type: String,
      default: "",
    },
    urlPrefix: {
      type: String,
    },
    machineDockerId: {
      type: String,
      default: "",
    },
    containerId: {
      type: String,
      default: "",
    },
    containerData: {
      type: Object,
      default: () => ({}),
    },
  },
  data() {
    return {
      temp: {},
      rules: {
        name: [
          { required: true, message: "容器名称必填", trigger: "blur" },
          { pattern: /[a-zA-Z0-9][a-zA-Z0-9_.-]$/, message: "容器名称数字字母,且长度大于1", trigger: "blur" },
        ],
      },
    }
  },
  computed: {
    reqDataId() {
      return this.id || this.machineDockerId;
    },
    getLabels() {
      if (!this.containerData.labels) {
        return ""
      }
      let labels = ""
      Object.keys(this.containerData.labels).map((key) => {
        labels += `${key}=${this.containerData.labels[key]}&`
      })
      return labels.slice(0, -1);
    },
  },
  mounted() {
    console.log(this.containerData)
    this.createContainer();
  },
  methods: {
    // 构建镜像
    createContainer() {
      // 判断 containerId
      if (this.containerId) {
        // form container
        this.inspectContainer();
      } else {
        // form image
        this.inspectImage();
      }
    },
    getPortsFromPorts(ports) {
      const _ports = ports.map((item) => {
        item.disabled = item.privatePort !== null;
        item.port = item.privatePort;
        return item;
      })
      return _ports.length > 0 ? _ports : null
    },
    getPortsFromExposedPorts(exposedPorts) {
      const _ports = exposedPorts.map((item) => {
        item.disabled = item.port !== null;
        item.ip = "0.0.0.0";
        item.scheme = item.scheme || "tcp";
        return item;
      })
      return _ports.length > 0 ? _ports : null
    },
    getVolumesFromMounts(mounts) {
      const _mounts = mounts.map((item) => {
        item.disabled = item.destination !== null;
        item.host = item.source;
        item.container = item.destination;
        return item;
      })
      return _mounts.length > 0 ? _mounts : null
    },
    // inspect container
    inspectContainer() {
      // 单独获取 image 信息
      dockerImageInspect(this.urlPrefix, {
        id: this.reqDataId,
        imageId: this.imageId,
      }).then((res) => {
        this.temp.image = (res.data.repoTags || []).join(",")
      })

      dockerInspectContainer(this.urlPrefix, {
        id: this.reqDataId,
        containerId: this.containerId,
      }).then(res => {
        this.buildVisible = true;
        this.temp = {
          name: res.data.name,
          labels: this.getLabels,
          volumes: this.getVolumesFromMounts(this.containerData.mounts) || [{}],
          exposedPorts: this.getPortsFromPorts(this.containerData.ports) || this.getPortsFromExposedPorts(res.data.config.exposedPorts) || [{}],
          autorun: true,
          imageId: this.imageId,
          env: [{}],
          storageOpt: [{}],
          commands: [{}],
          ...this.temp
        };
        this.$refs["editForm"]?.resetFields();
      });
    },
    // inspect image
    inspectImage() {
      dockerImageInspect(this.urlPrefix, {
        id: this.reqDataId,
        imageId: this.imageId,
      }).then((res) => {
        this.buildVisible = true;
        this.temp = {
          volumes: [{}],
          exposedPorts: (res.data?.config?.exposedPorts || [{}]).map((item) => {
            item.disabled = item.port !== null;
            item.ip = "0.0.0.0";
            item.scheme = item.scheme || "tcp";
            return item;
          }),
          image: (res.data.repoTags || []).join(","),
          autorun: true,
          imageId: this.imageId,
          env: [{}],
          storageOpt: [{}],
          commands: [{}],
        };
        this.$refs["editForm"]?.resetFields();
      });
    },
    // 创建容器
    handleBuildOk() {
      this.$refs["editForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
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
        };
        temp.volumes = (this.temp.volumes || [])
          .filter((item) => {
            return item.host;
          })
          .map((item) => {
            return item.host + ":" + item.container;
          })
          .join(",");
        // 处理端口
        temp.exposedPorts = (this.temp.exposedPorts || [])
          .filter((item) => {
            return item.publicPort && item.ip;
          })
          .map((item) => {
            return item.ip + ":" + item.publicPort + ":" + item.port;
          })
          .join(",");
        // 环境变量
        this.temp.env.forEach((item) => {
          if (item.key && item.key) {
            temp.env[item.key] = item.value;
          }
        });
        this.temp.storageOpt.forEach((item) => {
          if (item.key && item.key) {
            temp.storageOpt[item.key] = item.value;
          }
        });
        //
        temp.commands = (this.temp.commands || []).map((item) => {
          return item.value || "";
        });
        // 判断 containerId
        if (this.containerId) {
          temp.containerId = this.containerId;
          dockerContainerRebuildContainer(this.urlPrefix, temp).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });

              // 通知父组件关闭弹窗
              this.$emit('confirmBtnClick');
            }
          });
        } else {
          dockerImageCreateContainer(this.urlPrefix, temp).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              
              // 通知父组件关闭弹窗
              this.$emit('confirmBtnClick');
            }
          });
        }
      });
    },
  }
}
</script>
