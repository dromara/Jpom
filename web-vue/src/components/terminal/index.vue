<template>
  <div class="flex-100">
    <a-result v-if="disconnect" status="warning" title="已经断开连接啦">
      <template #extra>
        <a-button type="primary" @click="initSocket">重连 </a-button>
      </template>
    </a-result>
    <div :id="domId" v-else class="flex-100"></div>
  </div>
</template>

<script>
import 'xterm/css/xterm.css'
import 'xterm/lib/xterm.js'
import { Terminal } from 'xterm'
import { FitAddon } from 'xterm-addon-fit'
import { AttachAddon } from 'xterm-addon-attach'

// https://blog.csdn.net/qq_41840688/article/details/108636267

export default {
  props: {
    url: {
      type: String
    }
  },
  data() {
    return {
      socket: null,
      terminal: null,
      // 输入的字符
      text: '',
      keyCode: -1,
      heart: null,
      rows: 40,
      cols: 100,
      wp: 0,
      hp: 0,
      disconnect: false,
      domId: 'xterm'
    }
  },
  computed: {},
  created() {
    // console.log(this.$options._scopeId)
    this.domId =
      (this.$options._parentVnode?.tag || '' + '-' + this.$options._scopeId || '') + '-' + new Date().getTime()
  },

  mounted() {
    this.$nextTick(() => {
      setTimeout(() => {
        this.initSocket()
      }, 200)
    })
    // 监听窗口关闭事件，当窗口关闭时，主动去关闭websocket连接，防止连接还没断开就关闭窗口，server端会抛异常。
    window.onbeforeunload = () => {
      this.socket?.close()
      this.dispose()
    }
  },
  beforeUnmount() {
    this.socket?.close()
    this.dispose()
  },
  methods: {
    // 初始化 WebSocket
    initSocket() {
      this.socket = new WebSocket(this.url)
      this.disconnect = false
      // 连接成功后
      this.socket.onopen = () => {
        this.initTerminal()
      }
      this.socket.onerror = (err) => {
        console.error(err)
        $notification.error({
          message: 'web socket 错误,请检查是否开启 ws 代理'
        })
        this.dispose()
      }
      this.socket.onclose = (err) => {
        //当客户端收到服务端发送的关闭连接请求时，触发onclose事件
        console.error(err)
        this.$message.warning('会话已经关闭[ssh-terminal]')
        this.dispose()
      }
    },
    dispose() {
      this.terminal && this.terminal.dispose()
      clearInterval(this.heart)
      this.disconnect = true
    },
    // 初始化 Terminal
    initTerminal() {
      // 获取容器宽高/字号大小，定义行数和列数
      // // this.hp = ;
      // this.wp = 100;
      //;
      this.rows = document.querySelector('#' + this.domId).offsetHeight / 16
      this.cols = document.querySelector('#' + this.domId).offsetWidth / 8.4
      this.hp = this.rows * 8
      this.wp = this.cols * 8
      //
      this.terminal = new Terminal({
        fontSize: 14,
        rows: parseInt(this.rows), //行数
        // 不指定行数，自动回车后光标从下一行开始
        cols: parseInt(this.cols),
        convertEol: true, //启用时，光标将设置为下一行的开头
        cursorBlink: true,
        // Whether input should be disabled.
        disableStdin: false,
        rendererType: 'canvas',
        theme: {
          // foreground: "#7e9192", //字体
          // background: "#002833", //背景色
          cursor: 'help', //设置光标
          lineHeight: 16
        }
      })
      // const attachAddon = new AttachAddon(this.socket, { bidirectional: false });
      const attachAddon = new AttachAddon(this.socket)
      const fitAddon = new FitAddon()
      attachAddon.activate(this.terminal)
      fitAddon.activate(this.terminal)
      // this.terminal.loadAddon(attachAddon)
      // this.terminal.loadAddon(fitAddon)
      this.terminal.open(document.getElementById(this.domId))
      this.terminal.focus()
      // fitAddon.fit();
      //
      // window.addEventListener("resize", () => {
      //   try {
      //     // 窗口大小改变时，触发xterm的resize方法使自适应
      //     fitAddon.fit();
      //     // 窗口大小改变时触发xterm的resize方法，向后端发送行列数，格式由后端决定
      //     this.terminal.onResize((size) => {
      //       this.sendJson({ data: "resize", cols: size.cols, rows: size.rows, wp: this.wp, hp: this.hp });
      //     });
      //   } catch (e) {
      //     console.log("e", e.message);
      //   }
      // });
      this.sendJson({
        data: 'resize',
        cols: this.cols,
        rows: this.rows,
        wp: this.wp,
        hp: this.hp
      })
      // 创建心跳，防止掉线
      this.heart = setInterval(() => {
        const op = {
          data: 'jpom-heart'
        }
        this.sendJson(op)
      }, 5000)
    },
    sendJson(data) {
      this.socket.send(JSON.stringify(data))
    }
  }
}
</script>

<style scoped>
.flex-100 {
  display: flex;
  flex-flow: column;
  height: 100%;
  flex: 1;
}
.br {
  /* box-shadow: inset 0 0 10px 0 #e8e8e8; */
}
</style>
<style></style>
