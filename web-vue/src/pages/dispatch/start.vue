<template>
  <div>
    <a-modal
      destroy-on-close
      :confirm-loading="confirmLoading"
      :open="true"
      :closable="!uploading"
      :footer="uploading ? null : undefined"
      width="50%"
      :keyboard="false"
      :title="$t('pages.dispatch.start.d935498a') + data.name"
      :mask-closable="false"
      @ok="handleDispatchOk"
      @cancel="
        () => {
          $emit('cancel')
        }
      "
    >
      <a-form ref="dispatchForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item :label="$t('pages.dispatch.start.b96b97fb')" name="type">
          <a-radio-group v-model:value="temp.type" name="type" :disabled="!!percentage" @change="restForm">
            <a-radio :value="'use-build'">{{ $t('pages.dispatch.start.7bcf90e5') }}</a-radio>
            <a-radio :value="'file-storage'">{{ $t('pages.dispatch.start.f183985') }}</a-radio>
            <a-radio :value="'static-file-storage'">{{ $t('pages.dispatch.start.c3a20e28') }}</a-radio>
            <a-radio :value="'upload'">{{ $t('pages.dispatch.start.33ffacfb') }}</a-radio>
            <a-radio :value="'download'">{{ $t('pages.dispatch.start.367f115c') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <!-- 手动上传 -->
        <a-form-item v-if="temp.type === 'upload'" :label="$t('pages.dispatch.start.fc83ff3a')" name="clearOld">
          <a-progress v-if="percentage" :percent="percentage">
            <template #format="percent">
              {{ percent }}%
              <template v-if="percentageInfo.total"> ({{ renderSize(percentageInfo.total) }}) </template>
              <template v-if="percentageInfo.duration">
                {{ $t('pages.dispatch.start.bca9469e') }}:{{ formatDuration(percentageInfo.duration) }}
              </template>
            </template>
          </a-progress>

          <a-upload :file-list="fileList" :disabled="!!percentage" :before-upload="beforeUpload" @remove="handleRemove">
            <LoadingOutlined v-if="percentage" />
            <a-button v-else type="primary"><UploadOutlined />{{ $t('pages.dispatch.start.2a688d49') }}</a-button>
          </a-upload>
        </a-form-item>
        <!-- 远程下载 -->
        <a-form-item v-else-if="temp.type === 'download'" :label="$t('pages.dispatch.start.94149f7b')" name="url">
          <a-input v-model:value="temp.url" :placeholder="$t('pages.dispatch.start.40ced483')" />
        </a-form-item>
        <!-- 在线构建 -->
        <template v-else-if="temp.type == 'use-build'">
          <a-form-item :label="$t('pages.dispatch.start.afb1ec30')">
            <a-space>
              {{ chooseBuildInfo.name }}
              <a-button
                type="primary"
                @click="
                  () => {
                    chooseVisible = 1
                  }
                "
              >
                {{ $t('pages.dispatch.start.afb1ec30') }}
              </a-button>
            </a-space>
          </a-form-item>
          <a-form-item :label="$t('pages.dispatch.start.94cf01f8')">
            <a-space>
              <a-tag v-if="chooseBuildInfo.buildNumberId">#{{ chooseBuildInfo.buildNumberId }}</a-tag>
              <a-button
                type="primary"
                :disabled="!chooseBuildInfo.id"
                @click="
                  () => {
                    chooseVisible = 2
                  }
                "
              >
                {{ $t('pages.dispatch.start.94cf01f8') }}
              </a-button>
            </a-space>
          </a-form-item>
        </template>
        <!-- 文件中心 -->
        <template v-else-if="temp.type === 'file-storage'">
          <a-form-item :label="$t('pages.dispatch.start.2a688d49')">
            <a-space>
              {{ chooseFileInfo.name }}
              <a-button
                type="primary"
                @click="
                  () => {
                    chooseVisible = 3
                  }
                "
              >
                {{ $t('pages.dispatch.start.2a688d49') }}
              </a-button>
            </a-space>
          </a-form-item>
        </template>
        <!-- 静态文件 -->
        <template v-else-if="temp.type === 'static-file-storage'">
          <a-form-item :label="$t('pages.dispatch.start.2a688d49')">
            <a-space>
              {{ chooseFileInfo.name }}
              <a-button
                type="primary"
                @click="
                  () => {
                    chooseVisible = 4
                  }
                "
              >
                {{ $t('pages.dispatch.start.2a688d49') }}
              </a-button>
            </a-space>
          </a-form-item>
        </template>
        <a-form-item name="clearOld">
          <template #label>
            {{ $t('pages.dispatch.start.fa8d15d') }}
            <a-tooltip>
              <template #title>
                清空发布是指在上传新文件前,会将项目文件夹目录里面的所有文件先删除后再保存新文件
              </template>
              <QuestionCircleOutlined />
            </a-tooltip>
          </template>
          <a-switch
            v-model:checked="temp.clearOld"
            :checked-children="$t('pages.dispatch.start.d2fbce36')"
            :un-checked-children="$t('pages.dispatch.start.1c77d6fb')"
          />
        </a-form-item>
        <a-form-item v-if="temp.type !== 'use-build'" name="unzip">
          <template #label>
            {{ $t('pages.dispatch.start.23788e28') }}
            <a-tooltip>
              <template #title> {{ $t('pages.dispatch.start.240759e7') }}.bz2, tar.gz, tar, bz2, zip, gz </template>
              <QuestionCircleOutlined />
            </a-tooltip>
          </template>
          <a-switch
            v-model:checked="temp.autoUnzip"
            :checked-children="$t('pages.dispatch.start.d2fbce36')"
            :un-checked-children="$t('pages.dispatch.start.1c77d6fb')"
          />
        </a-form-item>
        <a-form-item v-if="temp.autoUnzip" :label="$t('pages.dispatch.start.89d09852')">
          <a-input-number
            v-model:value="temp.stripComponents"
            style="width: 100%"
            :min="0"
            :placeholder="$t('pages.dispatch.start.41a5bcc1')"
          />
        </a-form-item>

        <a-form-item :label="$t('pages.dispatch.start.3cb4eb3e')" name="afterOpt">
          <a-select v-model:value="temp.afterOpt" :placeholder="$t('pages.dispatch.start.9c2b1069')">
            <a-select-option v-for="item in afterOptList" :key="item.value">{{ item.title }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="secondaryDirectory" :label="$t('pages.dispatch.start.597f26e8')">
          <a-input v-model:value="temp.secondaryDirectory" :placeholder="$t('pages.dispatch.start.51be1a6d')" />
        </a-form-item>
        <a-form-item
          name="selectProject"
          :label="$t('pages.dispatch.start.ec12e21e')"
          :help="$t('pages.dispatch.start.72a6a7a9')"
        >
          <a-select
            v-model:value="temp.selectProjectArray"
            mode="multiple"
            :placeholder="$t('pages.dispatch.start.56724111')"
          >
            <a-select-option v-for="item in itemProjectList" :key="item.id" :value="`${item.projectId}@${item.nodeId}`">
              {{ item.nodeName }}-{{ item.cacheProjectName || item.projectId }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 选择构建 -->
    <a-drawer
      destroy-on-close
      :title="`${$t('pages.dispatch.start.afb1ec30')}`"
      placement="right"
      :open="chooseVisible === 1"
      width="80vw"
      :z-index="1009"
      :footer-style="{ textAlign: 'right' }"
      @close="
        () => {
          chooseVisible = 0
        }
      "
    >
      <build-list
        v-if="chooseVisible === 1"
        ref="buildList"
        :choose="'radio'"
        layout="table"
        mode="choose"
        @confirm="
          (data) => {
            chooseBuildInfo = {
              id: data[0].id,
              name: data[0].name
            }
            chooseVisible = 0
          }
        "
        @cancel="
          () => {
            chooseVisible = 0
          }
        "
      ></build-list>
      <template #footer>
        <a-space>
          <a-button
            @click="
              () => {
                chooseVisible = 0
              }
            "
          >
            {{ $t('pages.dispatch.start.43105e21') }}
          </a-button>
          <a-button
            type="primary"
            @click="
              () => {
                $refs['buildList'].handerConfirm()
              }
            "
          >
            {{ $t('pages.dispatch.start.7da4a591') }}
          </a-button>
        </a-space>
      </template>
    </a-drawer>
    <!-- 选择构建产物 -->
    <a-drawer
      destroy-on-close
      :title="`${$t('pages.dispatch.start.a2d28d82')}`"
      placement="right"
      :open="chooseVisible === 2"
      width="80vw"
      :z-index="1009"
      :footer-style="{ textAlign: 'right' }"
      @close="
        () => {
          chooseVisible = 0
        }
      "
    >
      <!-- 选择构建产物 -->
      <build-history
        v-if="chooseVisible === 2"
        ref="buildHistory"
        :choose="'radio'"
        mode="choose"
        :build-id="chooseBuildInfo.id"
        @confirm="
          (data) => {
            chooseBuildInfo = {
              ...chooseBuildInfo,
              buildNumberId: data[0]
            }
            chooseVisible = 0
          }
        "
        @cancel="
          () => {
            chooseVisible = 0
          }
        "
      ></build-history>
      <template #footer>
        <a-space>
          <a-button
            @click="
              () => {
                chooseVisible = 0
              }
            "
          >
            {{ $t('pages.dispatch.start.43105e21') }}
          </a-button>
          <a-button
            type="primary"
            @click="
              () => {
                $refs['buildHistory'].handerConfirm()
              }
            "
          >
            {{ $t('pages.dispatch.start.7da4a591') }}
          </a-button>
        </a-space>
      </template>
    </a-drawer>
    <!-- 选择文件 -->
    <a-drawer
      destroy-on-close
      :title="`${$t('pages.dispatch.start.2a688d49')}`"
      placement="right"
      :open="chooseVisible === 3"
      width="80vw"
      :z-index="1009"
      :footer-style="{ textAlign: 'right' }"
      @close="
        () => {
          chooseVisible = 0
        }
      "
    >
      <!-- 选择文件 -->
      <file-storage
        v-if="chooseVisible === 3"
        ref="fileStorage"
        :choose="'radio'"
        mode="choose"
        @confirm="
          (data) => {
            chooseFileInfo = { id: data[0].id, name: data[0].name }
            chooseVisible = 0
          }
        "
        @cancel="
          () => {
            chooseVisible = 0
          }
        "
      ></file-storage>
      <template #footer>
        <a-space>
          <a-button
            @click="
              () => {
                chooseVisible = 0
              }
            "
          >
            {{ $t('pages.dispatch.start.43105e21') }}
          </a-button>
          <a-button
            type="primary"
            @click="
              () => {
                $refs['fileStorage'].handerConfirm()
              }
            "
          >
            {{ $t('pages.dispatch.start.7da4a591') }}
          </a-button>
        </a-space>
      </template>
    </a-drawer>
    <!-- 选择静态文件 -->
    <a-drawer
      destroy-on-close
      :title="`${$t('pages.dispatch.start.e5aa6b98')}`"
      placement="right"
      :open="chooseVisible === 4"
      width="80vw"
      :z-index="1009"
      :footer-style="{ textAlign: 'right' }"
      @close="
        () => {
          chooseVisible = 0
        }
      "
    >
      <!-- 选择静态文件 -->
      <static-file-storage
        v-if="chooseVisible === 4"
        ref="staticFileStorage"
        :choose="'radio'"
        mode="choose"
        @confirm="
          (data) => {
            chooseFileInfo = { id: data[0].id, name: data[0].name }
            chooseVisible = 0
          }
        "
        @cancel="
          () => {
            chooseVisible = 0
          }
        "
      ></static-file-storage>
      <template #footer>
        <a-space>
          <a-button
            @click="
              () => {
                chooseVisible = 0
              }
            "
          >
            {{ $t('pages.dispatch.start.43105e21') }}
          </a-button>
          <a-button
            type="primary"
            @click="
              () => {
                $refs['staticFileStorage'].handerConfirm()
              }
            "
          >
            {{ $t('pages.dispatch.start.7da4a591') }}
          </a-button>
        </a-space>
      </template>
    </a-drawer>
  </div>
</template>

<script>
import { uploadPieces } from '@/utils/upload-pieces'
import {
  remoteDownload,
  uploadDispatchFile,
  uploadDispatchFileMerge,
  afterOptList,
  getDispatchProject,
  useBuild,
  useuseFileStorage,
  useuseStaticFileStorage
} from '@/api/dispatch'
import { renderSize, formatDuration } from '@/utils/const'
import BuildList from '@/pages/build/list-info'
import BuildHistory from '@/pages/build/history'
import FileStorage from '@/pages/file-manager/fileStorage/list'
import StaticFileStorage from '@/pages/file-manager/staticFileStorage/list'
import { getBuildGet } from '@/api/build-info'
import { hasFile } from '@/api/file-manager/file-storage'
import { hasStaticFile } from '@/api/file-manager/static-storage'
export default {
  components: {
    BuildList,
    BuildHistory,
    FileStorage,
    StaticFileStorage
  },
  inject: ['globalLoading'],
  props: {
    data: {
      type: Object,
      default: () => {}
    }
  },
  emits: ['cancel'],
  data() {
    return {
      afterOptList,
      percentage: 0,
      percentageInfo: {},
      uploading: false,
      itemProjectList: [],
      fileList: [],
      rules: {
        afterOpt: [{ required: true, message: this.$t('pages.dispatch.start.9c2b1069'), trigger: 'blur' }],
        url: [{ required: true, message: this.$t('pages.dispatch.start.b843270d'), trigger: 'blur' }]
      },
      temp: { type: 'upload' },
      chooseVisible: 0,
      chooseBuildInfo: {},
      chooseFileInfo: {},
      confirmLoading: false
    }
  },
  created() {
    this.temp = {
      ...this.temp,
      afterOpt: this.data.afterOpt,
      id: this.data.id,
      clearOld: this.data.clearOld,
      secondaryDirectory: this.data.secondaryDirectory,
      type: this.data.mode || 'upload'
    }
    getDispatchProject(this.data.id, true).then((res) => {
      if (res.code === 200) {
        this.itemProjectList = res.data?.projectList

        this.percentage = 0
        this.percentageInfo = {}
        this.fileList = []
        this.restForm()
      }
    })
    if (this.data.mode === 'use-build') {
      // 构建
      const buildData = (this.data.modeData || '').split(':')
      if (buildData.length === 2) {
        getBuildGet({
          id: buildData[0]
        }).then((res) => {
          if (res.code === 200 && res.data) {
            this.chooseBuildInfo = {
              id: res.data.id,
              name: res.data.name,
              buildNumberId: buildData[1]
            }
          }
        })
      }
    } else if (this.data.mode === 'download') {
      // 下载
      this.temp = { ...this.temp, url: this.data.modeData }
    } else if (this.data.mode === 'file-storage') {
      // 文件中心
      if (this.data.modeData) {
        hasFile({ fileSumMd5: this.data.modeData }).then((res) => {
          if (res.code === 200 && res.data) {
            this.chooseFileInfo = { id: res.data.id, name: res.data.name }
          }
        })
      }
    } else if (this.data.mode === 'static-file-storage') {
      // 静态文件
      if (this.data.modeData) {
        hasStaticFile({ fileId: this.data.modeData }).then((res) => {
          if (res.code === 200 && res.data) {
            this.chooseFileInfo = { id: res.data.id, name: res.data.name }
          }
        })
      }
    }
    // console.log(this.temp);
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.dispatch.start.${key}`, ...args)
    },
    renderSize,
    formatDuration,
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
    // 提交分发文件
    handleDispatchOk() {
      // console.log(this.temp);
      this.temp = {
        ...this.temp,
        selectProject: (this.temp.selectProjectArray && this.temp.selectProjectArray.join(',')) || ''
      }
      // 检验表单
      this.$refs['dispatchForm'].validate().then(() => {
        // const key = this.temp.type;
        if (this.temp.type == 'upload') {
          // 判断文件
          if (this.fileList.length === 0) {
            $notification.error({
              message: this.$t('pages.dispatch.start.25d31749')
            })
            return false
          }
          this.percentage = 0
          this.percentageInfo = {}
          let file = this.fileList[0]
          this.uploading = true
          this.confirmLoading = true
          uploadPieces({
            file,
            process: (process, end, total, duration) => {
              this.percentage = Math.max(this.percentage, process)
              this.percentageInfo = { end, total, duration }
            },
            resolveFileProcess: (msg) => {
              this.globalLoading({
                spinning: true,
                tip: msg
              })
            },
            resolveFileEnd: () => {
              this.globalLoading(false)
            },
            success: (uploadData) => {
              // 准备合并
              uploadDispatchFileMerge({
                ...uploadData[0],
                id: this.temp.id,
                afterOpt: this.temp.afterOpt,
                clearOld: this.temp.clearOld,
                autoUnzip: this.temp.autoUnzip,
                secondaryDirectory: this.temp.secondaryDirectory || '',
                stripComponents: this.temp.stripComponents || 0,
                selectProject: this.temp.selectProject
              })
                .then((res) => {
                  if (res.code === 200) {
                    this.fileList = []
                    $notification.success({
                      message: res.msg
                    })
                  }
                  setTimeout(() => {
                    this.percentage = 0
                    this.percentageInfo = {}
                    this.$emit('cancel')
                  }, 2000)
                  this.uploading = false
                })
                .catch(() => {
                  this.uploading = false
                })
                .finally(() => {
                  this.confirmLoading = false
                })
            },
            error: (msg) => {
              $notification.error({
                message: msg
              })
              this.uploading = false
              this.confirmLoading = false
            },
            uploadCallback: (formData) => {
              return new Promise((resolve, reject) => {
                formData.append('id', this.temp.id)
                // 上传文件
                uploadDispatchFile(formData)
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

          return true
        } else if (this.temp.type == 'download') {
          if (!this.temp.url) {
            $notification.error({
              message: this.$t('pages.dispatch.start.95dc7228')
            })
            return false
          }
          this.confirmLoading = true
          remoteDownload(this.temp)
            .then((res) => {
              if (res.code === 200) {
                $notification.success({
                  message: res.msg
                })

                this.$emit('cancel')
              }
            })
            .finally(() => {
              this.confirmLoading = false
            })
          return true
        } else if (this.temp.type == 'use-build') {
          // 构建
          if (!this.chooseBuildInfo || !this.chooseBuildInfo.id || !this.chooseBuildInfo.buildNumberId) {
            $notification.error({
              message: this.$t('pages.dispatch.start.de346cfe')
            })
            return false
          }
          this.confirmLoading = true
          useBuild({
            ...this.temp,
            buildId: this.chooseBuildInfo.id,
            buildNumberId: this.chooseBuildInfo.buildNumberId
          })
            .then((res) => {
              if (res.code === 200) {
                $notification.success({
                  message: res.msg
                })

                this.$emit('cancel')
              }
            })
            .finally(() => {
              this.confirmLoading = false
            })
          return true
        } else if (this.temp.type == 'file-storage') {
          // 文件中心
          if (!this.chooseFileInfo || !this.chooseFileInfo.id) {
            $notification.error({
              message: this.$t('pages.dispatch.start.346e59ed')
            })
            return false
          }
          this.confirmLoading = true
          useuseFileStorage({
            ...this.temp,
            fileId: this.chooseFileInfo.id
          })
            .then((res) => {
              if (res.code === 200) {
                $notification.success({
                  message: res.msg
                })

                this.$emit('cancel')
              }
            })
            .finally(() => {
              this.confirmLoading = false
            })
          return true
        } else if (this.temp.type == 'static-file-storage') {
          // 文件中心
          if (!this.chooseFileInfo || !this.chooseFileInfo.id) {
            $notification.error({
              message: this.$t('pages.dispatch.start.26b3a389')
            })
            return false
          }
          this.confirmLoading = true
          useuseStaticFileStorage({
            ...this.temp,
            fileId: this.chooseFileInfo.id
          })
            .then((res) => {
              if (res.code === 200) {
                $notification.success({
                  message: res.msg
                })
                this.$emit('cancel')
              }
            })
            .finally(() => {
              this.confirmLoading = false
            })
          return true
        }
      })
    },
    //
    restForm(e) {
      // console.log(e);
      if (e) {
        this.temp = { ...this.temp, type: e.target.value }
      }
      this.$refs['dispatchForm'] && this.$refs['dispatchForm'].clearValidate()
    }
  }
}
</script>

<style scoped>
:deep(.ant-progress-text) {
  width: auto;
}
</style>
