<!--

    Copyright (c) 2019 Of Him Code Technology Studio
    Jpom is licensed under Mulan PSL v2.
    You can use this software according to the terms and conditions of the Mulan PSL v2.
    You may obtain a copy of Mulan PSL v2 at:
    			http://license.coscl.org.cn/MulanPSL2
    THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
    See the Mulan PSL v2 for more details.

-->
<template>
  <div>
    <CustomModal
      destroy-on-close
      :open="true"
      :confirm-loading="confirmLoading"
      width="70%"
      :title="title"
      :ok-text="$t('i18n_be5fbbe34c')"
      :cancel-text="$t('i18n_625fb26b4b')"
      :mask-closable="false"
      @ok="handleSave"
      @cancel="$emit('cancel')"
    >
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 19 }">
        <a-form-item :label="$t('i18n_d7ec2d3fea')" name="name">
          <a-input v-model:value="temp.name" :max-length="50" :placeholder="$t('i18n_47d26a4994')" />
        </a-form-item>
        <a-form-item :label="$t('i18n_829abe5a8d')" name="group">
          <a-input v-model:value="temp.group" :max-length="50" :placeholder="$t('i18n_0a52738bb5')" />
        </a-form-item>
        <a-form-item :label="$t('i18n_1030348573')" name="sshId">
          <a-select
            v-model:value="temp.sshId"
            show-search
            allow-clear
            :filter-option="
              (input, option) => {
                const children = option.children && option.children()
                return (
                  children &&
                  children[0].children &&
                  children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                )
              }
            "
            :placeholder="$t('i18n_f2a0ad9344')"
            @change="handleSshChange"
          >
            <a-select-option v-for="item in sshList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('i18n_33d1f73e42')" name="path">
          <a-input v-model:value="temp.path" :placeholder="$t('i18n_4abb143437')" />
        </a-form-item>
        <a-form-item :label="$t('i18n_9a70960bc9')" name="logCharset">
          <a-input v-model:value="temp.logCharset" placeholder="UTF-8" />
        </a-form-item>
        <a-form-item :label="$t('i18n_8388c637f6')" name="autoStart">
          <a-switch
            v-model:checked="temp.autoStart"
            :checked-children="$t('i18n_8493205602')"
            :un-checked-children="$t('i18n_d58a55bcee')"
          />
        </a-form-item>
        <a-form-item :label="$t('i18n_93da367db1')" name="token">
          <a-input v-model:value="temp.token" :placeholder="$t('i18n_93da367db1')" />
        </a-form-item>
        <a-divider orientation="left">{{ $t('i18n_ec0dd46b3e') }}</a-divider>
        <a-form-item :label="$t('i18n_59cf15fe6b')" name="templateKey">
          <a-space wrap>
            <a-select
              v-model:value="temp.templateKey"
              allow-clear
              style="width: 320px"
              :placeholder="$t('i18n_93604a0e0a')"
              @change="handleTemplateChange"
            >
              <a-select-option v-for="item in templateList" :key="item.key" :value="item.key">
                {{ item.name }}
              </a-select-option>
            </a-select>
            <a-button type="primary" :loading="renderLoading" @click="handlePreview">{{
              $t('i18n_86862a3a43')
            }}</a-button>
          </a-space>
        </a-form-item>
        <a-form-item v-if="currentTemplate.description" :wrapper-col="{ span: 19, offset: 4 }">
          <a-alert type="info" show-icon :message="currentTemplate.description" />
        </a-form-item>
        <a-form-item v-for="field in templateFields" :key="field.key" :label="field.label" :required="!!field.required">
          <a-switch v-if="field.type === 'boolean'" v-model:checked="templateForm[field.key]" />
          <a-input-number
            v-else-if="field.type === 'number'"
            v-model:value="templateForm[field.key]"
            :placeholder="field.placeholder"
            style="width: 100%"
          />
          <a-select
            v-else-if="field.type === 'select' && field.options && field.options.length"
            v-model:value="templateForm[field.key]"
            :placeholder="field.placeholder || $t('i18n_708c9d6d2a')"
          >
            <a-select-option v-for="option in field.options" :key="optionValue(option)" :value="optionValue(option)">
              {{ optionLabel(option) }}
            </a-select-option>
          </a-select>
          <a-input v-else v-model:value="templateForm[field.key]" :placeholder="field.placeholder" />
        </a-form-item>
        <a-form-item :label="$t('i18n_1ae3d38e69')" name="dslContent">
          <a-form-item-rest>
            <code-editor
              v-model:content="temp.dslContent"
              file-suffix="config.yml"
              height="40vh"
              :options="{ mode: 'yaml', tabSize: 2 }"
              :show-tool="true"
            ></code-editor>
          </a-form-item-rest>
        </a-form-item>
        <a-form-item :wrapper-col="{ span: 19, offset: 4 }">
          <a-button :loading="renderLoading" @click="handlePreview">{{ $t('i18n_a52c5c72cf') }}</a-button>
        </a-form-item>
      </a-form>
    </CustomModal>
  </div>
</template>
<script>
import { getSshListAll } from '@/api/ssh'
import { getSshProjectTemplateList, renderSshProjectTemplate, saveSshProject } from '@/api/ssh-project'
import codeEditor from '@/components/codeEditor'
export default {
  components: { codeEditor },
  props: {
    data: {
      type: Object,
      default: () => {}
    }
  },
  emits: ['cancel'],
  data() {
    return {
      confirmLoading: false,
      renderLoading: false,
      sshList: [],
      templateList: [],
      templateFields: [],
      templateForm: {},
      currentTemplate: {},
      temp: {
        name: '',
        group: '',
        machineSshId: '',
        sshId: '',
        path: '',
        dslContent: '',
        templateKey: '',
        logCharset: '',
        autoStart: true,
        token: ''
      },
      rules: {
        name: [
          {
            required: true,
            message: this.$t('i18n_47d26a4994'),
            trigger: 'blur'
          }
        ],
        sshId: [
          {
            required: true,
            message: this.$t('i18n_f2a0ad9344'),
            trigger: 'change'
          }
        ],
        path: [
          {
            required: true,
            message: this.$t('i18n_4abb143437'),
            trigger: 'blur'
          }
        ]
      }
    }
  },
  computed: {
    title() {
      return (this.temp.id ? this.$t('i18n_95b351c862') : this.$t('i18n_66ab5e9f24')) + ' ' + this.$t('i18n_8215da30b4')
    }
  },
  created() {
    if (this.data && this.data.id) {
      this.temp = {
        ...this.temp,
        ...this.data,
        autoStart: this.data.autoStart === true || this.data.autoStart === 'true' || this.data.autoStart === 'on'
      }
    }
    this.loadSshList()
    this.loadTemplateList()
  },
  methods: {
    // 加载 ssh 列表
    loadSshList() {
      getSshListAll().then((res) => {
        if (res.code === 200 && res.data) {
          this.sshList = res.data
          this.syncMachineSshId()
        }
      })
    },
    // 加载模板列表（编辑态刷新模板字段）
    loadTemplateList() {
      getSshProjectTemplateList().then((res) => {
        if (res.code === 200 && res.data) {
          this.templateList = res.data.slice().sort((a, b) => (a.order || 0) - (b.order || 0))
          if (this.temp.templateKey) {
            this.initTemplateFields(this.temp.templateKey)
          }
        }
      })
    },
    // 编辑态补齐 machineSshId
    syncMachineSshId() {
      if (!this.temp.sshId || this.temp.machineSshId) {
        return
      }
      const sshItem = this.sshList.find((item) => item.id === this.temp.sshId)
      this.temp.machineSshId = sshItem ? sshItem.machineSshId : ''
    },
    // 选择 ssh 时带出机器 ssh id
    handleSshChange(sshId) {
      const sshItem = this.sshList.find((item) => item.id === sshId)
      this.temp.machineSshId = sshItem ? sshItem.machineSshId : ''
    },
    // 切换模板：只刷新模板参数，不覆盖已编辑的内容
    handleTemplateChange(templateKey) {
      this.templateFields = []
      this.templateForm = {}
      this.currentTemplate = {}
      if (templateKey) {
        this.initTemplateFields(templateKey)
      }
    },
    // 初始化模板参数字段及默认值
    initTemplateFields(templateKey) {
      const template = this.templateList.find((item) => item.key === templateKey)
      this.currentTemplate = template || {}
      this.templateFields = (template && template.fields) || []
      const form = {}
      this.templateFields.forEach((field) => {
        form[field.key] = this.fieldDefaultValue(field)
      })
      this.templateForm = form
    },
    fieldDefaultValue(field) {
      const value = field.defaultValue
      if (field.type === 'boolean') {
        return value === true || value === 'true' || value === 1 || value === '1'
      }
      if (field.type === 'number') {
        if (value === undefined || value === null || value === '') {
          return undefined
        }
        const num = Number(value)
        return isNaN(num) ? undefined : num
      }
      return value === undefined || value === null ? '' : value
    },
    optionValue(option) {
      return option && typeof option === 'object' ? option.value : option
    },
    optionLabel(option) {
      return option && typeof option === 'object' ? option.label : option
    },
    // 预览（重新生成）内容，已有内容需要二次确认
    handlePreview() {
      if (!this.temp.templateKey) {
        $notification.warn({ message: this.$t('i18n_93604a0e0a') })
        return false
      }
      const missing = this.templateFields.filter((field) => {
        if (!field.required) {
          return false
        }
        const value = this.templateForm[field.key]
        return value === undefined || value === null || value === ''
      })
      if (missing.length) {
        const labels = missing.map((field) => field.label).join(', ')
        $notification.warn({ message: `${this.$t('i18n_8d91dfbc48')}: ${labels}` })
        return false
      }
      if (!this.temp.dslContent) {
        return this.doRenderTemplate()
      }
      $confirm({
        title: this.$t('i18n_38cf16f220'),
        zIndex: 1009,
        content: this.$t('i18n_28a8593f3b'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => this.doRenderTemplate()
      })
    },
    // 调用模板渲染接口生成内容
    doRenderTemplate() {
      const fields = {}
      Object.keys(this.templateForm).forEach((key) => {
        const value = this.templateForm[key]
        fields[key] = value === undefined || value === null ? '' : String(value)
      })
      this.renderLoading = true
      return renderSshProjectTemplate({
        templateKey: this.temp.templateKey,
        fieldsJson: JSON.stringify(fields)
      })
        .then((res) => {
          if (res.code === 200 && res.data) {
            this.temp.dslContent = res.data.dslContent || ''
          }
        })
        .catch(() => {
          $notification.error({ message: this.$t('i18n_a17ff27e15') })
        })
        .finally(() => {
          this.renderLoading = false
        })
    },
    // 保存
    handleSave() {
      this.$refs['editForm'].validate().then(() => {
        if (!this.temp.dslContent) {
          $notification.warn({ message: this.$t('i18n_7b10ae27df') })
          return false
        }
        const params = {
          id: this.temp.id,
          name: this.temp.name,
          group: this.temp.group,
          machineSshId: this.temp.machineSshId,
          sshId: this.temp.sshId,
          path: this.temp.path,
          dslContent: this.temp.dslContent,
          templateKey: this.temp.templateKey,
          logCharset: this.temp.logCharset,
          autoStart: !!this.temp.autoStart,
          token: this.temp.token
        }
        this.confirmLoading = true
        saveSshProject(params)
          .then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.$emit('cancel')
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    }
  }
}
</script>
