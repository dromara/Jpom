<template>
  <div>
    <a-row>
      <a-col span="6" style="">
        <a-row>
          <a-space style="display: inline">
            <a-input v-model:value="addName" :placeholder="$t('i18n_cfeea27648')" style="width: 100%">
              <template #addonAfter>
                <a-button type="primary" size="small" :disabled="!addName" @click="addItemHander"
                  >{{ $t('i18n_e83a256e4f') }}
                </a-button>
              </template>
            </a-input>
          </a-space>
        </a-row>
        <a-directory-tree
          v-model:expandedKeys="expandedKeys"
          v-model:selectedKeys="selectedKeys"
          :tree-data="treeData"
          :field-names="replaceFields"
          @select="select"
        >
        </a-directory-tree>
      </a-col>
      <a-col span="18" style="padding-left: 5px">
        <a-space direction="vertical" style="display: flex">
          <code-editor
            v-model:content="temp.content"
            height="calc(100vh - 170px)"
            :show-tool="true"
            :file-suffix="temp.name"
          >
            <template #tool_before>
              <div v-show="temp.name">
                {{ $t('i18n_5b47861521') }} <a-tag color="red">{{ temp.name }}</a-tag>
              </div>
            </template>
          </code-editor>

          <a-row type="flex" justify="center">
            <a-space>
              <a-button type="primary" danger :disabled="!temp || !temp.name" @click="saveData">{{
                $t('i18n_be5fbbe34c')
              }}</a-button>
              <a-button v-if="temp.hasDefault" type="primary" :disabled="!temp || !temp.name" @click="readeDefault">
                {{ $t('i18n_3306c2a7c7') }}
              </a-button>
            </a-space>
          </a-row>
        </a-space>
      </a-col>
    </a-row>
  </div>
</template>
<script>
import codeEditor from '@/components/codeEditor'
import { addItem, listExtConf, getItem, saveItem, getDefaultItem } from '@/api/ext-config'

export default {
  components: {
    codeEditor
  },
  data() {
    return {
      loading: false,
      treeData: [],
      expandedKeys: [],
      selectedKeys: [],
      editVisible: false,
      temp: {},

      replaceFields: {
        children: 'children',
        title: 'name',
        key: 'id'
      },
      addName: ''
    }
  },
  computed: {},
  created() {
    this.loadData()
  },
  methods: {
    // 加载数据
    loadData() {
      this.loading = true
      listExtConf().then((res) => {
        if (res.code === 200) {
          this.treeData = res.data?.children
        }
        this.loading = false
      })
    },
    // 选择
    select(selectedKeys, { node }) {
      if (this.temp?.name === node.dataRef?.name) {
        return
      }
      if (!node.dataRef.isLeaf) {
        return
      }
      this.temp = {}
      getItem({ name: node.dataRef?.id }).then((res) => {
        if (res.code === 200) {
          this.temp = {
            name: node.dataRef?.id,
            content: res.data,
            hasDefault: node.dataRef?.hasDefault
          }
        }
      })
    },
    readeDefault() {
      getDefaultItem({ name: this.temp.name }).then((res) => {
        if (res.code === 200) {
          this.temp = { ...this.temp, content: res.data }
          $message.success({ content: this.$t('i18n_335258331a') })
        }
      })
    },
    addItemHander() {
      const title = this.$t('i18n_ef016ab402')
      const other = this.$t('i18n_956ab8a9f7')
      const content = `${title} 【${this.addName}】 ${other}`
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: content,
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return addItem({ name: this.addName }).then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.addName = ''
              this.loadData()
            }
          })
        }
      })
    },
    saveData() {
      saveItem(this.temp).then((res) => {
        if (res.code === 200) {
          // 成功
          $notification.success({
            message: res.msg
          })
        }
      })
    }
  }
}
</script>
