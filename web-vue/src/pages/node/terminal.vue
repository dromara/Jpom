<template>
  <div id="xterm" class="xterm" />
</template>
<script>
import 'xterm/css/xterm.css';
import { Terminal } from 'xterm';
import { FitAddon } from 'xterm-addon-fit';
import { AttachAddon } from 'xterm-addon-attach';
export default {
  props: {
    ssh: {
      type: Object
    }
  },
  data() {
    return {
      socket: null,
      terminal: null
    }
  },
  computed: {
    socketUrl() {
      const protocol = location.protocol === 'https' ? 'wss://' : 'ws://';
      return `${protocol}192.168.12.59:8080/ssh?userId=9f4ed3b6bef32f3b2572ac8fd69ce052&sshId=f2f00f94f05040e986c04f25421867c4&nodeId=121&type=ssh`;
    }
  },
  mounted() {
    this.initSocket();
  },
  methods: {
    // 初始化 WebSocket
    initSocket() {
      this.socket = new WebSocket(this.socketUrl);
      // 链接成功后
      this.socket.onopen = (res) => {
        console.log(res)
        this.initTerminal();
      }
    },
    // 初始化 Terminal 
    initTerminal() {
      const term = new Terminal({
        fontSize: 14,
        cursorBlink: true
      });
      const attachAddon = new AttachAddon(this.socket);
      const fitAddon = new FitAddon();
      term.loadAddon(attachAddon);
      term.loadAddon(fitAddon);
      term.open(document.getElementById('xterm'));
      fitAddon.fit();
      term.focus();
      this.terminal = term
    },
  }
}
</script>