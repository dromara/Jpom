<template>
  <div>
    <a-row justify="center" type="flex">
      <a-col :span="18">
        <a-space direction="vertical" style="width: 100%">
          <a-alert :message="$tl('p.description')" type="info" />
          <a-collapse>
            <a-collapse-panel key="1" :header="$tl('p.moreInfo')">
              {{ $tl('p.cronExpressionLabel') }}<br />{{ $tl('p.expressionIntro') }}
              <ol>
                <li>
                  <strong>{{ $tl('c.unit') }}</strong> ：{{ $tl('c.range1') }}~59
                </li>
                <li>
                  <strong>{{ $tl('p.hour') }}</strong> ：{{ $tl('c.range1') }}~23
                </li>
                <li>
                  <strong>{{ $tl('p.dayOfMonth') }}</strong> ：{{ $tl('c.range2') }}~31，<strong>"L"</strong>
                  {{ $tl('p.lastDayOfMonth') }}
                </li>
                <li>
                  <strong>{{ $tl('p.month') }}</strong>
                  ：{{ $tl('c.range2') }}~12，{{ $tl('p.aliases1') }}"jan","feb", "mar", "apr", "may","jun", "jul",
                  "aug","sep","oct", "nov", "dec"
                </li>
                <li>
                  <strong>{{ $tl('p.dayOfWeek') }}</strong> ：{{ $tl('c.range1') }} (Sunday)~6(Saturday)，7{{
                    $tl('p.sundayAlias')
                  }}"sun","mon", "tue", "wed", "thu","fri", "sat"，<strong>"L"</strong>
                  {{ $tl('p.saturdayAlias') }}
                </li>
              </ol>
              <p>{{ $tl('p.quartzCompatibility') }}<br /></p>

              <pre>
    {{$tl('p.firstFieldWhen6Digits')}}<strong>{{$tl('p.second')}}</strong> ，{{$tl('p.range0')}}~59，{{$tl('p.firstFieldNotMatched')}}
    {{$tl('p.lastFieldWhen7Digits')}}<strong>{{$tl('p.year')}}</strong> ，{{$tl('p.range1970')}}~2099，{{$tl('p.lastFieldNotParsedOrMatched')}}
    </pre>
              <p>
                {{ $tl('p.taskTriggering') }}<br />
                {{ $tl('p.note') }}
              </p>

              <pre>
                {{ $tl('p.isMatchSecond') }}
                </pre
              >
              <p>{{ $tl('p.supportedFormats') }}</p>

              <ul>
                <li><strong>*</strong> ：{{ $tl('p.allTimes') }}</li>
                <li>
                  <strong>?</strong>
                  ：{{ $tl('p.anyTime') }}"*"{{ $tl('p.anyTimeAlias') }}
                </li>
                <li>
                  <strong>*&#47;2</strong>
                  ：{{ $tl('p.interval') }}
                </li>
                <li>
                  <strong>2-8</strong>
                  ：{{ $tl('p.continuousRange') }},3,4,5,6,7,8{{ $tl('c.unit') }}
                </li>
                <li><strong>2,3,5,8</strong> ：{{ $tl('p.valueList') }}</li>
                <li><strong>cronA | cronB</strong> ：{{ $tl('p.multipleExpressions') }}</li>
              </ul>
              {{ $tl('p.priorityNote') }}

              <pre>
    {{ $tl('p.intervalPriority') }} &gt; {{ $tl('p.rangePriority') }} &gt; {{ $tl('p.listPriority') }},）
    </pre
              >
              <p>
                {{ $tl('p.example1') }},3,6/3{{ $tl('p.exampleNote') }}“/”{{ $tl('p.example2') }},3,(6/3)，{{
                  $tl('p.exampleResult')
                }},3,6{{ $tl('p.exampleEquivalence') }}<br />
                <br />
              </p>

              <p>{{ $tl('p.examples') }}</p>

              <ul>
                <li><strong>5 * * * *</strong> ：{{ $tl('p.exampleCron') }}:05,01:05……</li>
                <li><strong>* * * * *</strong> ：{{ $tl('p.scheduleType') }}</li>
                <li><strong>*&#47;2 * * * *</strong> ：{{ $tl('p.scheduleInterval') }}</li>
                <li><strong>* 12 * * *</strong> ：12{{ $tl('p.scheduleDetail') }}</li>
                <li><strong>59 11 * * 1,2</strong> ：{{ $tl('p.dayOfWeek_1') }}:59{{ $tl('p.action') }}</li>
                <li>
                  <strong>3-18&#47;5 * * * *</strong>
                  ：3~18{{ $tl('p.minute') }}:03, 0:08, 0:13, 0:18, 1:03, 1:08……
                </li>
              </ul>
            </a-collapse-panel>
          </a-collapse>
          <a-form
            ref="form"
            :model="temp"
            :rules="rules"
            :label-col="{ span: 4 }"
            :wrapper-col="{ span: 18 }"
            @finish="onSubmit"
          >
            <a-form-item :label="$tl('p.cron')" name="cron">
              <a-input v-model:value="temp.cron" :placeholder="$tl('c.cronExpression')" />
            </a-form-item>
            <a-form-item :label="$tl('p.scheduleCount')" name="count">
              <a-input-number
                v-model:value="temp.count"
                :min="1"
                :placeholder="$tl('c.executionTimes')"
                style="width: 100%"
              />
            </a-form-item>
            <a-form-item :label="$tl('p.matchSecond')">
              <a-switch
                v-model:checked="temp.isMatchSecond"
                :checked-children="$tl('p.isEnable')"
                :un-checked-children="$tl('p.disable')"
              />
            </a-form-item>
            <a-form-item :label="$tl('p.timeRange')" name="date" :help="$tl('p.defaultTimeRange')">
              <a-range-picker
                v-model:value="temp.date"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                :separator="$tl('p.to')"
                style="width: 100%"
              />
            </a-form-item>

            <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
              <a-button type="primary" html-type="submit"> {{ $tl('p.check') }} </a-button>
            </a-form-item>
          </a-form>
        </a-space>
      </a-col>

      <a-col :span="10">
        <a-list bordered :data-source="resultList" :locale="locale">
          <template #renderItem="{ item }">
            <a-list-item>
              {{ parseTime(item, 'YYYY-MM-DD HH:mm:ss') }}
            </a-list-item>
          </template>
          <template #header>
            <div>{{ $tl('p.result') }}</div>
          </template>
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
        emptyText: this.$tl('c.noData')
      },
      resultList: [],
      // 表单校验规则
      rules: {
        cron: [
          {
            required: true,
            message: this.$tl('c.cronExpression'),
            trigger: 'blur'
          }
        ],
        count: [
          {
            required: true,
            message: this.$tl('c.executionTimes'),
            trigger: 'blur'
          }
        ]
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
    $tl(key, ...args) {
      return this.$t(`pages.tools.cron.${key}`, ...args)
    },
    parseTime,
    onSubmit() {
      this.resultList = []
      this.locale = {
        emptyText: this.$tl('c.noData')
      }
      const temp = {
        ...this.temp,
        date: this.temp.date && this.temp.date[0] + ' ~ ' + this.temp.date[1]
      }

      cronTools(temp).then((res) => {
        //   console.log(res);
        this.resultList = res.data || []
        this.locale = {
          emptyText: res.msg
        }
      })
    }
  }
}
</script>
