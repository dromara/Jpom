<template>
  <div>
    <a-modal
      destroyOnClose
      :open="true"
      :closable="!uploading"
      :footer="uploading ? null : undefined"
      width="50%"
      :keyboard="false"
      :title="'分发项目-' + data.name"
      @ok="handleDispatchOk"
      :maskClosable="false"
      :confirmLoading="confirmLoading"
      @cancel="
        () => {
          $emit('cancel')
        }
      "
    >
      <a-form ref="dispatchForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item label="方式" name="type">
          <a-radio-group v-model:value="temp.type" name="type" :disabled="!!percentage" @change="restForm">
            <a-radio :value="'use-build'">构建产物</a-radio>
            <a-radio :value="'file-storage'">文件中心</a-radio>
            <a-radio :value="'static-file-storage'">静态文件</a-radio>
            <a-radio :value="'upload'">上传文件</a-radio>
            <a-radio :value="'download'">远程下载</a-radio>
          </a-radio-group>
        </a-form-item>
        <!-- 手动上传 -->
        <a-form-item label="选择分发文件" name="clearOld" v-if="temp.type === 'upload'">
          <a-progress v-if="percentage" :percent="percentage">
            <template #format="percent">
              {{ percent }}%
              <template v-if="percentageInfo.total"> ({{ renderSize(percentageInfo.total) }}) </template>
              <template v-if="percentageInfo.duration"> 用时:{{ formatDuration(percentageInfo.duration) }} </template>
            </template>
          </a-progress>

          <a-upload :file-list="fileList" :disabled="!!percentage" @remove="handleRemove" :before-upload="beforeUpload">
            <LoadingOutlined v-if="percentage" />
            <a-button v-else type="primary"><UploadOutlined />选择文件</a-button>
          </a-upload>
        </a-form-item>
        <!-- 远程下载 -->
        <a-form-item label="远程下载URL" name="url" v-else-if="temp.type === 'download'">
          <a-input v-model:value="temp.url" placeholder="远程下载地址" />
        </a-form-item>
        <!-- 在线构建 -->
        <template v-else-if="temp.type == 'use-build'">
          <a-form-item label="选择构建">
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
                选择构建
              </a-button>
            </a-space>
          </a-form-item>
          <a-form-item label="选择产物">
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
                选择产物
              </a-button>
            </a-space>
          </a-form-item>
        </template>
        <!-- 文件中心 -->
        <template v-else-if="temp.type === 'file-storage'">
          <a-form-item label="选择文件">
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
                选择文件
              </a-button>
            </a-space>
          </a-form-item></template
        >
        <!-- 静态文件 -->
        <template v-else-if="temp.type === 'static-file-storage'">
          <a-form-item label="选择文件">
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
                选择文件
              </a-button>
            </a-space>
          </a-form-item></template
        >
        <a-form-item name="clearOld">
          <template v-slot:label>
            清空发布
            <a-tooltip>
              <template v-slot:title>
                清空发布是指在上传新文件前,会将项目文件夹目录里面的所有文件先删除后再保存新文件
              </template>
              <QuestionCircleOutlined />
            </a-tooltip>
          </template>
          <a-switch v-model:checked="temp.clearOld" checked-children="是" un-checked-children="否" />
        </a-form-item>
        <a-form-item name="unzip" v-if="temp.type !== 'use-build'">
          <template v-slot:label>
            是否解压
            <a-tooltip>
              <template v-slot:title>
                如果上传的压缩文件是否自动解压 支持的压缩包类型有 tar.bz2, tar.gz, tar, bz2, zip, gz</template
              >
              <QuestionCircleOutlined />
            </a-tooltip>
          </template>
          <a-switch v-model:checked="temp.autoUnzip" checked-children="是" un-checked-children="否" />
        </a-form-item>
        <a-form-item label="剔除文件夹" v-if="temp.autoUnzip">
          <a-input-number
            style="width: 100%"
            v-model:value="temp.stripComponents"
            :min="0"
            placeholder="解压时候自动剔除压缩包里面多余的文件夹名"
          />
        </a-form-item>

        <a-form-item label="分发后操作" name="afterOpt">
          <a-select v-model:value="temp.afterOpt" placeholder="请选择发布后操作">
            <a-select-option v-for="item in afterOptList" :key="item.value">{{ item.title }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="secondaryDirectory" label="二级目录">
          <a-input v-model:value="temp.secondaryDirectory" placeholder="不填写则发布至项目的根目录" />
        </a-form-item>
        <a-form-item name="selectProject" label="筛选项目" help="筛选之后本次发布操作只发布筛选项,并且只对本次操作生效">
          <a-select mode="multiple" v-model:value="temp.selectProjectArray" placeholder="请选择指定发布的项目">
            <a-select-option v-for="item in itemProjectList" :key="item.id" :value="`${item.projectId}@${item.nodeId}`">
              {{ item.nodeName }}-{{ item.cacheProjectName || item.projectId }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 选择构建 -->
    <a-drawer
      destroyOnClose
      :title="`选择构建`"
      placement="right"
      :open="chooseVisible === 1"
      width="80vw"
      :zIndex="1009"
      @close="
        () => {
          this.chooseVisible = 0
        }
      "
      :footer-style="{ textAlign: 'right' }"
    >
      <build-list
        v-if="chooseVisible === 1"
        :choose="'radio'"
        layout="table"
        mode="choose"
        ref="buildList"
        @confirm="
          (data) => {
            this.chooseBuildInfo = {
              id: data[0].id,
              name: data[0].name
            }
            this.chooseVisible = 0
          }
        "
        @cancel="
          () => {
            this.chooseVisible = 0
          }
        "
      ></build-list>
      <template #footer>
        <a-space>
          <a-button
            @click="
              () => {
                this.chooseVisible = 0
              }
            "
          >
            取消
          </a-button>
          <a-button
            type="primary"
            @click="
              () => {
                this.$refs['buildList'].handerConfirm()
              }
            "
          >
            确认
          </a-button>
        </a-space>
      </template>
    </a-drawer>
    <!-- 选择构建产物 -->
    <a-drawer
      destroyOnClose
      :title="`选择构建产物`"
      placement="right"
      :open="chooseVisible === 2"
      width="80vw"
      :zIndex="1009"
      @close="
        () => {
          this.chooseVisible = 0
        }
      "
      :footer-style="{ textAlign: 'right' }"
    >
      <!-- 选择构建产物 -->
      <build-history
        v-if="chooseVisible === 2"
        :choose="'radio'"
        mode="choose"
        ref="buildHistory"
        @confirm="
          (data) => {
            this.chooseBuildInfo = {
              ...this.chooseBuildInfo,
              buildNumberId: data[0]
            }
            this.chooseVisible = 0
          }
        "
        @cancel="
          () => {
            this.chooseVisible = 0
          }
        "
      ></build-history>
      <template #footer>
        <a-space>
          <a-button
            @click="
              () => {
                this.chooseVisible = 0
              }
            "
          >
            取消
          </a-button>
          <a-button
            type="primary"
            @click="
              () => {
                this.$refs['buildHistory'].handerConfirm()
              }
            "
          >
            确认
          </a-button>
        </a-space>
      </template>
    </a-drawer>
    <!-- 选择文件 -->
    <a-drawer
      destroyOnClose
      :title="`选择文件`"
      placement="right"
      :open="chooseVisible === 3"
      width="80vw"
      :zIndex="1009"
      @close="
        () => {
          this.chooseVisible = 0
        }
      "
      :footer-style="{ textAlign: 'right' }"
    >
      <!-- 选择文件 -->
      <file-storage
        v-if="chooseVisible === 3"
        :choose="'radio'"
        mode="choose"
        ref="fileStorage"
        @confirm="
          (data) => {
            this.chooseFileInfo = { id: data[0].id, name: data[0].name }
            this.chooseVisible = 0
          }
        "
        @cancel="
          () => {
            this.chooseVisible = 0
          }
        "
      ></file-storage>
      <template #footer>
        <a-space>
          <a-button
            @click="
              () => {
                this.chooseVisible = 0
              }
            "
          >
            取消
          </a-button>
          <a-button
            type="primary"
            @click="
              () => {
                this.$refs['fileStorage'].handerConfirm()
              }
            "
          >
            确认
          </a-button>
        </a-space>
      </template>
    </a-drawer>
    <!-- 选择静态文件 -->
    <a-drawer
      destroyOnClose
      :title="`选择静态文件`"
      placement="right"
      :open="chooseVisible === 4"
      width="80vw"
      :zIndex="1009"
      @close="
        () => {
          this.chooseVisible = 0
        }
      "
      :footer-style="{ textAlign: 'right' }"
    >
      <!-- 选择静态文件 -->
      <static-file-storage
        v-if="chooseVisible === 4"
        :choose="'radio'"
        mode="choose"
        ref="staticFileStorage"
        @confirm="
          (data) => {
            this.chooseFileInfo = { id: data[0].id, name: data[0].name }
            this.chooseVisible = 0
          }
        "
        @cancel="
          () => {
            this.chooseVisible = 0
          }
        "
      ></static-file-storage>
      <template #footer>
        <a-space>
          <a-button
            @click="
              () => {
                this.chooseVisible = 0
              }
            "
          >
            取消
          </a-button>
          <a-button
            type="primary"
            @click="
              () => {
                this.$refs['staticFileStorage'].handerConfirm()
              }
            "
          >
            确认
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
  props: {
    data: Object
  },
  data() {
    return {
      afterOptList,
      percentage: 0,
      percentageInfo: {},
      uploading: false,
      itemProjectList: [],
      fileList: [],
      rules: {
        afterOpt: [{ required: true, message: '请选择发布后操作', trigger: 'blur' }],
        url: [{ required: true, message: '请输入远程地址', trigger: 'blur' }]
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
      this.$refs['dispatchForm'].validate().then((valid) => {
        if (!valid) {
          return false
        }
        // const key = this.temp.type;
        if (this.temp.type == 'upload') {
          // 判断文件
          if (this.fileList.length === 0) {
            this.$notification.error({
              message: '请选择文件'
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
                    this.$notification.success({
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
              this.$notification.error({
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
            this.$notification.error({
              message: '请填写远程URL'
            })
            return false
          }
          this.confirmLoading = true
          remoteDownload(this.temp)
            .then((res) => {
              if (res.code === 200) {
                this.$notification.success({
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
            this.$notification.error({
              message: '请填写构建和产物'
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
                this.$notification.success({
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
            this.$notification.error({
              message: '请选择文件中心的文件'
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
                this.$notification.success({
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
            this.$notification.error({
              message: '请选择静态文件中的文件'
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
                this.$notification.success({
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
  },
  emits: ['cancel']
}
</script>

<style scoped>
/deep/ .ant-progress-text {
  width: auto;
}
</style>
