layui.use(['layer', 'element', 'table', 'form'], function() {
	var $ = layui.$;
	var table = layui.table;
	var form = layui.form;
	var layer = layui.layer;

	if ('WebSocket' in window) {
		var ws = new WebSocket("ws://" + window.location.host + "/console/" + userInfo);

		ws.onopen = function() {
			$('.console .terminal').append('WebSocket连接成功！<br/>');
			ws.send('{"op": "status", "projectInfo": ' + JSON.stringify(projectInfo) + '}');
		}

		ws.onmessage = function(data) {
			// 如果是
			if (data.data.indexOf('{') === 0) {

				var json_data = JSON.parse(data.data);
				var op = json_data.data && json_data.data.op;
				switch(op) {
					case 'status':
						if ('运行中' == json_data.msg) {
							$('.status .status-div').removeClass('status-stop').addClass('status-run');
							$('.status span').text('运行中');
							setOpBtn(false);
							// 运行后需要实时加载日志
							showlog();
						} else {
							$('.status .status-div').removeClass('status-run').addClass('status-stop');
							$('.status span').text('未运行');
							setOpBtn(true);
						}
						break;

					case 'stop':
						if ('已停止' == json_data.msg) {
							$('.status .status-div').removeClass('status-run').addClass('status-stop');
							$('.status span').text('未运行');
							setOpBtn(true);
						}

						break;
					case 'start':
						if ('启动成功' == json_data.msg || '启动中' == json_data.msg) {
							$('.status .status-div').removeClass('status-stop').addClass('status-run');
							$('.status span').text('运行中');
							setOpBtn(false);
							// 运行后需要实时加载日志
							showlog();
						} else {
							$('.status .status-div').removeClass('status-run').addClass('status-stop');
							$('.status span').text(json_data.msg);
							setOpBtn(true);
						}
						break;
					case 'restart':
						if ('success' == json_data.msg) {

						}
						break;

					default:
						break;
				}

				$('.console .terminal').append(json_data.msg + '<br/>');
			} else {
				$('.console .terminal').append(data.data + '<br/>');
			}
			
		}

		ws.onclose = function() {
			$('.console .terminal').append('WebSocket连接已关闭！<br/>');
		}

		$('.btn-op').on('click', function() {
			var op = $(this).attr('op');

			if ('start' === op) {
				// 启动
				startApplication(ws);
			} else if ('restart' === op) {
				// 重启
				restartApplication();
			} else if ('stop' === op) {
				// 停止
				stopApplication();
			}
		});
	} else {
		$('.console .terminal').html('你的浏览器不支持WebSocket！');
	}

	// 启动项目
	function startApplication(ws) {
		ws.send('{"op": "start", "projectInfo": ' + JSON.stringify(projectInfo) + '}');
	}
	
	// 重启项目
	function restartApplication() {
		ws.send('{"op": "restart", "projectInfo": ' + JSON.stringify(projectInfo) + '}');
	}
	
	// 停止项目
	function stopApplication() {
		ws.send('{"op": "stop", "projectInfo": ' + JSON.stringify(projectInfo) + '}');
	}

	function setOpBtn(flag) {
		if (flag) {
			$('.btn-op[op="start"]').show();
			$('.btn-op[op="stop"],.btn-op[op="restart"]').hide();
		} else {
			$('.btn-op[op="start"]').hide();
			$('.btn-op[op="stop"],.btn-op[op="restart"]').show();
		}
	}

	function showlog() {
		ws.send('{"op": "showlog", "projectInfo": ' + JSON.stringify(projectInfo) + '}');
	}

});