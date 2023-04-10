<template>
  <div>
    <div v-if="this.triggerToken || this.triggerVisible">
      <a-form ref="editTriggerForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-tabs default-active-key="1" type="card">
          <template slot="tabBarExtraContent">
            <a-tooltip title="重置触发器 token 信息,重置后之前的触发器 token 将失效">
              <a-button type="primary" size="small" @click="resetTrigger">重置</a-button>
            </a-tooltip>
          </template>
          <a-tab-pane key="1" tab="执行构建">
            <a-space style="display: block" direction="vertical" align="baseline">
              <a-alert message="温馨提示" type="warning">
                <template slot="description">
                  <ul>
                    <li>单个触发器地址中：第一个随机字符串为构建ID，第二个随机字符串为 token</li>
                    <li>
                      重置为重新生成触发地址,重置成功后之前的触发器地址将失效,构建触发器绑定到生成触发器到操作人上,如果将对应的账号删除触发器将失效
                    </li>
                    <li>批量构建参数 BODY json： [ { "id":"1", "token":"a", "delay":"0" } ]</li>
                    <li>
                      批量构建参数还支持指定参数,delay（延迟执行构建,单位秒）
                      branchName（分支名）、branchTagName（标签）、script（构建脚本）、resultDirFile（构建产物）、webhook（通知
                      webhook）
                    </li>
                    <li>
                      批量构建全部参数举例 BODY json： [ { "id":"1", "token":"a",
                      "delay":"0","branchName":"test","branchTagName":"1.*","script":"mvn clean
                      package","resultDirFile":"/target/","webhook":"http://test.com/webhook" } ]
                    </li>
                    <li>
                      参数如果传入 useQueue=true
                      将使用微队列来排队构建，避免几乎同时触发构建被中断构建（一般用户仓库合并代码会触发多次请求）,队列保存在内存中,重启将丢失
                    </li>
                    <li>批量构建传入其他参数将同步执行修改</li>
                  </ul>
                </template>
              </a-alert>
              <a-alert
                v-clipboard:copy="temp.triggerBuildUrl"
                v-clipboard:success="
                  () => {
                    tempVue.prototype.$notification.success({ message: '复制成功' })
                  }
                "
                v-clipboard:error="
                  () => {
                    tempVue.prototype.$notification.error({ message: '复制失败' })
                  }
                "
                type="info"
                :message="`单个触发器地址(点击可以复制)`"
              >
                <template slot="description">
                  <a-tag>GET</a-tag> <span>{{ temp.triggerBuildUrl }} </span>
                  <a-icon type="copy" />
                </template>
              </a-alert>
              <a-alert
                v-clipboard:copy="temp.batchTriggerBuildUrl"
                v-clipboard:success="
                  () => {
                    tempVue.prototype.$notification.success({ message: '复制成功' })
                  }
                "
                v-clipboard:error="
                  () => {
                    tempVue.prototype.$notification.error({ message: '复制失败' })
                  }
                "
                type="info"
                :message="`批量触发器地址(点击可以复制)`"
              >
                <template slot="description">
                  <a-tag>POST</a-tag> <span>{{ temp.batchTriggerBuildUrl }} </span>
                  <a-icon type="copy" />
                </template>
              </a-alert>
            </a-space>
          </a-tab-pane>
          <a-tab-pane key="2" tab="查看当前状态">
            <a-space style="display: block" direction="vertical" align="baseline">
              <a-alert message="温馨提示" type="warning">
                <template slot="description">
                  <ul>
                    <li>批量构建参数 BODY json： [ { "id":"1", "token":"a" } ]</li>
                    <li>参数中的 id 、token 和触发构建一致</li>
                    <li>
                      <a-tag>No(0, "未构建")</a-tag>, <a-tag>Ing(1, "构建中")</a-tag>,
                      <a-tag>Success(2, "构建结束")</a-tag>, <a-tag>Error(3, "构建失败")</a-tag>,
                      <a-tag>PubIng(4, "发布中")</a-tag>, <a-tag>PubSuccess(5, "发布成功")</a-tag>,
                      <a-tag>PubError(6, "发布失败")</a-tag>, <a-tag>Cancel(7, "取消构建")</a-tag>,
                    </li>
                  </ul>
                </template>
              </a-alert>
              <a-alert
                v-clipboard:copy="temp.batchBuildStatusUrl2"
                v-clipboard:success="
                  () => {
                    tempVue.prototype.$notification.success({ message: '复制成功' })
                  }
                "
                v-clipboard:error="
                  () => {
                    tempVue.prototype.$notification.error({ message: '复制失败' })
                  }
                "
                type="info"
                :message="`获取单个构建状态地址(点击可以复制)`"
              >
                <template slot="description">
                  <a-tag>GET</a-tag> <span>{{ temp.batchBuildStatusUrl2 }} </span>
                  <a-icon type="copy" />
                </template>
              </a-alert>
              <a-alert
                v-clipboard:copy="temp.batchBuildStatusUrl"
                v-clipboard:success="
                  () => {
                    tempVue.prototype.$notification.success({ message: '复制成功' })
                  }
                "
                v-clipboard:error="
                  () => {
                    tempVue.prototype.$notification.error({ message: '复制失败' })
                  }
                "
                type="info"
                :message="`批量获取构建状态地址(点击可以复制)`"
              >
                <template slot="description">
                  <a-tag>POST</a-tag> <span>{{ temp.batchBuildStatusUrl }} </span>
                  <a-icon type="copy" />
                </template>
              </a-alert>
            </a-space>
          </a-tab-pane>
          <a-tab-pane key="3" tab="查看构建日志">
            <a-space style="display: block" direction="vertical" align="baseline">
              <a-alert message="温馨提示" type="warning">
                <template slot="description">
                  <ul>
                    <li>参数中的 id 、token 和触发构建一致、buildNumId 构建序号id</li>
                    <li>构建序号id需要跟进实际情况替换</li>
                  </ul>
                </template>
              </a-alert>
              <a-alert
                v-clipboard:copy="temp.buildLogUrl"
                v-clipboard:success="
                  () => {
                    tempVue.prototype.$notification.success({ message: '复制成功' })
                  }
                "
                v-clipboard:error="
                  () => {
                    tempVue.prototype.$notification.error({ message: '复制失败' })
                  }
                "
                type="info"
                :message="`获取单个构建日志地址(点击可以复制)`"
              >
                <template slot="description">
                  <a-tag>GET</a-tag> <span>{{ temp.buildLogUrl }} </span>
                  <a-icon type="copy" />
                </template>
              </a-alert>
            </a-space>
          </a-tab-pane>
        </a-tabs>
      </a-form>
    </div>
    <template v-else>
      <a-result title="当前构建还没有生成触发器">
        <template #extra>
          <a-button key="console" type="primary" @click="handleTrigger"> 现成生成 </a-button>
        </template>
      </a-result>
    </template>
  </div>
</template>
<script>
import Vue from 'vue'
import { getTriggerUrl } from '@/api/build-info'
export default {
  props: {
    id: {
      type: String
    },
    triggerToken: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      temp: {},
      tempVue: null,
      triggerVisible: false
    }
  },
  created() {
    if (this.triggerToken) {
      this.handleTrigger()
    }
  },
  methods: {
    // 触发器
    handleTrigger() {
      this.temp = {}
      this.tempVue = Vue
      getTriggerUrl({
        id: this.id
      }).then((res) => {
        if (res.code === 200) {
          this.fillTriggerResult(res)
          this.triggerVisible = true
        }
      })
    },
    // 重置触发器
    resetTrigger() {
      getTriggerUrl({
        id: this.id,
        rest: 'rest'
      }).then((res) => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg
          })
          this.fillTriggerResult(res)
        }
      })
    },
    fillTriggerResult(res) {
      this.temp.triggerBuildUrl = `${location.protocol}//${location.host}${res.data.triggerBuildUrl}`
      this.temp.batchTriggerBuildUrl = `${location.protocol}//${location.host}${res.data.batchTriggerBuildUrl}`
      this.temp.batchBuildStatusUrl = `${location.protocol}//${location.host}${res.data.batchBuildStatusUrl}`
      this.temp.buildLogUrl = `${location.protocol}//${location.host}${res.data.buildLogUrl}`
      // this.temp.id = res.data.id;
      // this.temp.token = res.data.token;
      this.temp.batchBuildStatusUrl2 = `${this.temp.batchBuildStatusUrl}?id=${res.data.id}&token=${res.data.token}`
      this.temp.buildLogUrl = `${this.temp.buildLogUrl}?id=${res.data.id}&token=${res.data.token}&buildNumId=0`
      this.temp = { ...this.temp }
    }
  }
}
</script>
