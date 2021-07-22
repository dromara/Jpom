<template>
  <div class="node-update">
    <div class="header">
      <div class="left">
        <a-select v-model="groupFilter" allowClear placeholder="请选择分组" class="group">
          <a-select-option v-for="group in groupList" :key="group">{{ group }}</a-select-option>
        </a-select>
        <a-button type="primary" @click="refresh">刷新</a-button>
        <a-button type="primary" @click="batchUpdate">批量更新</a-button>
      </div>
      <div class="right">
        <div class="title">Agent最新版本：</div>
        <div class="version">{{ agentVersion | version }}</div>
        <div class="action">
          <a-upload
              name="file"
              accept=".jar"
              action=""
              :showUploadList="false"
              :multiple="false"
              :before-upload="beforeUpload"
          >
            <a-button type="primary">
              <a-icon type="upload"/>
              上传新版本
            </a-button>
          </a-upload>
        </div>
      </div>
    </div>
    <div class="table-div">
      <a-table :columns="columns" :data-source="listComputed" bordered rowKey="id" class="table" :pagination="false"
               :row-selection="rowSelection">
        <template slot="version" slot-scope="text">
          {{ text | version }}
        </template>
        <template slot="status" slot-scope="text">
          <div class="restarting" v-if="text && text.type === 'restarting'">
            重启中，大约需要30秒
          </div>
          <div class="uploading" v-if="text && text.type === 'uploading'">
            <div class="text">{{ text.percent === 100 ? '上传成功' : '正在上传文件' }}</div>
            <a-progress :percent="text.percent"/>
          </div>
        </template>
        <template slot="operation" slot-scope="text, record">
          <a-button type="primary" @click="updateNodeHandler(record)">更新</a-button>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script>
import {getNodeGroupList, uploadAgentFile} from "@/api/node"
import {mapGetters} from "vuex";

export default {
  name: "NodeUpdate",
  filters: {
    version(value) {
      return value && value || '未知'
    },
    status(value) {
      return value && value || '未知'
    }
  },
  data() {
    return {
      agentVersion: '',
      websocket: null,
      groupFilter: undefined,
      groupList: [],
      list: [],
      columns: [
        {title: '节点 ID', dataIndex: 'id', ellipsis: true, scopedSlots: {customRender: 'id'}},
        {title: '节点名称', dataIndex: 'name', ellipsis: true, scopedSlots: {customRender: 'name'}},
        {title: '分组', dataIndex: 'group', ellipsis: true, scopedSlots: {customRender: 'group'}},
        {title: 'Agent版本号', dataIndex: 'version', ellipsis: true, scopedSlots: {customRender: 'version'}},
        {title: '打包时间', dataIndex: 'timeStamp', ellipsis: true, scopedSlots: {customRender: 'timeStamp'}},
        {title: '状态', dataIndex: 'status', ellipsis: true, scopedSlots: {customRender: 'status'}},
        // {title: '自动更新', dataIndex: 'autoUpdate', ellipsis: true, scopedSlots: {customRender: 'autoUpdate'}},
        {title: '操作', dataIndex: 'operation', width: '250px', scopedSlots: {customRender: 'operation'}, align: 'left'}
      ],
      nodeVersion: {},
      nodeStatus: {},
      tableSelections: []
    }
  },
  computed: {
    ...mapGetters([
      'getLongTermToken'
    ]),
    listComputed() {
      const data = []
      this.list.forEach(item => {
        if (item.group === this.groupFilter || this.groupFilter === undefined) {
          let itemData = this.nodeVersion[item.id];
          if (itemData) {
            try {
              itemData = JSON.parse(itemData);
              item.version = itemData.version;
              item.timeStamp = itemData.timeStamp;
            } catch (e) {
              item.version = itemData;
            }
          }

          item.status = this.nodeStatus[item.id]
          data.push(item)
        }
      })
      return data
    },
    rowSelection() {
      return {
        onChange: this.tableSelectionChange
      }
    },
    socketUrl() {
      const protocol = location.protocol === 'https:' ? 'wss://' : 'ws://';
      return `${protocol}${location.host}/node_update?userId=${this.getLongTermToken}`;
    }
  },
  mounted() {
    this.initWebsocket()
    this.loadGroupList()
  },
  methods: {
    initWebsocket() {
      if (!this.socket || this.socket.readyState !== this.socket.OPEN || this.socket.readyState !== this.socket.CONNECTING) {
        this.socket = new WebSocket(this.socketUrl)
      }

      this.socket.onopen = () => {
        this.init()
      }
      this.socket.onmessage = ({data: socketData}) => {
        const msgObj = JSON.parse(socketData)
        if (msgObj) {
          const {command, data, nodeId} = msgObj
          this[`${command}Result`] && this[`${command}Result`](data, nodeId)
        }
      }
    },
    init() {
      this.getNodeList()
      this.getAgentVersion()
    },
    loadGroupList() {
      getNodeGroupList().then(({code, data}) => {
        if (code === 200) {
          this.groupList = data
        }
      })
    },
    refresh() {
      if (this.socket) {
        this.socket.close()
      }
      this.nodeStatus = {}
      this.nodeVersion = {}
      this.initWebsocket()
      this.loadGroupList()
    },
    batchUpdate() {
      if (this.tableSelections.length === 0) {
        this.$notification.warning({
          message: "请选择要升级的节点"
        })
        return;
      }
      this.updateNode()
    },
    updateNodeHandler(record) {
      this.tableSelections = [record.id]
      this.updateNode()
    },
    tableSelectionChange(selectedRowKeys) {
      this.tableSelections = selectedRowKeys
    },
    sendMsg(command, params) {
      this.socket.send(JSON.stringify({
        command,
        params
      }))
    },
    getNodeList() {
      this.sendMsg('getNodeList')
    },
    getNodeListResult(data) {
      this.list = data
    },
    getAgentVersion() {
      this.sendMsg('getAgentVersion')
    },
    getAgentVersionResult(data) {
      this.agentVersion = data
    },
    getVersionResult(data, nodeId) {
      this.nodeVersion = Object.assign({}, this.nodeVersion, {
        [nodeId]: data
      })
    },
    updateNode() {
      this.sendMsg("updateNode", {
        ids: this.tableSelections
      })
      this.tableSelections = []
    },
    updateNodeResult(data, nodeId) {
      const {completeSize, size} = data
      this.nodeStatus = Object.assign({}, this.nodeStatus, {
        [nodeId]: {
          ...data,
          type: 'uploading',
          percent: (completeSize / size) * 100
        }
      })
    },
    restartResult(data, nodeId) {
      this.nodeStatus = Object.assign({}, this.nodeStatus, {
        [nodeId]: {
          type: 'restarting'
        }
      })
    },
    beforeUpload(file) {
      const formData = new FormData();
      formData.append('file', file);
      uploadAgentFile(formData).then(({code, msg}) => {
        if (code === 200) {
          this.$notification.success({message: "上传成功"})
          this.getAgentVersion()
        } else {
          this.$notification.error({message: msg})
        }
      })
      return false
    }
  }
}
</script>

<style scoped lang="stylus">
.node-update {
  width 100%
  height 100%
  display flex
  flex-direction column
  justify-content space-between


  .header {
    height 60px
    display flex
    align-items center

    > div {
      width 50%
    }

    .left {

      .group {
        width 200px
      }
    }

    .right {
      display flex
      justify-content flex-end
      align-items center

      > div {
        padding: 0 10px
      }
    }
  }

  .table-div {
    height calc(100% - 60px)
    overflow auto

    .table {
      height 100%

      .uploading {
        display flex
        flex-direction column
        align-items center
      }
    }
  }
}

.ant-select, .ant-btn {
  margin-right 20px
}


</style>