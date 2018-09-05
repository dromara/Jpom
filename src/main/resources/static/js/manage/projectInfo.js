layui.use(['layer', 'element', 'table', 'form'], function () {
    var $ = layui.$;
    var table = layui.table;
    var form = layui.form;
    var layer = layui.layer;

    table.render({
        id: 'table_project',
        elem: '#tab_project',
        url: '/manage/getProjectInfo',
        height: 'full-52',
        even: true,
        cols: [[
            {field: 'id', title: '项目名称', width: '10%'},
            {field: 'tag', title: 'Tag', width: '10%'},
            {field: 'mainClass', title: 'MainClass', width: '10%'},
            {field: 'lib', title: 'Lib', width: '10%'},
            {field: 'log', title: 'Log', width: '10%'},
            {field: 'port', title: 'Port', width: '5%'},
            {field: 'token', title: 'Token', width: '10%'},
            {field: 'createTime', title: '创建时间', width: '10%'},
            {field: 'op', title: '操作', align: 'center', toolbar: '#bar_projects', fixed: 'right'}
        ]],
        loading: true,
        response: {
            statusCode: 200
        }
    });

    // '添加项目'点击事件
    $('#addProject').on('click', function () {
        // 重置表单
        document.getElementById('form_project').reset();
        $('#form_project [name="id"]').attr('readonly', false).removeClass('layui-disabled');
        // 弹出
        layer.open({
            type: 1,
            title: '新增项目',
            content: $('#div-project'),
            area: ['80%', '80%'],
            btnAlign: 'c',
            btn: ['提交'],
            yes: function (index, layero) {
                $('#form_project').attr('action', '/manage/addProject');
                $('#project_submit').click();
            }
        });
    });

    // '刷新表格'点击事件
    $('#refresh').on('click', function () {
        table.reload('table_project', {height: 'full-52'});
    });

    // '修改启动文件'点击事件
    $('#updateRunBoot').on('click', function () {
        $.ajax({
            url: '/file/getRunBoot',
            type: 'get',
            dataType: 'json',
            success: function (data) {
                if (200 == data.code) {
                    $('#ta_runboot').val(data.data.content);
                    layer.open({
                        type: 1,
                        title: '修改启动文件',
                        content: $('#div-runboot'),
                        area: ['80%', '80%'],
                        btnAlign: 'c',
                        btn: ['提交'],
                        yes: function (index, layero) {
                            $('#form_runboot').attr('action', '/file/saveRunBoot');
                            $('#runboot_submit').click();
                        }
                    });
                } else {
                    layer.msg('启动文件不存在');
                }
            },
            error: function (error) {
                console.error(error);
            }
        });
    });

    // 提交项目表单
    form.on('submit(submitProject)', function (data) {
        $.ajax({
            url: data.form.action,
            type: 'POST',
            dataType: 'json',
            data: data.field,
            success: function (data) {
                if (200 == data.code) {
                    layer.closeAll('page');

                    // 刷新项目列表
                    table.reload('table_project', {height: 'full-52'});
                } else {
                    layer.msg(data.msg);
                }
            },
            error: function (err) {
                layer.msg('操作失败！');
            }
        });
        return false;
    });

    // 提交启动文件表单
    form.on('submit(submitRunBoot)', function (data) {
        $.ajax({
            url: data.form.action,
            type: 'POST',
            dataType: 'json',
            data: data.field,
            success: function (data) {
                if (200 == data.code) {
                    layer.closeAll('page');

                    layer.msg('修改成功！');
                } else {
                    layer.msg('修改失败！');
                }
            },
            error: function (err) {
                layer.msg('操作失败！');
            }
        });
        return false;
    });

    // 表单验证
    form.verify({
        port: function (value, item) {
            if (!/^[0-9]{2,5}$/.test(value) || 80 > value || value > 65530) {
                return 'Port请填写80-65530之间，避免占用系统进程端口！';
            }
        }
    });


    // 表格工具条事件
    table.on('tool(tab_project)', function (obj) {
        var data = obj.data;
        var event = obj.event;

        if ('update' === event) {
            // 配置
            updateApplication(data);
        } else if ('delete' === event) {
            // 删除
            deleteApplication(data);
        } else if ('manage' === event) {
            // 管理
            manageApplication(data);
        } else if ('file' === event) {
            fileManage(data);
        }
    });

    // 文件管理
    function fileManage(data) {
        var lay_id = 'tab_file_' + data.id;
        var url = '/file/filemanage?id=' + data.id;

        // 如果存在选项卡，切换，否则创建
        if (top.layui.$('[lay-id="tab_file_' + data.id + '"]').length > 0) {

            top.layui.element.tabChange('mainTabs', lay_id);
        } else {

            // 创建
            var tab_content = '<iframe src="' + url + '" frameborder="0" class="custom-iframe"></iframe>';
            top.layui.element.tabAdd('mainTabs', {
                title: data.id + ' - 文件',
                content: tab_content,
                id: lay_id
            });

            // 创建完后切换
            top.layui.element.tabChange('mainTabs', lay_id);
        }
    }

    // 管理
    function manageApplication(data) {

        var lay_id = 'tab_' + data.id;
        var url = '/manage/console?id=' + data.id;

        // 如果存在选项卡，切换，否则创建
        if (top.layui.$('[lay-id="tab_' + data.id + '"]').length > 0) {

            top.layui.element.tabChange('mainTabs', lay_id);
        } else {

            // 创建
            var tab_content = '<iframe src="' + url + '" frameborder="0" class="custom-iframe"></iframe>';
            top.layui.element.tabAdd('mainTabs', {
                title: data.id + ' - 管理',
                content: tab_content,
                id: lay_id
            });

            // 创建完后切换
            top.layui.element.tabChange('mainTabs', lay_id);
        }
    }

    // 配置项目
    function updateApplication(data) {
        // 重置表单
        document.getElementById('form_project').reset();
        $('#form_project [name="id"]').attr('readonly', true).addClass('layui-disabled');

        // 设置表单值
        for (var key in data) {
            $('#form_project [name="' + key + '"]').val(data[key]);
        }

        // 弹出
        layer.open({
            type: 1,
            title: '配置项目',
            content: $('#div-project'),
            area: ['80%', '80%'],
            btnAlign: 'c',
            btn: ['提交'],
            yes: function (index, layero) {
                $('#form_project').attr('action', '/manage/updateProject');
                $('#project_submit').click();
            }
        });
    }

    // 删除项目
    function deleteApplication(data) {
        layer.confirm('确定删除项目 ' + data.id + '？', {title: '系统提示'}, function (index) {
            layer.close(index);
            $.ajax({
                url: '/manage/deleteProject',
                type: 'POST',
                dataType: 'json',
                data: {id: data.id},
                success: function (data) {
                    if (200 == data.code) {
                        layer.msg('删除成功！');
                        // 刷新项目列表
                        table.reload('table_project', {height: 'full-52'});
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