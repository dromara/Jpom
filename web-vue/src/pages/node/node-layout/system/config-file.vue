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
          </template>
        </code-editor>
      </a-form-item>
      <a-form-item :wrapper-col="{ span: 14, offset: 2 }">
        <a-space>
          <a-button type="primary" :disabled="submitAble" @click="onSubmit(false)">{{
            $t('i18n_be5fbbe34c')
          }}</a-button>
          <a-button type="primary" danger :disabled="submitAble" @click="onSubmit(true)">{{
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
          this.temp.content = res.data
          this.temp.content = res.data.content
          this.temp.file = res.data.file
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
