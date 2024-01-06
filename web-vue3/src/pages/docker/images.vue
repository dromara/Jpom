<template>
  <div>
    <a-table
      size="middle"
      :data-source="list"
      :columns="columns"
      :pagination="false"
      bordered
      rowKey="id"
      :row-selection="rowSelection"
      :scroll="{
        x: 'max-content'
      }"
    >
      <template v-slot:title>
        <a-space>
          <!-- <a-input v-model="listQuery['name']" @pressEnter="loadData" placeholder="名称" class="search-input-item" /> -->
          <div>
            显示所有
            <a-switch checked-children="是" un-checked-children="否" v-model:value="listQuery['showAll']" />
          </div>
          <div>
            悬空
            <a-switch checked-children="是" un-checked-children="否" v-model:value="listQuery['dangling']" />
          </div>
          <a-button type="primary" @click="loadData" :loading="loading">搜索</a-button>
          <a-button type="primary" danger :disabled="!tableSelections || !tableSelections.length" @click="batchDelete"
            >批量删除</a-button
          >

          |

          <a-input-search
            v-model:value="pullImageName"
            @search="pullImage"
            style="width: 260px"
            placeholder="要拉取的镜像名称"
            class="search-input-item"
          >
            <template v-slot:enterButton>
              <a-button><CloudDownloadOutlined /> </a-button>
            </template>
          </a-input-search>
          <!-- <a-button type="primary" @click="pullImage">拉取</a-button> -->

          <a-upload
            name="file"
            accept=".tar"
            action=""
            :disabled="!!percentage"
            :showUploadList="false"
            :multiple="false"
            :before-upload="beforeUpload"
          >
            <LoadingOutlined v-if="percentage" />
            <a-button type="primary" v-else> <UploadOutlined />导入 </a-button>
          </a-upload>
        </a-space>
      </template>

      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'repoTags'">
          <a-tooltip placement="topLeft" :title="(text || []).join(',')">
            <span>{{ (text || []).join(',') }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'size'">
          <a-tooltip placement="topLeft" :title="renderSize(text) + ' ' + renderSize(record.virtualSize)">
            <span>{{ renderSize(text) }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.id">
          <a-tooltip :title="text">
            <span> {{ text && text.split(':')[1].slice(0, 12) }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <!-- <a-tooltip title="停止" v-if="record.state === 'running'">
            <a-button size="small" type="link" @click="doAction(record, 'stop')"><a-icon type="stop" /></a-button>
          </a-tooltip>
          <a-tooltip title="启动" v-else>
            <a-button size="small" type="link" @click="doAction(record, 'start')"> <a-icon type="play-circle" /></a-button>
          </a-tooltip>
          -->
            <a-tooltip title="使用当前镜像创建一个容器">
              <a-button size="small" type="link" @click="createContainer(record)"><SelectOutlined /></a-button>
            </a-tooltip>
            <a-tooltip title="更新镜像">
              <a-button size="small" type="link" :disabled="!record.repoTags" @click="tryPull(record)"
                ><CloudDownloadOutlined
              /></a-button>
            </a-tooltip>
            <a-tooltip title="导出镜像">
              <a-button size="small" type="link" @click="saveImage(record.id.split(':')[1])"
                ><DownloadOutlined
              /></a-button>
            </a-tooltip>
            <a-tooltip title="删除镜像">
              <a-button size="small" type="link" @click="doAction(record, 'remove')"><DeleteOutlined /></a-button>
            </a-tooltip>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 构建容器 -->
    <BuildContainer
      v-if="buildVisible"
      :id="this.id"
      :imageId="this.temp.id"
      :machineDockerId="this.machineDockerId"
      :urlPrefix="this.urlPrefix"
      @cancelBtnClick="
        () => {
          this.buildVisible = false
        }
      "
      @confirmBtnClick="
        () => {
          this.buildVisible = false
          this.loadData()
        }
      "
    />

    <!-- 日志 -->
    <pull-image-Log
      v-if="logVisible > 0"
      :id="temp.id"
      :visible="logVisible != 0"
      @close="
        () => {
          logVisible = 0
        }
      "
      :machineDockerId="this.machineDockerId"
      :urlPrefix="this.urlPrefix"
    />
  </div>
</template>

<script>
import { parseTime, renderSize } from '@/utils/const'
import {
  dockerImageCreateContainer,
  dockerImagePullImage,
  dockerImageRemove,
  dockerImagesList,
  dockerImageBatchRemove,
  dockerImageSaveImage,
  dockerImageLoadImage
} from '@/api/docker-api'
import PullImageLog from '@/pages/docker/pull-image-log'
import BuildContainer from './buildContainer.vue'

export default {
  components: {
    PullImageLog,
    BuildContainer
  },
  props: {
    id: {
      type: String,
      default: ''
    },
    urlPrefix: {
      type: String
    },
    machineDockerId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      list: [],
      loading: false,
      listQuery: {
        showAll: false
      },
      logVisible: 0,
      pullImageName: '',
      renderSize,
      temp: {},
      rules: {
        name: [
          { required: true, message: '容器名称必填', trigger: 'blur' },
          {
            pattern: /[a-zA-Z0-9][a-zA-Z0-9_.-]$/,
            message: '容器名称数字字母,且长度大于1',
            trigger: 'blur'
          }
        ]
      },
      columns: [
        {
          title: '序号',
          width: '80px',
          ellipsis: true,
          align: 'center',
          customRender: ({ text, record, index }) => `${index + 1}`
        },
        {
          title: '名称',
          dataIndex: 'repoTags',
          ellipsis: true
        },
        {
          title: '镜像ID',
          dataIndex: 'id',
          ellipsis: true,
          width: 140,
          align: 'center',
          id: true
        },
        {
          title: '父级ID',
          dataIndex: 'parentId',
          ellipsis: true,
          width: 140,
          align: 'center',
          id: true
        },
        {
          title: '占用空间',
          dataIndex: 'size',
          ellipsis: true,
          width: 120
        },
        {
          title: '创建时间',
          dataIndex: 'created',
          sorter: (a, b) => new Number(a.created) - new Number(b.created),
          sortDirections: ['descend', 'ascend'],
          defaultSortOrder: 'descend',
          ellipsis: true,
          customRender: ({ text }) => {
            return parseTime(text)
          },
          width: 180
        },

        {
          title: '操作',
          dataIndex: 'operation',
          fixed: 'right',
          width: '160px'
        }
      ],
      action: {
        remove: {
          msg: '您确定要删除当前镜像吗？',
          api: dockerImageRemove
        }
      },
      buildVisible: false,
      tableSelections: [],
      percentage: 0
    }
  },
  computed: {
    reqDataId() {
      return this.id || this.machineDockerId
    },
    rowSelection() {
      return {
        onChange: (selectedRowKeys) => {
          this.tableSelections = selectedRowKeys
        },
        selectedRowKeys: this.tableSelections
      }
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    // 加载数据
    loadData() {
      this.loading = true
      //this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.listQuery.id = this.reqDataId
      dockerImagesList(this.urlPrefix, this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data
        }
        this.loading = false
      })
    },
    doAction(record, actionKey) {
      const action = this.action[actionKey]
      if (!action) {
        return
      }
      this.$confirm({
        title: '系统提示',
        zIndex: 1009,
        content: action.msg,
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 组装参数
          const params = {
            id: this.reqDataId,
            imageId: record.id
          }
          action.api(this.urlPrefix, params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg
              })
              this.loadData()
            }
          })
        }
      })
    },
    tryPull(record) {
      const repoTags = record?.repoTags[0]
      if (!repoTags) {
        this.$notification.error({
          message: '镜像名称不正确 不能更新'
        })
        return
      }
      this.pullImageName = repoTags
      this.pullImage()
    },
    // 构建镜像
    createContainer(record) {
      this.temp = Object.assign({}, record)
      this.buildVisible = true
    },
    // 创建容器
    handleBuildOk() {
      this.$refs['editForm'].validate((valid) => {
        if (!valid) {
          return false
        }
        const temp = {
          id: this.reqDataId,
          autorun: this.temp.autorun,
          imageId: this.temp.imageId,
          name: this.temp.name,
          env: {},
          commands: [],
          networkMode: this.temp.networkMode,
          privileged: this.temp.privileged,
          restartPolicy: this.temp.restartPolicy,
          labels: this.temp.labels,
          runtime: this.temp.runtime,
          hostname: this.temp.hostname,
          storageOpt: {}
        }
        temp.volumes = (this.temp.volumes || [])
          .filter((item) => {
            return item.host
          })
          .map((item) => {
            return item.host + ':' + item.container
          })
          .join(',')
        // 处理端口
        temp.exposedPorts = (this.temp.exposedPorts || [])
          .filter((item) => {
            return item.publicPort && item.ip
          })
          .map((item) => {
            return item.ip + ':' + item.publicPort + ':' + item.port
          })
          .join(',')
        // 环境变量
        this.temp.env.forEach((item) => {
          if (item.key && item.key) {
            temp.env[item.key] = item.value
          }
        })
        this.temp.storageOpt.forEach((item) => {
          if (item.key && item.key) {
            temp.storageOpt[item.key] = item.value
          }
        })
        //
        temp.commands = (this.temp.commands || []).map((item) => {
          return item.value || ''
        })
        dockerImageCreateContainer(this.urlPrefix, temp).then((res) => {
          if (res.code === 200) {
            this.$notification.success({
              message: res.msg
            })
            this.buildVisible = false
          }
        })
      })
    },
    // 拉取镜像
    pullImage() {
      if (!this.pullImageName) {
        this.$notification.warn({
          message: '请填写要拉取的镜像名称'
        })
        return
      }
      dockerImagePullImage(this.urlPrefix, {
        id: this.reqDataId,
        repository: this.pullImageName
      }).then((res) => {
        if (res.code === 200) {
          this.logVisible = new Date() * Math.random()
          this.temp = {
            id: res.data
          }
        }
      })
    },
    // 导出镜像
    saveImage(imageId) {
      const url = dockerImageSaveImage(this.urlPrefix, {
        id: this.reqDataId,
        imageId: imageId
      })
      window.open(url, '_blank')
    },
    // 分配
    batchDelete() {
      let ids = this.tableSelections
      this.$confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '真的要批量删除选择的镜像吗？已经被容器使用的镜像无法删除！',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 组装参数
          const params = {
            id: this.reqDataId,
            imagesIds: ids.join(',')
          }
          dockerImageBatchRemove(this.urlPrefix, params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg
              })
              this.loadData()
            }
          })
        }
      })
    },
    // 导入镜像
    beforeUpload(file) {
      this.percentage = 1
      const formData = new FormData()
      formData.append('file', file)
      formData.append('id', this.reqDataId)
      // 上传文件
      dockerImageLoadImage(this.urlPrefix, formData)
        .then((res) => {
          if (res.code === 200) {
            this.$notification.success({
              message: res.msg
            })
            this.loadData()
          }
        })
        .finally(() => {
          this.percentage = 0
        })
    }
  }
}
</script>
