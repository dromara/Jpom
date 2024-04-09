<template>
  <div>
    <a-form :label-col="formLable.labelCol" :wrapper-col="formLable.wrapperCol">
      <a-form-item label="代码仓库" name="repositoryId" :validate-status="useData.repositoryId ? '' : 'error'">
        <a-input-search
          :value="`${tempRepository.raw ? tempRepository.raw.name + '[' + tempRepository.raw.gitUrl + ']' : '请选择仓库'}`"
          read-only
          placeholder="请选择仓库"
          enter-button="选择仓库"
          @search="
            () => {
              repositoryisible = true
            }
          "
        />
        <template v-if="!useData || !useData.repositoryId" #extra>
          <span class="ant-form-item-explain-error">请选择代码仓库</span>
        </template>
      </a-form-item>
      <a-form-item
        label="使用分支"
        name="branchName"
        :validate-status="useData.branchName || useData.branchTagName ? '' : 'error'"
      >
        <template v-if="!useData.branchName && !useData.branchTagName" #extra>
          <span class="ant-form-item-explain-error">请选择仓库分支或者选择标签</span>
        </template>
        <custom-select
          v-model:value="useData.branchName"
          :disabled="!!useData.branchTagName || !useData.repositoryId"
          :data="tempRepository.branchList"
          :can-reload="true"
          input-placeholder="自定义分支通配表达式"
          select-placeholder="请选择构建对应的分支,必选"
          @on-refresh-select="loadBranchList(useData.repositoryId)"
        >
          <template #inputTips>
            <div>
              支持通配符(AntPathMatcher)
              <ul>
                <li>? 匹配一个字符</li>
                <li>* 匹配零个或多个字符</li>
                <li>** 匹配路径中的零个或多个目录</li>
              </ul>
            </div>
          </template>
        </custom-select>
      </a-form-item>
      <a-form-item
        label="标签(TAG)"
        name="branchTagName"
        :validate-status="useData.branchName || useData.branchTagName ? '' : 'error'"
      >
        <template v-if="!useData.branchName && !useData.branchTagName" #extra>
          <span class="ant-form-item-explain-error">请选择仓库分支或者选择标签</span>
        </template>
        <custom-select
          v-model:value="useData.branchTagName"
          :disabled="!useData.repositoryId"
          :data="tempRepository.branchTagList"
          :can-reload="true"
          input-placeholder="自定义标签通配表达式"
          select-placeholder="选择构建的标签,不选为最新提交"
          @on-refresh-select="loadBranchList(useData.repositoryId)"
        >
          <template #inputTips>
            <div>
              支持通配符(AntPathMatcher)
              <ul>
                <li>? 匹配一个字符</li>
                <li>* 匹配零个或多个字符</li>
                <li>** 匹配路径中的零个或多个目录</li>
              </ul>
            </div>
          </template>
        </custom-select>
      </a-form-item>
      <a-form-item v-if="guideStore.getExtendPlugins.indexOf('system-git') > -1" label="克隆深度" name="cloneDepth">
        <a-input-number
          v-model:value="useData.cloneDepth"
          style="width: 100%"
          placeholder="自定义克隆深度，避免大仓库全部克隆"
        />
      </a-form-item>
    </a-form>

    <!-- 选择仓库 -->
    <a-drawer
      destroy-on-close
      :title="`选择仓库`"
      placement="right"
      :open="repositoryisible"
      width="85vw"
      :z-index="1009"
      :footer-style="{ textAlign: 'right' }"
      @close="
        () => {
          repositoryisible = false
        }
      "
    >
      <repository-list
        v-if="repositoryisible"
        ref="repositoryComponent"
        :choose="true"
        :choose-val="tempRepository.raw && tempRepository.raw.id"
        @confirm="
          (repositoryId: any) => {
            changeRepositpry(repositoryId)
          }
        "
        @cancel="
          () => {
            repositoryisible = false
          }
        "
      >
      </repository-list>
      <template #footer>
        <a-space>
          <a-button
            @click="
              () => {
                repositoryisible = false
              }
            "
          >
            取消
          </a-button>
          <a-button
            type="primary"
            @click="
              () => {
                repositoryComponent.handerConfirm()
              }
            "
          >
            确认
          </a-button>
        </a-space>
      </template>
    </a-drawer>
  </div>
</template>
<script setup lang="ts">
import { getRepositoryInfo } from '@/api/repository'
import { getBranchList } from '@/api/build-info'
import { useGuideStore } from '@/stores/guide'
import repositoryList from '@/pages/repository/list.vue'
import { Repository } from './types'
const props = defineProps({
  data: {
    type: Object as PropType<Repository>,
    required: true
  },
  loading: {
    type: Boolean,
    required: true
  },
  formLable: {
    type: Object,
    required: true
  }
})
const emit = defineEmits<{ (e: 'update:data', value: object): void; (e: 'update:loading', value: boolean): void }>()

const guideStore = useGuideStore()
const repositoryComponent = ref()

const useData = ref(props.data)
const useLoading = ref(props.loading)

watch(
  () => useData.value,
  (val) => {
    emit('update:data', val)
  },
  {
    immediate: true
  }
)
watch(
  () => useLoading.value,
  (val) => {
    emit('update:loading', val)
  }
)

const tempRepository = ref<{
  raw?: any
  branchList?: Array<string>
  branchTagList?: Array<string>
}>({})
const repositoryisible = ref(false)

// const repositoryisible = ref(false)

// 选择仓库
const changeRepositpry = (repositoryId: string) => {
  useData.value = { ...useData.value, repositoryId: repositoryId, branchName: '', branchTagName: '' }
  repositoryisible.value = false
  useLoading.value = true
  //const repositoryId = jsonConfig.value.repositories[repositoryTag].repositoryId
  getRepositoryInfo({
    id: repositoryId
  })
    .then((res) => {
      if (res.code === 200) {
        tempRepository.value = {
          ...tempRepository.value,
          raw: res.data
        }

        // 刷新分支
        loadBranchList(repositoryId)
      }
    })
    .finally(() => {
      useLoading.value = false
    })
}
// 获取仓库分支
const loadBranchList = (repositoryId?: string) => {
  if (!repositoryId) {
    return
  }
  tempRepository.value = {
    ...tempRepository.value,
    branchList: [],
    branchTagList: []
  }

  const params = {
    repositoryId: repositoryId
  }
  useLoading.value = true
  getBranchList(params)
    .then((res) => {
      if (res.code === 200) {
        tempRepository.value = {
          ...tempRepository.value,
          branchList: res.data?.branch || [],
          branchTagList: res.data?.tags || []
        }
      }
    })
    .finally(() => {
      useLoading.value = false
    })
}
</script>
