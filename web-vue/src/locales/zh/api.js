export default {
  backup: {
    full: "全量备份",
    partial: "部份备份",
    import: "导入备份",
    auto: "自动备份",

    processing: "处理中",
    processSucceed: "处理成功",
    processFailed: "处理失败",
  },

  buildInfo: {
    loadingBranchList: "正在加载项目分支",

    status: {
      building: "构建中",
      buildCompleted: "构建完成",
      buildFailed: "构建失败",
      publishing: "发布中",
      publishSucceed: "发布成功",
      publishFailed: "发布失败",
      cancelBuild: "取消构建",
      buildInterrupted: "构建中断",
      queueWaiting: "队列等待",
      abnormalShutdown: "异常关闭",
    },

    releaseMethod: {
      noRelease: "不发布",
      nodeDistribution: "节点分发",
      project: "项目",
      ssh: "SSH",
      localCommand: "本地命令",
      dockerImage: "Docker 镜像",
    }
  },

  triggerBuildType: {
    manual: "手动",
    trigger: "触发器",
    cron: "定时",
  },

  buildMethod: {
    local: "本地构建",
    container: "容器构建",
  },

  command: {
    status: {
      executing: "执行中",
      executeCompleted: "执行结束",
      executeError: "执行错误",
      sessionException: "会话异常",
    },

    triggerExecType: {
      manual: "手动",
      auto: "自动",
      trigger: "触发器",
    }
  },

  config: {
    prompt: "提示信息 ",

    requestInterceptor: {
      loading: "加载数据中，请稍候...",
    },

    responseInterceptor: {
      networkError: "网络开了小差！请重试...：",
    },

    wrapResult: {
      forbidden: "禁止访问",
      forbiddenDesc: "禁止访问,当前 IP 限制访问",
    },

    jwt: {
      renew: "登录信息过期，尝试自动续签...",
      renewDesc: "如果不需要自动续签，请修改配置文件。该续签将不会影响页面。",
    },
  },

  dispatch: {
    afterOptList: {
      noOp: "不做任何操作",
      concurrentExecution: "并发执行",
      fullSequenceExecution: "完整顺序执行（有执行失败将结束本次）",
      sequenceExecution: "顺序执行（有执行失败将继续）",
    },

    afterOptListSimple: {
      noOp: "不做任何操作",
      restart: "执行重启",
    },

    dispatchStatus: {
      notDispatched: "未分发",
      dispatching: "分发中",
      dispatchSucceed: "分发成功",
      dispatchFailed: "分发失败",
      systemCancelDispatch: "系统取消分发",
      readyDispatch: "准备分发",
      manualCancelDispatch: "手动取消分发",
    },

    status: {
      notDispatched: "未分发",
      dispatching: "分发中",
      dispatchFinished: "分发结束",
      cancelDispatch: "取消分发",
      dispatchFailed: "分发失败",
    }
  }
}
