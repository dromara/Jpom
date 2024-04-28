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
      :title="$tl('p.distributeProject') + data.name"
      :mask-closable="false"
      @ok="handleDispatchOk"
      @cancel="
        () => {
          $emit('cancel')
        }
      "
    >
      <a-form ref="dispatchForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item :label="$tl('p.method')" name="type">
          <a-radio-group v-model:value="temp.type" name="type" :disabled="!!percentage" @change="restForm">
            <a-radio :value="'use-build'">{{ $tl('p.buildProduct') }}</a-radio>
            <a-radio :value="'file-storage'">{{ $tl('p.fileCenter') }}</a-radio>
            <a-radio :value="'static-file-storage'">{{ $tl('p.staticFile') }}</a-radio>
            <a-radio :value="'upload'">{{ $tl('p.uploadFile') }}</a-radio>
            <a-radio :value="'download'">{{ $tl('p.remoteDownload') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <!-- 手动上传 -->
        <a-form-item v-if="temp.type === 'upload'" :label="$tl('p.selectDistributeFile')" name="clearOld">
          <a-progress v-if="percentage" :percent="percentage">
            <template #format="percent">
              {{ percent }}%
              <template v-if="percentageInfo.total"> ({{ renderSize(percentageInfo.total) }}) </template>
              <template v-if="percentageInfo.duration">
                {{ $tl('p.usedTime') }}:{{ formatDuration(percentageInfo.duration) }}
              </template>
            </template>
          </a-progress>

          <a-upload :file-list="fileList" :disabled="!!percentage" :before-upload="beforeUpload" @remove="handleRemove">
            <LoadingOutlined v-if="percentage" />
            <a-button v-else type="primary"><UploadOutlined />{{ $tl('c.selectFile') }}</a-button>
          </a-upload>
        </a-form-item>
        <!-- 远程下载 -->
        <a-form-item v-else-if="temp.type === 'download'" :label="$tl('p.remoteDownloadURL')" name="url">
          <a-input v-model:value="temp.url" :placeholder="$tl('p.remoteDownloadAddress')" />
        </a-form-item>
        <!-- 在线构建 -->
        <template v-else-if="temp.type == 'use-build'">
          <a-form-item :label="$tl('c.selectBuild')">
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
                {{ $tl('c.selectBuild') }}
              </a-button>
            </a-space>
          </a-form-item>
          <a-form-item :label="$tl('c.selectProduct')">
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
                {{ $tl('c.selectProduct') }}
              </a-button>
            </a-space>
          </a-form-item>
        </template>
        <!-- 文件中心 -->
        <template v-else-if="temp.type === 'file-storage'">
          <a-form-item :label="$tl('c.selectFile')">
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
                {{ $tl('c.selectFile') }}
              </a-button>
            </a-space>
          </a-form-item>
        </template>
        <!-- 静态文件 -->
        <template v-else-if="temp.type === 'static-file-storage'">
          <a-form-item :label="$tl('c.selectFile')">
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
                {{ $tl('c.selectFile') }}
              </a-button>
            </a-space>
          </a-form-item>
        </template>
        <a-form-item name="clearOld">
          <template #label>
            {{ $tl('p.clearPublish') }}
            <a-tooltip>
              <template #title> {{ $tl('undefined') }},{{ $tl('undefined') }} </template>
              <QuestionCircleOutlined />
            </a-tooltip>
          </template>
          <a-switch
            v-model:checked="temp.clearOld"
            :checked-children="$tl('c.yes')"
            :un-checked-children="$tl('c.no')"
          />
        </a-form-item>
        <a-form-item v-if="temp.type !== 'use-build'" name="unzip">
          <template #label>
            {{ $tl('p.unZip') }}
            <a-tooltip>
              <template #title> {{ $tl('p.autoUnZip') }}.bz2, tar.gz, tar, bz2, zip, gz </template>
              <QuestionCircleOutlined />
            </a-tooltip>
          </template>
          <a-switch
            v-model:checked="temp.autoUnzip"
            :checked-children="$tl('c.yes')"
            :un-checked-children="$tl('c.no')"
          />
        </a-form-item>
        <a-form-item v-if="temp.autoUnzip" :label="$tl('p.excludeFolder')">
          <a-input-number
            v-model:value="temp.stripComponents"
            style="width: 100%"
            :min="0"
            :placeholder="$tl('p.excludeFolderDescription')"
          />
        </a-form-item>

        <a-form-item :label="$tl('p.postPublishAction')" name="afterOpt">
          <a-select v-model:value="temp.afterOpt" :placeholder="$tl('c.selectPostPublishAction')">
            <a-select-option v-for="item in afterOptList" :key="item.value">{{ item.title }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="secondaryDirectory" :label="$tl('p.subDirectory')">
          <a-input v-model:value="temp.secondaryDirectory" :placeholder="$tl('p.subDirectoryDescription')" />
        </a-form-item>
        <a-form-item name="selectProject" :label="$tl('p.filterProject')" :help="$tl('p.filterProjectDescription')">
          <a-select
            v-model:value="temp.selectProjectArray"
            mode="multiple"
            :placeholder="$tl('p.selectPublishProject')"
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
      :title="`${$tl('c.selectBuild')}`"
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
            {{ $tl('c.cancel') }}
          </a-button>
          <a-button
            type="primary"
            @click="
              () => {
                $refs['buildList'].handerConfirm()
              }
            "
          >
            {{ $tl('c.confirm') }}
          </a-button>
        </a-space>
      </template>
    </a-drawer>
    <!-- 选择构建产物 -->
    <a-drawer
      destroy-on-close
      :title="`${$tl('p.selectBuildProduct')}`"
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
            {{ $tl('c.cancel') }}
          </a-button>
          <a-button
            type="primary"
            @click="
              () => {
                $refs['buildHistory'].handerConfirm()
              }
            "
          >
            {{ $tl('c.confirm') }}
          </a-button>
        </a-space>
      </template>
    </a-drawer>
    <!-- 选择文件 -->
    <a-drawer
      destroy-on-close
      :title="`${$tl('c.selectFile')}`"
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
            {{ $tl('c.cancel') }}
          </a-button>
          <a-button
            type="primary"
            @click="
              () => {
                $refs['fileStorage'].handerConfirm()
              }
            "
          >
            {{ $tl('c.confirm') }}
          </a-button>
        </a-space>
      </template>
    </a-drawer>
    <!-- 选择静态文件 -->
    <a-drawer
      destroy-on-close
      :title="`${$tl('p.selectStaticFile')}`"
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
            {{ $tl('c.cancel') }}
          </a-button>
          <a-button
            type="primary"
            @click="
              () => {
                $refs['staticFileStorage'].handerConfirm()
              }
            "
          >
            {{ $tl('c.confirm') }}
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
        afterOpt: [{ required: true, message: this.$tl('c.selectPostPublishAction'), trigger: 'blur' }],
        url: [{ required: true, message: this.$tl('p.pleaseInputRemoteAddress'), trigger: 'blur' }]
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
              message: this.$tl('p.pleaseSelectFile')
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
              message: this.$tl('p.pleaseFillRemoteURL')
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
              message: this.$tl('p.pleaseFillBuildAndProduct')
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
              message: this.$tl('p.pleaseSelectFileCenterFile')
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
              message: this.$tl('p.pleaseSelectStaticFileFile')
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
