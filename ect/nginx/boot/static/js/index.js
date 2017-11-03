layui.use(['layer', 'element', 'form'], function() {
	var $ = layui.$;
	var element = layui.element;
	var layer = layui.layer;
	var form = layui.form;

	// 左侧导航条事件
	element.on('nav(menu)', function(data) {
		var options = eval('(' + this.getAttribute('data-options') + ')');
		var lay_id = 'tab_' + options.id;

		// 如果存在选项卡，切换，否则创建
		if ($('[lay-id="tab_' + options.id + '"]').length > 0) {
		
			element.tabChange('mainTabs', lay_id);
		} else {
		
			// 创建
			var tab_content = '<iframe src="' + options.url + '" frameborder="0" class="custom-iframe"></iframe>';
			element.tabAdd('mainTabs', {
				title: options.title,
				content: tab_content,
				id: lay_id
			});
		
			// 创建完后切换
			element.tabChange('mainTabs', lay_id);
		}
	});

	// 操作
	element.on('nav(tab_operation)', function(elem) {
		var op = this.getAttribute('op');

		if ('refresh' == op) {
			// 刷新当前页

		} else if ('closeother' == op) {
			// 关闭其他页面

		} else if ('closeall' == op) {
			// 关闭全部页面

		}

	});

	// 用户操作
	element.on('nav(userOperation)', function(elem) {
		var op = this.getAttribute('op');
		
		// 修改密码
		if ('updatePwd' == op) {
			document.getElementById('form_updatePwd').reset();
			layer.open({
				type: 1,
				title: '修改密码',
				btn: ['确认'],
				content: $('#div-updatePwd'),
				area: '300px',
				yes: function(index, layero) {
					$('#pwd_submit').click();
					layer.close(index);
				},
				cancel: function() {
					console.log('cancel');
				}
			});
		}
	});

	// 表单验证
	form.verify({
		pass: [/^[\S]{6,12}$/, '密码必须6-12位，且不能出现空格！'],
		confirmPwd: function(value, item) {
			if(value != $('#newPwd').val()) {
				return '两次输入的密码不一致！'
			}
		}
	});

	// 提交修改密码表单
	form.on('submit(updatePwd)', function(data) {
		$.ajax({
			url: '/updatePwd',
			type: 'POST',
			dataType: 'json',
			data: data.field,
			success: function(data) {
				if(200 == data.code) {
					layer.confirm('修改成功，请重新登录！', {'title': '系统提示'}, function(index_confirm) {
						
						window.location.href = '/'
						layer.close(index_confirm);
					});
				} else {
					layer.alert(data.msg);
				}
			},
			error: function(err) {
				layer.alert('修改失败！');
			}
		});
		return false;
	});

	// 退出系统
	$('#li-exit').click(function() {
		layer.confirm('确定退出系统？', {'title': '系统提示'}, function(index) {
			window.location.href = "/logout";
			layer.close(index);
		})
	});
});