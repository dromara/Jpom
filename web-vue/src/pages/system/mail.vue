<template>
  <div class="">
    <a-form ref="editForm" :model="temp" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
      <a-form-item :label="$tl('p.555k7g')" name="host">
        <a-auto-complete v-model:value="temp.host" :options="hostDataSource" :placeholder="$tl('p.7585gx')">
          <template #option="item"> {{ item.title }} {{ item.value }} </template>
        </a-auto-complete>
      </a-form-item>
      <a-form-item :label="$tl('p.g1x2v0')" name="port">
        <a-auto-complete v-model:value="temp.port" :placeholder="$tl('p.b43f3c')" :options="portDataSource">
          <template #option="item"> {{ item.title }} {{ item.value }} </template>
        </a-auto-complete>
      </a-form-item>
      <a-form-item :label="$tl('p.x123f5')" name="user">
        <a-input v-model:value="temp.user" type="text" :placeholder="$tl('p.e3x5s6')" />
      </a-form-item>
      <a-form-item :label="$tl('p.967dv3')" :name="`${temp.type === 'add' ? 'pass' : 'pass-update'}`">
        <a-input-password v-model:value="temp.pass" type="text" :placeholder="$tl('p.24bc67')" />
      </a-form-item>
      <a-form-item :label="$tl('p.11lc4u')" name="from">
        <!-- <a-input v-model="temp.from" type="text" placeholder="发送方邮箱账号" /> -->
        <a-tooltip>
          <template #title
            >{{ $tl('p.hlr145') }}
            <ul>
              <li>1. user@xxx.xx</li>
              <li>2. name &lt;user@xxx.xx&gt;</li>
            </ul>
          </template>
          <a-auto-complete
            v-model:value="temp.from"
            :options="fromResult"
            :placeholder="$tl('p.jo851t')"
            @search="handleFromSearch"
          >
            <template #option="{ value: val }">
              {{ val.split('@')[0] }} @
              <span style="font-weight: bold">{{ val.split('@')[1] }}</span>
            </template>
          </a-auto-complete>
        </a-tooltip>
      </a-form-item>
      <a-form-item :label="$tl('p.65jswb')" name="sslEnable">
        <a-switch
          v-model:checked="temp.sslEnable"
          :checked-children="$tl('p.9m81o3')"
          :un-checked-children="$tl('p.12qqll')"
        />
        <!-- <a-input v-show="temp.sslEnable" v-model="temp.socketFactoryPort" type="text" placeholder="SSL 端口" /> -->
      </a-form-item>
      <a-form-item :label="$tl('p.3ke6sj')" name="timeout">
        <a-input-number
          v-model:value="temp.timeout"
          style="width: 100%"
          :min="3"
          type="text"
          :placeholder="$tl('p.1h5ni7')"
        />
      </a-form-item>

      <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
        <a-button type="primary" class="btn" :disabled="submitAble" @click="onSubmit">{{ $tl('p.1i9kfq') }}</a-button>
      </a-form-item>
    </a-form>
    <a-alert :message="$tl('p.17x6wc')" :description="$tl('p.kj0r1z')" type="info" show-icon />
    <br />
    <a-alert :message="$tl('p.jl3v34')" type="info" :description="$tl('p.x5fn7v')" show-icon />
    <br />
    <a-alert :message="$tl('p.6k71aq')" :description="$tl('p.276h03')" type="info" show-icon />
    <br />
    <a-alert :message="$tl('p.2puoo6')" :description="$tl('p.yp8615')" type="info" show-icon />
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
            message: this.$tl('p.dj8v8p'),
            trigger: 'blur'
          }
        ],
        pass: [{ required: true, message: this.$tl('p.5p2791'), trigger: 'blur' }],
        user: [
          {
            required: true,
            message: this.$tl('p.e113am'),
            trigger: 'blur'
          }
        ],
        from: [
          {
            required: true,
            message: this.$tl('p.dw7gk9'),
            trigger: 'blur'
          }
        ]
      },
      fromResult: [],
      hostDataSource: [
        {
          title: this.$tl('p.cp4ium'),
          options: [
            {
              title: this.$tl('p.88m1o4'),
              value: 'smtp.qq.com'
            },
            {
              title: this.$tl('p.87531i'),
              value: 'smtp.163.com'
            },
            {
              title: this.$tl('p.qy3mk5'),
              value: 'smtp.126.com'
            },
            {
              title: this.$tl('p.3g1t1h'),
              value: 'smtp.mxhichina.com'
            },
            {
              title: this.$tl('p.24wg08'),
              value: 'smtp.gmail.com'
            }
          ]
        }
      ],
      portDataSource: [
        {
          title: this.$tl('p.88m1o4'),
          options: [
            {
              title: this.$tl('p.88m1o4'),
              value: '587'
            },
            {
              title: this.$tl('p.1u96ou'),
              value: '465'
            }
          ]
        },
        {
          title: this.$tl('p.87531i'),
          options: [
            {
              title: this.$tl('p.87531i'),
              value: '25'
            },
            {
              title: this.$tl('p.ddrbyl'),
              value: '465'
            }
          ]
        },
        {
          title: this.$tl('p.3g1t1h'),
          options: [
            {
              title: this.$tl('p.65lhy6'),
              value: '465'
            }
          ]
        },
        {
          title: this.$tl('p.c1c5d2'),
          options: [
            {
              title: this.$tl('p.5ehoes'),
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
