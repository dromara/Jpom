layui.use(['layer', 'element', 'table', 'form', 'upload'], function() {
	var $ = layui.$;
	var table = layui.table;
	var form = layui.form;
	var layer = layui.layer;
	var upload = layui.upload;

	
	table.render({
		id: 'table_file',
		elem: '#tab_file',
		url: '/file/getFileList',
		height: 'full-52',
		even: true,
		where: {
			id: id
		},
		cols: [[
			{field: 'filename', title: '文件名称', width: '500'},
			{field: 'modifytime', title: '修改时间', width: '200'},
			{field: 'filesize', title: '文件大小', width: '150'},
			{field: 'op', title: '操作', width: '80', align: 'center', toolbar: '#bar_projects', fixed:'right'}
		]],
		loading: true,
		response: {
			statusCode: 200
		}
	});

	var uploadInst = upload.render({
		elem: '#uploadFile',
		accept: 'file',
		data: {
			id: id
		},
		multiple: true,
		url: '/file/upload',
		done: function() {
			table.reload('table_file', {height: 'full-52'});
			layer.msg('success');
		},
		error: function() {
			layer.msg('fail');
		}
	});

	// 点击'上传文件'事件
	$('#refresh').on('click', function() {
		table.reload('table_file', {height: 'full-52'});
	});

	
	// 表格工具条事件
	table.on('tool(tab_file)', function(obj) {
		var data = obj.data;
		var event = obj.event;
		if ('delete' === event) {
			// 删除文件
			deleteFile(data);
		}
	});
	
	// 删除文件
	function deleteFile(data) {
		
		layer.confirm('确定删除文件 ' + data.filename + '？', {title: '系统提示'}, function(index) {
			layer.close(index);
			$.ajax({
				url: '/file/deleteFile',
				type: 'POST',
				dataType: 'json',
				data: {id: data.projectid, filename: data.filename},
				success: function(data) {
					if (200 == data.code) {
						layer.msg('删除成功！');
						// 刷新项目列表
						table.reload('table_file', {height: 'full-52'});
					} else {
						layer.msg(data.msg);
					}
				},
				error: function(err) {
					layer.msg('删除失败！');
				}
			});
		});
	}
});