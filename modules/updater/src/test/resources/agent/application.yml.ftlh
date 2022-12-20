jpom:
  # jpom 数据存储路径, 如果调试模式运行默认路径为【<#noparse>${user.home}</#noparse>/jpom/】,安装运行默认为jar包文件的父级
  path: ${jpom.path!''}
  authorize:
    # agent 端管理账号，必填。
    agent-name: ${jpom.authorize.agentName!'jpomAgent'}
    # agent 端管理密码，非必填，如果为空Jpom 会自动生成一串随机字符串当密码
    agent-pwd: ${jpom.authorize.agentPwd!''}
  whitelist:
    # 白名单目录是否验证包含关系
    check-starts-with: ${whitelistDirectory.checkStartsWith!true}
  project:
    # 停止、启动项目(项目状态检测)等待的时长 单位秒
    status-wait-time: ${project.stopWaitTime!10}
    # 项目文件备份保留个数,大于 0 才会备份
    file-backup-count: ${project.fileBackupCount!1}
    # 限制备份指定文件后缀（支持正则）
    file-backup-suffix: ${project.fileBackupSuffix!"[ '.jar','.html','^.+\\.(?i)(txt)$' ]"}
    # 项目状态检测间隔时间 单位毫秒，最小为1毫秒
    status-detection-interval: ${project.statusDetectionInterval!500}
    log:
      # 检测控制台日志周期，防止日志文件过大，目前暂只支持linux 不停服备份
      auto-backup-console-cron: ${log.autoBackConsoleCron!'0 0/10 * * * ?'}
      # 控制台日志文件保留大小
      auto-backup-size: ${log.autoBackSize!'50MB'}
      # 是否自动将控制台日志文件备份
      auto-backup-to-file: ${log.autoBackToFile!true}
      # 保存天数 配置错误或者没有，默认是7天
      save-days: ${log.saveDays!7}
      # 日志文件的编码格式
      file-charset:${log.fileCharset!''}
  # 查看日志时初始读取最后多少行（默认10，0不读取）
  init-read-line: ${log.intiReadLine!10}
  system:
    # cron 定时器是否开启匹配秒
    timer-match-second: ${system.timerMatchSecond!false}
    # 旧包文件保留个数
    old-jars-count: ${system.oldJarsCount!2}
    # Check the url for the new version
    remote-version-url:${system.remoteVersionUrl!''}
    # 系统日志编码格式
    log-charset: UTF-8
    # 控制台编码格式
    console-charset:${consoleLog.charset!''}

server:
  #运行端口号
  port: 2123
  servlet:
    encoding:
      charset: UTF-8
      force: true
      enabled: true
      forceRequest: true
      forceResponse: true
  tomcat:
    uri-encoding: UTF-8
spring:
  servlet:
    session:
      timeout: 1H
      cookie:
        name: JPOMID-AGENT
    multipart:
      # 上传文件大小限制 12KB -- parses as 12 kilobytes 5MB  -- parses as 5 megabytes 20  -- parses as 20 kilobytes
      max-request-size: 2GB
      max-file-size: 1GB
  mvc:
    throw-exception-if-no-handler-found: true
    log-request-details: true
