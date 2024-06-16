<template>
  <div>
    <div v-show="viewList">
      <a-table
        size="middle"
        :data-source="backupListData.list"
        :loading="backupListLoading"
        :columns="columns"
        :pagination="false"
        bordered
        :scroll="{
          x: 'max-content'
        }"
      >
        <template v-if="backupListData.path" #title>
          {{ $t('pages.node.node-layout.project.project-file-backup.d0c9ff5d') }}{{ backupListData.path }}
        </template>

        <template #bodyCell="{ column, text, record }">
          <template v-if="column.dataIndex === 'filename'">
            <a-tooltip placement="topLeft" :title="text">
              <span>{{ text }}</span>
            </a-tooltip>
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
              <a-button size="small" type="primary" @click="handleBackupFile(record)">{{
                $t('pages.node.node-layout.project.project-file-backup.151c73eb')
              }}</a-button>
              <a-button size="small" type="primary" danger @click="handlBackupeDelete(record)">{{
                $t('pages.node.node-layout.project.project-file-backup.2f14e7d4')
              }}</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>
    <!-- 布局 -->
    <a-layout v-show="!viewList" class="file-layout">
      <!-- 目录树 -->
      <a-layout-sider theme="light" class="sider" width="25%">
        <div class="dir-container">
          <a-space>
            <a-button
              size="small"
              type="primary"
              @click="
                () => {
                  viewList = true
                }
              "
              >{{ $t('pages.node.node-layout.project.project-file-backup.a21788bc') }}
            </a-button>
            <a-button size="small" type="primary" @click="loadData">{{
              $t('pages.node.node-layout.project.project-file-backup.71bd4892')
            }}</a-button>
          </a-space>
        </div>

        <a-directory-tree
          v-model:expandedKeys="expandedKeys"
          v-model:selectedKeys="selectedKeys"
          :field-names="treeReplaceFields"
          :load-data="onTreeData"
          :tree-data="treeList"
          @select="nodeClick"
        ></a-directory-tree>
      </a-layout-sider>
      <!-- 表格 -->
      <a-layout-content class="file-content">
        <a-table
          :data-source="fileList"
          size="middle"
          :loading="loading"
          :columns="fileColumns"
          :pagination="false"
          bordered
          :scroll="{
            x: 'max-content'
          }"
        >
          <template #title>
            <a-popconfirm
              :title="`${
                uploadPath
                  ? $t('pages.node.node-layout.project.project-file-backup.e1aa5b23') +
                    uploadPath +
                    $t('pages.node.node-layout.project.project-file-backup.7fa8492f')
                  : ''
              } ${$t('pages.node.node-layout.project.project-file-backup.2c42e7cd')},${$t(
                'pages.node.node-layout.project.project-file-backup.69fd8524'
              )}`"
              :ok-text="$t('pages.node.node-layout.project.project-file-backup.7f268593')"
              :cancel-text="$t('pages.node.node-layout.project.project-file-backup.2a382b54')"
              :ok-button-props="{
                loading: recoverLoading
              }"
              @confirm="recoverNet('', uploadPath)"
              @cancel="recoverNet('clear', uploadPath)"
            >
              <template #icon>
                <QuestionCircleOutlined style="color: red" />
              </template>
              <!-- @click="recoverPath(uploadPath)" -->
              <a-button size="small" type="primary">{{
                $t('pages.node.node-layout.project.project-file-backup.75a0292a')
              }}</a-button>
            </a-popconfirm>

            <a-space>
              <a-tag v-if="uploadPath" color="#2db7f5"
                >{{ $t('pages.node.node-layout.project.project-file-backup.5a7e230f') }}{{ uploadPath || '' }}</a-tag
              >
            </a-space>
          </template>

          <template #bodyCell="{ column, text, record }">
            <!-- <template v-if="column.dataIndex === 'filename'"> -->
            <template v-if="column.dataIndex === 'filename'">
              <a-tooltip placement="topLeft" :title="text">
                <span>{{ text }}</span>
              </a-tooltip>
            </template>
            <template v-else-if="column.dataIndex === 'isDirectory'">
              <a-tooltip placement="topLeft" :title="text">
                <span>{{
                  text
                    ? $t('pages.node.node-layout.project.project-file-backup.64408008')
                    : $t('pages.node.node-layout.project.project-file-backup.69cad40b')
                }}</span>
              </a-tooltip>
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
                  <a-tooltip :title="$t('pages.node.node-layout.project.project-file-backup.88600d3e')">
                    <a-button size="small" type="primary" :disabled="true">{{
                      $t('pages.node.node-layout.project.project-file-backup.8e51d32d')
                    }}</a-button>
                  </a-tooltip>
                </template>
                <template v-else>
                  <a-button size="small" type="primary" @click="handleDownload(record)">{{
                    $t('pages.node.node-layout.project.project-file-backup.8e51d32d')
                  }}</a-button>
                </template>
                <template v-if="record.isDirectory">
                  <!-- record.filename -->
                  <a-popconfirm
                    :title="`${
                      record.filename
                        ? $t('pages.node.node-layout.project.project-file-backup.e1aa5b23') +
                          record.filename +
                          $t('pages.node.node-layout.project.project-file-backup.7fa8492f')
                        : ''
                    } ${$t('pages.node.node-layout.project.project-file-backup.2c42e7cd')},${$t(
                      'pages.node.node-layout.project.project-file-backup.69fd8524'
                    )}`"
                    :ok-text="$t('pages.node.node-layout.project.project-file-backup.7f268593')"
                    :cancel-text="$t('pages.node.node-layout.project.project-file-backup.2a382b54')"
                    :ok-button-props="{
                      loading: recoverLoading
                    }"
                    @confirm="recoverNet('', record.filename)"
                    @cancel="recoverNet('clear', record.filename)"
                  >
                    <template #icon>
                      <QuestionCircleOutlined style="color: red" />
                    </template>
                    <a-button size="small" type="primary">{{
                      $t('pages.node.node-layout.project.project-file-backup.75a0292a')
                    }}</a-button>
                  </a-popconfirm>
                </template>
                <template v-else>
                  <a-button size="small" type="primary" :loading="recoverLoading" @click="recover(record)">{{
                    $t('pages.node.node-layout.project.project-file-backup.75a0292a')
                  }}</a-button>
                </template>

                <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
                  $t('pages.node.node-layout.project.project-file-backup.2f14e7d4')
                }}</a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-layout-content>
    </a-layout>
  </div>
</template>
<script>
import {
  backupDeleteProjectFile,
  backupDownloadProjectFile,
  backupFileList,
  backupRecoverProjectFile,
  listBackup
} from '@/api/node-project-backup'
import { renderSize, parseTime } from '@/utils/const'
export default {
  components: {},
  props: {
    nodeId: {
      type: String,
      default: ''
    },
    projectId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      viewList: true,
      loading: false,
      treeList: [],
      fileList: [],
      backupListData: {
        list: []
      },
      backupListLoading: false,
      tempNode: {},
      temp: {},
      treeReplaceFields: {
        title: 'filename',
        isLeaf: 'isDirectory'
      },

      defaultProps: {
        children: 'children',
        label: 'filename'
      },
      expandedKeys: [],
      selectedKeys: [],
      columns: [
        {
          title: this.$t('pages.node.node-layout.project.project-file-backup.6a721706'),
          dataIndex: 'filename',
          ellipsis: true
        },

        {
          title: this.$t('pages.node.node-layout.project.project-file-backup.c3914d6a'),
          dataIndex: 'fileSizeLong',
          width: 120,
          ellipsis: true
        },
        {
          title: this.$t('pages.node.node-layout.project.project-file-backup.e7410f94'),
          dataIndex: 'modifyTimeLong',
          width: 180,
          ellipsis: true
        },
        {
          title: this.$t('pages.node.node-layout.project.project-file-backup.cadc075'),
          dataIndex: 'operation',
          width: 180,
          align: 'center',
          fixed: 'right'
        }
      ],

      fileColumns: [
        {
          title: this.$t('pages.node.node-layout.project.project-file-backup.6a721706'),
          dataIndex: 'filename',
          ellipsis: true
        },
        {
          title: this.$t('pages.node.node-layout.project.project-file-backup.741604c2'),
          dataIndex: 'isDirectory',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$t('pages.node.node-layout.project.project-file-backup.c3914d6a'),
          dataIndex: 'fileSizeLong',
          width: 120,
          ellipsis: true
        },
        {
          title: this.$t('pages.node.node-layout.project.project-file-backup.e7410f94'),
          dataIndex: 'modifyTimeLong',
          width: 180,
          ellipsis: true
        },
        {
          title: this.$t('pages.node.node-layout.project.project-file-backup.cadc075'),
          dataIndex: 'operation',
          width: 180,
          align: 'center',
          fixed: 'right'
        }
      ],

      recoverLoading: false
    }
  },
  computed: {
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
    this.loadBackupList()
  },
  methods: {
    renderSize,
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
    // 查询备份列表
    loadBackupList() {
      listBackup({
        nodeId: this.nodeId,
        id: this.projectId
      }).then((res) => {
        if (res.code === 200 && res.data) {
          this.backupListData = res.data
        }
        this.backupListLoading = false
      })
    },
    // 加载数据
    loadData() {
      const key = 'root-' + new Date().getTime()
      this.treeList = [
        {
          filename: this.$t('pages.node.node-layout.project.project-file-backup.6344a175') + (this.temp.filename || ''),
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
            path: this.uploadPath,
            backupId: this.temp.filename
          }
          // if (node.level === 1) {
          //   params.path = ''
          // } else {
          //   params.path = data.levelName || '' + '/' + data.filename
          // }
          // 加载文件
          backupFileList(params).then((res) => {
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

    // 点击树节点
    nodeClick(selectedKeys, { node }) {
      if (node.dataRef.isDirectory) {
        this.tempNode = node.dataRef
        this.loadFileList()
      }
    },

    // 加载文件列表
    loadFileList() {
      if (Object.keys(this.tempNode).length === 0) {
        $notification.warn({
          message: this.$t('pages.node.node-layout.project.project-file-backup.580e6c10')
        })
        return false
      }
      // 请求参数
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        path: this.uploadPath,
        backupId: this.temp.filename
      }
      this.fileList = []
      this.loading = true
      // 加载文件
      backupFileList(params).then((res) => {
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

    // 下载
    handleDownload(record) {
      $notification.info({
        message: this.$t('pages.node.node-layout.project.project-file-backup.1fce73f8')
      })
      // 请求参数
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        levelName: record.levelName,
        filename: record.filename,
        backupId: this.temp.filename
      }
      // 请求接口拿到 blob
      window.open(backupDownloadProjectFile(params), '_blank')
    },
    // 删除
    handleDelete(record) {
      const msg = record.isDirectory
        ? this.$t('pages.node.node-layout.project.project-file-backup.b658d8ec') +
          record.filename +
          this.$t('pages.node.node-layout.project.project-file-backup.1cad79c0')
        : this.$t('pages.node.node-layout.project.project-file-backup.b658d8ec') +
          record.filename +
          this.$t('pages.node.node-layout.project.project-file-backup.194806c7')
      $confirm({
        title: this.$t('pages.node.node-layout.project.project-file-backup.a8fe4c17'),
        zIndex: 1009,
        content: msg,
        okText: this.$t('pages.node.node-layout.project.project-file-backup.7da4a591'),
        cancelText: this.$t('pages.node.node-layout.project.project-file-backup.43105e21'),
        onOk: () => {
          return backupDeleteProjectFile({
            nodeId: this.nodeId,
            id: this.projectId,
            levelName: record.levelName,
            filename: record.filename,
            backupId: this.temp.filename
          }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.loadData()
              this.loadFileList()
            }
          })
        }
      })
    },
    // 删除备份
    handlBackupeDelete(record) {
      const msg =
        this.$t('pages.node.node-layout.project.project-file-backup.b658d8ec') +
        record.filename +
        this.$t('pages.node.node-layout.project.project-file-backup.fe7b030a')
      $confirm({
        title: this.$t('pages.node.node-layout.project.project-file-backup.a8fe4c17'),
        zIndex: 1009,
        content: msg,
        okText: this.$t('pages.node.node-layout.project.project-file-backup.7da4a591'),
        cancelText: this.$t('pages.node.node-layout.project.project-file-backup.43105e21'),
        onOk: () => {
          return backupDeleteProjectFile({
            nodeId: this.nodeId,
            id: this.projectId,
            levelName: '/',
            filename: '/',
            backupId: record.filename
          }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.loadBackupList()
            }
          })
        }
      })
    },
    handleBackupFile(record) {
      this.viewList = false
      this.temp = Object.assign({}, record)
      this.loadData()
    },

    recover(record) {
      if (record.isDirectory) {
        this.recoverPath(record.filename)
      } else {
        $confirm({
          title: this.$t('pages.node.node-layout.project.project-file-backup.a8fe4c17'),
          zIndex: 1009,
          content:
            this.$t('pages.node.node-layout.project.project-file-backup.24fdeed2') +
            record.filename +
            this.$t('pages.node.node-layout.project.project-file-backup.f4ea8f84'),
          okText: this.$t('pages.node.node-layout.project.project-file-backup.7da4a591'),
          cancelText: this.$t('pages.node.node-layout.project.project-file-backup.43105e21'),
          onOk() {
            // // 请求参数
            this.recoverNet('', record.filename)
          }
        })
      }
    },
    // 请求参数
    recoverNet(type, filename) {
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        type,
        levelName: this.uploadPath,
        filename,
        backupId: this.temp.filename
      }
      this.recoverLoading = true
      // 删除
      backupRecoverProjectFile(params)
        .then((res) => {
          if (res.code === 200) {
            $notification.success({
              message: res.msg
            })
          }
        })
        .finally(() => {
          this.recoverLoading = false
        })
    }
  }
}
</script>
<style scoped>
.file-layout {
  padding: 0;
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
.successTag {
  height: 32px;
  line-height: 30px;
}
</style>
