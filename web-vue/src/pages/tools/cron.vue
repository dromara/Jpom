<template>
  <div>
    <a-row justify="center" type="flex">
      <a-col :span="18">
        <a-space direction="vertical" style="width: 100%">
          <a-alert :message="$t('i18n.9880bd3ba1')" type="info" />
          <a-collapse>
            <a-collapse-panel key="1" :header="$t('i18n.ef734bf850')">
              {{ $t('i18n.71c6871780') }}<br />{{ $t('i18n.e930e7890f') }}
              <ol>
                <li>
                  <strong>{{ $t('i18n.daf783c8cd') }}</strong> ：{{ $t('i18n.3444bf320c') }}~59
                </li>
                <li>
                  <strong>{{ $t('i18n.609b5f0a08') }}</strong> ：{{ $t('i18n.3444bf320c') }}~23
                </li>
                <li>
                  <strong>{{ $t('i18n.3edddd85ac') }}</strong> ：{{ $t('i18n.0b35ccf5df') }}~31，<strong>"L"</strong>
                  {{ $t('i18n.7b961e05d0') }}
                </li>
                <li>
                  <strong>{{ $t('i18n.e42b99d599') }}</strong>
                  ：{{ $t('i18n.0b35ccf5df') }}~12，{{ $t('i18n.41cb3d959a') }}"jan","feb", "mar", "apr", "may","jun",
                  "jul", "aug","sep","oct", "nov", "dec"
                </li>
                <li>
                  <strong>{{ $t('i18n.a657f46f5b') }}</strong> ：{{ $t('i18n.3444bf320c') }} (Sunday)~6(Saturday)，7{{
                    $t('i18n.6d4ca2c602')
                  }}"sun","mon", "tue", "wed", "thu","fri", "sat"，<strong>"L"</strong>
                  {{ $t('i18n.207d9580c1') }}
                </li>
              </ol>
              <p>{{ $t('i18n.f6d96c1c8c') }}<br /></p>

              <pre>
    {{$t('i18n.0c4eef1b88')}}<strong >{{$t('i18n.0c1fec657f')}}</strong> ，{{$t('i18n.fb413eeaaf')}}~59，{{$t('i18n.689fe0fdcf')}}
    {{$t('i18n.4803a725e9')}}<strong >{{$t('i18n.465260fe80')}}</strong> ，{{$t('i18n.ee11dbb340')}}~2099，{{$t('i18n.66af7eed0d')}}
    </pre>
              <p>
                {{ $t('i18n.3ae4c953fe') }}<br />
                {{ $t('i18n.ba8d1dca4a') }}
              </p>

              <pre>
                {{ $t('i18n.674a284936') }}
                </pre
              >
              <p>{{ $t('i18n.01226f48fc') }}</p>

              <ul>
                <li><strong>*</strong> ：{{ $t('i18n.0bff05c9cf') }}</li>
                <li>
                  <strong>?</strong>
                  ：{{ $t('i18n.f553f6fac6') }}"*"{{ $t('i18n.290c19383c') }}
                </li>
                <li>
                  <strong>*&#47;2</strong>
                  ：{{ $t('i18n.e7fc1b7414') }}
                </li>
                <li>
                  <strong>2-8</strong>
                  ：{{ $t('i18n.c4a665c9a4') }},3,4,5,6,7,8{{ $t('i18n.daf783c8cd') }}
                </li>
                <li><strong>2,3,5,8</strong> ：{{ $t('i18n.fb5616f2af') }}</li>
                <li><strong>cronA | cronB</strong> ：{{ $t('i18n.1c82793e77') }}</li>
              </ul>
              {{ $t('i18n.932b4b7f79') }}

              <pre>
    {{ $t('i18n.66aff87835') }} &gt; {{ $t('i18n.f0f0cf0143') }} &gt; {{ $t('i18n.d2fbc22d41') }},）
    </pre
              >
              <p>
                {{ $t('i18n.b2c5b372f0') }},3,6/3{{ $t('i18n.e924fb42bc') }}“/”{{ $t('i18n.a138d343fb') }},3,(6/3)，{{
                  $t('i18n.d814a262ef')
                }},3,6{{ $t('i18n.755ac8eaae') }}<br />
                <br />
              </p>

              <p>{{ $t('i18n.c2add44a1d') }}</p>

              <ul>
                <li><strong>5 * * * *</strong> ：{{ $t('i18n.b0407a7dd0') }}:05,01:05……</li>
                <li><strong>* * * * *</strong> ：{{ $t('i18n.5114ebf444') }}</li>
                <li><strong>*&#47;2 * * * *</strong> ：{{ $t('i18n.a22fa8941a') }}</li>
                <li><strong>* 12 * * *</strong> ：12{{ $t('i18n.c1fc70650e') }}</li>
                <li><strong>59 11 * * 1,2</strong> ：{{ $t('i18n.11103e19bd') }}:59{{ $t('i18n.1a6aa24e76') }}</li>
                <li>
                  <strong>3-18&#47;5 * * * *</strong>
                  ：3~18{{ $t('i18n.71a312821c') }}:03, 0:08, 0:13, 0:18, 1:03, 1:08……
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
            <a-form-item :label="$t('i18n.3c6fa6f667')" name="cron">
              <a-input v-model:value="temp.cron" :placeholder="$t('i18n.cfa72dd73a')" />
            </a-form-item>
            <a-form-item :label="$t('i18n.d87940854f')" name="count">
              <a-input-number
                v-model:value="temp.count"
                :min="1"
                :placeholder="$t('i18n.25c6bd712c')"
                style="width: 100%"
              />
            </a-form-item>
            <a-form-item :label="$t('i18n.481ffce5a9')">
              <a-switch
                v-model:checked="temp.isMatchSecond"
                :checked-children="$t('i18n.0a60ac8f02')"
                :un-checked-children="$t('i18n.c9744f45e7')"
              />
            </a-form-item>
            <a-form-item :label="$t('i18n.cd649f76d4')" name="date" :help="$t('i18n.07d2261f82')">
              <a-range-picker
                v-model:value="temp.date"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                :separator="$t('i18n.981cbe312b')"
                style="width: 100%"
              />
            </a-form-item>

            <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
              <a-button type="primary" html-type="submit"> {{ $t('i18n.4328677c29') }} </a-button>
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
            <div>{{ $t('i18n.5ad7f5a8b2') }}</div>
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
        emptyText: this.$t('i18n.21efd88b67')
      },
      resultList: [],
      // 表单校验规则
      rules: {
        cron: [
          {
            required: true,
            message: this.$t('i18n.cfa72dd73a'),
            trigger: 'blur'
          }
        ],

        count: [
          {
            required: true,
            message: this.$t('i18n.25c6bd712c'),
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
        emptyText: this.$t('i18n.21efd88b67')
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
