<template>
  <a-tabs default-active-key="1" @change="tabChange">
    <a-tab-pane key="1">
      <template #tab>
        <SettingOutlined />
        服务端系统配置
      </template>
      <a-alert v-if="temp.file" :message="`配置文件路径:${temp.file}`" style="margin-top: 10px; margin-bottom: 20px" banner />
      <a-form ref="editForm" :model="temp">
        <a-form-item class="config-editor">
          <code-editor v-model:code="temp.content" :options="{ mode: 'yaml', tabSize: 2 }"></code-editor>
        </a-form-item>
        <a-form-item :wrapper-col="{ span: 14, offset: 2 }">
          <a-space>
            <a-button type="primary" class="btn" @click="onSubmit(false)">保存</a-button>
            <a-button type="danger" class="btn" @click="onSubmit(true)">保存并重启</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-tab-pane>
    <a-tab-pane key="2" class="ip-config-panel">
      <template #tab>
        <LockOutlined />
        服务端IP白名单配置
      </template>
      <a-alert :message="`当前访问IP：${ipTemp.ip}`" type="success" />
      <a-alert
        message="请仔细确认后配置，ip配置后立即生效。配置时需要保证当前ip能访问！127.0.0.1 该IP不受访问限制.支持配置IP段 192.168.1.1/192.168.1.254,192.168.1.0/24"
        style="margin-top: 10px" banner />
      <a-alert message="如果配置错误需要重启服务端并添加命令行参数 --rest:ip_config 将恢复默认配置" style="margin-top: 10px" banner />
      <a-form style="margin-top: 10px" ref="editIpConfigForm" :model="temp" :label-col="{ span: 3 }"
        :wrapper-col="{ span: 20 }">
        <a-form-item name="content">
          <template #label>
            <a-space align="center">
              <a-tooltip>
                <template #title>禁止访问的 IP 地址 </template>
                <StopOutlined />
                <!-- <a-icon type="stop" theme="twoTone" /> -->
                IP黑名单
              </a-tooltip>
            </a-space>
          </template>
          <a-input v-model:value="ipTemp.prohibited" type="textarea" :rows="8" class="ip-list-config"
            placeholder="请输入IP黑名单,多个使用换行,支持配置IP段 192.168.1.1/192.168.1.254,192.168.1.0/24" />
        </a-form-item>
        <a-form-item name="content">
          <template #label>
            <a-space align="center">
              <a-tooltip>
                <template #title> 只允许访问的 IP 地址 </template>
                <CheckCircleOutlined />
                IP白名单
              </a-tooltip>
            </a-space>
          </template>
          <a-input v-model:value="ipTemp.allowed" type="textarea" :rows="8" class="ip-list-config"
            placeholder="请输入IP白名单,多个使用换行,0.0.0.0 是开放所有IP,支持配置IP段 192.168.1.1/192.168.1.254,192.168.1.0/24" />
        </a-form-item>

        <a-form-item :wrapper-col="{ offset: 10 }" class="ip-config-button">
          <a-button type="primary" class="btn" @click="onSubmitIp()">保存</a-button>
        </a-form-item>
      </a-form>
    </a-tab-pane>

    <!-- 全局代理 -->
    <a-tab-pane key="6">
      <template #tab>
        <ApiOutlined />
        全局代理
      </template>
      <a-alert :message="`全局代理配置后将对服务端的网络生效，代理实现方式：ProxySelector`" style="margin-top: 10px; margin-bottom: 20px" banner />
      <a-row justify="center" type="flex">
        <a-form layout="inline" ref="editProxyForm" :model="proxyConfigData">
          <a-row v-for="(item, index) in proxyConfigData.globalProxy" :key="index">
            <a-form-item label="通配符" name="pattern">
              <a-input style="width: 30vw" :maxLength="200" v-model:value="item.pattern"
                placeholder="地址通配符,* 表示所有地址都将使用代理">
              </a-input>
            </a-form-item>
            <a-form-item label="代理" name="httpProxy">
              <a-input style="width: 30vw" v-model:value="item.proxyAddress" placeholder="代理地址 (127.0.0.1:8888)">
                <template #addonBefore>
                  <a-select v-model:value="item.proxyType" style="width: 100px">
                    <a-select-option value="HTTP">HTTP</a-select-option>
                    <a-select-option value="SOCKS">SOCKS</a-select-option>
                    <a-select-option value="DIRECT">DIRECT</a-select-option>
                  </a-select>
                </template>
              </a-input>
            </a-form-item>
            <a-form-item>
              <a-button danger @click="() => {
                proxyConfigData.globalProxy && proxyConfigData.globalProxy.splice(index, 1);
              }
                " size="small" :disabled="proxyConfigData.globalProxy && proxyConfigData.globalProxy.length <= 1">
                删除
              </a-button>
            </a-form-item>
          </a-row>
          <a-row type="flex" justify="center">
            <a-form-item>
              <a-space>
                <a-button type="primary" @click="() => {
                  proxyConfigData = {
                    ...proxyConfigData,
                    globalProxy: [
                      ...proxyConfigData.globalProxy,
                      {
                        proxyType: 'HTTP',
                      },
                    ],
                  };
                }
                  ">添加</a-button>
                <a-button type="primary" @click="saveProxyConfigHannder">保存</a-button>
              </a-space>
            </a-form-item>
          </a-row>
        </a-form>
      </a-row>
    </a-tab-pane>
  </a-tabs>
</template>
<script setup lang="ts">
import { editConfig, editIpConfig, getConfigData, getIpConfigData, getProxyConfig, saveProxyConfig, systemInfo } from "@/api/system";
import codeEditor from "@/components/codeEditor";
import { ref, reactive } from "vue";
import { RESTART_UPGRADE_WAIT_TIME_COUNT } from "@/utils/const";
import { message } from "ant-design-vue";


const checkCount = ref(0);

const proxyConfigData = reactive({
  globalProxy: [
    {
      proxyType: "HTTP",
    },
  ],
});

const temp = reactive({
  content: "",
  file: '',
  restart: false
});
const loadConfitData = () => {
  getConfigData().then((res) => {
    if (res.code === 200) {
      temp.content = res.data.content;
      temp.file = res.data.file;
    }
  });
};

const ipTemp = reactive({
  allowed: "",
  prohibited: "",
  ip: '',
});
const loadIpConfigData = () => {
  getIpConfigData().then((res) => {
    if (res.code === 200) {
      if (res.data) {
        ipTemp.allowed = res.data.allowed;
        ipTemp.prohibited = res.data.prohibited;
        ipTemp.ip = res.data.ip;
      }
    }
  });
};



const onSubmit = (restart: boolean) => {
  $confirm({
    title: "系统提示",
    content: "真的要保存当前配置吗？如果配置有误,可能无法启动服务需要手动还原奥！！！",
    okText: "确认",
    cancelText: "取消",
    onOk: () => {
      temp.restart = restart;
      editConfig(temp).then((res) => {
        if (res.code === 200) {
          message.success(res.msg)
          if (temp.restart) {
            startCheckRestartStatus(res.msg);
          }
        }
      });
    },
  });
};

const startCheckRestartStatus = (msg: string) => {
  checkCount.value = 0;
  $notification.info({
    message: (msg || "重启中，请稍候...") + ",请耐心等待暂时不用刷新页面,重启成功后会自动刷新",
    duration: null,
  });
  setTimeout(() => {
    //
    const timer = setInterval(() => {
      systemInfo()
        .then((res) => {
          if (res.code === 200) {
            clearInterval(timer);
            $notification.destroy()
            $message.success("重启成功");

            setTimeout(() => {
              location.reload();
            }, 1000);
          } else {
            if (checkCount.value > RESTART_UPGRADE_WAIT_TIME_COUNT) {
              $notification.destroy()
              $message.error("重启失败：" + (res.msg || ""));

              clearInterval(timer);
            }
          }
        })
        .catch((error) => {
          console.error(error);
          if (checkCount.value > RESTART_UPGRADE_WAIT_TIME_COUNT) {
            $notification.destroy()

            $message.error("重启超时,请去服务器查看控制台日志排查问题");

            clearInterval(timer);
          }
        });
      checkCount.value = checkCount.value + 1;
    }, 2000);
  }, 6000);
};

const onSubmitIp = () => {
  $confirm({
    title: "系统提示",
    content: "真的要保存当前配置吗？IP 白名单请慎重配置奥( 白名单是指只允许访问的 IP ),配置后立马生效 如果配置错误将出现无法访问的情况,需要手动恢复奥！！！",
    okText: "确认",
    cancelText: "取消",
    onOk: () => {
      editIpConfig(ipTemp).then((res) => {
        if (res.code === 200) {
          message.success(res.msg)
        }
      });
    },
  });
};

const loadProxyConfig = () => {
  getProxyConfig().then((res) => {
    if (res.data && res.data.length) {
      proxyConfigData.globalProxy = res.data;
    }
  });
};

const saveProxyConfigHannder = () => {
  saveProxyConfig(proxyConfigData.globalProxy).then((res) => {
    if (res.code === 200) {
      $notification.success({
        message: res.msg,
      });
    }
  });
};

const tabChange = (activeKey: string) => {
  if (activeKey === "1") {
    loadConfitData();
  } else if (activeKey === "2") {
    loadIpConfigData();
  } else if (activeKey === "6") {
    loadProxyConfig();
  }
};

tabChange('1')

</script>
<style scoped>
textarea {
  resize: none;
}

.config-editor {
  height: calc(100vh - 300px);
  width: 100%;
  overflow-y: scroll;
  border: 1px solid #d9d9d9;
}
</style>
