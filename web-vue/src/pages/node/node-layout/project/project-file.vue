<template>
  <div>
    <!-- 布局 -->
    <a-layout class="file-layout">
      <!-- 目录树 -->
      <a-layout-sider theme="light" class="sider" width="25%">
        <div class="dir-container">
          <a-space>
            <a-button size="small" type="primary" @click="loadData">刷新目录</a-button>
            <a-button
              size="small"
              type="primary"
              v-if="showConsole"
              @click="goConsole"
              v-show="noFileModes.includes(runMode)"
              >控制台</a-button
            >
            <a-button size="small" type="primary" @click="backupList">备份列表</a-button>
          </a-space>
        </div>

        <a-directory-tree
          :fieldNames="treeReplaceFields"
          @select="nodeClick"
          :loadData="onTreeData"
          :treeData="treeList"
        ></a-directory-tree>
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
          <template v-slot:title>
            <!-- <a-tag color="#2db7f5">项目目录: {{ absPath }}</a-tag>-->
            <a-space>
              <a-dropdown :disabled="!Object.keys(this.tempNode).length">
                <a-button size="small" type="primary" @click="(e) => e.preventDefault()"
                  ><UploadOutlined />上传</a-button
                >
                <template v-slot:overlay>
                  <a-menu>
                    <a-menu-item @click="handleUpload">
                      <a-space><FileOutlined />上传文件</a-space>
                    </a-menu-item>
                    <a-menu-item @click="handleZipUpload">
                      <a-space><FileZipOutlined />上传压缩包并自动解压</a-space>
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
              <a-dropdown :disabled="!Object.keys(this.tempNode).length">
                <a-button size="small" type="primary" @click="(e) => e.preventDefault()">新建</a-button>
                <template v-slot:overlay>
                  <a-menu>
                    <a-menu-item @click="handleAddFile(1)">
                      <a-space>
                        <FolderAddOutlined />
                        <a-space>新建目录</a-space>
                      </a-space>
                    </a-menu-item>
                    <a-menu-item @click="handleAddFile(2)">
                      <a-space>
                        <FileAddOutlined />
                        <a-space>新建空白文件</a-space>
                      </a-space>
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
              <a-tooltip
                title="通过 URL 下载远程文件到项目文件夹,需要到对应的工作空间下授权目录配置中配置允许的 HOST 授权"
              >
                <a-button size="small" type="primary" @click="openRemoteUpload"><CloudDownloadOutlined /></a-button>
              </a-tooltip>
              <a-tooltip title="刷新文件表格">
                <a-button size="small" type="primary" @click="loadFileList"><ReloadOutlined /></a-button>
              </a-tooltip>
              <a-tooltip title="清空当前目录文件">
                <a-button size="small" type="primary" danger @click="clearFile"><DeleteOutlined /></a-button>
              </a-tooltip>

              <a-tag color="#2db7f5" v-if="uploadPath">当前目录: {{ uploadPath || '' }}</a-tag>
              <div>文件名栏支持右键菜单</div>
            </a-space>
          </template>

          <template #bodyCell="{ column, text, record, index }">
            <template v-if="column.dataIndex === 'filename'">
              <a-tooltip placement="topLeft" :title="text">
                <a-dropdown :trigger="['contextmenu']">
                  <div>{{ text }}</div>
                  <template v-slot:overlay>
                    <a-menu>
                      <a-menu-item key="1">
                        <a-button @click="goReadFile(record)" :disabled="!record.textFileEdit" type="link">
                          <BarsOutlined /> 跟踪文件
                        </a-button>
                      </a-menu-item>
                      <a-menu-item key="2">
                        <a-button @click="handleRenameFile(record)" type="link"> <HighlightOutlined />重命名 </a-button>
                      </a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
              </a-tooltip>
            </template>
            <template v-else-if="column.dataIndex === 'isDirectory'">
              <span>{{ text ? '目录' : '文件' }}</span>
            </template>
            <template v-else-if="column.dataIndex === 'fileSizeLong'">
              <a-tooltip placement="topLeft" :title="`${text ? renderSize(text) : item.fileSize}`">
                {{ text ? renderSize(text) : item.fileSize }}
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
                  <a-tooltip title="目录不能编辑">
                    <a-button size="small" type="primary" :disabled="true">编辑</a-button>
                  </a-tooltip>
                  <a-tooltip title="不能下载目录">
                    <a-button size="small" type="primary" :disabled="true">下载</a-button>
                  </a-tooltip>
                </template>
                <template v-else>
                  <a-tooltip title="需要到 节点管理中的【插件端配置】的授权配置中配置允许编辑的文件后缀">
                    <a-button
                      size="small"
                      type="primary"
                      :loading="editLoading"
                      :disabled="!record.textFileEdit"
                      @click="handleEditFile(record)"
                      >编辑</a-button
                    >
                  </a-tooltip>
                  <a-button size="small" type="primary" @click="handleDownload(record)">下载</a-button>
                </template>
                <a-button size="small" type="primary" danger @click="handleDelete(record)">删除</a-button>
              </a-space>
            </template>
          </template>
        </a-table>
        <!-- 批量上传文件 -->
        <a-modal
          destroyOnClose
          v-model:open="uploadFileVisible"
          :closable="!uploading"
          :keyboard="false"
          width="35vw"
          title="上传项目文件"
          :footer="null"
          :maskClosable="false"
        >
          <a-space direction="vertical" size="large" style="width: 100%">
            <a-upload
              :file-list="uploadFileList"
              @remove="
                (file) => {
                  const index = this.uploadFileList.indexOf(file)
                  //const newFileList = this.uploadFileList.slice();

                  this.uploadFileList.splice(index, 1)
                  return true
                }
              "
              :before-upload="
                (file) => {
                  this.uploadFileList = [...this.uploadFileList, file]
                  return false
                }
              "
              multiple
              :disabled="!!percentage"
            >
              <template v-if="percentage">
                <LoadingOutlined v-if="this.uploadFileList.length" />
                <span v-else>-</span>
              </template>

              <a-button v-else><UploadOutlined />选择文件</a-button>
            </a-upload>

            <a-row v-if="percentage">
              <a-col span="24">
                <a-progress :percent="percentage" class="max-progress">
                  <template #format="percent">
                    {{ percent }}%<template v-if="percentageInfo.total">
                      ({{ renderSize(percentageInfo.total) }})
                    </template>
                    <template v-if="percentageInfo.duration">
                      当前文件用时:{{ formatDuration(percentageInfo.duration) }}
                    </template>
                    <template v-if="uploadFileList.length">
                      完成 {{ successSize }} 个 / 共{{ uploadFileList.length }}个
                    </template>
                  </template>
                </a-progress>
              </a-col>
            </a-row>

            <a-button type="primary" :disabled="fileUploadDisabled" @click="startUpload">开始上传</a-button>
          </a-space>
        </a-modal>
        <!-- 上传压缩文件 -->
        <a-modal
          destroyOnClose
          v-model:open="uploadZipFileVisible"
          :closable="!uploading"
          :keyboard="false"
          width="35vw"
          title="上传压缩文件"
          :footer="null"
          :maskClosable="false"
        >
          <a-space direction="vertical" size="large" style="width: 100%">
            <a-upload
              :file-list="uploadFileList"
              @remove="
                () => {
                  this.uploadFileList = []
                  return true
                }
              "
              :disabled="!!percentage"
              :before-upload="
                (file) => {
                  this.uploadFileList = [file]
                  return false
                }
              "
              :accept="ZIP_ACCEPT"
            >
              <LoadingOutlined v-if="percentage" />
              <a-button v-else><UploadOutlined />选择压缩文件</a-button>
            </a-upload>
            <a-row v-if="percentage">
              <a-col span="24">
                <a-progress :percent="percentage" class="max-progress">
                  <template #format="percent">
                    {{ percent }}%<template v-if="percentageInfo.total">
                      ({{ renderSize(percentageInfo.total) }})
                    </template>
                    <template v-if="percentageInfo.duration">
                      当前文件用时:{{ formatDuration(percentageInfo.duration) }}
                    </template>
                    <template v-if="uploadFileList.length">
                      完成 {{ successSize }} 个 / 共{{ uploadFileList.length }}个
                    </template>
                  </template>
                </a-progress>
              </a-col>
            </a-row>

            <a-switch
              v-model:checked="uploadData.checkBox"
              checked-children="清空覆盖"
              un-checked-children="不清空"
              style="margin-bottom: 10px"
            />

            <a-input-number
              style="width: 100%"
              v-model:value="uploadData.stripComponents"
              :min="0"
              placeholder="解压时候自动剔除压缩包里面多余的文件夹名"
            />

            <a-button type="primary" :disabled="fileUploadDisabled" @click="startZipUpload">开始上传</a-button>
          </a-space>
        </a-modal>

        <a-modal
          destroyOnClose
          v-model:open="editFileVisible"
          width="80vw"
          :title="`编辑文件 ${filename}`"
          :maskClosable="true"
          @cancel="handleCloseModal"
        >
          <code-editor
            height="60vh"
            showTool
            v-if="editFileVisible"
            v-model:content="fileContent"
            :fileSuffix="filename"
          >
            <template #tool_before>
              <a-tag>
                {{ filename }}
                <!-- {{ temp.name }} -->
              </a-tag>
            </template>
          </code-editor>

          <template v-slot:footer>
            <a-button @click="handleCloseModal"> 关闭 </a-button>
            <a-button type="primary" @click="updateFileData"> 保存 </a-button>
            <a-button
              type="primary"
              @click="
                () => {
                  updateFileData()
                  handleCloseModal()
                }
              "
            >
              保存并关闭
            </a-button>
          </template>
        </a-modal>
        <!--远程下载  -->
        <a-modal
          destroyOnClose
          :confirmLoading="confirmLoading"
          v-model:open="uploadRemoteFileVisible"
          title="远程下载文件"
          @ok="handleRemoteUpload"
          @cancel="closeRemoteUpload"
          :maskClosable="false"
        >
          <a-form
            :model="remoteDownloadData"
            :label-col="{ span: 6 }"
            :wrapper-col="{ span: 18 }"
            :rules="rules"
            ref="ruleForm"
          >
            <a-form-item label="远程下载URL" name="url">
              <a-input v-model:value="remoteDownloadData.url" placeholder="远程下载地址" />
            </a-form-item>
            <a-form-item label="是否为压缩包">
              <a-switch v-model:checked="remoteDownloadData.unzip" checked-children="是" un-checked-children="否" />
            </a-form-item>
            <a-form-item label="剔除文件夹" v-if="remoteDownloadData.unzip">
              <a-input-number
                style="width: 100%"
                v-model:value="remoteDownloadData.stripComponents"
                :min="0"
                placeholder="解压时候自动剔除压缩包里面多余的文件夹名"
              />
            </a-form-item>
          </a-form>
        </a-modal>
        <!-- 创建文件/文件夹 -->
        <a-modal
          destroyOnClose
          v-model:open="addFileFolderVisible"
          width="300px"
          :title="addFileOrFolderType === 1 ? '新增目录' : '新建文件'"
          :footer="null"
          :maskClosable="true"
        >
          <a-space direction="vertical" style="width: 100%">
            <span v-if="uploadPath">当前目录:{{ uploadPath }}</span>
            <!-- <a-tag v-if="">目录创建成功后需要手动刷新右边树才能显示出来哟</a-tag> -->
            <a-tooltip :title="this.addFileOrFolderType === 1 ? '目录创建成功后需要手动刷新右边树才能显示出来哟' : ''">
              <a-input v-model:value="fileFolderName" placeholder="输入文件或者文件夹名" />
            </a-tooltip>
            <a-row type="flex" justify="center">
              <a-button type="primary" :disabled="fileFolderName.length === 0" @click="startAddFileFolder"
                >确认</a-button
              >
            </a-row>
          </a-space>
        </a-modal>
        <!-- 从命名文件/文件夹 -->
        <a-modal
          destroyOnClose
          v-model:open="renameFileFolderVisible"
          width="300px"
          :title="`重命名`"
          :footer="null"
          :maskClosable="true"
        >
          <a-space direction="vertical" style="width: 100%">
            <a-input v-model:value="fileFolderName" placeholder="输入新名称" />

            <a-row type="flex" justify="center">
              <a-button type="primary" :disabled="fileFolderName.length === 0" @click="renameFileFolder">确认</a-button>
            </a-row>
          </a-space>
        </a-modal>
      </a-layout-content>
    </a-layout>
    <!-- 查看备份列表 -->
    <a-modal
      destroyOnClose
      v-model:open="backupListVisible"
      width="80vw"
      height="80vh"
      title="备份列表"
      :footer="null"
      :maskClosable="true"
      @cancel="
        () => {
          loadData()
        }
      "
    >
      <projectFileBackup v-if="backupListVisible" :nodeId="this.nodeId" :projectId="this.projectId"></projectFileBackup>
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
  inject: ['globalLoading'],
  components: {
    codeEditor,
    projectFileBackup
  },
  props: {
    nodeId: {
      type: String
    },
    projectId: {
      type: String
    },
    runMode: {
      type: String
    },
    absPath: {
      type: String
    },
    showConsole: {
      type: Boolean,
      default: true
    }
  },
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
          title: '文件名称',
          dataIndex: 'filename',
          ellipsis: true
        },
        {
          title: '文件类型',
          dataIndex: 'isDirectory',
          width: '100px',
          ellipsis: true
        },
        {
          title: '文件大小',
          dataIndex: 'fileSizeLong',
          width: 120,
          ellipsis: true,

          sorter: (a, b) => a.fileSizeLong - b.fileSizeLong
        },
        {
          title: '修改时间',
          dataIndex: 'modifyTimeLong',
          width: '180px',
          ellipsis: true,

          sorter: (a, b) => a.modifyTimeLong - b.modifyTimeLong
        },
        {
          title: '操作',
          dataIndex: 'operation',
          width: '180px',
          align: 'center',
          fixed: 'right'
        }
      ],
      rules: {
        url: [{ required: true, message: '远程下载Url不为空', trigger: 'change' }]
      },
      addFileFolderVisible: false,
      // 目录1 文件2 标识
      addFileOrFolderType: 1,
      fileFolderName: '',
      oldFileFolderName: '',
      renameFileFolderVisible: false,
      editLoading: false
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
    renderSize,
    uploadPieces,
    formatDuration,
    parseTime,
    onTreeData(treeNode) {
      return new Promise((resolve) => {
        if (treeNode.dataRef.children || !treeNode.dataRef.isDirectory) {
          resolve()
          return
        }
        this.loadNode(treeNode.dataRef, resolve)
      })
    },
    // 加载数据
    loadData() {
      const key = 'root-' + new Date().getTime()
      this.treeList = [
        {
          filename: '目录：' + (this.absPath || ''),
          level: 1,
          isDirectory: true,
          key: key,
          isLeaf: false
        }
      ]
      // 设置默认展开第一个
      setTimeout(() => {
        const node = this.treeList[0]
        this.tempNode = node
        this.expandKeys = [key]
        this.loadFileList()
      }, 1000)
    },
    // 加载子节点
    loadNode(data, resolve) {
      this.tempNode = data
      // 如果是目录
      if (data.isDirectory) {
        setTimeout(() => {
          // 请求参数
          const params = {
            nodeId: this.nodeId,
            id: this.projectId,
            path: this.uploadPath
          }
          // if (node.level === 1) {
          //   params.path = ''
          // } else {
          //   params.path = data.levelName || '' + '/' + data.filename
          // }
          // 加载文件
          getFileList(params).then((res) => {
            if (res.code === 200) {
              const treeData = res.data
                .filter((ele) => {
                  return ele.isDirectory
                })
                .map((ele) => {
                  ele.isLeaf = !ele.isDirectory
                  ele.key = ele.filename + '-' + new Date().getTime()
                  return ele
                })
              data.children = treeData

              this.treeList = [...this.treeList]
              resolve()
            } else {
              resolve()
            }
          })
        }, 500)
      } else {
        resolve()
      }
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
        this.loadFileList()
      }
    },
    // 上传文件
    handleUpload() {
      if (Object.keys(this.tempNode).length === 0) {
        $notification.error({
          message: '请选择一个节点'
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
          message: '请选择一个节点'
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
      this.$refs.ruleForm.validate((valid) => {
        if (valid) {
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
        } else {
          return false
        }
      })
    },
    // 加载文件列表
    loadFileList() {
      if (Object.keys(this.tempNode).length === 0) {
        $notification.warn({
          message: '请选择一个节点'
        })
        return false
      }
      // 请求参数
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        path: this.uploadPath
      }
      this.fileList = []
      this.loading = true
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
        }
        this.loading = false
      })
    },
    // 清空文件
    clearFile() {
      const msg = this.uploadPath
        ? '真的要清空 【' + this.uploadPath + '】目录和文件么？'
        : '真的要清空项目目录和文件么？'
      const that = this
      this.$confirm({
        title: '系统提示',
        content: msg,
        okText: '确认',
        zIndex: 1009,
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            // 请求参数
            const params = {
              nodeId: that.nodeId,
              id: that.projectId,
              type: 'clear',
              levelName: that.uploadPath
            }
            // 删除
            deleteProjectFile(params)
              .then((res) => {
                if (res.code === 200) {
                  $notification.success({
                    message: res.msg
                  })
                  that.loadFileList()
                }
                resolve()
              })
              .catch(reject)
          })
        }
      })
    },
    // 下载
    handleDownload(record) {
      $notification.info({
        message: '正在下载，请稍等...'
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
        ? '真的要删除【' + record.filename + '】文件夹么？'
        : '真的要删除【' + record.filename + '】文件么？'
      const that = this
      this.$confirm({
        title: '系统提示',
        content: msg,
        okText: '确认',
        zIndex: 1009,
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            // 请求参数
            const params = {
              nodeId: that.nodeId,
              id: that.projectId,
              levelName: record.levelName,
              filename: record.filename
            }
            // 删除
            deleteProjectFile(params)
              .then((res) => {
                if (res.code === 200) {
                  $notification.success({
                    message: res.msg
                  })
                  that.loadData()
                  // this.loadFileList();
                }
                resolve()
              })
              .catch(reject)
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
          this.loadData()
          // this.loadFileList();
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
  },
  emits: ['goReadFile', 'goConsole']
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
