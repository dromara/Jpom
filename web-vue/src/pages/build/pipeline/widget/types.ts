export type publishProject = {
  nodeId?: string
  projectId?: string
  secondaryDirectory?: string
  uploadCloseFirst?: boolean
  unCompression?: boolean
  clearOld?: boolean
  diffSync?: boolean
  afterOpt?: 'No' | 'Restart'
} & publishBase

export type publishBase = {
  publishType?: 'PROJECT'
  artifacts?: Array<{
    path: Array<string>
    format?: 'ZIP' | 'TAR_GZ'
  }>
} & stagesBase

export type stagesBase = {
  stageType?: 'EXEC' | 'PUBLISH'
  repoTag?: string
  description?: string
}

export type stagesExec = { commands: string; timeout: number; env: Record<string, string> } & stagesBase

export type stagesConfig = publishBase | publishProject | stagesExec

export type repository = {
  repositoryId?: string
  branchName?: string
  branchTagName?: string
  cloneDepth?: number
  sort?: number
}

export type stageGroupsType = {
  name?: string
  description?: string
  stages?: Array<stagesConfig & stagesExec>
}

export type jsonConfigType = {
  version?: string
  repositories: Record<string, repository>
  stageGroups: Array<stageGroupsType>
}
