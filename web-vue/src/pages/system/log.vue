<template>
  <a-layout class="log-layout">
    <!-- 侧边栏 文件树 -->
    <a-layout-sider theme="light" class="sider jpom-log-tree" width="20%">
      <a-empty v-if="list.length === 0" />
      <a-directory-tree :treeData="list" :fieldNames="replaceFields" @select="select" default-expand-all>
      </a-directory-tree>
    </a-layout-sider>
    <!-- 单个文件内容 -->
    <a-layout-content class="log-content">
      <log-view2 :ref="`logView`" height="calc(100vh - 165px)">
        <template #before>
          <a-space>
            <a-button type="primary" size="small" @click="loadData">刷新</a-button>
            <a-button type="primary" danger size="small" :disabled="!this.temp.path" @click="deleteLog">删除</a-button>
            <a-button type="primary" size="small" :disabled="!this.temp.path" @click="downloadLog">下载</a-button>
            |
          </a-space>
        </template>
      </log-view2>
    </a-layout-content>
  </a-layout>
</template>

<script>
import { getLogList, downloadFile, deleteLog } from '@/api/system'
import { mapState } from 'pinia'
import { useUserStore } from '@/stores/user'
import { getWebSocketUrl } from '@/api/config'
import LogView2 from '@/components/logView/index2.vue'

export default {
  components: {
    LogView2
  },
  data() {
    return {
      list: [],
      socket: null,
      // 日志内容
      // logContext: "choose file loading context...",

      replaceFields: {
        children: 'children',
        title: 'title',
        key: 'path'
      },
      visible: false,
      temp: {}
    }
  },
  computed: {
    ...mapState(useUserStore, ['getLongTermToken']),
    socketUrl() {
      return getWebSocketUrl('/socket/system_log', `userId=${this.getLongTermToken}&nodeId=system&type=systemLog`)
    }
  },
  watch: {},
  created() {
    this.loadData()

    // 监听窗口关闭事件，当窗口关闭时，主动去关闭websocket连接，防止连接还没断开就关闭窗口，server端会抛异常。
    window.onbeforeunload = () => {
      this.socket?.close()
    }
  },
  beforeUnmount() {
    this.socket?.close()
  },
  methods: {
    // 加载数据
    loadData() {
      this.list = []
      getLogList().then((res) => {
        if (res.code === 200) {
          res.data.forEach((element) => {
            if (element.children) {
              this.calcTreeNode(element.children)
            }
            // 组装数据
            this.list.push({
              ...element,
              isLeaf: !element.children ? true : false
            })
          })
        }
      })
    },
    // 递归处理节点
    calcTreeNode(list) {
      list.forEach((element) => {
        if (element.children) {
          this.calcTreeNode(element.children)
        } else {
          // 叶子节点
          element.isLeaf = true
        }
      })
    },
    // 选择节点
    select(selectedKeys, { node }) {
      if (this.temp?.path === node.dataRef?.path) {
        return
      }
      if (!node.dataRef.isLeaf) {
        return
      }
      const data = {
        op: 'showlog',
        tomcatId: this.tomcatId,
        fileName: node.dataRef.path
      }
      this.temp = node.dataRef
      this.$refs.logView?.clearLogCache()

      this.socket?.close()

      this.socket = new WebSocket(this.socketUrl)
      // 连接成功后
      this.socket.onopen = () => {
        this.socket.send(JSON.stringify(data))
      }
      this.socket.onmessage = (msg) => {
        // this.logContext += `${msg.data}\r\n`;
        this.$refs.logView?.appendLine(msg.data)
      }
      this.socket.onerror = (err) => {
        console.error(err)
        $notification.error({
          message: 'web socket 错误,请检查是否开启 ws 代理'
        })
      }
      this.socket.onclose = (err) => {
        //当客户端收到服务端发送的关闭连接请求时，触发onclose事件
        console.error(err)
        this.$message.warning('会话已经关闭[system-log] ' + node.dataRef.path)
        // clearInterval(this.heart);
      }
    },
    // // 右键点击
    // rightClick({ node }) {
    //   this.temp = node.dataRef;
    //   // 弹出提示 下载还是删除
    //   this.visible = true;
    // },
    // 下载文件
    downloadLog() {
      // 请求参数
      const params = {
        nodeId: null,
        path: this.temp.path
      }
      // 请求接口拿到 blob
      window.open(downloadFile(params), '_blank')
    },
    // 删除文件
    deleteLog() {
      const that = this
      $confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '真的要删除日志文件么？',
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            const params = {
              nodeId: null,
              path: that.temp.path
            }
            // 删除日志
            deleteLog(params)
              .then((res) => {
                if (res.code === 200) {
                  $notification.success({
                    message: res.msg
                  })
                  that.visible = false
                  that.loadData()
                }
                resolve()
              })
              .catch(reject)
          })
        }
      })
    }
  }
}
</script>

<style scoped>
.log-layout {
  padding: 0;
  margin: 0;
}
.sider {
  border: 1px solid #e2e2e2;
  height: calc(100vh - 110px);
  overflow-y: auto;
}
.log-content {
  margin: 0;
  padding-left: 15px;
  background: #fff;
  height: calc(100vh - 110px);
  overflow-y: auto;
}
</style>
