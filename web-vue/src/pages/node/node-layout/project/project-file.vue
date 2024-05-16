<template>
  <div>
    <!-- 布局 -->
    <a-spin tip="Loading..." :spinning="loading">
      <a-layout class="file-layout">
        <!-- 目录树 -->
        <a-layout-sider theme="light" class="sider" width="25%">
          <div class="dir-container">
            <a-space>
              <a-button size="small" type="primary" @click="loadData">{{ $tl('p.refreshDir') }}</a-button>
              <a-button v-show="noFileModes.includes(runMode)" size="small" type="primary" @click="goConsole">
                {{ $tl('p.console') }}
              </a-button>
              <a-button size="small" type="primary" @click="backupList"> {{ $tl('c.backupList') }} </a-button>
            </a-space>
          </div>
          <a-directory-tree
            v-model:selectedKeys="selectedKeys"
            v-model:expandedKeys="expandedKeys"
            auto-expand-parent
            :field-names="treeReplaceFields"
            :tree-data="treeList"
            @select="nodeClick"
          ></a-directory-tree>
          <!--   :loadData="onTreeData" -->
        </a-layout-sider>
        <!-- 表格 -->
        <a-layout-content class="file-content">
          <a-table
            :data-source="fileList"
            size="middle"
            :loading="loading"
            :columns="columns"
            :pagination="false"
            bordered
            :scroll="{
              x: 'max-content'
            }"
          >
            <template #title>
              <!-- <a-tag color="#2db7f5">项目目录: {{ absPath }}</a-tag>-->
              <a-space>
                <a-dropdown :disabled="!Object.keys(tempNode).length">
                  <a-button size="small" type="primary" @click="(e) => e.preventDefault()"
                    ><UploadOutlined />{{ $tl('p.upload') }}</a-button
                  >
                  <template #overlay>
                    <a-menu>
                      <a-menu-item @click="handleUpload">
                        <a-space><FileOutlined />{{ $tl('p.uploadFile') }}</a-space>
                      </a-menu-item>
                      <a-menu-item @click="handleZipUpload">
                        <a-space><FileZipOutlined />{{ $tl('p.uploadAndUnzip') }}</a-space>
                      </a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
                <a-dropdown :disabled="!Object.keys(tempNode).length">
                  <a-button size="small" type="primary" @click="(e) => e.preventDefault()"
                    ><PlusOutlined />{{ $tl('p.create') }}</a-button
                  >
                  <template #overlay>
                    <a-menu>
                      <a-menu-item @click="handleAddFile(1)">
                        <a-space>
                          <FolderAddOutlined />
                          <a-space>{{ $tl('p.newDir') }}</a-space>
                        </a-space>
                      </a-menu-item>
                      <a-menu-item @click="handleAddFile(2)">
                        <a-space>
                          <FileAddOutlined />
                          <a-space>{{ $tl('p.newBlankFile') }}</a-space>
                        </a-space>
                      </a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
                <a-tooltip :title="$tl('p.downloadRemoteFile')">
                  <a-button size="small" type="primary" @click="openRemoteUpload"><CloudDownloadOutlined /></a-button>
                </a-tooltip>
                <a-tooltip :title="$tl('p.refreshTable')">
                  <a-button size="small" type="primary" @click="loadFileList"><ReloadOutlined /></a-button>
                </a-tooltip>
                <a-tooltip :title="$tl('p.clearDir')">
                  <a-button size="small" type="primary" danger @click="clearFile"><DeleteOutlined /></a-button>
                </a-tooltip>

                <a-tag v-if="uploadPath" color="#2db7f5">{{ $tl('p.currentDir') }}{{ uploadPath || '' }}</a-tag>
                <div>{{ $tl('p.fileNameRightClick') }}</div>
              </a-space>
            </template>

            <template #bodyCell="{ column, text, record }">
              <template v-if="column.dataIndex === 'filename'">
                <a-tooltip placement="topLeft" :title="text">
                  <a-dropdown :trigger="['contextmenu']">
                    <div>{{ text }}</div>
                    <template #overlay>
                      <a-menu>
                        <a-menu-item key="1">
                          <a-button :disabled="!record.textFileEdit" type="link" @click="goReadFile(record)">
                            <BarsOutlined /> {{ $tl('p.trackFile') }}
                          </a-button>
                        </a-menu-item>
                        <a-menu-item key="2">
                          <a-button type="link" @click="handleRenameFile(record)">
                            <HighlightOutlined />{{ $tl('c.rename') }}
                          </a-button>
                        </a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
                </a-tooltip>
              </template>
              <template v-else-if="column.dataIndex === 'isDirectory'">
                <span>{{ text ? $tl('p.dir') : $tl('p.file') }}</span>
              </template>
              <template v-else-if="column.dataIndex === 'fileSizeLong'">
                <a-tooltip placement="topLeft" :title="`${text ? renderSize(text) : record.fileSize}`">
                  {{ text ? renderSize(text) : record.fileSize }}
                </a-tooltip>
              </template>
              <template v-else-if="column.dataIndex === 'modifyTimeLong'">
                <a-tooltip :title="`${parseTime(record.modifyTimeLong)}}`">
                  <span>{{ parseTime(record.modifyTimeLong) }}</span>
                </a-tooltip>
              </template>
              <template v-else-if="column.dataIndex === 'operation'">
                <a-space>
                  <template v-if="record.isDirectory">
                    <a-tooltip :title="$tl('p.dirNotEditable')">
                      <a-button size="small" type="primary" :disabled="true">{{ $tl('c.edit') }}</a-button>
                    </a-tooltip>
                    <a-tooltip :title="$tl('p.cantDownloadDir')">
                      <a-button size="small" type="primary" :disabled="true">{{ $tl('c.download') }}</a-button>
                    </a-tooltip>
                  </template>
                  <template v-else>
                    <a-tooltip :title="$tl('p.editFileSuffixConfig')">
                      <a-button
                        size="small"
                        type="primary"
                        :loading="editLoading"
                        :disabled="!record.textFileEdit"
                        @click="handleEditFile(record)"
                        >{{ $tl('c.edit') }}</a-button
                      >
                    </a-tooltip>
                    <a-button size="small" type="primary" @click="handleDownload(record)">{{
                      $tl('c.download')
                    }}</a-button>
                  </template>
                  <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
                    $tl('p.delete')
                  }}</a-button>
                </a-space>
              </template>
            </template>
          </a-table>
          <!-- 批量上传文件 -->
          <a-modal
            v-model:open="uploadFileVisible"
            destroy-on-close
            :closable="!uploading"
            :keyboard="false"
            width="35vw"
            :title="$tl('p.uploadProjectFile')"
            :footer="null"
            :mask-closable="false"
          >
            <a-space direction="vertical" size="large" style="width: 100%">
              <a-upload
                :file-list="uploadFileList"
                :before-upload="
                  (file) => {
                    uploadFileList = [...uploadFileList, file]
                    return false
                  }
                "
                multiple
                :disabled="!!percentage"
                @remove="
                  (file) => {
                    const index = uploadFileList.indexOf(file)
                    //const newFileList = this.uploadFileList.slice();

                    uploadFileList.splice(index, 1)
                    return true
                  }
                "
              >
                <template v-if="percentage">
                  <LoadingOutlined v-if="uploadFileList.length" />
                  <span v-else>-</span>
                </template>

                <a-button v-else><UploadOutlined />{{ $tl('p.selectFile') }}</a-button>
              </a-upload>

              <a-row v-if="percentage">
                <a-col span="24">
                  <a-progress :percent="percentage" class="max-progress">
                    <template #format="percent">
                      {{ percent }}%<template v-if="percentageInfo.total">
                        ({{ renderSize(percentageInfo.total) }})
                      </template>
                      <template v-if="percentageInfo.duration">
                        {{ $tl('c.currentFileDuration') }}:{{ formatDuration(percentageInfo.duration) }}
                      </template>
                      <template v-if="uploadFileList.length">
                        {{ $tl('c.complete') }} {{ successSize }} {{ $tl('c.countFormat') }}{{ uploadFileList.length
                        }}{{ $tl('c.count') }}
                      </template>
                    </template>
                  </a-progress>
                </a-col>
              </a-row>

              <a-button type="primary" :disabled="fileUploadDisabled" @click="startUpload">{{
                $tl('c.startUpload')
              }}</a-button>
            </a-space>
          </a-modal>
          <!-- 上传压缩文件 -->
          <a-modal
            v-model:open="uploadZipFileVisible"
            destroy-on-close
            :closable="!uploading"
            :keyboard="false"
            width="35vw"
            :title="$tl('p.uploadZipFile')"
            :footer="null"
            :mask-closable="false"
          >
            <a-space direction="vertical" size="large" style="width: 100%">
              <a-upload
                :file-list="uploadFileList"
                :disabled="!!percentage"
                :before-upload="
                  (file) => {
                    uploadFileList = [file]
                    return false
                  }
                "
                :accept="ZIP_ACCEPT"
                @remove="
                  () => {
                    uploadFileList = []
                    return true
                  }
                "
              >
                <LoadingOutlined v-if="percentage" />
                <a-button v-else><UploadOutlined />{{ $tl('p.selectZipFile') }}</a-button>
              </a-upload>
              <a-row v-if="percentage">
                <a-col span="24">
                  <a-progress :percent="percentage" class="max-progress">
                    <template #format="percent">
                      {{ percent }}%<template v-if="percentageInfo.total">
                        ({{ renderSize(percentageInfo.total) }})
                      </template>
                      <template v-if="percentageInfo.duration">
                        {{ $tl('c.currentFileDuration') }}:{{ formatDuration(percentageInfo.duration) }}
                      </template>
                      <template v-if="uploadFileList.length">
                        {{ $tl('c.complete') }} {{ successSize }} {{ $tl('c.countFormat') }}{{ uploadFileList.length
                        }}{{ $tl('c.count') }}
                      </template>
                    </template>
                  </a-progress>
                </a-col>
              </a-row>

              <a-switch
                v-model:checked="uploadData.checkBox"
                :checked-children="$tl('p.overwrite')"
                :un-checked-children="$tl('p.keep')"
                style="margin-bottom: 10px"
              />

              <a-input-number
                v-model:value="uploadData.stripComponents"
                style="width: 100%"
                :min="0"
                :placeholder="$tl('c.excludeExtraFolder')"
              />

              <a-button type="primary" :disabled="fileUploadDisabled" @click="startZipUpload">{{
                $tl('c.startUpload')
              }}</a-button>
            </a-space>
          </a-modal>

          <a-modal
            v-model:open="editFileVisible"
            destroy-on-close
            width="80vw"
            :title="`${$tl('p.editFileContent')} ${filename}`"
            :mask-closable="true"
            @cancel="handleCloseModal"
          >
            <code-editor
              v-if="editFileVisible"
              v-model:content="fileContent"
              height="60vh"
              show-tool
              :file-suffix="filename"
            >
              <template #tool_before>
                <a-tag>
                  {{ filename }}
                  <!-- {{ temp.name }} -->
                </a-tag>
              </template>
            </code-editor>

            <template #footer>
              <a-button @click="handleCloseModal"> {{ $tl('p.close') }} </a-button>
              <a-button type="primary" @click="updateFileData"> {{ $tl('p.save') }} </a-button>
              <a-button
                type="primary"
                @click="
                  () => {
                    updateFileData()
                    handleCloseModal()
                  }
                "
              >
                {{ $tl('p.saveAndClose') }}
              </a-button>
            </template>
          </a-modal>
          <!--远程下载  -->
          <a-modal
            v-model:open="uploadRemoteFileVisible"
            destroy-on-close
            :confirm-loading="confirmLoading"
            :title="$tl('p.remoteDownload')"
            :mask-closable="false"
            @ok="handleRemoteUpload"
            @cancel="closeRemoteUpload"
          >
            <a-form
              ref="ruleForm"
              :model="remoteDownloadData"
              :label-col="{ span: 6 }"
              :wrapper-col="{ span: 18 }"
              :rules="rules"
            >
              <a-form-item :label="$tl('p.remoteDownloadUrl')" name="url">
                <a-input v-model:value="remoteDownloadData.url" :placeholder="$tl('p.remoteDownloadAddress')" />
              </a-form-item>
              <a-form-item :label="$tl('p.isZipFile')">
                <a-switch
                  v-model:checked="remoteDownloadData.unzip"
                  :checked-children="$tl('p.yes')"
                  :un-checked-children="$tl('p.no')"
                />
              </a-form-item>
              <a-form-item v-if="remoteDownloadData.unzip" :label="$tl('p.excludeFolder')">
                <a-input-number
                  v-model:value="remoteDownloadData.stripComponents"
                  style="width: 100%"
                  :min="0"
                  :placeholder="$tl('c.excludeExtraFolder')"
                />
              </a-form-item>
            </a-form>
          </a-modal>
          <!-- 创建文件/文件夹 -->
          <a-modal
            v-model:open="addFileFolderVisible"
            destroy-on-close
            width="300px"
            :title="addFileOrFolderType === 1 ? $tl('p.addNewDir') : $tl('p.createNewFile')"
            :footer="null"
            :mask-closable="true"
          >
            <a-space direction="vertical" style="width: 100%">
              <span v-if="uploadPath">{{ $tl('p.currentDirPath') }}{{ uploadPath }}</span>
              <!-- <a-tag v-if="">目录创建成功后需要手动刷新右边树才能显示出来哟</a-tag> -->

              <a-input v-model:value="fileFolderName" :placeholder="$tl('p.inputName')" />
              <a-row type="flex" justify="center">
                <a-button type="primary" :disabled="fileFolderName.length === 0" @click="startAddFileFolder">{{
                  $tl('c.confirm')
                }}</a-button>
              </a-row>
            </a-space>
          </a-modal>
          <!-- 从命名文件/文件夹 -->
          <a-modal
            v-model:open="renameFileFolderVisible"
            destroy-on-close
            width="300px"
            :title="`${$tl('c.rename')}`"
            :footer="null"
            :mask-closable="true"
          >
            <a-space direction="vertical" style="width: 100%">
              <a-input v-model:value="fileFolderName" :placeholder="$tl('p.inputNewName')" />

              <a-row type="flex" justify="center">
                <a-button type="primary" :disabled="fileFolderName.length === 0" @click="renameFileFolder">{{
                  $tl('c.confirm')
                }}</a-button>
              </a-row>
            </a-space>
          </a-modal>
        </a-layout-content>
      </a-layout>
    </a-spin>
    <!-- 查看备份列表 -->
    <a-modal
      v-model:open="backupListVisible"
      destroy-on-close
      width="80vw"
      height="80vh"
      :title="$tl('c.backupList')"
      :footer="null"
      :mask-closable="true"
      @cancel="
        () => {
          loadData()
        }
      "
    >
      <projectFileBackup v-if="backupListVisible" :node-id="nodeId" :project-id="projectId"></projectFileBackup>
    </a-modal>
  </div>
</template>

<script>
import {
  deleteProjectFile,
  downloadProjectFile,
  getFileList,
  newFileFolder,
  noFileModes,
  readFile,
  remoteDownload,
  renameFileFolder,
  updateFile,
  uploadProjectFile,
  shardingMerge
} from '@/api/node-project'
import { ZIP_ACCEPT, renderSize, formatDuration, concurrentExecution, parseTime } from '@/utils/const'
import codeEditor from '@/components/codeEditor'
import projectFileBackup from './project-file-backup.vue'
import { uploadPieces } from '@/utils/upload-pieces'

export default {
  components: {
    codeEditor,
    projectFileBackup
  },
  inject: ['globalLoading'],
  props: {
    nodeId: {
      type: String,
      default: ''
    },
    projectId: {
      type: String,
      default: ''
    },
    runMode: {
      type: String,
      default: ''
    },
    absPath: {
      type: String,
      default: ''
    }
  },
  emits: ['goReadFile', 'goConsole'],
  data() {
    return {
      ZIP_ACCEPT,
      noFileModes,
      loading: false,
      treeList: [],
      fileList: [],
      uploadFileList: [],
      tempNode: {},
      temp: {},
      filename: '',
      uploadFileVisible: false,
      uploadZipFileVisible: false,
      uploadRemoteFileVisible: false,
      editFileVisible: false,
      backupListVisible: false,
      successSize: 0,
      fileContent: '',
      treeReplaceFields: {
        title: 'filename',
        isLeaf: 'isDirectory'
      },

      // 是否是上传状态
      uploading: false,
      percentage: 0,
      percentageInfo: {},
      uploadData: {
        checkBox: false
      },

      // tableHeight: "80vh",
      defaultProps: {
        children: 'children',
        label: 'filename'
      },
      remoteDownloadData: {
        id: '',
        url: '',
        unzip: false
      },
      columns: [
        {
          title: this.$tl('p.fileName'),
          dataIndex: 'filename',
          ellipsis: true
        },
        {
          title: this.$tl('p.fileType'),
          dataIndex: 'isDirectory',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$tl('p.fileSize'),
          dataIndex: 'fileSizeLong',
          width: 120,
          ellipsis: true,

          sorter: (a, b) => a.fileSizeLong - b.fileSizeLong
        },
        {
          title: this.$tl('p.modifyTime'),
          dataIndex: 'modifyTimeLong',
          width: '180px',
          ellipsis: true,

          sorter: (a, b) => a.modifyTimeLong - b.modifyTimeLong
        },
        {
          title: this.$tl('p.operation'),
          dataIndex: 'operation',
          width: '180px',
          align: 'center',
          fixed: 'right'
        }
      ],
      rules: {
        url: [{ required: true, message: this.$tl('p.remoteUrlNotEmpty'), trigger: 'change' }]
      },
      addFileFolderVisible: false,
      // 目录1 文件2 标识
      addFileOrFolderType: 1,
      fileFolderName: '',
      oldFileFolderName: '',
      renameFileFolderVisible: false,
      editLoading: false,
      confirmLoading: false,
      selectedKeys: [],
      expandedKeys: []
    }
  },
  computed: {
    fileUploadDisabled() {
      return this.uploadFileList.length === 0 || this.uploading
    },
    uploadPath() {
      if (!Object.keys(this.tempNode).length) {
        return ''
      }
      if (this.tempNode.level === 1) {
        return ''
      } else {
        return (this.tempNode.levelName || '') + '/' + this.tempNode.filename
      }
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.node.nodeLayout.project.projectFile.${key}`, ...args)
    },
    renderSize,
    uploadPieces,
    formatDuration,
    parseTime,
    // 加载数据
    loadData() {
      const key = 'root-' + new Date().getTime()
      this.tempNode = {
        filename: this.$tl('p.dirLabel') + (this.absPath || ''),
        level: 1,
        isDirectory: true,
        key: key,
        isLeaf: false,
        activeKey: [0]
      }
      this.treeList = [this.tempNode]
      this.selectedKeys = [key]
      this.expandedKeys = [key]
      this.loadFileList()
      // // 设置默认展开第一个
      // setTimeout(() => {
      //   const node = this.treeList[0]
      //   this.tempNode = node
      //   this.expandKeys = [key]
      //   this.loadFileList()
      // }, 1000)
    },
    handleEditFile(record) {
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        filePath: this.uploadPath,
        filename: record.filename
      }
      this.editLoading = true
      // 读取文件数据
      readFile(params)
        .then((res) => {
          if (res.code === 200) {
            this.editFileVisible = true
            this.filename = record.filename

            this.fileContent = res.data
          }
        })
        .finally(() => {
          this.editLoading = false
        })
    },
    // 关闭编辑器弹窗
    handleCloseModal() {
      this.fileContent = ''
      this.editFileVisible = false
    },

    updateFileData() {
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        filePath: this.uploadPath,
        filename: this.filename,
        fileText: this.fileContent
      }

      updateFile(params).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
        }
      })
    },

    // 点击树节点
    nodeClick(selectedKeys, { node }) {
      if (node.dataRef.isDirectory) {
        this.tempNode = node.dataRef
        this.expandedKeys = [this.tempNode.key]
        this.loadFileList()
      }
    },
    // 上传文件
    handleUpload() {
      if (Object.keys(this.tempNode).length === 0) {
        $notification.error({
          message: this.$tl('c.selectNode')
        })
        return
      }
      //初始化成功数
      this.successSize = 0
      this.uploadFileList = []
      this.uploading = false
      this.percentage = 0
      this.percentageInfo = {}
      this.uploadFileVisible = true
    },
    // handleRemove(file) {},

    // 开始上传文件
    startUpload() {
      // 设置上传状态
      this.uploading = true
      this.successSize = 0
      // 遍历上传文件
      concurrentExecution(
        this.uploadFileList.map((item, index) => {
          // console.log(item);
          return index
        }),
        1,
        (curItem) => {
          const file = this.uploadFileList[curItem]
          this.uploadFileList = this.uploadFileList.map((fileItem, fileIndex) => {
            if (fileIndex === curItem) {
              fileItem.status = 'uploading'
            }
            return fileItem
          })
          this.percentage = 0
          this.percentageInfo = {}
          return new Promise((resolve, reject) => {
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
              process: (process, end, total, duration) => {
                this.percentage = Math.max(this.percentage, process)
                this.percentageInfo = { end, total, duration }
              },
              success: (uploadData, name) => {
                // 准备合并
                shardingMerge({
                  ...uploadData[0],
                  nodeId: this.nodeId,
                  id: this.projectId,
                  levelName: this.uploadPath
                }).then((res) => {
                  if (res.code === 200) {
                    this.successSize++
                    $notification.success({
                      message: name + ' ' + res.msg
                    })
                    this.uploadFileList = this.uploadFileList.map((fileItem, fileIndex) => {
                      if (fileIndex === curItem) {
                        fileItem.status = 'done'
                      }
                      return fileItem
                    })

                    resolve()
                  } else {
                    this.uploadFileList = this.uploadFileList.map((fileItem, fileIndex) => {
                      if (fileIndex === curItem) {
                        fileItem.status = 'error'
                      }
                      return fileItem
                    })
                    reject()
                  }
                })
              },
              error: (msg) => {
                this.uploadFileList = this.uploadFileList.map((fileItem, fileIndex) => {
                  if (fileIndex === curItem) {
                    fileItem.status = 'error'
                  }
                  return fileItem
                })
                $notification.error({
                  message: msg
                })
                reject()
              },
              uploadCallback: (formData) => {
                return new Promise((resolve, reject) => {
                  formData.append('nodeId', this.nodeId)
                  formData.append('id', this.projectId)
                  // 计算属性 uploadPath
                  formData.append('levelName', this.uploadPath)

                  // 上传文件
                  uploadProjectFile(formData)
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
          })
        }
      ).then(() => {
        this.uploading = this.successSize !== this.uploadFileList.length
        // // 判断是否全部上传完成
        if (!this.uploading) {
          this.uploadFileList = []
          setTimeout(() => {
            this.percentage = 0
            this.percentageInfo = {}
            this.loadFileList()
            this.uploadFileVisible = false
          }, 2000)
        }
      })
    },
    // 上传压缩文件
    handleZipUpload() {
      if (Object.keys(this.tempNode).length === 0) {
        $notification.error({
          message: this.$tl('c.selectNode')
        })
        return
      }
      this.uploadData = {}
      this.successSize = 0
      this.uploadFileList = []
      this.uploading = false
      this.percentage = 0
      this.percentageInfo = {}
      this.uploadZipFileVisible = true
    },

    // 开始上传压缩文件
    startZipUpload() {
      // 设置上传状态
      this.uploading = true
      this.percentage = 0
      this.percentageInfo = {}
      const file = this.uploadFileList[0]
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
        process: (process, end, total, duration) => {
          this.percentage = Math.max(this.percentage, process)
          this.percentageInfo = { end, total, duration }
        },
        success: (uploadData, name) => {
          // 准备合并
          // formData.append("type", "unzip");
          // formData.append("stripComponents", this.uploadData.stripComponents || 0);
          // formData.append("clearType", this.uploadData.checkBox ? "clear" : "noClear");
          shardingMerge({
            ...uploadData[0],
            nodeId: this.nodeId,
            id: this.projectId,
            levelName: this.uploadPath,
            type: 'unzip',
            stripComponents: this.uploadData.stripComponents || 0,
            clearType: this.uploadData.checkBox ? 'clear' : 'noClear'
          }).then((res) => {
            if (res.code === 200) {
              this.successSize++
              $notification.success({
                message: name + ' ' + res.msg
              })
              this.uploading = this.successSize !== this.uploadFileList.length
              // // 判断是否全部上传完成
              if (!this.uploading) {
                this.uploadFileList = []
                setTimeout(() => {
                  this.percentage = 0
                  this.percentageInfo = {}
                  this.loadFileList()
                  this.uploadZipFileVisible = false
                }, 2000)
              }
            }
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
            formData.append('id', this.projectId)
            // 计算属性 uploadPath
            formData.append('levelName', this.uploadPath)
            formData.append('type', 'unzip')
            formData.append('stripComponents', this.uploadData.stripComponents || 0)
            formData.append('clearType', this.uploadData.checkBox ? 'clear' : 'noClear')

            // 上传文件
            uploadProjectFile(formData)
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

      // // 上传文件
      // const file = this.uploadFileList[0];
      // const formData = new FormData();

      // // 上传文件
      // uploadProjectFile(formData).then((res) => {
      //   if (res.code === 200) {
      //     $notification.success({
      //       message: res.msg,
      //     });
      //     this.successSize++;
      //     this.percentage = 100;
      //     setTimeout(() => {
      //       this.percentage = 0;
      //       this.uploading = false;
      //       clearInterval(this.timer);
      //       this.uploadData = false;
      //       this.uploadFileList = [];
      //       this.loadFileList();
      //     }, 1000);
      //   }
      //   this.percentage = 0;
      // });
    },
    //打开远程上传
    openRemoteUpload() {
      // this.$refs.ruleForm.resetFields();
      this.uploadRemoteFileVisible = true
    },
    //关闭远程上传
    closeRemoteUpload() {
      //   this.$refs.ruleForm.resetFields();
      this.uploadRemoteFileVisible = false
    },
    //处理上传文件
    handleRemoteUpload() {
      this.$refs.ruleForm.validate().then(() => {
        const params = {
          id: this.projectId,
          nodeId: this.nodeId,
          url: this.remoteDownloadData.url,
          levelName: this.uploadPath,
          unzip: this.remoteDownloadData.unzip,
          stripComponents: this.remoteDownloadData.stripComponents || 0
        }
        this.confirmLoading = true
        remoteDownload(params)
          .then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.remoteDownloadData = {}
              this.uploadRemoteFileVisible = false
              this.loadFileList()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    /**
     * 根据key获取树节点
     * @param keys
     * @returns {*}
     */
    getTreeNode(keys) {
      let node = this.treeList[keys[0]]
      for (let key of keys.slice(1)) {
        if (key >= 0 && key < node.children.length) {
          node = node.children[key]
        } else {
          throw new Error('Invalid key: ' + key)
        }
      }
      return node
    },
    /**
     * 更新树节点的方法抽离封装
     * @param keys
     * @param value
     */
    updateTreeChildren(keys, value) {
      const node = this.getTreeNode(keys)
      node.children = value
    },
    /**
     * 文件列表转树结构
     * @param data
     */
    fileList2TreeData(data) {
      const node = this.tempNode
      const children = data
        .filter((ele) => {
          return ele.isDirectory
        })
        .map((ele, index) => {
          ele.isLeaf = !ele.isDirectory
          ele.key = ele.filename + '-' + new Date().getTime()
          ele.activeKey = node.activeKey.concat([index])
          return ele
        })
      this.updateTreeChildren(node.activeKey, children)
    },
    // 加载文件列表
    loadFileList() {
      if (Object.keys(this.tempNode).length === 0) {
        $notification.warn({
          message: this.$tl('c.selectNode')
        })
        return false
      }
      // 请求参数
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        path: this.uploadPath
      }
      this.loading = true
      this.fileList = []
      // 加载文件
      getFileList(params).then((res) => {
        if (res.code === 200) {
          // 区分目录和文件
          res.data.forEach((element) => {
            // if (!element.isDirectory) {
            // 设置文件表格
            this.fileList.push({
              ...element
            })
            // }
          })
          this.fileList2TreeData(res.data)
        }
        this.loading = false
      })
    },
    // 清空文件
    clearFile() {
      const msg = this.uploadPath
        ? this.$tl('p.question50') + this.uploadPath + this.$tl('p.question51')
        : this.$tl('p.question52')
      $confirm({
        title: this.$tl('c.systemPrompt'),
        content: msg,
        okText: this.$tl('c.confirm'),
        zIndex: 1009,
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          return deleteProjectFile({
            nodeId: this.nodeId,
            id: this.projectId,
            type: 'clear',
            levelName: this.uploadPath
          }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.loadFileList()
            }
          })
        }
      })
    },
    // 下载
    handleDownload(record) {
      $notification.info({
        message: this.$tl('p.downloading')
      })
      // 请求参数
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        levelName: record.levelName,
        filename: record.filename
      }
      window.open(downloadProjectFile(params), '_blank')
    },
    // 删除
    handleDelete(record) {
      const msg = record.isDirectory
        ? this.$tl('c.deletePrompt') + record.filename + this.$tl('p.question53')
        : this.$tl('c.deletePrompt') + record.filename + this.$tl('p.question54')
      $confirm({
        title: this.$tl('c.systemPrompt'),
        content: msg,
        okText: this.$tl('c.confirm'),
        zIndex: 1009,
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          return deleteProjectFile({
            nodeId: this.nodeId,
            id: this.projectId,
            levelName: record.levelName,
            filename: record.filename
          }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.loadFileList()
            }
          })
        }
      })
    },
    goConsole() {
      this.$emit('goConsole')
    },
    goReadFile(record) {
      // const filePath = this.uploadPath + record.filename;
      this.$emit('goReadFile', this.uploadPath, record.filename)
    },
    handleAddFile(type) {
      this.addFileFolderVisible = true
      this.addFileOrFolderType = type
      this.fileFolderName = ''
    },
    // 确认新增文件  目录
    startAddFileFolder() {
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        levelName: this.uploadPath,
        filename: this.fileFolderName,
        unFolder: this.addFileOrFolderType === 1 ? false : true
      }
      newFileFolder(params).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
          this.addFileFolderVisible = false
          this.loadFileList()
        }
      })
    },
    handleRenameFile(record) {
      this.renameFileFolderVisible = true
      this.fileFolderName = record.filename
      this.oldFileFolderName = record.filename
    },
    // 确认修改文件 目录名称
    renameFileFolder() {
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        levelName: this.uploadPath,
        newname: this.fileFolderName,
        filename: this.oldFileFolderName
      }
      renameFileFolder(params).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
          this.renameFileFolderVisible = false
          this.loadFileList()
        }
      })
    },
    // 查看备份列表
    backupList() {
      this.backupListVisible = true
    }
  }
}
</script>

<style scoped>
:deep(.ant-progress-text) {
  width: auto;
}
.sider {
  border: 1px solid #e2e2e2;
  height: calc(100vh - 80px);
  overflow-y: auto;
}
.dir-container {
  padding: 10px;
  border-bottom: 1px solid #eee;
}
.file-content {
  height: calc(100vh - 100px);
  overflow-y: auto;
  margin: 10px 10px 0;
  padding: 10px;
  /* background-color: #fff; */
}
</style>
