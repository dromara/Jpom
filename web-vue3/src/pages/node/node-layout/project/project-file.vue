<template>
  <div>
    <!-- 布局 -->
    <a-layout class="file-layout node-full-content">
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
          :replace-fields="treeReplaceFields"
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
          :rowKey="(record, index) => index"
        >
          <template #title>
            <!-- <a-tag color="#2db7f5">项目目录: {{ absPath }}</a-tag>-->
            <a-space>
              <a-dropdown :disabled="!Object.keys(this.tempNode).length">
                <a-button size="small" type="primary" @click="(e) => e.preventDefault()"
                  ><a-icon type="upload" />上传</a-button
                >
                <template #overlay>
                  <a-menu>
                    <a-menu-item @click="handleUpload">
                      <a-space><a-icon type="file" />上传文件</a-space>
                    </a-menu-item>
                    <a-menu-item @click="handleZipUpload">
                      <a-space><a-icon type="file-zip" />上传压缩包并自动解压</a-space>
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
              <a-dropdown :disabled="!Object.keys(this.tempNode).length">
                <a-button size="small" type="primary" @click="(e) => e.preventDefault()">新建</a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item @click="handleAddFile(1)">
                      <a-space>
                        <a-icon type="folder-add" />
                        <a-space>新建目录</a-space>
                      </a-space>
                    </a-menu-item>
                    <a-menu-item @click="handleAddFile(2)">
                      <a-space>
                        <a-icon type="file-add" />
                        <a-space>新建空白文件</a-space>
                      </a-space>
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
              <a-tooltip
                title="通过 URL 下载远程文件到项目文件夹,需要到节点系统配置->白名单配置中配置允许的 HOST 白名单"
              >
                <a-button size="small" type="primary" @click="openRemoteUpload"
                  ><a-icon type="cloud-download"
                /></a-button>
              </a-tooltip>
              <a-tooltip title="刷新文件表格">
                <a-button size="small" type="primary" @click="loadFileList"><a-icon type="reload" /></a-button>
              </a-tooltip>
              <a-tooltip title="清空当前目录文件">
                <a-button size="small" type="danger" @click="clearFile"><a-icon type="delete" /></a-button>
              </a-tooltip>

              <a-tag color="#2db7f5" v-if="uploadPath">当前目录: {{ uploadPath || '' }}</a-tag>
              <div>文件名栏支持右键菜单</div>
            </a-space>
          </template>
          <a-tooltip #filename slot-scope="text, record" placement="topLeft" :title="text">
            <a-dropdown :trigger="['contextmenu']">
              <div>{{ text }}</div>
              <template #overlay>
                <a-menu>
                  <a-menu-item key="1">
                    <a-button icon="bars" @click="goReadFile(record)" :disabled="!record.textFileEdit" type="link">
                      跟踪文件
                    </a-button>
                  </a-menu-item>
                  <a-menu-item key="2">
                    <a-button icon="highlight" @click="handleRenameFile(record)" type="link"> 重命名 </a-button>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-tooltip>
          <template #isDirectory slot-scope="text">
            <span>{{ text ? '目录' : '文件' }}</span>
          </template>
          <a-tooltip
            #fileSizeLong
            slot-scope="text, item"
            placement="topLeft"
            :title="`${text ? renderSize(text) : item.fileSize}`"
          >
            <template v-if="text">
              {{ renderSize(text) }}
            </template>
            <span v-else>{{ item.fileSize }}</span>
          </a-tooltip>
          <a-tooltip #modifyTimeLong slot-scope="text, record" :title="`${parseTime(record.modifyTimeLong)}}`">
            <span>{{ parseTime(record.modifyTimeLong) }}</span>
          </a-tooltip>
          <template #operation slot-scope="text, record">
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
                <a-tooltip title="需要到 节点管理中的【插件端配置】的白名单配置中配置允许编辑的文件后缀">
                  <a-button size="small" type="primary" :disabled="!record.textFileEdit" @click="handleEditFile(record)"
                    >编辑</a-button
                  >
                </a-tooltip>
                <a-button size="small" type="primary" @click="handleDownload(record)">下载</a-button>
              </template>
              <a-button size="small" type="danger" @click="handleDelete(record)">删除</a-button>
            </a-space>
          </template>
        </a-table>
        <!-- 批量上传文件 -->
        <a-modal
          destroyOnClose
          v-model="uploadFileVisible"
          :closable="!uploading"
          :keyboard="false"
          width="35vw"
          title="上传项目文件"
          :footer="null"
          :maskClosable="false"
        >
          <a-space direction="vertical" style="display: block" size="large">
            <a-upload
              :file-list="uploadFileList"
              :remove="
                (file) => {
                  const index = this.uploadFileList.indexOf(file)
                  //const newFileList = this.uploadFileList.slice();

                  this.uploadFileList.splice(index, 1)
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
                <a-icon type="loading" v-if="uploadFileList.length" /><span v-else>-</span>
              </template>

              <a-button v-else icon="upload">选择文件</a-button>
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
          v-model="uploadZipFileVisible"
          :closable="!uploading"
          :keyboard="false"
          width="35vw"
          title="上传压缩文件"
          :footer="null"
          :maskClosable="false"
        >
          <a-space direction="vertical" style="display: block" size="large">
            <a-upload
              :file-list="uploadFileList"
              :remove="
                () => {
                  this.uploadFileList = []
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
              <a-icon type="loading" v-if="percentage" />
              <a-button v-else icon="upload">选择压缩文件</a-button>
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
              v-model="uploadData.checkBox"
              checked-children="清空覆盖"
              un-checked-children="不清空"
              style="margin-bottom: 10px"
            />

            <a-input-number
              style="width: 100%"
              v-model="uploadData.stripComponents"
              :min="0"
              placeholder="解压时候自动剔除压缩包里面多余的文件夹名"
            />

            <a-button type="primary" :disabled="fileUploadDisabled" @click="startZipUpload">开始上传</a-button>
          </a-space>
        </a-modal>

        <a-modal
          destroyOnClose
          v-model="editFileVisible"
          width="80vw"
          :title="`编辑文件 ${filename}`"
          :maskClosable="true"
          @cancel="handleCloseModal"
        >
          <div style="height: 60vh">
            <code-editor showTool v-if="editFileVisible" v-model="fileContent" :fileSuffix="filename"></code-editor>
          </div>

          <template #footer>
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
          v-model="uploadRemoteFileVisible"
          title="远程下载文件"
          @ok="handleRemoteUpload"
          @cancel="openRemoteUpload"
          :maskClosable="false"
        >
          <a-form
            :model="remoteDownloadData"
            :label-col="{ span: 6 }"
            :wrapper-col="{ span: 18 }"
            :rules="rules"
            ref="ruleForm"
          >
            <a-form-item label="远程下载URL" prop="url">
              <a-input v-model="remoteDownloadData.url" placeholder="远程下载地址" />
            </a-form-item>
            <a-form-item label="是否为压缩包">
              <a-switch
                v-model="remoteDownloadData.unzip"
                checked-children="是"
                un-checked-children="否"
                v-decorator="['unzip', { valuePropName: 'checked' }]"
              />
            </a-form-item>
            <a-form-item label="剔除文件夹" v-if="remoteDownloadData.unzip">
              <a-input-number
                style="width: 100%"
                v-model="remoteDownloadData.stripComponents"
                :min="0"
                placeholder="解压时候自动剔除压缩包里面多余的文件夹名"
              />
            </a-form-item>
          </a-form>
        </a-modal>
        <!-- 创建文件/文件夹 -->
        <a-modal
          destroyOnClose
          v-model="addFileFolderVisible"
          width="300px"
          :title="addFileOrFolderType === 1 ? '新增目录' : '新建文件'"
          :footer="null"
          :maskClosable="true"
        >
          <a-space direction="vertical" style="width: 100%">
            <span v-if="uploadPath">当前目录:{{ uploadPath }}</span>
            <!-- <a-tag v-if="">目录创建成功后需要手动刷新右边树才能显示出来哟</a-tag> -->
            <a-tooltip :title="addFileOrFolderType === 1 ? '目录创建成功后需要手动刷新右边树才能显示出来哟' : ''">
              <a-input v-model="fileFolderName" placeholder="输入文件或者文件夹名" />
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
          v-model="renameFileFolderVisible"
          width="300px"
          :title="`重命名`"
          :footer="null"
          :maskClosable="true"
        >
          <a-space direction="vertical" style="width: 100%">
            <a-input v-model="fileFolderName" placeholder="输入新名称" />

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
      v-model="backupListVisible"
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
      <projectFileBackup v-if="backupListVisible" :nodeId="nodeId" :projectId="projectId"></projectFileBackup>
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
      ZIP_ACCEPT: ZIP_ACCEPT,
      noFileModes: noFileModes,
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
        { title: '文件名称', dataIndex: 'filename', ellipsis: true, scopedSlots: { customRender: 'filename' } },
        {
          title: '文件类型',
          dataIndex: 'isDirectory',
          width: '100px',
          ellipsis: true,
          scopedSlots: { customRender: 'isDirectory' }
        },
        {
          title: '文件大小',
          dataIndex: 'fileSizeLong',
          width: 120,
          ellipsis: true,
          scopedSlots: { customRender: 'fileSizeLong' },
          sorter: (a, b) => a.fileSizeLong - b.fileSizeLong
        },
        {
          title: '修改时间',
          dataIndex: 'modifyTimeLong',
          width: '180px',
          ellipsis: true,
          scopedSlots: { customRender: 'modifyTimeLong' },
          sorter: (a, b) => a.modifyTimeLong - b.modifyTimeLong
        },
        {
          title: '操作',
          dataIndex: 'operation',
          width: '180px',
          align: 'center',
          scopedSlots: { customRender: 'operation' }
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
      renameFileFolderVisible: false
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
      // 读取文件数据
      readFile(params).then((res) => {
        if (res.code === 200) {
          this.editFileVisible = true
          this.filename = record.filename
          setTimeout(() => {
            this.fileContent = res.data
          }, 300)
        }
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
          remoteDownload(params).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.remoteDownloadData = {}
              this.uploadRemoteFileVisible = false
              this.loadFileList()
            }
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
      $confirm({
        title: '系统提示',
        content: msg,
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 请求参数
          const params = {
            nodeId: this.nodeId,
            id: this.projectId,
            type: 'clear',
            levelName: this.uploadPath
          }
          // 删除
          deleteProjectFile(params).then((res) => {
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
      $confirm({
        title: '系统提示',
        content: msg,
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 请求参数
          const params = {
            nodeId: this.nodeId,
            id: this.projectId,
            levelName: record.levelName,
            filename: record.filename
          }
          // 删除
          deleteProjectFile(params).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.loadData()
              // this.loadFileList();
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
  }
}
</script>

<style scoped>
/deep/ .ant-progress-text {
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
  background-color: #fff;
}
</style>
