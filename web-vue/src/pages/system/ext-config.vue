<template>
  <div>
    <a-row>
      <a-col span="6" style="">
        <a-row>
          <a-space style="display: inline">
            <a-input v-model:value="addName" :placeholder="$t('pages.system.ext-config.e9f092b5')" style="width: 100%">
              <template #addonAfter>
                <a-button type="primary" size="small" :disabled="!addName" @click="addItemHander"
                  >{{ $t('pages.system.ext-config.a0fe2109') }}
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
                {{ $t('pages.system.ext-config.b8fcb65d') }} <a-tag color="red">{{ temp.name }}</a-tag>
              </div>
            </template>
          </code-editor>

          <a-row type="flex" justify="center">
            <a-space>
              <a-button type="primary" danger :disabled="!temp || !temp.name" @click="saveData">{{
                $t('pages.system.ext-config.b033d8c5')
              }}</a-button>
              <a-button v-if="temp.hasDefault" type="primary" :disabled="!temp || !temp.name" @click="readeDefault">
                {{ $t('pages.system.ext-config.7b36cb7d') }}
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
          $message.success({ content: this.$t('pages.system.ext-config.1dcf3cc4') })
        }
      })
    },
    addItemHander() {
      $confirm({
        title: this.$t('pages.system.ext-config.b22d55a0'),
        zIndex: 1009,
        content:
          this.$t('pages.system.ext-config.2db8b0aa') + this.addName + this.$t('pages.system.ext-config.f20f0bb2'),
        okText: this.$t('pages.system.ext-config.a0fe2109'),
        cancelText: this.$t('pages.system.ext-config.b12468e9'),
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
