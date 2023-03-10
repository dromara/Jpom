<template>
  <div>
    <a-row justify="center" type="flex">
      <a-col :span="18">
        <a-alert message="此工具用于检查 cron 表达式是否正确,以及计划运行时间" type="info" />
        <a-form-model :model="temp" ref="form" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
          <a-form-model-item label="cron表达式" prop="cron">
            <a-input v-model="temp.cron" placeholder="请输入要检查的 cron 表达式" />
          </a-form-model-item>
          <a-form-model-item label="计划次数" prop="count">
            <a-input-number v-model="temp.count" :min="1" placeholder="请输入获取的计划运行次数" style="width: 100%" />
          </a-form-model-item>
          <a-form-model-item label="匹配秒">
            <a-switch v-model="temp.isMatchSecond" checked-children="是" un-checked-children="否" />
          </a-form-model-item>
          <a-form-model-item label="时间范围" prop="date" help="默认是当前时间到今年结束">
            <a-range-picker format="YYYY-MM-DD" valueFormat="YYYY-MM-DD" separator="至" v-model="temp.date" style="width: 100%" />
          </a-form-model-item>
        </a-form-model>
        <a-form-model-item :wrapper-col="{ span: 14, offset: 4 }">
          <a-button type="primary" @click="onSubmit"> 检查 </a-button>
        </a-form-model-item>
      </a-col>
      <a-col :span="10">
        <a-list bordered :data-source="resultList" :locale="locale">
          <a-list-item slot="renderItem" slot-scope="item">
            {{ parseTime(item) }}
          </a-list-item>
          <div slot="header">结果</div>
        </a-list>
      </a-col>
    </a-row>
  </div>
</template>
<script>
import { cronTools } from "@/api/tools";
import { parseTime } from "@/utils/const";
export default {
  data() {
    return {
      temp: {
        count: 10,
      },
      locale: {
        emptyText: "暂无数据",
      },
      resultList: [],
      // 表单校验规则
      rules: {
        cron: [{ required: true, message: "请输入要检查的 cron 表达式", trigger: "blur" }],
        count: [{ required: true, message: "请输入获取的计划运行次数", trigger: "blur" }],
      },
    };
  },
  methods: {
    parseTime,
    onSubmit() {
      this.$refs["form"].validate((valid) => {
        if (!valid) {
          return false;
        }
        this.resultList = [];
        this.locale = {
          emptyText: "暂无数据",
        };
        const temp = { ...this.temp, date: this.temp.date && this.temp.date[0] + " ~ " + this.temp.date[1] };

        cronTools(temp).then((res) => {
          //   console.log(res);
          this.resultList = res.data || [];
          this.locale = {
            emptyText: res.msg,
          };
        });
      });
    },
  },
};
</script>
