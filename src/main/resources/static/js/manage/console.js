layui.use(['layer', 'element', 'table', 'form'], function () {
    var $ = layui.$;
    var table = layui.table;
    var form = layui.form;
    var layer = layui.layer;

    var showLogDom = $('.console .terminal');

    if ('WebSocket' in window) {
        var ws = new WebSocket("ws://" + window.location.host + "/console/" + userInfo);

        ws.onopen = function () {
            showLogDom.append('WebSocket连接成功！<br/>');
            ws.send('{"op": "status", "projectInfo": ' + JSON.stringify(projectInfo) + '}');
        };

        ws.onmessage = function (data) {
            // 如果是
            if (data.data.indexOf('{') === 0) {
                var json_data = JSON.parse(data.data);
                var op = json_data.op;
                if (json_data.code !== 200) {
                    layer.msg(json_data.msg);
                }
                switch (op) {
                    case 'status':
                    case 'start':
                    case 'restart':
                        if (200 === json_data.code) {
                            $('.status .status-div').removeClass('status-stop').addClass('status-run');
                            $('.status span').text('运行中');
                            setOpBtn(false);
                            // 运行后需要实时加载日志
                            setMsg("showlog");
                        } else {
                            $('.status .status-div').removeClass('status-run').addClass('status-stop');
                            $('.status span').text(json_data.msg);
                            setOpBtn(true);
                        }
                        break;
                    case 'stop':
                        if (200 === json_data.code) {
                            $('.status .status-div').removeClass('status-run').addClass('status-stop');
                            $('.status span').text('未运行');
                            setOpBtn(true);
                        } else {
                            $('.status span').text(json_data.msg);
                        }
                        break;
                    case  'showlog':
                        if (200 === json_data.code) {
                            openLog = true;
                        }
                        break;
                    default:
                        break;
                }
            } else {
                showLogDom.append(data.data + '<br/>');
            }
            scrollToBotomm();
        };

        ws.onclose = function () {
            showLogDom.append('WebSocket连接已关闭！<br/>');
        };

        $('.btn-op').on('click', function () {
            var op = $(this).attr('op');
            setMsg(op);
        });

        function setMsg(opt) {
            var data = {
                op: opt,
                projectInfo: projectInfo
            };
            ws.send(JSON.stringify(data));
        }
    } else {
        showLogDom.html('你的浏览器不支持WebSocket！');
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

    function scrollToBotomm() {
        var h = showLogDom[0].scrollHeight;
        showLogDom.scrollTop(h);
    }

});