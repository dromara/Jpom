<template>
  <div>
    <a-timeline>
      <a-timeline-item>
        <span class="layui-elem-quote">
          当前程序打包时间：{{ temp.timeStamp }}
          <a-tag v-if="nodeId || this.machineId">agent</a-tag>
          <a-tag v-else>server</a-tag>
        </span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">当前前端打包时间：{{ temp.vueTimeStamp }}</span>
      </a-timeline-item>
      <a-timeline-item v-if="!this.nodeId && !this.machineId">
        <span class="layui-elem-quote">beta计划：</span>
        <a-space>
          <a-switch
            checked-children="加入"
            un-checked-children="未加入"
            :disabled="true"
            v-model="temp.joinBetaRelease"
          />
          <template v-if="temp.joinBetaRelease">
            <a-button type="link" @click="handleChangeBetaRelease(false)">关闭 beat计划</a-button>
          </template>
          <template v-else>
            <a-tooltip>
              <template #title>
                加入 beta 计划可以及时获取到最新的功能、一些优化功能、最快修复 bug 的版本，但是 beta
                版也可能在部分新功能上存在不稳定的情况。您需要根据您业务情况来评估是否可以加入 beta，在使用 beta
                版过程中遇到问题可以随时反馈给我们，我们会尽快为您解答。
              </template>
              <a-button icon="question-circle" type="link" @click="handleChangeBetaRelease(true)">我要加入</a-button>
            </a-tooltip>
          </template>
        </a-space>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">当前版本号：{{ temp.version }} </span>
        <template v-if="temp.upgrade !== undefined">
          <a-tag v-if="temp.upgrade" color="pink" @click="upgrageVerion"
            >新版本：{{ temp.newVersion }} {{ temp.newBeta ? '/beta' : '' }} <a-icon type="download"
          /></a-tag>
          <a-tag v-else color="orange" @click="checkVersion">
            <a-icon type="rocket" />
          </a-tag>
        </template>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">已经运行时间：{{ formatDuration(temp.upTime) }}</span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote"
          >端口号：<a-tag>{{ temp.port }}</a-tag></span
        >
        <span class="layui-elem-quote">&nbsp;&nbsp;</span>
        <span class="layui-elem-quote"
          >进程号：<a-tag>{{ temp.pid }}</a-tag></span
        >
      </a-timeline-item>
      <a-timeline-item>
        <a-alert
          message="请勿手动删除数据目录下面文件,如果需要删除需要提前备份或者已经确定对应文件弃用后才能删除"
          type="warning"
        />
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">
          数据存储目录：
          <a-tag>{{ temp.dataPath }}</a-tag>
        </span>
        <span class="layui-elem-quote" v-if="temp.jarFile">
          运行的Jar包：
          <a-tag>{{ temp.jarFile }}</a-tag>
        </span>
      </a-timeline-item>
    </a-timeline>

    <a-row>
      <a-col span="22">
        <a-space direction="vertical" style="display: block">
          <a-upload
            :file-list="fileList"
            :remove="handleRemove"
            :disabled="!!percentage"
            :before-upload="beforeUpload"
            accept=".jar,.zip"
          >
            <a-icon type="loading" v-if="percentage" />
            <a-button icon="upload" v-else>选择升级文件</a-button>
          </a-upload>
          <a-row v-if="percentage">
            <a-col span="20">
              <a-progress :percent="percentage"></a-progress>
            </a-col>
          </a-row>
          <a-button type="primary" :disabled="fileList.length === 0 || !!percentage" @click="startUpload"
            >上传升级包</a-button
          >
        </a-space>
      </a-col>
    </a-row>

    <a-divider dashed />
    <markdown-it-vue class="md-body" :content="changelog" :options="markdownOptions" />
  </div>
</template>
<script>
import {
  systemInfo,
  uploadUpgradeFile,
  changelog,
  checkVersion,
  remoteUpgrade,
  uploadUpgradeFileMerge,
  changBetaRelease
} from '@/api/system'
import Vue from 'vue'
import MarkdownItVue from 'markdown-it-vue'
import 'markdown-it-vue/dist/markdown-it-vue.css'
import {
  RESTART_UPGRADE_WAIT_TIME_COUNT,
  parseTime,
  compareVersion,
  pageBuildInfo,
  formatDuration
} from '@/utils/const'
import { uploadPieces } from '@/utils/upload-pieces'
import { executionRequest } from '@/api/external'

export default {
  name: 'Upgrade',
  components: {
    MarkdownItVue
  },
  props: {
    nodeId: {
      type: String,
      default: ''
    },
    machineId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      temp: {},

      checkCount: 0,
      fileList: [],
      percentage: 0,
      changelog: '',
      markdownOptions: {
        markdownIt: {
          linkify: true
        },
        linkAttributes: {
          attrs: {
            target: '_blank',
            rel: 'noopener'
          }
        }
      }
    }
  },
  mounted() {
    this.loadData()
  },
  beforeDestroy() {},
  methods: {
    uploadPieces,
    formatDuration,
    // 加载数据
    loadData() {
      systemInfo({
        nodeId: this.nodeId,
        machineId: this.machineId
      }).then((res) => {
        this.temp = res.data?.manifest
        //
        // vueTimeStamp
        this.temp = {
          ...this.temp,
          vueTimeStamp: parseTime(this.getMeta('build-time')),
          joinBetaRelease: res.data?.joinBetaRelease
        }
        //
        changelog({
          nodeId: this.nodeId,
          machineId: this.machineId
        }).then((resLog) => {
          this.changelog = resLog.data
          //
          // res.data.
          this.showVersion(false, res.data?.remoteVersion).then((upgrade) => {
            // 本地网络检测
            this.loaclCheckVersion(!upgrade)
          })
        })
      })
    },
    getMeta(metaName) {
      const metas = document.getElementsByTagName('meta')
      for (let i = 0; i < metas.length; i++) {
        try {
          if (metas[i].getAttribute('name') === metaName) {
            return metas[i].getAttribute('content')
          }
        } catch (e) {
          console.error(e)
        }
      }
      return ''
    },
    // 处理文件移除
    handleRemove(file) {
      const index = this.fileList.indexOf(file)
      const newFileList = this.fileList.slice()
      newFileList.splice(index, 1)
      this.fileList = newFileList
    },
    // 准备上传文件
    beforeUpload(file) {
      // 只允许上传单个文件
      this.fileList = [file]
      return false
    },
    // 开始上传文件
    startUpload() {
      const html =
        "确认要上传文件更新到最新版本吗？<ul style='color:red;'>" +
        '<li>上传更新前请阅读更新日志里面的说明和注意事项并且<b>请注意备份数据防止数据丢失！！</b></li>' +
        '<li>上传前请检查包是否完整,否则可能出现更新后无法正常启动的情况！！</li>' +
        '<li>如果升级失败需要手动恢复奥</li>' +
        ' </ul>'
      const h = this.$createElement
      $confirm({
        title: '系统提示',
        content: h('div', null, [h('p', { domProps: { innerHTML: html } }, null)]),
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          const file = this.fileList[0]
          this.percentage = 0
          uploadPieces({
            file,
            process: (process) => {
              this.percentage = Math.max(this.percentage, process)
            },
            success: (uploadData) => {
              // 准备合并
              uploadUpgradeFileMerge({
                ...uploadData[0],
                nodeId: this.nodeId,
                machineId: this.machineId
              }).then((res) => {
                if (res.code === 200) {
                  this.fileList = []
                  this.startCheckUpgradeStatus(res.msg)
                }
                setTimeout(() => {
                  this.percentage = 0
                }, 2000)
              })
            },
            error: (msg) => {
              $notification.error({
                message: msg
              })
            },
            uploadCallback: (formData) => {
              return new Promise((resolve, reject) => {
                formData.append('nodeId', this.nodeId)
                formData.append('machineId', this.machineId)
                // 上传文件
                uploadUpgradeFile(formData)
                  .then((res) => {
                    if (res.code === 200) {
                      resolve()
                    } else {
                      reject()
                    }
                  })
                  .catch(() => {
                    reject()
                  })
              })
            }
          })
          // const formData = new FormData();
          // formData.append("file", this.fileList[0]);

          // // 上传文件
          // uploadUpgradeFile(formData).then((res) => {
          //   if (res.code === 200) {
          //     $notification.success({
          //       message: res.msg,
          //     });

          //     this.startCheckUpgradeStatus(res.msg);
          //   }
          // });
          // this.fileList = [];
        }
      })
    },
    startCheckUpgradeStatus(msg) {
      this.checkCount = 0
      Vue.prototype.$setLoading({
        spinning: true,
        tip: (msg || '升级中，请稍候...') + ',请耐心等待暂时不用刷新页面,升级成功后会自动刷新'
      })
      //
      this.timer = setInterval(() => {
        systemInfo({
          nodeId: this.nodeId,
          machineId: this.machineId
        })
          .then((res) => {
            let manifest = res.data?.manifest
            if (res.code === 200 && manifest?.timeStamp !== this.temp.timeStamp) {
              Vue.prototype.$setLoading('closeAll')
              clearInterval(this.timer)
              $notification.success({
                message: '升级成功'
              })
              this.temp = manifest
              setTimeout(() => {
                location.reload()
              }, 1000)
            } else {
              if (this.checkCount > RESTART_UPGRADE_WAIT_TIME_COUNT) {
                $notification.warning({
                  message: '未升级成功：' + (res.msg || '')
                })
                Vue.prototype.$setLoading('closeAll')
                clearInterval(this.timer)
              }
            }
          })
          .catch((error) => {
            console.error(error)
            if (this.checkCount > RESTART_UPGRADE_WAIT_TIME_COUNT) {
              Vue.prototype.$setLoading('closeAll')
              clearInterval(this.timer)
              $notification.error({
                message: '升级超时,请去服务器查看控制台日志排查问题'
              })
            } else {
              Vue.prototype.$setLoading({
                spinning: true,
                tip: (msg || '升级中，请稍候...') + ',请耐心等待暂时不用刷新页面,升级成功后会自动刷新'
              })
            }
          })
        this.checkCount = this.checkCount + 1
      }, 2000)
    },
    // 检查新版本
    checkVersion() {
      checkVersion({
        nodeId: this.nodeId,
        machineId: this.machineId
      }).then((res) => {
        if (res.code === 200) {
          this.showVersion(true, res.data).then((upgrade) => {
            // 远程检测失败才本地检测
            if (!upgrade) {
              this.loaclCheckVersion(true)
            }
          })
        }
      })
    },
    // 本地网络检测
    loaclCheckVersion(tip) {
      //console.log(compareVersion("1.0.0", "1.0.1"), compareVersion("2.4.3", "2.4.2"));
      //console.log(compareVersion("1.0.2", "dev"));
      const buildInfo = pageBuildInfo()

      executionRequest('https://jpom.top/docs/release-versions.json', {
        ...buildInfo,
        type: this.nodeId || this.machineId ? 'agent' : 'server'
      }).then((data) => {
        if (!data || !data.tag_name) {
          return
        }

        const tagName = data.tag_name.replace('v', '')
        const upgrade = compareVersion(this.temp.version, tagName) < 0

        if (upgrade && tip) {
          $notification.success({
            duration: 10,
            message: function (h) {
              //
              const dUrl = data.downloadUrl || 'https://jpom.top'
              const html =
                '检测到新版本 ' +
                tagName +
                "。请前往：<a target='_blank' href='" +
                dUrl +
                "'>" +
                dUrl +
                '</a> 下载安装包'
              return h('div', null, [h('p', { domProps: { innerHTML: html } }, null)])
            }
          })
        }
      })
    },
    showVersion(tip, data) {
      return new Promise((resolve) => {
        if (!data) {
          this.temp = { ...this.temp, upgrade: false }
          if (tip) {
            $notification.success({
              message: '没有检查到最新版'
            })
          }
          resolve(false)
          return
        }
        this.temp = { ...this.temp, upgrade: data.upgrade, newVersion: data.tagName, newBeta: data.beta }

        if (this.temp.upgrade && data.changelog) {
          this.changelog = data.changelog
        }
        if (tip) {
          $notification.success({
            message: this.temp.upgrade ? '检测到新版本 ' + data.tagName : '没有检查到最新版'
          })
        }
        resolve(data.upgrade)
      })
    },
    // 升级
    upgrageVerion() {
      // "确认要升级到最新版本吗？,升级前请阅读更新日志里面的说明和注意事项并且请注意备份数据防止数据丢失！！"
      const html =
        "确认要下载更新最新版本吗？<ul style='color:red;'>" +
        '<li>下载速度根据网速来确定,如果网络不佳下载会较慢</li>' +
        '<li>下载前请阅读更新日志里面的说明和注意事项并且<b>请注意备份数据防止数据丢失！！</b></li>' +
        '<li>如果升级失败需要手动恢复奥</li>' +
        ' </ul>'
      const h = this.$createElement
      $confirm({
        title: '系统提示',
        content: h('div', null, [h('p', { domProps: { innerHTML: html } }, null)]),
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          //
          remoteUpgrade({
            nodeId: this.nodeId,
            machineId: this.machineId
          }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })

              this.startCheckUpgradeStatus(res.msg)
            }
          })
        }
      })
    },
    // 加入beta计划
    handleChangeBetaRelease(beta) {
      const html = beta
        ? "确认要加入 beta 计划吗？<ul style='color:red;'>" +
          '<li><b> 加入 beta 计划可以及时获取到最新的功能、一些优化功能、最快修复 bug 的版本，但是 beta 版也可能在部分新功能上存在不稳定的情况。</b></li>' +
          '<li><b>您需要根据您业务情况来评估是否可以加入 beta。</b></li>' +
          '<li>在使用 beta 版过程中遇到问题可以随时反馈给我们，我们会尽快为您解答。</li>' +
          ' </ul>'
        : '确认要关闭 beta 计划吗？'
      const h = this.$createElement
      $confirm({
        title: '系统提示',
        content: h('div', null, [h('p', { domProps: { innerHTML: html } }, null)]),
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          //
          changBetaRelease({
            beta: beta
          }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })

              this.loadData()
            }
          })
        }
      })
    }
  }
}
</script>
<style scoped></style>
