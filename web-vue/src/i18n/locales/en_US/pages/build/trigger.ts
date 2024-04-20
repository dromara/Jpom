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
    warmPrompt: 'Kind tips',
    copyByClick: 'Click to copy'
  },
  p: {
    resetTriggerTokenInfo:
      'Reset trigger token information, the previous trigger token will be invalidated after reset',
    reset: 'Reset',
    executeBuild: 'Execute build',
    singleTriggerAddress: 'Single trigger address',
    batchTriggerAddress: 'Batch trigger address',
    viewCurrentStatus: 'View current status',
    batchBuildParamsBodyJson: 'Batch build parameters BODY json: [ { "id":"1", "token":"a" } ]',
    idAndTokenSameAsTriggerBuild: 'The id and token in the parameters are the same as the trigger build',
    notBuilt: 'Not built',
    building: 'Building',
    buildEnded: 'Build ended',
    buildFailed: 'Build failed',
    publishing: 'Publishing',
    publishSuccess: 'Publish success',
    publishFailed: 'Publish failed',
    cancelBuild: 'Cancel build',
    getSingleBuildStatusAddress: 'Get single build status address',
    batchGetBuildStatusAddress: 'Batch get build status address',
    viewBuildLog: 'View build log',
    idAndTokenAndBuildNumIdSameAsTriggerBuild:
      'The id, token, and buildNumId in the parameters are the same as the trigger build',
    replaceBuildNumIdAccordingToActualSituation:
      'The buildNumId needs to be replaced according to the actual situation',
    getSingleBuildLogAddress: 'Get single build log address',
    noTriggerGeneratedForCurrentBuild: 'No trigger has been generated for the current build',
    generateNow: 'Generate now',
    warmPrompt1:
      'In a single trigger address: the first random string is the build ID, and the second random string is the token',
    warmPrompt2:
      'Reset to regenerate the trigger address. After the reset is successful, the previous trigger address will be invalid. The build trigger is bound to the generated trigger to the operator. If the corresponding account is deleted, the trigger will be invalid.',
    warmPrompt3: 'Batch build parameters',
    warmPrompt4:
      'Batch build parameters also support specified parameters, delay (delayed execution of build, unit seconds) branchName (branch name), branchTagName (tag), script (build script), resultDirFile (build product), webhook (notification webhook)',
    warmPrompt5: 'Example of building all parameters in batches',
    warmPrompt6: 'If the parameters are passed in',
    warmPrompt7:
      'Microqueues will be used to queue builds to avoid interrupted builds that are triggered almost simultaneously (generally user warehouse merge code will trigger multiple requests). The queue is saved in memory and will be lost upon restart.',
    warmPrompt8: 'Passing in other parameters in batch build will execute the modification synchronously'
  }
}
