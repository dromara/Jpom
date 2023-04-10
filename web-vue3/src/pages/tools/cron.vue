<template>
  <div>
    <a-row justify="center" type="flex">
      <a-col :span="18">
        <a-space direction="vertical" style="display: inline">
          <a-alert message="此工具用于检查 cron 表达式是否正确,以及计划运行时间" type="info" />
          <a-collapse>
            <a-collapse-panel key="1" header="更多说明">
              定时任务表达式<br />
              表达式类似于Linux的crontab表达式，表达式使用空格分成5个部分，按顺序依次为：
              <ol>
                <li><strong>分</strong> ：范围：0~59</li>
                <li><strong>时</strong> ：范围：0~23</li>
                <li><strong>日</strong> ：范围：1~31，<strong>"L"</strong> 表示月的最后一天</li>
                <li>
                  <strong>月</strong> ：范围：1~12，同时支持不区分大小写的别名："jan","feb", "mar", "apr", "may","jun",
                  "jul", "aug", "sep","oct", "nov", "dec"
                </li>
                <li>
                  <strong>周</strong> ：范围：0
                  (Sunday)~6(Saturday)，7也可以表示周日，同时支持不区分大小写的别名："sun","mon", "tue", "wed",
                  "thu","fri", "sat"，<strong>"L"</strong> 表示周六
                </li>
              </ol>
              <p>为了兼容Quartz表达式，同时支持6位和7位表达式，其中：<br /></p>

              <pre>
  当为6位时，第一位表示<strong>秒</strong> ，范围0~59，但是第一位不做匹配
  当为7位时，最后一位表示<strong>年</strong> ，范围1970~2099，但是第7位不做解析，也不做匹配
  </pre
              >
              <p>
                当定时任务运行到的时间匹配这些表达式后，任务被启动。<br />
                注意：
              </p>

              <pre>
  当isMatchSecond为 true 时才会匹配秒部分
  默认都是关闭的
  </pre
              >
              <p>对于每一个子表达式，同样支持以下形式：</p>

              <ul>
                <li><strong>*</strong> ：表示匹配这个位置所有的时间</li>
                <li><strong>?</strong> ：表示匹配这个位置任意的时间（与"*"作用一致）</li>
                <li>
                  <strong>*&#47;2</strong> ：表示间隔时间，例如在分上，表示每两分钟，同样*可以使用数字列表代替，逗号分隔
                </li>
                <li><strong>2-8</strong> ：表示连续区间，例如在分上，表示2,3,4,5,6,7,8分</li>
                <li><strong>2,3,5,8</strong> ：表示列表</li>
                <li><strong>cronA | cronB</strong> ：表示多个定时表达式</li>
              </ul>
              注意：在每一个子表达式中优先级：

              <pre>
  间隔（/） &gt; 区间（-） &gt; 列表（,）
  </pre
              >
              <p>
                例如 2,3,6/3中，由于“/”优先级高，因此相当于2,3,(6/3)，结果与 2,3,6等价<br />
                <br />
              </p>

              <p>一些例子：</p>

              <ul>
                <li><strong>5 * * * *</strong> ：每个点钟的5分执行，00:05,01:05……</li>
                <li><strong>* * * * *</strong> ：每分钟执行</li>
                <li><strong>*&#47;2 * * * *</strong> ：每两分钟执行</li>
                <li><strong>* 12 * * *</strong> ：12点的每分钟执行</li>
                <li><strong>59 11 * * 1,2</strong> ：每周一和周二的11:59执行</li>
                <li>
                  <strong>3-18&#47;5 * * * *</strong> ：3~18分，每5分钟执行一次，即0:03, 0:08, 0:13, 0:18, 1:03, 1:08……
                </li>
              </ul>
            </a-collapse-panel>
          </a-collapse>
          <a-form :model="temp" ref="form" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
            <a-form-item label="cron表达式" prop="cron">
              <a-input v-model="temp.cron" placeholder="请输入要检查的 cron 表达式" />
            </a-form-item>
            <a-form-item label="计划次数" prop="count">
              <a-input-number
                v-model="temp.count"
                :min="1"
                placeholder="请输入获取的计划运行次数"
                style="width: 100%"
              />
            </a-form-item>
            <a-form-item label="匹配秒">
              <a-switch v-model="temp.isMatchSecond" checked-children="是" un-checked-children="否" />
            </a-form-item>
            <a-form-item label="时间范围" prop="date" help="默认是当前时间到今年结束">
              <a-range-picker
                format="YYYY-MM-DD"
                valueFormat="YYYY-MM-DD"
                separator="至"
                v-model="temp.date"
                style="width: 100%"
              />
            </a-form-item>

            <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
              <a-button type="primary" @click="onSubmit"> 检查 </a-button>
            </a-form-item>
          </a-form>
        </a-space>
      </a-col>

      <a-col :span="10">
        <a-list bordered :data-source="resultList" :locale="locale">
          <a-list-item slot="renderItem" slot-scope="item">
            {{ parseTime(item, '{y}-{m}-{d} {h}:{i}:{s} 周{a}') }}
          </a-list-item>
          <div slot="header">结果</div>
        </a-list>
      </a-col>
    </a-row>
  </div>
</template>
<script>
import { cronTools } from '@/api/tools'
import { parseTime } from '@/utils/const'
export default {
  data() {
    return {
      temp: {
        count: 10
      },
      locale: {
        emptyText: '暂无数据'
      },
      resultList: [],
      // 表单校验规则
      rules: {
        cron: [{ required: true, message: '请输入要检查的 cron 表达式', trigger: 'blur' }],
        count: [{ required: true, message: '请输入获取的计划运行次数', trigger: 'blur' }]
      }
    }
  },
  mounted() {
    const cron = this.$route.query.cron
    if (cron) {
      this.temp = { ...this.temp, cron: cron }
      this.$nextTick(() => {
        this.onSubmit()
      })
    }
  },
  methods: {
    parseTime,
    onSubmit() {
      this.$refs['form'].validate((valid) => {
        if (!valid) {
          return false
        }
        this.resultList = []
        this.locale = {
          emptyText: '暂无数据'
        }
        const temp = { ...this.temp, date: this.temp.date && this.temp.date[0] + ' ~ ' + this.temp.date[1] }

        cronTools(temp).then((res) => {
          //   console.log(res);
          this.resultList = res.data || []
          this.locale = {
            emptyText: res.msg
          }
        })
      })
    }
  }
}
</script>
