<template>
  <div id="xterm" class="xterm" />
</template>
<script>
import "xterm/css/xterm.css";
import "xterm/lib/xterm.js";
import { Terminal } from "xterm";
// import { FitAddon } from "xterm-addon-fit";
import { AttachAddon } from "xterm-addon-attach";

// https://blog.csdn.net/qq_41840688/article/details/108636267

export default {
  props: {
    url: {
      type: String,
    },
  },
  data() {
    return {
      socket: null,
      terminal: null,
      // 输入的字符
      text: "",
      keyCode: -1,
      heart: null,
      rows: 40,
      cols: 100,
      wp: 0,
      hp: 0,
    };
  },
  computed: {},
  mounted() {
    this.$nextTick(() => {
      setTimeout(() => {
        this.initSocket();
      }, 200);
    });
  },
  beforeDestroy() {
    this.socket && this.socket.close();
    this.terminal && this.terminal.dispose();
    clearInterval(this.heart);
  },
  methods: {
    // 初始化 WebSocket
    initSocket() {
      this.socket = new WebSocket(this.url);
      // 连接成功后
      this.socket.onopen = () => {
        this.initTerminal();
      };
      this.socket.onerror = (err) => {
        console.error(err);
        this.$notification.error({
          message: "web socket 错误,请检查是否开启 ws 代理",
        });
        clearInterval(this.heart);
      };
      this.socket.onclose = (err) => {
        //当客户端收到服务端发送的关闭连接请求时，触发onclose事件
        console.error(err);
        this.$notification.info({
          message: "会话已经关闭",
        });
        clearInterval(this.heart);
      };
    },
    // 初始化 Terminal
    initTerminal() {
      // 获取容器宽高/字号大小，定义行数和列数
      // // this.hp = ;
      // this.wp = 100;
      //;
      this.rows = document.querySelector("#xterm").offsetHeight / 16;
      this.cols = document.querySelector("#xterm").offsetWidth / 8;
      this.hp = this.rows * 8;
      this.wp = this.cols * 8;
      //
      this.terminal = new Terminal({
        fontSize: 14,
        rows: parseInt(this.rows), //行数
        cols: parseInt(this.cols), // 不指定行数，自动回车后光标从下一行开始
        convertEol: true, //启用时，光标将设置为下一行的开头
        cursorBlink: true,
        // Whether input should be disabled.
        disableStdin: false,
        rendererType: "canvas",
        theme: {
          // foreground: "#7e9192", //字体
          // background: "#002833", //背景色
          cursor: "help", //设置光标
          lineHeight: 16,
        },
      });
      // const attachAddon = new AttachAddon(this.socket, { bidirectional: false });
      const attachAddon = new AttachAddon(this.socket);
      // const fitAddon = new FitAddon();
      this.terminal.loadAddon(attachAddon);
      // this.terminal.loadAddon(fitAddon);
      this.terminal.open(document.getElementById("xterm"));
      this.terminal.focus();
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
      this.sendJson({ data: "resize", cols: this.cols, rows: this.rows, wp: this.wp, hp: this.hp });
      // 创建心跳，防止掉线
      this.heart = setInterval(() => {
        let op = {
          data: "jpom-heart",
        };
        this.sendJson(op);
      }, 5000);
    },
    sendJson(data) {
      this.socket.send(JSON.stringify(data));
    },
  },
};
</script>

<style scoped>
.xterm {
  display: flex;
  flex-flow: column;
  height: 100%;
  flex: 1;
}
</style>
