///
/// Copyright (c) 2019 Of Him Code Technology Studio
/// Jpom is licensed under Mulan PSL v2.
/// You can use this software according to the terms and conditions of the Mulan PSL v2.
/// You may obtain a copy of Mulan PSL v2 at:
/// 			http://license.coscl.org.cn/MulanPSL2
/// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
/// See the Mulan PSL v2 for more details.
///

export default {
  c: {
    warmPrompt: '温馨提示',
    copyByClick: '点击可以复制'
  },
  p: {
    resetTriggerTokenInfo: '重置触发器 token 信息,重置后之前的触发器 token 将失效',
    reset: '重置',
    executeBuild: '执行构建',
    singleTriggerAddress: '单个触发器地址',
    batchTriggerAddress: '批量触发器地址',
    viewCurrentStatus: '查看当前状态',
    batchBuildParamsBodyJson: '批量构建参数 BODY json： [ { "id":"1", "token":"a" } ]',
    idAndTokenSameAsTriggerBuild: '参数中的 id 、token 和触发构建一致',
    notBuilt: '未构建',
    building: '构建中',
    buildEnded: '构建结束',
    buildFailed: '构建失败',
    publishing: '发布中',
    publishSuccess: '发布成功',
    publishFailed: '发布失败',
    cancelBuild: '取消构建',
    getSingleBuildStatusAddress: '获取单个构建状态地址',
    batchGetBuildStatusAddress: '批量获取构建状态地址',
    viewBuildLog: '查看构建日志',
    idAndTokenAndBuildNumIdSameAsTriggerBuild: '参数中的 id 、token 和触发构建一致、buildNumId 构建序号id',
    replaceBuildNumIdAccordingToActualSituation: '构建序号id需要跟进实际情况替换',
    getSingleBuildLogAddress: '获取单个构建日志地址',
    noTriggerGeneratedForCurrentBuild: '当前构建还没有生成触发器',
    generateNow: '现成生成',
    warmPrompt1: '单个触发器地址中：第一个随机字符串为构建ID，第二个随机字符串为 token',
    warmPrompt2:
      '重置为重新生成触发地址,重置成功后之前的触发器地址将失效,构建触发器绑定到生成触发器到操作人上,如果将对应的账号删除触发器将失效',
    warmPrompt3: '批量构建参数',
    warmPrompt4:
      '批量构建参数还支持指定参数,delay（延迟执行构建,单位秒）branchName（分支名）、branchTagName（标签）、script（构建脚本）、resultDirFile（构建产物）、webhook（通知webhook）',
    warmPrompt5: '批量构建全部参数举例',
    warmPrompt6: '参数如果传入',
    warmPrompt7:
      '将使用微队列来排队构建，避免几乎同时触发构建被中断构建（一般用户仓库合并代码会触发多次请求）,队列保存在内存中,重启将丢失',
    warmPrompt8: '批量构建传入其他参数将同步执行修改'
  }
}
