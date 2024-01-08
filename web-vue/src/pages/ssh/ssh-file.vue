<template>
  <!-- 布局 -->
  <a-layout class="ssh-file-layout">
    <!-- 目录树 -->
    <a-layout-sider theme="light" class="sider" width="25%">
      <a-row class="dir-container">
        <a-space>
          <a-button size="small" type="primary" @click="loadData()">刷新</a-button>
          <a-dropdown>
            <template v-slot:overlay>
              <a-menu>
                <a-menu-item
                  @click="
                    () => {
                      changeSort(item.key, sortMethod.asc)
                    }
                  "
                  v-for="item in sortMethodList"
                  :key="item.key"
                  >{{ item.name }}</a-menu-item
                >
              </a-menu>
            </template>

            <a-button
              size="small"
              type="primary"
              @click="
                () => {
                  changeSort(sortMethod.key, !sortMethod.asc)
                }
              "
            >
              {{
                sortMethodList.find((item) => {
                  return item.key === sortMethod.key
                }) &&
                sortMethodList.find((item) => {
                  return item.key === sortMethod.key
                }).name
              }}排序
              <SortAscendingOutlined v-if="sortMethod.asc" />
              <SortDescendingOutlined v-else />
            </a-button>
          </a-dropdown>
        </a-space>
      </a-row>
      <a-empty :image="Empty.PRESENTED_IMAGE_SIMPLE" v-if="treeList.length === 0" />
      <a-directory-tree :treeData="treeList" :fieldNames="replaceFields" @select="onSelect"> </a-directory-tree>
    </a-layout-sider>
    <!-- 表格 -->
    <a-layout-content class="file-content">
      <!-- <div ref="filter" class="filter"></div> -->
      <a-table
        size="middle"
        :data-source="fileList"
        :loading="loading"
        :columns="columns"
        :pagination="false"
        bordered
        :scroll="{
          x: 'max-content'
        }"
      >
        <template v-slot:title>
          <a-space>
            <a-dropdown :disabled="!this.tempNode.nextPath">
              <a-button size="small" type="primary" @click="(e) => e.preventDefault()">上传小文件</a-button>
              <template v-slot:overlay>
                <a-menu>
                  <a-menu-item @click="handleUpload">
                    <a-space><FileAddOutlined />上传文件</a-space>
                  </a-menu-item>
                  <a-menu-item @click="handleUploadZip">
                    <a-space><FileZipOutlined />上传压缩文件（自动解压）</a-space>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
            <a-dropdown :disabled="!this.tempNode.nextPath">
              <a-button size="small" type="primary" @click="(e) => e.preventDefault()">新建</a-button>
              <template v-slot:overlay>
                <a-menu>
                  <a-menu-item @click="handleAddFolder">
                    <a-space>
                      <FolderAddOutlined />
                      <a-space>新建目录</a-space>
                    </a-space>
                  </a-menu-item>
                  <a-menu-item @click="handleAddFile">
                    <a-space>
                      <FileAddOutlined />
                      <a-space>新建空白文件</a-space>
                    </a-space>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
            <a-button size="small" :disabled="!this.tempNode.nextPath" type="primary" @click="loadFileList()"
              >刷新</a-button
            >
            <a-button size="small" :disabled="!this.tempNode.nextPath" type="primary" danger @click="handleDeletePath()"
              >删除</a-button
            >
            <div>
              文件夹：
              <a-switch
                :disabled="!this.tempNode.nextPath"
                @change="changeListShowDir"
                checked-children="显示"
                un-checked-children="隐藏"
                v-model:checked="listShowDir"
              />
            </div>
            <span v-if="this.nowPath">当前目录:{{ this.nowPath }}</span>
            <!-- <span v-if="this.nowPath">{{ this.tempNode.parentDir }}</span> -->
          </a-space>
        </template>

        <template #bodyCell="{ column, text, record }">
          <template v-if="column.dataIndex === 'name'">
            <a-tooltip placement="topLeft" :title="` 名称：${text} 长名称：${record.longname}`">
              <a-dropdown :trigger="['contextmenu']">
                <div>{{ text }}</div>
                <template v-slot:overlay>
                  <a-menu>
                    <a-menu-item key="2">
                      <a-button icon="highlight" @click="handleRenameFile(record)" type="link"> 重命名 </a-button>
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>

              <!-- <span>{{ text }}</span> -->
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'dir'">
            <a-tooltip placement="topLeft" :title="`${record.link ? '链接' : text ? '目录' : '文件'}`">
              <span>{{ record.link ? '链接' : text ? '目录' : '文件' }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'size'">
            <a-tooltip placement="topLeft" :title="renderSize(text)">
              <span>{{ renderSize(text) }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.tooltip">
            <a-tooltip placement="topLeft" :title="text">
              <span>{{ text }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'operation'">
            <a-space>
              <a-tooltip title="需要到 ssh 信息中配置允许编辑的文件后缀">
                <a-button size="small" type="primary" :disabled="!record.textFileEdit" @click="handleEdit(record)"
                  >编辑</a-button
                >
              </a-tooltip>
              <a-tooltip title="修改文件权限">
                <a-button size="small" type="primary" @click="handleFilePermission(record)">权限</a-button>
              </a-tooltip>
              <a-button size="small" type="primary" :disabled="record.dir" @click="handleDownload(record)"
                >下载</a-button
              >
              <a-button size="small" type="primary" danger @click="handleDelete(record)">删除</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
      <!-- 上传文件 -->
      <a-modal
        destroyOnClose
        @cancel="closeUploadFile"
        v-model:open="uploadFileVisible"
        width="300px"
        title="上传文件"
        :confirmLoading="confirmLoading"
        :footer="null"
        :maskClosable="true"
      >
        <a-upload
          :file-list="uploadFileList"
          @remove="handleRemove"
          :before-upload="beforeUpload"
          :accept="`${uploadFileZip ? ZIP_ACCEPT : ''}`"
          :multiple="!uploadFileZip"
        >
          <a-button>
            <UploadOutlined />
            选择文件
            {{ uploadFileZip ? '压缩包' : '' }}
          </a-button>
        </a-upload>
        <br />
        <a-button type="primary" :disabled="uploadFileList.length === 0" :loading="confirmLoading" @click="startUpload"
          >开始上传</a-button
        >
      </a-modal>
      <!--  新增文件 目录    -->
      <a-modal
        v-model:open="addFileFolderVisible"
        width="300px"
        :title="temp.addFileOrFolderType === 1 ? '新增目录' : '新建文件'"
        :footer="null"
        :maskClosable="true"
      >
        <a-space direction="vertical" style="width: 100%">
          <span v-if="this.nowPath">当前目录:{{ this.nowPath }}</span>
          <!-- <a-tag v-if="">目录创建成功后需要手动刷新右边树才能显示出来哟</a-tag> -->
          <a-tooltip
            :title="this.temp.addFileOrFolderType === 1 ? '目录创建成功后需要手动刷新右边树才能显示出来哟' : ''"
          >
            <a-input v-model:value="temp.fileFolderName" placeholder="输入文件或者文件夹名" />
          </a-tooltip>
          <a-row type="flex" justify="center">
            <a-button
              type="primary"
              :disabled="!temp.fileFolderName || temp.fileFolderName.length === 0"
              @click="startAddFileFolder"
              >确认</a-button
            >
          </a-row>
        </a-space>
      </a-modal>

      <a-modal
        destroyOnClose
        :confirmLoading="confirmLoading"
        v-model:open="editFileVisible"
        width="80vw"
        title="编辑文件"
        cancelText="关闭"
        :maskClosable="true"
        @ok="updateFileData"
      >
        <div style="height: 60vh">
          <code-editor showTool v-model:content="temp.fileContent" :fileSuffix="temp.name"></code-editor>
        </div>
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
          <a-input v-model:value="temp.fileFolderName" placeholder="输入新名称" />

          <a-row type="flex" justify="center" v-if="temp.fileFolderName">
            <a-button
              :loading="confirmLoading"
              type="primary"
              :disabled="temp.fileFolderName.length === 0 || temp.fileFolderName === temp.oldFileFolderName"
              @click="renameFileFolder"
              >确认</a-button
            >
          </a-row>
        </a-space>
      </a-modal>

      <!-- 修改文件权限 -->
      <a-modal
        destroyOnClose
        v-model:open="editFilePermissionVisible"
        width="400px"
        :title="`修改文件权限`"
        :footer="null"
        :maskClosable="true"
      >
        <a-row>
          <a-col :span="6"><span class="title">权限</span></a-col>
          <a-col :span="6"><span class="title">所属用户</span></a-col>
          <a-col :span="6"><span class="title">用户组</span></a-col>
          <a-col :span="6"><span class="title">其他</span></a-col>
        </a-row>
        <a-row>
          <a-col :span="6">
            <span>读</span>
          </a-col>
          <a-col :span="6">
            <a-checkbox v-model:checked="permissions.owner.read" />
          </a-col>
          <a-col :span="6">
            <a-checkbox v-model:checked="permissions.group.read" />
          </a-col>
          <a-col :span="6">
            <a-checkbox v-model:checked="permissions.others.read" />
          </a-col>
        </a-row>
        <a-row>
          <a-col :span="6">
            <span>写</span>
          </a-col>
          <a-col :span="6">
            <a-checkbox v-model:checked="permissions.owner.write" />
          </a-col>
          <a-col :span="6">
            <a-checkbox v-model:checked="permissions.group.write" />
          </a-col>
          <a-col :span="6">
            <a-checkbox v-model:checked="permissions.others.write" />
          </a-col>
        </a-row>
        <a-row>
          <a-col :span="6">
            <span>执行</span>
          </a-col>
          <a-col :span="6">
            <a-checkbox v-model:checked="permissions.owner.execute" />
          </a-col>
          <a-col :span="6">
            <a-checkbox v-model:checked="permissions.group.execute" />
          </a-col>
          <a-col :span="6">
            <a-checkbox v-model:checked="permissions.others.execute" />
          </a-col>
        </a-row>
        <a-row type="flex" style="margin-top: 20px">
          <a-button type="primary" @click="updateFilePermissions">确认修改</a-button>
        </a-row>
        <!-- <a-row>
            <a-alert style="margin-top: 20px" :message="permissionTips" type="success" />
          </a-row> -->
      </a-modal>
    </a-layout-content>
  </a-layout>
</template>

<script>
import {
  deleteFile,
  downloadFile,
  getFileList,
  getRootFileList,
  newFileFolder,
  readFile,
  renameFileFolder,
  updateFileData,
  uploadFile,
  parsePermissions,
  calcFilePermissionValue,
  changeFilePermission
} from '@/api/ssh-file'

import codeEditor from '@/components/codeEditor'
import { ZIP_ACCEPT, renderSize, parseTime } from '@/utils/const'
import { Empty } from 'ant-design-vue'
export default {
  props: {
    sshId: {
      type: String,
      default: ''
    },
    machineSshId: {
      type: String,
      default: ''
    }
  },
  components: {
    codeEditor
  },
  data() {
    return {
      Empty,
      loading: false,
      treeList: [],
      fileList: [],
      uploadFileList: [],
      tempNode: {},
      temp: {},
      uploadFileVisible: false,
      uploadFileZip: false,
      ZIP_ACCEPT: ZIP_ACCEPT,
      renameFileFolderVisible: false,
      listShowDir: false,
      tableHeight: '80vh',
      replaceFields: {
        children: 'children',
        title: 'name',
        key: 'key'
      },
      columns: [
        {
          title: '文件名称',
          dataIndex: 'name',
          width: 200,
          ellipsis: true,

          sorter: (a, b) => (a.name || '').localeCompare(b.name || '')
        },
        {
          title: '文件类型',
          dataIndex: 'dir',
          width: '100px',
          ellipsis: true
        },
        {
          title: '文件大小',
          dataIndex: 'size',
          width: 120,
          ellipsis: true,

          sorter: (a, b) => Number(a.size) - new Number(b.size)
        },
        {
          title: '权限',
          dataIndex: 'permissions',
          width: 120,
          ellipsis: true,
          tooltip: true
        },
        {
          title: '修改时间',
          dataIndex: 'modifyTime',
          width: '170px',
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          sorter: (a, b) => Number(a.modifyTime) - new Number(b.modifyTime)
        },
        {
          title: '操作',
          dataIndex: 'operation',
          align: 'center',
          fixed: 'right',

          width: '220px'
        }
      ],
      editFileVisible: false,
      addFileFolderVisible: false,
      editFilePermissionVisible: false,
      permissions: {
        owner: { read: false, write: false, execute: false },
        group: { read: false, write: false, execute: false },
        others: { read: false, write: false, execute: false }
      },
      // permissionTips: "",
      sortMethodList: [
        {
          name: '文件名',
          key: 'name'
        },
        {
          name: '修改时间',
          key: 'modifyTime'
        }
      ],
      sortMethod: {
        key: 'name',
        asc: true
      },
      confirmLoading: false
    }
  },
  mounted() {
    this.listShowDir = Boolean(localStorage.getItem('ssh-list-show-dir'))
    try {
      this.sortMethod = JSON.parse(localStorage.getItem('ssh-list-sort') || JSON.stringify(this.sortMethod))
    } catch (e) {
      console.error(e)
    }
    this.loadData()
  },
  computed: {
    nowPath() {
      if (!this.tempNode.allowPathParent) {
        return ''
      }
      return ((this.tempNode.allowPathParent || '') + '/' + (this.tempNode.nextPath || '')).replace(
        new RegExp('//+', 'gm'),
        '/'
      )
    },
    baseUrl() {
      if (this.sshId) {
        return '/node/ssh/'
      }
      return '/system/assets/ssh-file/'
    },
    reqDataId() {
      return this.sshId || this.machineSshId
    }
  },
  methods: {
    changeSort(key, asc) {
      this.sortMethod = { key: key, asc: asc }
      localStorage.setItem('ssh-list-sort', JSON.stringify(this.sortMethod))
      this.loadData()
    },
    renderSize,
    // 加载数据
    loadData() {
      this.loading = true
      getRootFileList(this.baseUrl, this.reqDataId).then((res) => {
        if (res.code === 200) {
          this.treeList = res.data
            .map((element) => {
              return {
                key: element.id,
                name: element.allowPathParent,
                allowPathParent: element.allowPathParent,
                nextPath: '/',
                isLeaf: false,
                // 配置的授权目录可能不存在
                disabled: !!element.error,
                modifyTime: element.modifyTime
              }
            })
            .sort((a, b) => {
              const aV = a[this.sortMethod.key] || ''
              const bV = b[this.sortMethod.key] || ''
              return this.sortMethod.asc ? bV.localeCompare(aV) : aV.localeCompare(bV)
            })
        }
        this.loading = false
      })
    }, // 选中目录
    onSelect(selectedKeys, { node }) {
      if (node.dataRef.disabled) {
        return
      }
      // console.log(node.dataRef, this.tempNode.key);
      if (node.dataRef.key === this.tempNode.key) {
        return
      }
      this.tempNode = node.dataRef
      // 请求参数
      const params = {
        id: this.reqDataId,
        allowPathParent: node.dataRef.allowPathParent,
        nextPath: node.dataRef.nextPath
      }
      this.fileList = []
      this.loading = true
      // 加载文件
      getFileList(this.baseUrl, params).then((res) => {
        if (res.code === 200) {
          let children = []
          // 区分目录和文件
          res.data.forEach((element) => {
            if (element.dir) {
              if (this.listShowDir) {
                this.fileList.push({
                  // path: node.dataRef.path,
                  ...element
                })
              }
              children.push({
                key: element.id,
                name: element.name,
                allowPathParent: node.dataRef.allowPathParent,
                nextPath: (element.nextPath + '/' + element.name).replace(new RegExp('//+', 'gm'), '/'),
                isLeaf: !element.dir,
                // 可能有错误
                disabled: !!element.error,
                modifyTime: element.modifyTime
              })
            } else {
              // 设置文件表格
              this.fileList.push({
                // path: node.dataRef.path,
                ...element
              })
            }
          })
          // 设置目录树
          node.dataRef.children = children.sort((a, b) => {
            const aV = a[this.sortMethod.key] || ''
            const bV = b[this.sortMethod.key] || ''
            return this.sortMethod.asc ? bV.localeCompare(aV) : aV.localeCompare(bV)
          })
          this.treeList = [...this.treeList]
        }
        this.loading = false
      })
    },
    changeListShowDir() {
      this.loadFileList()
      localStorage.setItem('ssh-list-show-dir', this.listShowDir)
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
        id: this.reqDataId,
        allowPathParent: this.tempNode.allowPathParent,
        nextPath: this.tempNode.nextPath
      }
      // this.fileList = [];
      this.loading = true
      // 加载文件
      getFileList(this.baseUrl, params).then((res) => {
        if (res.code === 200) {
          // 区分目录和文件
          this.fileList = res.data
            .filter((element) => {
              if (this.listShowDir) {
                return true
              }
              return !element.dir
            })
            .map((element) => {
              // 设置文件表格
              return {
                // path: this.tempNode.path,
                ...element
              }
            })
        }
        this.loading = false
      })
    },
    // 上传文件
    handleUpload() {
      if (Object.keys(this.tempNode).length === 0) {
        $notification.error({
          message: '请选择一个节点'
        })
        return
      }
      this.uploadFileVisible = true
      this.uploadFileZip = false
    },
    handleUploadZip() {
      this.handleUpload()
      this.uploadFileZip = true
    },
    handleAddFolder() {
      this.addFileFolderVisible = true
      // 目录1 文件2 标识
      // addFileOrFolderType: 1,
      //       fileFolderName: "",
      this.temp = {
        fileFolderName: '',
        addFileOrFolderType: 1,
        allowPathParent: this.tempNode.allowPathParent,
        nextPath: this.tempNode.nextPath
      }
    },
    handleAddFile() {
      this.addFileFolderVisible = true
      // 目录1 文件2 标识
      // addFileOrFolderType: 1,
      //       fileFolderName: "",
      this.temp = {
        fileFolderName: '',
        addFileOrFolderType: 2,
        allowPathParent: this.tempNode.allowPathParent,
        nextPath: this.tempNode.nextPath
      }
    },
    // closeAddFileFolder() {
    //   this.addFileFolderVisible = false;
    //   this.fileFolderName = "";
    // },
    // 确认新增文件  目录
    startAddFileFolder() {
      const params = {
        id: this.reqDataId,
        allowPathParent: this.temp.allowPathParent,
        nextPath: this.temp.nextPath,
        name: this.temp.fileFolderName,
        unFolder: this.temp.addFileOrFolderType !== 1
      }
      newFileFolder(this.baseUrl, params).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
          this.addFileFolderVisible = false
          this.loadFileList()
          // this.closeAddFileFolder();
        }
      })
    },
    handleRemove(file) {
      const index = this.uploadFileList.indexOf(file)
      const newFileList = this.uploadFileList.slice()
      newFileList.splice(index, 1)
      this.uploadFileList = newFileList
      return true
    },
    beforeUpload(file) {
      this.uploadFileList = [...this.uploadFileList, file]
      return false
    },
    closeUploadFile() {
      this.uploadFileList = []
    },
    // 开始上传文件
    startUpload() {
      this.uploadFileList.forEach((file) => {
        const formData = new FormData()
        formData.append('file', file)
        formData.append('id', this.reqDataId)
        formData.append('allowPathParent', this.tempNode.allowPathParent)
        formData.append('unzip', this.uploadFileZip)
        formData.append('nextPath', this.tempNode.nextPath)
        this.confirmLoading = true
        // 上传文件
        uploadFile(this.baseUrl, formData)
          .then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.loadFileList()
              this.closeUploadFile()
              this.uploadFileVisible = false
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },

    // 编辑
    handleEdit(record) {
      this.temp = Object.assign({}, record)
      const params = {
        id: this.reqDataId,
        allowPathParent: record.allowPathParent,
        nextPath: record.nextPath,
        name: record.name
      }
      readFile(this.baseUrl, params).then((res) => {
        if (res.code === 200) {
          this.temp = { ...this.temp, fileContent: res.data }
          this.editFileVisible = true
        }
      })
      //
    },
    updateFileData() {
      const params = {
        id: this.reqDataId,
        allowPathParent: this.temp.allowPathParent,
        nextPath: this.temp.nextPath,
        name: this.temp.name,
        content: this.temp.fileContent
      }
      this.confirmLoading = true
      updateFileData(this.baseUrl, params)
        .then((res) => {
          if (res.code === 200) {
            $notification.success({
              message: res.msg
            })
            this.editFileVisible = false
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
    },
    // 修改文件权限
    handleFilePermission(record) {
      this.temp = Object.assign({}, record)
      this.permissions = parsePermissions(this.temp.permissions)
      //const permissionsValue = calcFilePermissionValue(this.permissions);
      //this.permissionTips = `cd ${this.temp.nextPath} && chmod ${permissionsValue} ${this.temp.name}`;
      this.editFilePermissionVisible = true
    },
    // 更新文件权限提示
    renderFilePermissionsTips() {
      //const permissionsValue = calcFilePermissionValue(this.permissions);
      //this.permissionTips = `cd ${this.temp.nextPath} && chmod ${permissionsValue} ${this.temp.name}`;
    },
    // 确认修改文件权限
    updateFilePermissions() {
      // 请求参数
      const params = {
        id: this.reqDataId,
        allowPathParent: this.temp.allowPathParent,
        nextPath: this.temp.nextPath,
        fileName: this.temp.name,
        permissionValue: calcFilePermissionValue(this.permissions)
      }
      changeFilePermission(this.baseUrl, params).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
          this.editFilePermissionVisible = false
          this.loadFileList()
        }
      })
    },

    // 下载
    handleDownload(record) {
      // 请求参数
      const params = {
        id: this.reqDataId,
        allowPathParent: record.allowPathParent,
        nextPath: record.nextPath,
        name: record.name
      }
      // 请求接口拿到 blob
      window.open(downloadFile(this.baseUrl, params), '_blank')
    },
    // 删除文件夹
    handleDeletePath() {
      const that = this
      this.$confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '真的要删除当前文件夹么？',
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            // 请求参数
            const params = {
              id: that.reqDataId,
              allowPathParent: that.tempNode.allowPathParent,
              nextPath: that.tempNode.nextPath
            }
            // 删除
            deleteFile(that.baseUrl, params)
              .then((res) => {
                if (res.code === 200) {
                  $notification.success({
                    message: res.msg
                  })
                  // 刷新树
                  that.loadData()
                  that.fileList = []
                  //this.loadFileList();
                }
                resolve()
              })
              .catch(reject)
          })
        }
      })
    },
    // 删除
    handleDelete(record) {
      const that = this
      this.$confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '真的要删除文件么？',
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            // 请求参数
            const params = {
              id: that.reqDataId,
              allowPathParent: record.allowPathParent,
              nextPath: record.nextPath,
              name: record.name
            }
            // 删除
            deleteFile(that.baseUrl, params)
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
    handleRenameFile(record) {
      this.renameFileFolderVisible = true
      this.temp = {
        fileFolderName: record.name,
        oldFileFolderName: record.name,
        allowPathParent: record.allowPathParent,
        nextPath: record.nextPath
      }
    },
    // 确认修改文件 目录名称
    renameFileFolder() {
      const params = {
        id: this.reqDataId,
        name: this.temp.oldFileFolderName,
        newname: this.temp.fileFolderName,
        allowPathParent: this.temp.allowPathParent,
        nextPath: this.temp.nextPath
      }
      this.confirmLoading = true
      renameFileFolder(this.baseUrl, params)
        .then((res) => {
          if (res.code === 200) {
            $notification.success({
              message: res.msg
            })
            this.renameFileFolderVisible = false
            this.loadFileList()
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
    }
  }
}
</script>

<style scoped>
.ssh-file-layout {
  padding: 0;
  min-height: calc(100vh - 75px);
}
.dir-container {
  padding: 10px;
  border-bottom: 1px solid #eee;
}
.sider {
  /* border: 1px solid #e2e2e2; */
  /* overflow-x: auto; */
}
.file-content {
  margin: 10px 10px 0;
  padding: 10px;
  /* background-color: #fff; */
}
.title {
  font-weight: 600;
  font-size: larger;
}
</style>
