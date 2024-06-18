<template>
  <div>
    <a-row justify="center" type="flex">
      <a-col :span="18">
        <a-space direction="vertical" style="width: 100%">
          <a-alert :message="$t('i18n_9880bd3ba1')" type="info" />
          <a-collapse>
            <a-collapse-panel key="1" :header="$t('i18n_ef734bf850')"
              >{{ $t('i18n_71c6871780') }}<br />{{ $t('i18n_e930e7890f') }}
              <ol>
                <li>
                  <strong>{{ $t('i18n_daf783c8cd') }}</strong
                  >{{ $t('i18n_d57796d6ac') }}
                </li>
                <li>
                  <strong>{{ $t('i18n_609b5f0a08') }}</strong
                  >{{ $t('i18n_867cc1aac4') }}
                </li>
                <li>
                  <strong>{{ $t('i18n_3edddd85ac') }}</strong
                  >{{ $t('i18n_9b7ada2613') }}<strong>"L"</strong>{{ $t('i18n_7b961e05d0') }}
                </li>
                <li>
                  <strong>{{ $t('i18n_e42b99d599') }}</strong
                  >{{ $t('i18n_ffd67549cf') }}
                </li>
                <li>
                  <strong>{{ $t('i18n_a657f46f5b') }}</strong
                  >{{ $t('i18n_312e044529') }}<strong>"L"</strong>{{ $t('i18n_207d9580c1') }}
                </li>
              </ol>
              <p>{{ $t('i18n_f6d96c1c8c') }}<br /></p>

              <pre>{{$t('i18n_0c4eef1b88')}}<strong >{{$t('i18n_0c1fec657f')}}</strong>{{$t('i18n_55da97b631')}}<strong >{{$t('i18n_465260fe80')}}</strong>{{$t('i18n_9443399e7d')}}</pre>
              <p>{{ $t('i18n_3ae4c953fe') }}<br />{{ $t('i18n_ba8d1dca4a') }}</p>

              <pre>{{ $t('i18n_674a284936') }}</pre>
              <p>{{ $t('i18n_01226f48fc') }}</p>

              <ul>
                <li><strong>*</strong>{{ $t('i18n_0ccaa1c8b2') }}</li>
                <li><strong>?</strong>{{ $t('i18n_6470685fcd') }}</li>
                <li><strong>*&#47;2</strong>{{ $t('i18n_d0be2fcd05') }}</li>
                <li><strong>2-8</strong>{{ $t('i18n_8c0283435b') }}</li>
                <li><strong>2,3,5,8</strong>{{ $t('i18n_61341628ab') }}</li>
                <li><strong>cronA | cronB</strong>{{ $t('i18n_ed6a8ee039') }}</li>
              </ul>
              {{ $t('i18n_932b4b7f79') }}
              <pre>{{ $t('i18n_8724641ba8') }}</pre>
              <p>
                {{ $t('i18n_3c99ea4ec2') }}<br />
                <br />
              </p>

              <p>{{ $t('i18n_c2add44a1d') }}</p>

              <ul>
                <li><strong>5 * * * *</strong>{{ $t('i18n_4a6f3aa451') }}</li>
                <li><strong>* * * * *</strong>{{ $t('i18n_1f0c93d776') }}</li>
                <li><strong>*&#47;2 * * * *</strong>{{ $t('i18n_e97a16a6d7') }}</li>
                <li><strong>* 12 * * *</strong>{{ $t('i18n_a3751dc408') }}</li>
                <li><strong>59 11 * * 1,2</strong>{{ $t('i18n_c0996d0a94') }}</li>
                <li><strong>3-18&#47;5 * * * *</strong>{{ $t('i18n_b3f9beb536') }}</li>
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
            <a-form-item :label="$t('i18n_3c6fa6f667')" name="cron">
              <a-input v-model:value="temp.cron" :placeholder="$t('i18n_cfa72dd73a')" />
            </a-form-item>
            <a-form-item :label="$t('i18n_d87940854f')" name="count">
              <a-input-number
                v-model:value="temp.count"
                :min="1"
                :placeholder="$t('i18n_25c6bd712c')"
                style="width: 100%"
              />
            </a-form-item>
            <a-form-item :label="$t('i18n_481ffce5a9')">
              <a-switch
                v-model:checked="temp.isMatchSecond"
                :checked-children="$t('i18n_0a60ac8f02')"
                :un-checked-children="$t('i18n_c9744f45e7')"
              />
            </a-form-item>
            <a-form-item :label="$t('i18n_cd649f76d4')" name="date" :help="$t('i18n_07d2261f82')">
              <a-range-picker
                v-model:value="temp.date"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                :separator="$t('i18n_981cbe312b')"
                style="width: 100%"
              />
            </a-form-item>

            <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
              <a-button type="primary" html-type="submit">{{ $t('i18n_e26dcacfb1') }}</a-button>
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
            <div>{{ $t('i18n_5ad7f5a8b2') }}</div>
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
        emptyText: this.$t('i18n_21efd88b67')
      },
      resultList: [],
      // 表单校验规则
      rules: {
        cron: [
          {
            required: true,
            message: this.$t('i18n_cfa72dd73a'),
            trigger: 'blur'
          }
        ],

        count: [
          {
            required: true,
            message: this.$t('i18n_25c6bd712c'),
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
        emptyText: this.$t('i18n_21efd88b67')
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
