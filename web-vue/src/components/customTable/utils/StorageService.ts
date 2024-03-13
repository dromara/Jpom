export type ProvideType = 'sessionStorage' | 'localStorage'

import { SizeType } from 'ant-design-vue/es/config-provider'
import { CustomColumnType, TableLayoutType } from '../types'

export interface StorageObjectType {
  /** 布局大小设置 */
  tableSize: SizeType
  /** 列设置 */
  column: CustomColumnType[]
  /** 布局配置 */
  layout: TableLayoutType
  /** 刷新配置 */
  refresh: {
    isAutoRefresh: number
    autoRefreshTime: number
  }
}

const defaultConfig: StorageObjectType = {
  tableSize: undefined,
  column: [],
  layout: undefined,
  refresh: {
    isAutoRefresh: -1,
    autoRefreshTime: -1
  }
}

export type StorageServiceOptions = {
  provide: ProvideType
  prefix: string
}

export class StorageService {
  name: string | undefined
  provide: ProvideType
  prefix: string
  constructor(name: string | undefined, options: StorageServiceOptions) {
    this.name = name
    this.provide = options.provide || 'localStorage'
    this.prefix = options.prefix || 'catch__'
  }
  exitOpenStorage() {
    return !!this.getStorageKey()
  }
  /**
   * 获取存储key
   */
  getStorageKey() {
    return this.name ? `${this.prefix}__${this.name}` : ''
  }
  /**
   * 获取嵌套属性
   * @param path 属性路径
   * @param storageObject 存储对象
   * @param emptyValue 默认值
   */
  getNestedProperty<T>(path: string, storageObject: StorageObjectType, emptyValue: T) {
    const val = path.split('.').reduce((returnVal: any, key: string) => {
      if (typeof returnVal !== 'object' || returnVal === null || !(key in returnVal)) {
        return null
      }
      return returnVal[key]
    }, storageObject) as T
    return val || emptyValue
  }
  /**
   * 获取存储值
   * @param path 属性路径
   * @param emptyValue 默认值
   */
  getStorage<T = any>(path: string, emptyValue: T) {
    const key = this.getStorageKey()
    let storageObject: StorageObjectType = defaultConfig
    if (!key) {
      return emptyValue || ({} as T)
    }
    try {
      if (this.provide === 'sessionStorage') {
        storageObject = JSON.parse(sessionStorage.getItem(key) || '{}')
      }
      if (this.provide === 'localStorage') {
        storageObject = JSON.parse(localStorage.getItem(key) || '{}')
      }
    } catch (error) {
      console.error(error)
    }
    if (!path) {
      return storageObject as T
    }
    return this.getNestedProperty(path, storageObject, emptyValue)
  }
  /**
   * 设置存储值
   * @param path 属性路径
   * @param value 值
   */
  setStorage(path: string, value: any) {
    if (!this.getStorageKey()) {
      return
    }
    const storageObject = this.getStorage<StorageObjectType>('', defaultConfig)
    const keys = path.split('.')
    let currentObj = storageObject as any
    for (let i = 0; i < keys.length - 1; i++) {
      if (typeof currentObj !== 'object' || currentObj === null) {
        throw new Error(`Cannot set property '${path}' on object. Intermediate property does not exist.`)
      }
      const key = keys[i]
      if (!(key in currentObj)) {
        currentObj[key] = {}
      }
      currentObj = currentObj[key]
    }
    const lastKey = keys[keys.length - 1]
    currentObj[lastKey] = value
    // 存储
    const key = this.getStorageKey()
    if (this.provide === 'sessionStorage') {
      sessionStorage.setItem(key, JSON.stringify(storageObject))
    }
    if (this.provide === 'localStorage') {
      localStorage.setItem(key, JSON.stringify(storageObject))
    }
  }
  getTableSizeConfig() {
    return this.getStorage<StorageObjectType['tableSize']>('tableSize', undefined)
  }
  setTableSizeConfig(value: SizeType) {
    this.setStorage('tableSize', value || 'middle')
  }
  getColumnConfig() {
    return this.getStorage<StorageObjectType['column']>('column', [])
  }
  setColumnConfig(value: CustomColumnType[]) {
    this.setStorage('column', value || [])
  }
  getLayoutConfig() {
    return this.getStorage<StorageObjectType['layout']>('layout', 'table')
  }
  setLayoutConfig(value: TableLayoutType) {
    this.setStorage('layout', value || 'table')
  }
  getRefreshConfig() {
    return this.getStorage<StorageObjectType['refresh']>('refresh', {
      isAutoRefresh: -1,
      autoRefreshTime: -1
    })
  }
  setRefreshConfig(value: StorageObjectType['refresh']) {
    this.setStorage('refresh', value)
  }
}
