<template>
  <div>
    <a-table
      size="middle"
      :data-source="list"
      :columns="columns"
      :pagination="false"
      bordered
      row-key="id"
      :row-selection="rowSelection"
      :scroll="{
        x: 'max-content'
      }"
    >
      <template #title>
        <a-space wrap class="search-box">
          <!-- <a-input v-model="listQuery['name']" @pressEnter="loadData" placeholder="名称" class="search-input-item" /> -->
          <div>
            {{ $tl('p.showAll') }}
            <a-switch
              v-model:checked="listQuery['showAll']"
              :checked-children="$tl('c.is')"
              :un-checked-children="$tl('c.no')"
            />
          </div>
          <div>
            {{ $tl('p.suspended') }}
            <a-switch
              v-model:checked="listQuery['dangling']"
              :checked-children="$tl('c.is')"
              :un-checked-children="$tl('c.no')"
            />
          </div>
          <a-button type="primary" :loading="loading" @click="loadData">{{ $tl('p.search') }}</a-button>
          <a-button
            type="primary"
            danger
            :disabled="!tableSelections || !tableSelections.length"
            @click="batchDelete"
            >{{ $tl('p.batchDelete') }}</a-button
          >

          |

          <a-input-search
            v-model:value="pullImageName"
            style="width: 260px"
            :placeholder="$tl('p.pullImageName')"
            class="search-input-item"
            @search="pullImage"
          >
            <template #enterButton>
              <a-button><CloudDownloadOutlined /> </a-button>
            </template>
          </a-input-search>
          <!-- <a-button type="primary" @click="pullImage">拉取</a-button> -->

          <a-upload
            name="file"
            accept=".tar"
            action=""
            :disabled="!!percentage"
            :show-upload-list="false"
            :multiple="false"
            :before-upload="beforeUpload"
          >
            <LoadingOutlined v-if="percentage" />
            <a-button v-else type="primary"> <UploadOutlined />{{ $tl('p.importImage') }} </a-button>
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
            <a-tooltip :title="$tl('p.createContainerWithImage')">
              <a-button size="small" type="link" @click="createContainer(record)"><SelectOutlined /></a-button>
            </a-tooltip>
            <a-tooltip :title="$tl('p.updateImage')">
              <a-button size="small" type="link" :disabled="!record.repoTags" @click="tryPull(record)"
                ><CloudDownloadOutlined
              /></a-button>
            </a-tooltip>
            <a-tooltip :title="$tl('p.exportImage')">
              <a-button size="small" type="link" @click="saveImage(record.id.split(':')[1])"
                ><DownloadOutlined
              /></a-button>
            </a-tooltip>
            <a-tooltip :title="$tl('p.deleteImage')">
              <a-button size="small" type="link" @click="doAction(record, 'remove')"><DeleteOutlined /></a-button>
            </a-tooltip>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 构建容器 -->
    <BuildContainer
      v-if="buildVisible"
      :id="id"
      :image-id="temp.id"
      :machine-docker-id="machineDockerId"
      :url-prefix="urlPrefix"
      @cancel-btn-click="
        () => {
          buildVisible = false
        }
      "
      @confirm-btn-click="
        () => {
          buildVisible = false
          loadData()
        }
      "
    />

    <!-- 日志 -->
    <pull-image-Log
      v-if="logVisible > 0"
      :id="temp.id"
      :visible="logVisible != 0"
      :machine-docker-id="machineDockerId"
      :url-prefix="urlPrefix"
      @close="
        () => {
          logVisible = 0
        }
      "
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
      type: String,
      default: ''
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
          { required: true, message: this.$tl('p.containerNameRequired'), trigger: 'blur' },
          {
            pattern: /[a-zA-Z0-9][a-zA-Z0-9_.-]$/,
            message: this.$tl('p.containerNameAlphanumeric'),
            trigger: 'blur'
          }
        ]
      },
      columns: [
        {
          title: this.$tl('p.serialNumber'),
          width: '80px',
          ellipsis: true,
          align: 'center',
          customRender: ({ index }) => `${index + 1}`
        },
        {
          title: this.$tl('p.name'),
          dataIndex: 'repoTags',
          ellipsis: true
        },
        {
          title: this.$tl('p.imageId'),
          dataIndex: 'id',
          ellipsis: true,
          width: 140,
          align: 'center',
          id: true
        },
        {
          title: this.$tl('p.parentId'),
          dataIndex: 'parentId',
          ellipsis: true,
          width: 140,
          align: 'center',
          id: true
        },
        {
          title: this.$tl('p.spaceOccupied'),
          dataIndex: 'size',
          ellipsis: true,
          width: 120
        },
        {
          title: this.$tl('p.creationTime'),
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
          title: this.$tl('p.operation'),
          dataIndex: 'operation',
          fixed: 'right',
          width: '160px'
        }
      ],
      action: {
        remove: {
          msg: this.$tl('p.confirmDeleteImage'),
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
    $tl(key, ...args) {
      return this.$t(`pages.docker.images.${key}`, ...args)
    },
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
      $confirm({
        title: this.$tl('c.systemHint'),
        zIndex: 1009,
        content: action.msg,
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          return action
            .api(this.urlPrefix, {
              id: this.reqDataId,
              imageId: record.id
            })
            .then((res) => {
              if (res.code === 200) {
                $notification.success({
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
        $notification.error({
          message: this.$tl('p.incorrectImageName')
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
      this.$refs['editForm'].validate().then(() => {
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
            $notification.success({
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
        $notification.warn({
          message: this.$tl('p.fillPullImageName')
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

      $confirm({
        title: this.$tl('c.systemHint'),
        zIndex: 1009,
        content: this.$tl('p.confirmBatchDeleteImage'),
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          return dockerImageBatchRemove(this.urlPrefix, {
            id: this.reqDataId,
            imagesIds: ids.join(',')
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
            $notification.success({
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
