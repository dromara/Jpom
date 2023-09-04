export default {
    node_layout:{
        nginx:{
            cert:{
                alert:"当前功能将择机下架，请提前使用服务端证书管理来统一实现证书分发",
                configTemp:"Cert 配置模板",
                deleteContent:"真的要删除该记录么？",
                downloadContent:"正在下载，请稍等...",
            },
            list:{
                alert:"当前功能将择机下架，请提前使用 DSL 项目管理来代替 nginx 管理",
                editNginx:"编辑 Nginx 服务",
                openNginx:"启动 Nginx",
                reloadNginx:"重新加载 Nginx",
                closeNginx:"停止 Nginx",
                nginxManage:"nginx 管理是指在想编辑配置文件，并自动重新加载(reload)",
                li_1:"linux 服务器默认执行 nginx -s reload 、service xxxx start、service xxxx top",
                li_2:"linux 服务器如果为编译安装则需要将 nginx 服务名称配置到 nginx执行文件的绝对路径，如",
                li_3:"windows 服务器是需要提前安装 nginx 并配置服务,默认执行 net start xxxx、net stop xxxx、net、sc query xxxx",
                handleEdit:"`名称：${text}  server 节点数 ${record.serverCount}`",
                editNginxConfig:"编辑 Nginx 配置文件",
                whitePath:"白名单路径",
                chooseWhitePath:"请选择白名单路径",
                fileNameMessage:"需要以 .conf 结尾",
                editMessage:"编辑 Nginx 服务名称",
                serviceNameMessage:"请输入 Nginx 服务名称",
                reallyWant:"真的要",
                ifPermDelete:"永久删除文件么？",
                ifRestoreConfig:"还原配置文件么？",
                ifDelete:"删除文件么？"
            }
        },
        other:{
            script_console:{
                addArgs:"添加运行参数",
                paramsHelp:'所有参数将拼接成字符串以空格分隔形式执行脚本,需要注意参数顺序和未填写值的参数将自动忽略',
                paramsBefore:"`参数${index + 1}值`",
                paramsAfter:"`参数值 ${item.desc ? ',' + item.desc : ''}`",
                socketErrMessage:"web socket 错误,请检查是否开启 ws 代理",
                socketOncloseMessage:"会话已经关闭[node-script-consloe]",

            },
            script_list:{

            },
            script_log:{
                clearCache:"清除服务端缓存节点所有的脚步模版信息并重新同步",
                serverSync:'服务端分发同步的脚本不能直接删除,需要到服务端去操作',
                editScript:"编辑 Script",
                serverSyncMessage:"服务端同步的脚本不能在此修改",
                scriptName:"Script 名称",
                scriptContent:"Script 内容"
            },
        }
    }
}
