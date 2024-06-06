<template>
  <div class="">
    <a-form ref="editForm" :model="temp" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
      <a-form-item :label="$t('pages.system.mail.ab20d54d')" name="host">
        <a-auto-complete
          v-model:value="temp.host"
          :options="hostDataSource"
          :placeholder="$t('pages.system.mail.4513ff80')"
        >
          <template #option="item"> {{ item.title }} {{ item.value }} </template>
        </a-auto-complete>
      </a-form-item>
      <a-form-item :label="$t('pages.system.mail.998f5bd')" name="port">
        <a-auto-complete
          v-model:value="temp.port"
          :placeholder="$t('pages.system.mail.c9790920')"
          :options="portDataSource"
        >
          <template #option="item"> {{ item.title }} {{ item.value }} </template>
        </a-auto-complete>
      </a-form-item>
      <a-form-item :label="$t('pages.system.mail.34db7143')" name="user">
        <a-input v-model:value="temp.user" type="text" :placeholder="$t('pages.system.mail.afcb4023')" />
      </a-form-item>
      <a-form-item :label="$t('pages.system.mail.bf730bf9')" :name="`${temp.type === 'add' ? 'pass' : 'pass-update'}`">
        <a-input-password v-model:value="temp.pass" type="text" :placeholder="$t('pages.system.mail.a1fcca53')" />
      </a-form-item>
      <a-form-item :label="$t('pages.system.mail.a5b30280')" name="from">
        <!-- <a-input v-model="temp.from" type="text" placeholder="发送方邮箱账号" /> -->
        <a-tooltip>
          <template #title
            >{{ $t('pages.system.mail.e793cb27') }}
            <ul>
              <li>1. user@xxx.xx</li>
              <li>2. name &lt;user@xxx.xx&gt;</li>
            </ul>
          </template>
          <a-auto-complete
            v-model:value="temp.from"
            :options="fromResult"
            :placeholder="$t('pages.system.mail.62f7853a')"
            @search="handleFromSearch"
          >
            <template #option="{ value: val }">
              {{ val.split('@')[0] }} @
              <span style="font-weight: bold">{{ val.split('@')[1] }}</span>
            </template>
          </a-auto-complete>
        </a-tooltip>
      </a-form-item>
      <a-form-item :label="$t('pages.system.mail.2cfcf155')" name="sslEnable">
        <a-switch
          v-model:checked="temp.sslEnable"
          :checked-children="$t('pages.system.mail.59a4a036')"
          :un-checked-children="$t('pages.system.mail.49cbff90')"
        />
        <!-- <a-input v-show="temp.sslEnable" v-model="temp.socketFactoryPort" type="text" placeholder="SSL 端口" /> -->
      </a-form-item>
      <a-form-item :label="$t('pages.system.mail.bf970307')" name="timeout">
        <a-input-number
          v-model:value="temp.timeout"
          style="width: 100%"
          :min="3"
          type="text"
          :placeholder="$t('pages.system.mail.b9aa7fe4')"
        />
      </a-form-item>

      <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
        <a-button type="primary" class="btn" :disabled="submitAble" @click="onSubmit">{{
          $t('pages.system.mail.d0f0d36d')
        }}</a-button>
      </a-form-item>
    </a-form>
    <a-alert
      :message="$t('pages.system.mail.b07f7224')"
      :description="$t('pages.system.mail.36924742')"
      type="info"
      show-icon
    />
    <br />
    <a-alert
      :message="$t('pages.system.mail.c4606860')"
      type="info"
      :description="$t('pages.system.mail.ad258eaa')"
      show-icon
    />
    <br />
    <a-alert
      :message="$t('pages.system.mail.7980fa27')"
      :description="$t('pages.system.mail.ff51a408')"
      type="info"
      show-icon
    />
    <br />
    <a-alert
      :message="$t('pages.system.mail.d4257413')"
      :description="$t('pages.system.mail.6983a03')"
      type="info"
      show-icon
    />
  </div>
</template>

<script>
import { getMailConfigData, editMailConfig } from '@/api/system'

export default {
  data() {
    return {
      temp: {},
      submitAble: false,
      rules: {
        host: [
          {
            required: true,
            message: this.$t('pages.system.mail.346e27f3'),
            trigger: 'blur'
          }
        ],
        pass: [{ required: true, message: this.$t('pages.system.mail.dfc93456'), trigger: 'blur' }],
        user: [
          {
            required: true,
            message: this.$t('pages.system.mail.b3d780e9'),
            trigger: 'blur'
          }
        ],
        from: [
          {
            required: true,
            message: this.$t('pages.system.mail.770f5a72'),
            trigger: 'blur'
          }
        ]
      },
      fromResult: [],
      hostDataSource: [
        {
          title: this.$t('pages.system.mail.78469e0c'),
          options: [
            {
              title: this.$t('pages.system.mail.bc591b'),
              value: 'smtp.qq.com'
            },
            {
              title: this.$t('pages.system.mail.be33607a'),
              value: 'smtp.163.com'
            },
            {
              title: this.$t('pages.system.mail.10672991'),
              value: 'smtp.126.com'
            },
            {
              title: this.$t('pages.system.mail.615763f6'),
              value: 'smtp.mxhichina.com'
            },
            {
              title: this.$t('pages.system.mail.7d77f35'),
              value: 'smtp.gmail.com'
            }
          ]
        }
      ],
      portDataSource: [
        {
          title: this.$t('pages.system.mail.bc591b'),
          options: [
            {
              title: this.$t('pages.system.mail.bc591b'),
              value: '587'
            },
            {
              title: this.$t('pages.system.mail.c72b6b5d'),
              value: '465'
            }
          ]
        },
        {
          title: this.$t('pages.system.mail.be33607a'),
          options: [
            {
              title: this.$t('pages.system.mail.be33607a'),
              value: '25'
            },
            {
              title: this.$t('pages.system.mail.99bb086c'),
              value: '465'
            }
          ]
        },
        {
          title: this.$t('pages.system.mail.615763f6'),
          options: [
            {
              title: this.$t('pages.system.mail.eb615a7b'),
              value: '465'
            }
          ]
        },
        {
          title: this.$t('pages.system.mail.86a4604f'),
          options: [
            {
              title: this.$t('pages.system.mail.3f0a8e8e'),
              value: '465'
            }
          ]
        }
      ]
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.system.mail.${key}`, ...args)
    },
    // load data
    loadData() {
      getMailConfigData().then((res) => {
        if (res.code === 200) {
          this.temp = res.data || { type: 'add' }
          if (this.temp.port) {
            this.temp.port = this.temp.port + ''
          }
        }
      })
    },
    // submit
    onSubmit() {
      // disabled submit button
      this.submitAble = true
      // if (this.temp.sslsslEnable === false) {
      //   this.temp.socketFactoryPort = "";
      // }
      editMailConfig(this.temp).then((res) => {
        if (res.code === 200) {
          // 成功
          $notification.success({
            message: res.msg
          })
        }
        // button recover
        this.submitAble = false
      })
    },
    handleFromSearch(val) {
      // let result
      // if (!value || value.indexOf('@') >= 0) {
      //   result = []
      // } else {
      //   result = ['gmail.com', '163.com', 'qq.com'].map((domain) => {
      //     value: `${value}@${domain}`
      //   })
      // }
      // console.log(result)

      let res = []
      if (!val || val.indexOf('@') >= 0) {
        res = []
      } else {
        res = ['gmail.com', '163.com', 'qq.com'].map((domain) => ({ value: `${val}@${domain}` }))
      }

      this.fromResult = res
    }
  }
}
</script>

<style scoped>
.btn {
  margin-left: 20px;
}
</style>
