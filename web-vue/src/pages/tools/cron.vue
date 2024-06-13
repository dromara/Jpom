<template>
  <div>
    <a-row justify="center" type="flex">
      <a-col :span="18">
        <a-space direction="vertical" style="width: 100%">
          <a-alert :message="$t('pages.tools.cron.42e3c32a')" type="info" />
          <a-collapse>
            <a-collapse-panel key="1" :header="$t('pages.tools.cron.3427283b')">
              {{ $t('pages.tools.cron.ecf3c957') }}<br />{{ $t('pages.tools.cron.287d8892') }}
              <ol>
                <li>
                  <strong>{{ $t('pages.tools.cron.bcac9e7d') }}</strong> ：{{ $t('pages.tools.cron.99490a8a') }}~59
                </li>
                <li>
                  <strong>{{ $t('pages.tools.cron.954bf355') }}</strong> ：{{ $t('pages.tools.cron.99490a8a') }}~23
                </li>
                <li>
                  <strong>{{ $t('pages.tools.cron.4ec579d4') }}</strong> ：{{
                    $t('pages.tools.cron.405b30')
                  }}~31，<strong>"L"</strong>
                  {{ $t('pages.tools.cron.b4e42843') }}
                </li>
                <li>
                  <strong>{{ $t('pages.tools.cron.4368c08') }}</strong>
                  ：{{ $t('pages.tools.cron.405b30') }}~12，{{ $t('pages.tools.cron.e6541eb2') }}"jan","feb", "mar",
                  "apr", "may","jun", "jul", "aug","sep","oct", "nov", "dec"
                </li>
                <li>
                  <strong>{{ $t('pages.tools.cron.925f16bf') }}</strong> ：{{
                    $t('pages.tools.cron.99490a8a')
                  }}
                  (Sunday)~6(Saturday)，7{{ $t('pages.tools.cron.1443146c') }}"sun","mon", "tue", "wed", "thu","fri",
                  "sat"，<strong>"L"</strong>
                  {{ $t('pages.tools.cron.50b54654') }}
                </li>
              </ol>
              <p>{{ $t('pages.tools.cron.3f4889d6') }}<br /></p>

              <pre>
    {{$t('pages.tools.cron.5b44a2be')}}<strong >{{$t('pages.tools.cron.512dbcf2')}}</strong> ，{{$t('pages.tools.cron.1c7a0cd4')}}~59，{{$t('pages.tools.cron.1b17e4d')}}
    {{$t('pages.tools.cron.c3619377')}}<strong >{{$t('pages.tools.cron.5ed7912c')}}</strong> ，{{$t('pages.tools.cron.4c7cba2e')}}~2099，{{$t('pages.tools.cron.f450c90d')}}
    </pre>
              <p>
                {{ $t('pages.tools.cron.53484a75') }}<br />
                {{ $t('pages.tools.cron.2ae8180f') }}
              </p>

              <pre>
                {{ $t('pages.tools.cron.6538cef8') }}
                </pre
              >
              <p>{{ $t('pages.tools.cron.4c0836b9') }}</p>

              <ul>
                <li><strong>*</strong> ：{{ $t('pages.tools.cron.6aa5cbdf') }}</li>
                <li>
                  <strong>?</strong>
                  ：{{ $t('pages.tools.cron.ff49e4bd') }}"*"{{ $t('pages.tools.cron.7d881567') }}
                </li>
                <li>
                  <strong>*&#47;2</strong>
                  ：{{ $t('pages.tools.cron.2314847d') }}
                </li>
                <li>
                  <strong>2-8</strong>
                  ：{{ $t('pages.tools.cron.307f6078') }},3,4,5,6,7,8{{ $t('pages.tools.cron.bcac9e7d') }}
                </li>
                <li><strong>2,3,5,8</strong> ：{{ $t('pages.tools.cron.d85c00e2') }}</li>
                <li><strong>cronA | cronB</strong> ：{{ $t('pages.tools.cron.8d03f3cb') }}</li>
              </ul>
              {{ $t('pages.tools.cron.84815e4a') }}

              <pre>
    {{ $t('pages.tools.cron.cefae188') }} &gt; {{ $t('pages.tools.cron.67b01a7f') }} &gt; {{
                  $t('pages.tools.cron.3a4ec9a9')
                }},）
    </pre
              >
              <p>
                {{ $t('pages.tools.cron.d9d0e64f') }},3,6/3{{ $t('pages.tools.cron.c2c807a') }}“/”{{
                  $t('pages.tools.cron.40d9b7f5')
                }},3,(6/3)，{{ $t('pages.tools.cron.58c4dc89') }},3,6{{ $t('pages.tools.cron.f2dae167') }}<br />
                <br />
              </p>

              <p>{{ $t('pages.tools.cron.4102c6f3') }}</p>

              <ul>
                <li><strong>5 * * * *</strong> ：{{ $t('pages.tools.cron.d45c7a96') }}:05,01:05……</li>
                <li><strong>* * * * *</strong> ：{{ $t('pages.tools.cron.f5d7ef4f') }}</li>
                <li><strong>*&#47;2 * * * *</strong> ：{{ $t('pages.tools.cron.34dd9a6d') }}</li>
                <li><strong>* 12 * * *</strong> ：12{{ $t('pages.tools.cron.d81bf105') }}</li>
                <li>
                  <strong>59 11 * * 1,2</strong> ：{{ $t('pages.tools.cron.baadc627') }}:59{{
                    $t('pages.tools.cron.a0fe2109')
                  }}
                </li>
                <li>
                  <strong>3-18&#47;5 * * * *</strong>
                  ：3~18{{ $t('pages.tools.cron.89f0062e') }}:03, 0:08, 0:13, 0:18, 1:03, 1:08……
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
            <a-form-item :label="$t('pages.tools.cron.f298e2e3')" name="cron">
              <a-input v-model:value="temp.cron" :placeholder="$t('pages.tools.cron.9f134416')" />
            </a-form-item>
            <a-form-item :label="$t('pages.tools.cron.217189b7')" name="count">
              <a-input-number
                v-model:value="temp.count"
                :min="1"
                :placeholder="$t('pages.tools.cron.2f75f261')"
                style="width: 100%"
              />
            </a-form-item>
            <a-form-item :label="$t('pages.tools.cron.aae6046e')">
              <a-switch
                v-model:checked="temp.isMatchSecond"
                :checked-children="$t('pages.tools.cron.81aa95e')"
                :un-checked-children="$t('pages.tools.cron.287f3530')"
              />
            </a-form-item>
            <a-form-item :label="$t('pages.tools.cron.80e6c8a9')" name="date" :help="$t('pages.tools.cron.25e421ca')">
              <a-range-picker
                v-model:value="temp.date"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                :separator="$t('pages.tools.cron.eef6d451')"
                style="width: 100%"
              />
            </a-form-item>

            <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
              <a-button type="primary" html-type="submit"> {{ $t('pages.tools.cron.b60e301d') }} </a-button>
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
            <div>{{ $t('pages.tools.cron.f4586c88') }}</div>
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
        emptyText: this.$t('pages.tools.cron.a1dd3707')
      },
      resultList: [],
      // 表单校验规则
      rules: {
        cron: [
          {
            required: true,
            message: this.$t('pages.tools.cron.9f134416'),
            trigger: 'blur'
          }
        ],

        count: [
          {
            required: true,
            message: this.$t('pages.tools.cron.2f75f261'),
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
    parseTime,
    onSubmit() {
      this.resultList = []
      this.locale = {
        emptyText: this.$t('pages.tools.cron.a1dd3707')
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
