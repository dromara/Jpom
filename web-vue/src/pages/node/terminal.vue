<template>
  <div id="xterm" class="xterm" />
</template>
<script>
import 'xterm/css/xterm.css';
import { Terminal } from 'xterm';
import { FitAddon } from 'xterm-addon-fit';
import { AttachAddon } from 'xterm-addon-attach';
import { mapGetters } from 'vuex';
export default {
  props: {
    ssh: {
      type: Object
    }
  },
  data() {
    return {
      socket: null,
      terminal: null,
      // 输入的字符
      text: '',
      keyCode: -1
    }
  },
  computed: {
    ...mapGetters([
      'getToken'
    ]),
    socketUrl() {
      const protocol = location.protocol === 'https' ? 'wss://' : 'ws://';
      return `${protocol}${location.host}/ssh?userId=${this.getToken}&sshId=${this.ssh.id}&nodeId=${this.ssh.nodeModel.id}&type=ssh&tail=`;
    }
  },
  mounted() {
    this.initSocket();
  },
  beforeDestroy() {
    this.socket.close()
    this.terminal.dispose()
  },
  methods: {
    // 初始化 WebSocket
    initSocket() {
      console.log(this.socketUrl)
      this.socket = new WebSocket(this.socketUrl);
      // 连接成功后
      this.socket.onopen = () => {
        this.initTerminal();
      }
      // 接收到消息
      // this.socket.onmessage = (msg) => {
      //   console.log('msg')
      //   console.log(new window.TextDecoder.decode(msg.data))
      //   // this.terminal.write(new TextDecoder.decode(msg.data));
      // }
    },
    // 初始化 Terminal 
    initTerminal() {
      this.terminal = new Terminal({
        fontSize: 14,
        cursorBlink: true,
        // Whether input should be disabled.
        disableStdin: false,
      });
      this.terminal.open(document.getElementById('xterm'));
      const attachAddon = new AttachAddon(this.socket, { bidirectional: false });
      const fitAddon = new FitAddon();
      this.terminal.loadAddon(attachAddon);
      this.terminal.loadAddon(fitAddon);
      fitAddon.fit();
      this.terminal.focus();

      this.terminal.onKey(key => {
        // this.terminal.write(key);
        this.keyCode = key.domEvent.keyCode
        if (this.keyCode === 8) {
          if (this.text.length > 0) {
            this.text = this.text.substring(0, this.text.length - 1);
          } else {
            this.text = ''
          }
        }
        // const ev = key.domEvent;
        // const printable = !ev.altKey && !ev.altGraphKey && !ev.ctrlKey && !ev.metaKey;
        // if (ev.keyCode === 8) {
        //     // Do not delete the prompt
        //     if (this.terminal._core.buffer.x > 2) {
        //       this.terminal.write(key);
        //     }
        // }
      })
      
      this.terminal.onData(data => {
        this.text += data;
        console.log('text:', this.text, 'data', data)
        // 回车
        if (this.keyCode === 13) {
          if (this.socket.readyState == this.socket.OPEN) {
            let op = {
              'data': this.text
            }
            this.socket.send(JSON.stringify(op))
            this.text = '';
          }
        }
        if (this.keyCode !== 8 && this.keyCode !== 13 && data.length > 0) {
          this.terminal.write(data);
        }
      })
    },
  }
}
</script>