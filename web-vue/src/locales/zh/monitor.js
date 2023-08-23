export default {
    list:{
        goBackP1:"按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页",
        autoRun:"如果需要定时自动执行则填写,cron 表达式.默认未开启秒级别,需要去修改配置文件中:[system.timerMatchSecond]）",
        selectToMonitor:"选择要监控的项目,file 类型项目不可以监控",
        remindSetEmail:"如果这里的报警联系人无法选择，说明这里面的管理员没有设置邮箱，在右上角下拉菜单里面的用户资料里可以设置。",
        reqAtAlarm:"发生报警时请求",
        passParam:"传入参数有：monitorId、monitorName、nodeId、nodeName、projectId、projectName、title、content、runStatus",
        judge:"runStatus 值为 true 表示项目当前为运行中(异常恢复),false 表示项目当前未运行(发生异常)",
        receiveAlarm:"接收报警消息,非必填，GET请求",
        intro:"如果这里的报警联系人无法选择，说明这里面的管理员没有设置邮箱，在右上角下拉菜单里面的用户资料里可以设置。",
        renderItem:"如果不可以选择则表示对应的用户没有配置邮箱",
        webhookWarn:"请选择一位报警联系人或者填写webhook",
        deleteMonitor:"真的要删除监控么？"
    }
};
