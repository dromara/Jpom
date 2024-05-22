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
    buildMethod: 'Build method',
    name: 'Name',
    selectRepository: 'Select repository',
    chooseRepository: 'Choose repository',
    supportWildcard: 'Support wildcard',
    matchOneCharacter: 'Match one character',
    matchZeroOrMoreCharacters: 'Match zero or more characters',
    matchZeroOrMoreDirectories: 'Match zero or more directories in the path',
    configuration: 'Configuration',
    configurationExample: 'Configuration example',
    publishOperation: 'Publish operation',
    secondLevelDirectory: 'Second level directory',
    sshNotConfigured: 'If SSH is not configured, the authorized directory cannot be selected',
    selectSSH: 'Select SSH',
    sshDirectoryConfiguration:
      'If multiple SSH are selected, only the first item in the directory below is displayed, but the authorized directory needs to ensure that each item is configured with the corresponding directory',
    yes: 'Yes',
    no: 'No',
    selectScript: 'Select script',
    chooseScript: 'Choose script',
    cancel: 'Cancel',
    confirm: 'Confirm'
  },
  p: {
    loadingBuildData: 'Loading build data',
    howToChooseBuildMethod: 'How to choose build method',
    viewAvailableContainers: 'View available containers',
    availableTags: 'Available tags',
    loadingContainerTags: 'Loading container tags',
    noContainerOrTag: 'No container or tag is configured, cannot use container build',
    containerBuildNote: 'Container build note',
    groupName: 'Group name:',
    addGroup: 'Add group',
    selectGroup: 'Select group',
    sourceRepository: 'Source repository',
    branch: 'Branch',
    customBranchWildcard: 'Custom branch wildcard expression',
    selectBranchForBuild: 'Select the branch for build, required',
    tag: 'Tag',
    customTagWildcard: 'Custom tag wildcard expression',
    selectTagForBuild: 'Select the tag for build, default to the latest commit if not selected',
    cloneDepth: 'Clone depth',
    customCloneDepth: 'Custom clone depth, to avoid cloning the entire large repository',
    buildCommand: 'Build command',
    commonBuildCommandExample: 'Common build command example',
    scriptTemplate: 'Script template',
    content: 'Content',
    buildDslConfigContent:
      'Please fill in the build DSL configuration content, you can click the tab above to switch and view the configuration example',
    artifactDirectory: 'Artifact directory',
    onlyUseFirstMatched: 'Currently only use the first matched item',
    noBuildMethodSelected: 'No build method selected',
    environmentVariables: 'Environment variables',
    enterBuildEnvVars: 'Enter build environment variables: xx=abc, multiple variables can be entered with line breaks',
    executionMethod: 'Execution method',
    default: 'Default',
    multiThread: 'Multi-thread',
    experimentalFeature: 'This option is an experimental attribute, the actual effect is basically no difference',
    selectPublishMethod: 'Select publish method',
    noPublish: 'Do not publish: only execute the build process and save the build history',
    noPublishProcess: 'Do not execute the publish process',
    distributeProject: 'Distribute project',
    selectDistributeProject: 'Select the project to distribute',
    useNodeDistributeConfig: 'If not filled, use the second-level directory of the node distribution configuration',
    publishProject: 'Publish project',
    selectNodeProject: 'Select the node project',
    publishPostOperation: 'Select the operation after publishing',
    publishToRoot: 'If not filled, publish to the root directory of the project',
    publishViaSSH: 'Publish via SSH',
    publishDirectory: 'Publish directory',
    prePublishCommand: 'Pre-publish command',
    nonMandatory: 'Optional',
    postPublishCommand: 'Post-publish command',
    mandatory: 'Mandatory',
    clearPublish: 'Clear publish',
    diffPublish: 'Diff publish:',
    preStopPublish: 'Stop before publishing:',
    executeContainer: 'Execute container',
    executeContainerTag: 'Execute container tag',
    dockerfilePath:
      'A Dockerfile is required in the repository, if multiple folders are viewed, you can specify the second-level directory, such as springboot-test-jar:springboot-test-jar/Dockerfile',
    imageTag: 'Image tag',
    containerTag:
      'Container tag, such as xxxx:latest, multiple tags are separated by commas, additional environment variable files are supported to load .env files in the repository directory, such as xxxx:${VERSION}',
    buildParams: 'Build parameters',
    buildParamsInput: 'Build parameters, such as key1=value1&keyvalue2, use URL encoding',
    imageTagInput: 'Image tag, such as key1=value1&keyvalue2, use URL encoding',
    publishCluster: 'Publish cluster',
    dockerSwarmCluster:
      'Currently using the Docker Swarm cluster, you need to create a Swarm cluster first before you can select it',
    selectClusterForPublish: 'Select the Docker cluster to publish to',
    noClusterPublish: 'Do not publish to the Docker cluster',
    pushToRepository: 'Whether to push the built image to the remote repository after the image is built successfully',
    versionIncrement: 'Version increment',
    noCacheBuild: 'Do not use cache in the process of building the image',
    updateImage: 'Update image',
    attemptToUpdateBaseImage: 'Attempt to update the new version of the base image when building the image',
    clusterService: 'Cluster service',
    selectServiceForPublish:
      'Select the service name to publish to the cluster, you need to create the service in the cluster in advance',
    cacheBuild: 'Cache build',
    retainBuildArtifacts: 'Retain artifacts:',
    diffBuild: 'Diff build:',
    strictExecution: 'Strict execution:',
    buildProcessRequest: 'Build process request, optional, GET request',
    eventScript: 'Event script',
    resetSelection: 'Reset selection',
    additionalEnvVars: 'Additional environment variables',
    additionalEnvVarsInput: 'Additional environment variables, add multiple with commas',
    fileManagementCenter: 'File management center',
    syncToFile:
      'If synchronization to the file management center is enabled, the build and publish process will automatically execute the synchronization to the file management center operation.',
    sync: 'Sync',
    noSync: 'Do not sync',
    publishHiddenFiles: 'Publish hidden files',
    defaultIgnoreHiddenFiles: 'By default, build errors will automatically ignore hidden files',
    publishAllFiles: 'After enabling this option, hidden files can be published normally',
    retainDays: 'Retention days',
    retainCount: 'Retention count',
    aliasCode: 'Alias code',
    aliasCodeInput: 'If artifacts are synchronized to the file center, the current value will be shared',
    enterAliasCode: 'Enter alias code',
    generateAliasCode: 'Generate randomly',
    retainDaysForArtifacts: 'Retention days for artifacts synchronized to the file center',
    excludePublish: 'Exclude publish',
    excludePublishAntExpression: 'Use ANT expression to filter specified directories for publishing exclusion',
    viewContainer: 'View container',
    buildCommandExample: 'Build command example',
    buildName: 'Please enter build name',
    buildMethod: 'Please select build method',
    publishAction: 'Please select publish operation',
    selectBranch: 'Please select branch',
    writeBuildCommand: 'Please enter build command',
    writeArtifactDirectory: 'Please enter artifact directory',
    repositorySelection: 'Please select the repository for build',
    basicInfo: 'Basic information',
    buildProcess: 'Build process',
    additionalConfig: 'Additional configuration',
    unknown: 'Unknown',
    nodeProjectSelection:
      'Please select node project, there may be no projects in the node, you need to create a project in the node',
    buildArtifactsPath:
      "Build artifacts path, relative to the repository path, such as java project's target/xxx.jar, vue project's dist",
    postBuildActions: 'Post-build actions',
    uploadToDirectory: 'Upload build artifacts to the corresponding directory',
    dockerfilePathTip: 'Folder path, a Dockerfile is required in the repository',
    containerTags: 'Container tags, such as: xxxx:latest. Multiple tags are separated by commas.',
    buildParamsTip: 'Build parameters, for example: key1=value1&key2=value2',
    imageTags: 'Image tags:',
    imageTagParamsTip: 'Image tag parameters, for example: key1=value1&key2=value2',
    pushToRepositoryLabel: 'Push to repository',
    selectedServiceForPublish: 'Please select the service name for publishing to the cluster',
    cacheBuildDirectory:
      'Enabling cache for the build directory will retain repository files. A second build will pull the code. Without cache, the repository code will be re-pulled for each build (large projects are not recommended to disable cache).',
    retainBuildArtifactsInfo:
      'Retaining artifacts means whether to keep the build result files after the build is completed, for rollback purposes.',
    incrementalBuild:
      'Incremental build means whether to determine if there are changes in the repository code during the build. If there are no changes, the build will not be executed.',
    timedBuild: 'Scheduled build',
    cronExpression:
      'If you need to schedule automatic builds, fill in the cron expression. By default, the second level is not enabled (you need to modify the [system.timerMatchSecond] in the configuration file).',
    retentionDays: 'Retention days:',
    exclusionForPublish: 'Exclude ANT expression for publishing, multiple expressions are separated by commas.',
    howToList1: 'Local build refers to directly executing build commands on the server in the server',
    howToList2:
      'Please note that executing relevant commands requires the presence of a corresponding environment on the server',
    howToList3: 'And configure the correct environment variables',
    howToList4:
      'If the environment variables are installed and configured after starting the server, they need to be restarted through terminal commands to take effect',
    howToList5:
      'Container construction refers to using Docker containers to perform construction, which can achieve isolation from the host environment without installing dependent environments',
    howToList6:
      'Using container construction, the host where the Docker container is located needs to have a public network, as it requires remote downloading of environment dependent SDK and images',
    howToList7: 'The post creation build method does not support modification',
    howToList8:
      'The server for container installation cannot use local builds (because local builds rely on starting the local environment of the server, and container installation is not convenient for managing local dependency plugins)',
    containerList1:
      'To implement, you need to configure Docker containers to be managed on the server and assigned to the current workspace',
    containerList2: 'Configure labels for containers in the current workspace',
    containerList3: 'It is necessary to configure the label values into the construction of the DSL',
    containerList4: 'field',
    buildCommandL1:
      'The build command here will ultimately be executed on the server. If there are multiple lines of commands, then the',
    buildCommandL2: 'Execute line by line',
    buildCommandL3: 'If you want to switch paths and execute commands, you need to',
    buildCommandHelp:
      "Build and execute commands (non blocking commands), such as: mvn clean package, npm run build. Supported variables: {'${BUILD_ID}'}、{'${BUILD_NAME}'}、{'${BUILD_SOURCE_FILE}'}、{'${BUILD_NUMBER_ID}'},. env in the warehouse directory, workspace variables",
    dsl1: 'Configure in yaml/yml format',
    dsl2: 'Configuration requires declaring the use of specific dockers to perform build related operations (it is recommended to use dockers from the server where the server is located)',
    dsl3: 'Container construction will generate relevant mounting directories in Docker, and generally does not require manual operation',
    dsl4: 'When executing a build, a container will be generated for execution, and the corresponding container will be automatically deleted after the build is completed',
    dsl5: 'Currently, there are all supported plugins available (look forward to more plugins):',
    dsl6: 'Java SDK image usage: https://mirrors.tuna.tsinghua.edu.cn/ Supported versions include: 8, 9, 10, 11, 12, 13, 14, 15, 16, 17',
    dsl7: 'Maven SDK image usage: https://mirrors.tuna.tsinghua.edu.cn/apache/maven/maven-3/',
    dsl8: 'node sdk 镜像使用：https://registry.npmmirror.com/-/binary/node',
    dsl9: '(There are compatibility issues that need to be tested in advance during actual use) Python 3 SDK image usage: https://repo.huaweicloud.com/python/ ${PYTHON3_VERSION}/Python - ${PYTHON3_VERSION}. tar. xz',
    dsl10:
      '(There are compatibility issues that need to be tested in advance during actual use) GO SDK image usage: https://studygolang.com/dl/golang/go ${GO_VERSION}. Linux - ${ARCH}. tar. gz',
    artifact1: 'It can be understood as a directory for project packaging. Execution of Jpom project (build command)',
    artifact2: 'Build command, the relative path to build the product is:',
    publish1:
      'Publishing operation refers to publishing (uploading) the files in the build product directory in different ways to the corresponding location after executing the build command',
    publish2:
      'Node distribution refers to the deployment of a project across multiple nodes using node distribution to complete project publishing operations across multiple nodes in one step',
    publish3: 'Project refers to a project within a node that needs to be created in advance within the node',
    publish4:
      'SSH refers to publishing products through SSH commands or executing multiple commands to achieve publishing (requiring early addition in SSH)',
    publish5: 'Local commands refer to executing multiple commands locally on the server to achieve publishing',
    publish6:
      'SSH and local command publishing both execute variable replacement. The system reserves variables including: ${BUILL_ID}',
    publish7:
      'The environment variable placeholder ${xxxx} xxxx in the workspace can be referenced as the variable name',
    releasePath2P: 'Publish directory, build products and upload them to the corresponding directory',
    prePublish1: 'The command executed before release (non blocking command), usually the command to close the project',
    prePublish2: '支持变量替换：${BUILD_ID}、${BUILD_NAME}、${BUILD_RESULT_FILE}、${BUILD_NUMBER_ID}',
    prePublish3:
      'The environment variable placeholder ${xxxx} xxxx in the workspace can be referenced as the variable name',
    prePublishHelp:
      'The command executed before publication (non blocking command) is usually the close project command, which supports variable replacement: ${BUILL_ID}, ${BUILD0AME}, ${BUILD0RESULT-FILE}, ${BUILD0HUMBER-ID}',
    postPublish1:
      'The command executed after publication (non blocking command) is usually the command to start the project, such as ps - aux | grep Java',
    postPublish2: '支持变量替换：${BUILD_ID}、${BUILD_NAME}、${BUILD_RESULT_FILE}、${BUILD_NUMBER_ID}',
    postPublish3:
      'The environment variable placeholder ${xxxx} xxxx in the workspace can be referenced as the variable name',
    postPublishHelp:
      'The command executed after publication (non blocking command) is usually a startup project command such as ps - aux | grep Java, supporting variable replacement: ${BUILL_ID}',
    clearPublishTip:
      'Clear publishing refers to deleting all files in the project folder directory before uploading new files, and then saving the new files',
    diffTip1:
      'Differential publishing refers to whether there are differences between the files in the corresponding construction products and project folders. If there are incremental differences, upload or overwrite the files.',
    diffTip2:
      'Enabling differential publishing and clearing publishing will automatically delete files under the project directory but not under the construction product directory. Before clearing publishing differential uploads, the deletion of differential files will be performed first, followed by uploading differential files',
    diffTip3:
      'Enabling differential publishing but not clearing publishing is equivalent to only doing incremental and change updates',
    preStopPublishTip:
      'Stopping before publishing refers to closing the project before replacing the file when publishing it to the project file. To avoid situations where files are occupied in the Windows environment',
    executeContainerTip:
      'Which Docker to use for building, fill in the Docker tag (the tag is configured on the Docker editing page), and default to the first available query. If multiple tags are found, they will be built one by one',
    versionIncrementTip:
      'After enabling DockerTag version increment, the last digit of the version number will be automatically synchronized with the build sequence ID during each build. For example, the current build is the 100th build with testtag: 1.0->testtag: 1.100, testtag: 1.0. release ->testtag: 1.100. release. If no number is matched, the increment operation will be ignored',
    cacheBuildTip:
      'Enabling the cache build directory will retain the repository files, and a second build will pull the code. If the cache directory is not enabled, each build will pull the repository code again (it is not recommended to close the cache for larger projects). Special note: If version control related files are missing from the cache directory, they will be automatically deleted and the code will be pulled again',
    strictExecutionTip:
      'Strictly execute scripts (build command, event script, local publish script, container build command). The return status code must be 0, otherwise the build status will be marked as failed',
    webHookTip1:
      'The construction process requests the corresponding address, starts construction, completes construction, starts publishing, completes publishing, builds exceptions, publishes exceptions',
    webHookTip2: 'The parameters passed in include: buildId, buildName, type, statusMsg, triggerTime',
    webHookTip3: 'type 的值有：startReady、pull、executeCommand、release、done、stop、success、error',
    webHookTip4: 'Asynchronous requests cannot guarantee orderliness',
    eventScriptTip1:
      'Execute the corresponding script during the construction process, start building, complete building, start publishing, complete publishing, build exceptions, publish exceptions',
    eventScriptTip2:
      'The environment variables passed in include: buildId, buildName, type, statusMsg, triggerTime, buildNumberId, buildSourceFile',
    eventScriptTip3:
      'The parameters passed in during script execution include: startReady, pull, executeCommand, release, done, stop, and success',
    eventScriptTip4:
      "Note: To avoid unnecessary event execution scripts, the selected script's comments should include the event parameter keywords that need to be implemented. If the success event needs to be executed, the selected script's comments should include the success keyword",
    additionalEnvVars1:
      'Additional environment variables refer to reading the specified environment variable file from the repository to add to the execution build runtime',
    additionalEnvVars2: 'For example, common. env files',
    additionalEnvVars3:
      'File content format requirement: envname=xxxxx. Rows that do not meet the format will be automatically ignored',
    additionalEnvVars4: 'Also supports URL parameter formats: test_par=123abc&test_par2=abc21',
    additionalEnvVars5: 'Support configuration of system parameters:',
    additionalEnvVars6: 'When the building product is a folder, it will be packaged as',
    additionalEnvVars7: 'Compress the package for publishing',
    retainDaysTip:
      'Build product retention days, less than or equal to 0 to follow the global retention configuration. Note that automatic cleaning only cleans up data with a record status of (build completed, publish in progress, publish failed, publish failed) to avoid some abnormal construction effects and retain the number of records',
    retainCountTip:
      'The number of reserved products for construction, less than or equal to 0, follows the global reserved configuration (if the value is greater than 0, the minimum value will be compared with the global configuration for reference). Note that automatic cleaning only cleans up data with a record status of: (Build Completed, In Progress, Publish Failed, Publish Failed) to avoid some abnormal constructions that may affect the number of retained records. Check the number of reservations when creating new build records',
    buildSciptsInfo:
      'Java project (for example reference, specific decisions need to be made based on the actual situation of the project)',
    rulesBuildName: 'Please fill in the construction name',
    rulesBuildMode: 'Please choose the construction method',
    rulesReleaseMethod: 'Please select a publishing operation',
    rulesBranchName: 'Please select a branch',
    rulesScript: 'Please fill in the build command',
    rulesResultDirFile: 'Please fill in the product catalog'
  }
}
