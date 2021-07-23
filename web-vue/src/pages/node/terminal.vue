<template>
	<div id="xterm" class="xterm" style="height: 70vh"/>
</template>
<script>
import 'xterm/css/xterm.css';
import "xterm/lib/xterm.js";
import {Terminal} from 'xterm';
import {FitAddon} from 'xterm-addon-fit';
import {AttachAddon} from 'xterm-addon-attach';
import {mapGetters} from 'vuex';

// https://blog.csdn.net/qq_41840688/article/details/108636267

export default {
	props: {
		sshId: {
			type: String
		},
		nodeId: {
			type: String,
			default: 'system'
		},
		tail: {
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
			heart: null
		}
	},
	computed: {
		...mapGetters([
			'getLongTermToken'
		]),
		socketUrl() {
			const protocol = location.protocol === 'https:' ? 'wss://' : 'ws://';
			const domain = window.routerBase;
			const url = (domain + '/ssh').replace(new RegExp('//', 'gm'), '/');
			return `${protocol}${location.host}${url}?userId=${this.getLongTermToken}&sshId=${this.sshId}&nodeId=${this.nodeId}&type=ssh&tail=${this.tail}`;
		}
	},
	mounted() {
		this.$nextTick(() => {
			setTimeout(() => {
				this.initSocket();
			}, 200);
		})
	},
	beforeDestroy() {
		this.socket && this.socket.close()
		this.terminal && this.terminal.dispose()
		clearInterval(this.heart);
	},
	methods: {
		// 初始化 WebSocket
		initSocket() {
			this.socket = new WebSocket(this.socketUrl);
			// 连接成功后
			this.socket.onopen = () => {
				this.initTerminal();
			}
		},
		// 初始化 Terminal
		initTerminal() {
			this.terminal = new Terminal({
				fontSize: 14,
				cursorBlink: true,
				// Whether input should be disabled.
				disableStdin: false,
				rendererType: 'canvas',
			});
			// const attachAddon = new AttachAddon(this.socket, { bidirectional: false });
			const attachAddon = new AttachAddon(this.socket);
			const fitAddon = new FitAddon();
			this.terminal.loadAddon(attachAddon);
			this.terminal.loadAddon(fitAddon);
			this.terminal.open(document.getElementById('xterm'));
			this.terminal.focus();
			fitAddon.fit();
			// 创建心跳，防止掉线
			this.heart = setInterval(() => {
				let op = {
					'data': 'jpom-heart'
				}
				this.socket.send(JSON.stringify(op));
			}, 5000);
			// this.terminal.onKey(data => {
			//   this.keyCode = data.domEvent.keyCode;
			//   // 将输入的字符打印到黑板中
			//   this.terminal.write(data.key);
			//   // 输入回车
			//   if (this.keyCode === 13) {
			//     // 使用 webscoket 发送数据
			//     let op = {
			//       'data': this.text + '\r'
			//     }
			//     this.socket.send(JSON.stringify(op));
			//     this.text = '';
			//     return;
			//   }
			//   // 删除按钮
			//   if (this.keyCode === 8) {
			//     // 截取字符串[0,lenth-1]
			//     this.text = this.text.substr(0,this.text.length-1);
			//     // 清空当前一条的命令(光标前移 n 个字符，删除光标之后的数据)
			//     this.terminal.write(`\x1b[${this.text.length + 1}D\x1b[0J`);
			//     // 简化当前的新的命令显示上
			//     this.terminal.write(this.text);
			//     return;
			//   }
			//   // 将每次输入的字符拼凑起来
			//   this.text += data.key;
			// })
		},
	}
}
</script>
