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
  <div class="">
    <a-form ref="editForm" :model="temp">
      <a-form-item>
        <code-editor
          v-model:content="temp.content"
          height="calc(100vh - 200px)"
          :options="{ mode: 'yaml', tabSize: 2 }"
          :show-tool="true"
        >
          <template #tool_before>
            <a-alert v-if="temp.file" show-icon :message="`${$t('i18n_37c1eb9b23')}:${temp.file}`" />
            <a-alert v-else-if="temp.readonly" type="warning" show-icon :message="temp.readonlyMsg" />
          </template>
        </code-editor>
      </a-form-item>
      <a-form-item :wrapper-col="{ span: 14, offset: 2 }">
        <a-space>
          <a-button type="primary" :disabled="submitAble || temp.readonly" @click="onSubmit(false)">{{
            $t('i18n_be5fbbe34c')
          }}</a-button>
          <a-button type="primary" danger :disabled="submitAble || temp.readonly" @click="onSubmit(true)">{{
            $t('i18n_6aab88d6a3')
          }}</a-button>
        </a-space>
      </a-form-item>
    </a-form>
  </div>
</template>
<script>
import { getConfigData, editConfig, systemInfo } from '@/api/system'
import codeEditor from '@/components/codeEditor'
import { RESTART_UPGRADE_WAIT_TIME_COUNT } from '@/utils/const'

export default {
  components: {
    codeEditor
  },
  inject: ['globalLoading'],
  props: {
    machineId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      temp: {
        content: ''
      },
      submitAble: false,

      checkCount: 0
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    // load data
    loadData() {
      getConfigData({ machineId: this.machineId }).then((res) => {
        if (res.code === 200) {
          this.temp.content = res.data.content
          this.temp.file = res.data.file
          // 配置文件在程序包内（未按 bin/Agent.sh 启动）时后端返回 readonly，前端同步禁用保存
          this.temp.readonly = res.data.readonly === true
          this.temp.readonlyMsg = res.data.readonlyMsg
        }
      })
    },
    // submit
    onSubmit(restart) {
      // disabled submit button
      this.submitAble = true
      this.temp.machineId = this.machineId
      this.temp.restart = restart
      editConfig(this.temp).then((res) => {
        if (res.code === 200) {
          // 成功
          $notification.success({
            message: res.msg
          })
          if (this.temp.restart) {
            this.startCheckRestartStatus(res.msg)
          }
        }
        // button recover
        this.submitAble = false
      })
    },
    startCheckRestartStatus(msg) {
      this.checkCount = 0
      this.globalLoading({
        spinning: true,
        tip: (msg || this.$t('i18n_85da2e5bb1')) + `,${this.$t('i18n_809b12d6a0')},${this.$t('i18n_af013dd9dc')}`
      })
      setTimeout(() => {
        //
        this.timer = setInterval(() => {
          systemInfo(this.machineId)
            .then((res) => {
              if (res.code === 200) {
                clearInterval(this.timer)
                this.globalLoading(false)
                $notification.success({
                  message: this.$t('i18n_906f6102a7')
                })

                setTimeout(() => {
                  location.reload()
                }, 1000)
              } else {
                if (this.checkCount > RESTART_UPGRADE_WAIT_TIME_COUNT) {
                  $notification.warning({
                    message: this.$t('i18n_953ec2172b') + (res.msg || '')
                  })
                  this.globalLoading(false)
                  clearInterval(this.timer)
                }
              }
            })
            .catch((error) => {
              console.error(error)
              if (this.checkCount > RESTART_UPGRADE_WAIT_TIME_COUNT) {
                this.globalLoading(false)
                $notification.error({
                  message: this.$t('i18n_0e502fed63')
                })
                clearInterval(this.timer)
              }
            })
          this.checkCount = this.checkCount + 1
        }, 2000)
      }, 6000)
    }
  }
}
</script>
