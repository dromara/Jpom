export type PublishProject = {
  nodeId?: string
  projectId?: string
  secondaryDirectory?: string
  uploadCloseFirst?: boolean
  unCompression?: boolean
  clearOld?: boolean
  diffSync?: boolean
  afterOpt?: 'No' | 'Restart'
} & PublishBase

export type PublishBase = {
  publishType?: 'PROJECT'
  artifacts?: Array<{
    path: Array<string>
    format?: 'ZIP' | 'TAR_GZ'
  }>
} & StagesBase

export type StagesBase = {
  stageType?: 'EXEC' | 'PUBLISH'
  repoTag?: string
  description?: string
}

export type StagesExec = { commands?: string; timeout?: number; env?: Record<string, string> } & StagesBase

export type StagesConfig = PublishBase | PublishProject | StagesExec

export type Repository = {
  repositoryId?: string
  branchName?: string
  branchTagName?: string
  cloneDepth?: number
  sort?: number
}

export type StageGroupsType = {
  name?: string
  description?: string
  stages: Array<StagesConfig>
}

export type JsonConfigType = {
  version?: string
  repositories: Record<string, Repository>
  stageGroups: Array<StageGroupsType>
}
