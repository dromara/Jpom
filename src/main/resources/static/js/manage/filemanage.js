layui.use(['layer', 'element', 'table', 'form', 'upload'], function () {
    var $ = layui.$;
    var table = layui.table;
    var form = layui.form;
    var layer = layui.layer;
    var upload = layui.upload;

    table.render({
        id: 'table_file',
        elem: '#tab_file',
        url: '/file/getFileList',
        method: 'post',
        height: 'full-52',
        even: true,
        where: {
            id: id
        },
        cols: [[
            {field: 'index', title: '编号', width: '10%'},
            {field: 'filename', title: '文件名称', width: '30%'},
            {field: 'modifytime', title: '修改时间', width: '15%'},
            {field: 'filesize', title: '文件大小', width: '15%'},
            {field: 'op', title: '操作', toolbar: '#bar_projects'}
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
        before: function () {
            layer.load(1, {
                shade: [0.5, '#fff'] //0.1透明度的白色背景
            });
        },
        allDone: function (obj) {
            layer.msg(JSON.stringify(obj));
            setTimeout(function () {
                table.reload('table_file', {height: 'full-52'});
                layer.closeAll();
            }, 2000);
        },
        done: function (res, index, upload) {
            //layer.msg('success');
        },
        error: function () {
            layer.msg('fail');
        }
    });

    // 点击'上传文件'事件
    $('#refresh').on('click', function () {
        table.reload('table_file', {height: 'full-52'});
    });

    // 点击'上传文件'事件
    $('#clear').on('click', function () {
        layer.confirm('确定清空此项目文件吗？',
            {title: '系统提示'},
            function (index) {
                layer.close(index);
                $.ajax({
                    url: '/file/clear',
                    type: 'POST',
                    dataType: 'json',
                    data: {id: id},
                    success: function (data) {
                        if (200 === data.code) {
                            layer.msg(data.msg);
                            // 刷新项目列表
                            table.reload('table_file', {height: 'full-52'});
                        } else {
                            layer.msg(data.msg);
                        }
                    },
                    error: function (err) {
                        layer.msg('删除失败！');
                    }
                });
            });
    });


    // 表格工具条事件
    table.on('tool(tab_file)', function (obj) {
        var data = obj.data;
        var event = obj.event;
        if ('delete' === event) {
            // 删除文件
            deleteFile(data);
        }
    });

    // 删除文件
    function deleteFile(data) {
        layer.confirm('确定删除文件 ' + data.filename + '？', {title: '系统提示'}, function (index) {
            layer.close(index);
            $.ajax({
                url: '/file/deleteFile',
                type: 'POST',
                dataType: 'json',
                data: {
                    id: data.projectid,
                    filename: data.filename
                },
                success: function (data) {
                    if (200 === data.code) {
                        layer.msg('删除成功！');
                        // 刷新项目列表
                        table.reload('table_file', {height: 'full-52'});
                    } else {
                        layer.msg(data.msg);
                    }
                },
                error: function (err) {
                    layer.msg('删除失败！');
                }
            });
        });
    }
});