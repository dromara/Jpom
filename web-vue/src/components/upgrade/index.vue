<template>
  <div>
    <a-timeline>
      <a-timeline-item>
        <span class="layui-elem-quote"
          >{{ $t('components.upgrade.index.d2c7486a') }}{{ temp.timeStamp }}
          <a-tag v-if="nodeId || machineId">agent</a-tag>
          <a-tag v-else>server</a-tag>
        </span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">{{ $t('components.upgrade.index.56586b89') }}{{ temp.vueTimeStamp }}</span>
      </a-timeline-item>
      <a-timeline-item v-if="!nodeId && !machineId">
        <span class="layui-elem-quote">{{ $t('components.upgrade.index.54432619') }}</span>
        <a-space>
          <a-switch
            v-model:checked="temp.joinBetaRelease"
            :checked-children="$t('components.upgrade.index.e17b386')"
            :un-checked-children="$t('components.upgrade.index.8b04d374')"
            :disabled="true"
          />
          <template v-if="temp.joinBetaRelease">
            <a-button type="link" @click="handleChangeBetaRelease(false)">{{
              $t('components.upgrade.index.75e06a52')
            }}</a-button>
          </template>
          <template v-else>
            <a-tooltip>
              <template #title>{{ $t('components.upgrade.index.543ffe88') }}</template>
              <a-button type="link" @click="handleChangeBetaRelease(true)"
                ><QuestionCircleOutlined />{{ $t('components.upgrade.index.434e69f1') }}</a-button
              >
            </a-tooltip>
          </template>
        </a-space>
      </a-timeline-item>
      <a-timeline-item>
        <a-space>
          <span class="layui-elem-quote">{{ $t('components.upgrade.index.463be5e9') }}{{ temp.version }} </span>
          <template v-if="temp.upgrade !== undefined">
            <a-tag v-if="temp.upgrade" color="pink" @click="upgrageVerion"
              >{{ $t('components.upgrade.index.e3b19e4f') }}{{ temp.newVersion }} {{ temp.newBeta ? '/beta' : '' }}
              <DownloadOutlined />
            </a-tag>
            <a-tag v-else color="orange" @click="checkVersion">
              <RocketOutlined />
            </a-tag>
          </template>
        </a-space>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote"
          >{{ $t('components.upgrade.index.44a6a440') }}{{ formatDuration(temp.upTime) }}</span
        >
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote"
          >{{ $t('components.upgrade.index.4ea00d5b') }}<a-tag>{{ temp.port }}</a-tag></span
        >
        <span class="layui-elem-quote">&nbsp;&nbsp;</span>
        <span class="layui-elem-quote"
          >{{ $t('components.upgrade.index.62143313') }}<a-tag>{{ temp.pid }}</a-tag></span
        >
      </a-timeline-item>
      <a-timeline-item>
        <a-alert :message="$t('components.upgrade.index.35d94d7e')" type="warning" show-icon />
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote"
          >{{ $t('components.upgrade.index.4593b845') }}<a-tag>{{ temp.dataPath }}</a-tag>
        </span>
        <span v-if="temp.jarFile" class="layui-elem-quote"
          >{{ $t('components.upgrade.index.3a69c395') }}<a-tag>{{ temp.jarFile }}</a-tag>
        </span>
      </a-timeline-item>
    </a-timeline>

    <a-row>
      <a-col span="22">
        <a-space direction="vertical" style="width: 100%">
          <a-upload
            :file-list="fileList"
            :disabled="!!percentage"
            :before-upload="beforeUpload"
            accept=".jar,.zip"
            @remove="handleRemove"
          >
            <LoadingOutlined v-if="percentage" />
            <a-button v-else><UploadOutlined />{{ $t('components.upgrade.index.dc168b2') }}</a-button>
          </a-upload>
          <a-row v-if="percentage">
            <a-col span="20">
              <a-progress :percent="percentage" style="width: 100%"></a-progress>
            </a-col>
          </a-row>
          <a-button type="primary" :disabled="fileList.length === 0 || !!percentage" @click="startUpload">{{
            $t('components.upgrade.index.b6ecd3cc')
          }}</a-button>
        </a-space>
      </a-col>
    </a-row>

    <a-divider dashed />
    <div
      :style="{
        color: getThemeStyle().color
      }"
      v-html="changelog"
    ></div>
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
import { useGuideStore } from '@/stores/guide'
import markdownit from 'markdown-it'
// import 'markdown-it-vue/dist/markdown-it-vue.css'

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
    // MarkdownItVue
  },
  inject: ['globalLoading'],
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
      markdownit: null
    }
  },
  computed: {
    ...mapState(useGuideStore, ['getThemeStyle'])
  },
  mounted() {
    this.loadData()
    const md = markdownit()
    const proxy = (tokens, idx, options, env, self) => self.renderToken(tokens, idx, options)
    const defaultBulletListOpenRenderer = md.renderer.rules.link_open || proxy

    md.renderer.rules.link_open = function (tokens, idx, options, env, self) {
      let aIndex = tokens[idx].attrIndex('target')

      if (aIndex < 0) {
        tokens[idx].attrPush(['target', '_blank']) // add new attribute
      } else {
        tokens[idx].attrs[aIndex][1] = '_blank' // replace value of existing attr
      }
      // Make your changes here ...
      // ... then render it using the existing logic
      return defaultBulletListOpenRenderer(tokens, idx, options, env, self)
    }
    this.markdownit = md
  },
  beforeUnmount() {},
  methods: {
    uploadPieces,
    formatDuration,
    renderMarkdown(markdown) {
      return (this.markdownit && this.markdownit.render(markdown)) || this.$t('components.upgrade.index.50cfdf2b')
    },
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
          this.changelog = this.renderMarkdown(resLog.data)
          //
          // res.data.
          this.showVersion(false, res.data?.remoteVersion).then((upgrade) => {
            // 本地网络检测
            this.localCheckVersion(!upgrade)
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
      return true
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
        this.$t('components.upgrade.index.597788c9') +
        this.$t('components.upgrade.index.5739dd8b') +
        this.$t('components.upgrade.index.ccb42e58') +
        this.$t('components.upgrade.index.50e3ae59') +
        ' </ul>'

      $confirm({
        title: this.$t('components.upgrade.index.3875bf60'),
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okText: this.$t('components.upgrade.index.1cbe2507'),
        zIndex: 1009,
        cancelText: this.$t('components.upgrade.index.a0451c97'),
        onOk: () => {
          const file = this.fileList[0]
          this.percentage = 0
          uploadPieces({
            file,
            resolveFileProcess: (msg) => {
              this.globalLoading({
                spinning: true,
                tip: msg
              })
            },
            resolveFileEnd: () => {
              this.globalLoading(false)
            },
            process: (process) => {
              this.percentage = Math.max(this.percentage, process)
            },
            success: (uploadData) => {
              // 准备合并
              uploadUpgradeFileMerge({
                ...uploadData[0],
                nodeId: this.nodeId,
                machineId: this.machineId
              })
                .then((res) => {
                  if (res.code === 200) {
                    this.fileList = []
                    this.startCheckUpgradeStatus(res.msg)
                  }
                })
                .finally(() => {
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

      this.globalLoading({
        spinning: true,
        tip: (msg || this.$t('components.upgrade.index.351dd143')) + this.$t('components.upgrade.index.ad5617c0')
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
              this.globalLoading({
                spinning: false
              })
              clearInterval(this.timer)
              $notification.success({
                message: this.$t('components.upgrade.index.7a1c1ded')
              })
              this.temp = manifest
              setTimeout(() => {
                location.reload()
              }, 1000)
            } else {
              if (this.checkCount > RESTART_UPGRADE_WAIT_TIME_COUNT) {
                $notification.warning({
                  message: this.$t('components.upgrade.index.3fa2ee24') + (res.msg || '')
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
              clearInterval(this.timer)
              $notification.error({
                message: this.$t('components.upgrade.index.306c5167')
              })
            } else {
              this.globalLoading({
                spinning: true,
                tip:
                  (msg || this.$t('components.upgrade.index.351dd143')) + this.$t('components.upgrade.index.ad5617c0')
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
              this.localCheckVersion(true)
            }
          })
        }
      })
    },
    // 本地网络检测
    localCheckVersion(tip) {
      //console.log(compareVersion("1.0.0", "1.0.1"), compareVersion("2.4.3", "2.4.2"));
      //console.log(compareVersion("1.0.2", "dev"));
      const buildInfo = pageBuildInfo()

      const url = this.temp?.joinBetaRelease
        ? 'https://jpom.top/docs/beta-versions.json'
        : 'https://jpom.top/docs/release-versions.json'

      executionRequest(url, {
        ...buildInfo,
        type: this.nodeId || this.machineId ? 'agent' : 'server'
      }).then((data) => {
        if (!data || !data.tag_name) {
          return
        }

        const tagName = data.tag_name.replace('v', '')
        const upgrade = compareVersion(this.temp.version, tagName) < 0

        if (upgrade && tip) {
          //
          const dUrl = data.downloadUrl || 'https://jpom.top'
          const htmlAref = `<a href='${dUrl}' target='_blank'>${dUrl}</a>`
          const html =
            this.$t('components.upgrade.index.c133eb37') +
            tagName +
            htmlAref +
            this.$t('components.upgrade.index.3f944483')
          $notification.success({
            duration: 10,
            message: h('div', null, [h('p', { innerHTML: html }, null)])
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
              message: this.$t('components.upgrade.index.4b9cc2c4')
            })
          }
          resolve(false)
          return
        }
        this.temp = {
          ...this.temp,
          upgrade: data.upgrade,
          newVersion: data.tagName,
          newBeta: data.beta
        }

        if (this.temp.upgrade && data.changelog) {
          this.changelog = this.renderMarkdown(data.changelog)
        }
        if (tip) {
          $notification.success({
            message: this.temp.upgrade
              ? this.$t('components.upgrade.index.c133eb37') + data.tagName
              : this.$t('components.upgrade.index.4b9cc2c4')
          })
        }
        resolve(data.upgrade)
      })
    },
    // 升级
    upgrageVerion() {
      // "components.upgrade.index.1cbe2507要升级到最新版本吗？,升级前请阅读更新日志里面的说明和注意事项并且请注意备份数据防止数据丢失！！"
      const html =
        this.$t('components.upgrade.index.444e366f') +
        this.$t('components.upgrade.index.95b27c31') +
        this.$t('components.upgrade.index.4e82a639') +
        this.$t('components.upgrade.index.50e3ae59') +
        ' </ul>'
      $confirm({
        title: this.$t('components.upgrade.index.3875bf60'),
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okText: this.$t('components.upgrade.index.1cbe2507'),
        zIndex: 1009,
        cancelText: this.$t('components.upgrade.index.a0451c97'),
        onOk: () => {
          return remoteUpgrade({
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
    // components.upgrade.index.e17b386beta计划
    handleChangeBetaRelease(beta) {
      const html = beta
        ? this.$t('components.upgrade.index.85c315ec') +
          this.$t('components.upgrade.index.f35152df') +
          this.$t('components.upgrade.index.c6733249') +
          this.$t('components.upgrade.index.1365438c') +
          ' </ul>'
        : this.$t('components.upgrade.index.dd255b35')

      $confirm({
        title: this.$t('components.upgrade.index.3875bf60'),
        content: h('div', {}, [h('p', { innerHTML: html })]),
        okText: this.$t('components.upgrade.index.1cbe2507'),
        zIndex: 1009,
        cancelText: this.$t('components.upgrade.index.a0451c97'),
        onOk: () => {
          return changBetaRelease({
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
