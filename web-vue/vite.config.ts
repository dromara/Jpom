///
/// Copyright (c) 2019 Of Him Code Technology Studio
/// Jpom is licensed under Mulan PSL v2.
/// You can use this software according to the terms and conditions of the Mulan PSL v2.
/// You may obtain a copy of Mulan PSL v2 at:
/// 			http://license.coscl.org.cn/MulanPSL2
/// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
/// See the Mulan PSL v2 for more details.
///

import path from 'node:path'
import { ConfigEnv, defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import { createHtmlPlugin } from 'vite-plugin-html'
import { visualizer } from 'rollup-plugin-visualizer'
//自动导入vue中hook reactive ref等
import AutoImport from 'unplugin-auto-import/vite'
//自动导入ui-组件 比如说ant-design-vue  element-plus等
import Components from 'unplugin-vue-components/vite'
//ant-design-vue
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers'
import postcss from 'postcss'

// https://vitejs.dev/config/
export default defineConfig(({ mode }: ConfigEnv) => {
  // 加载环境配置
  const env: Record<string, string> = loadEnv(mode, __dirname, 'JPOM')
  const { JPOM_PROXY_HOST: HOST, JPOM_BASE_URL, JPOM_PORT }: Record<string, string> = env
  console.log(env, `当前为${mode}环境`)

  return {
    base: JPOM_BASE_URL, // 公共基础路径，如当值为jpom时网站访问路径为：https://jpom.top/jpom
    envPrefix: 'JPOM_', // 可在项目中通过import.meta.env.JPOM_xxx获取环境变量

    resolve: {
      alias: {
        '@/': `${path.resolve(__dirname, 'src')}/`
      },

      // 忽略后缀名的配置选项
      extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue']
    },
    build: {
      sourcemap: mode !== 'production', // 非生产环境都生成sourcemap
      outDir: '../modules/server/src/main/resources/dist',
      rollupOptions: {
        output: {
          // 用于从入口点创建的块的打包输出格式[name]表示文件名,[hash]表示该文件内容hash值
          entryFileNames: 'assets/js/[name].[hash].js', // 用于命名代码拆分时创建的共享块的输出命名
          chunkFileNames: 'assets/js/[name].[hash].js', // 用于输出静态资源的命名，[ext]表示文件扩展名
          assetFileNames: 'assets/[ext]/[name].[hash].[ext]'
        }
      },
      //打包前清空文件，默认true
      emptyOutDir: true,
      modulePreload: { polyfill: true },
      polyfillModulePreload: true,
      manifest: false
    },
    server: {
      port: Number(JPOM_PORT),
      host: '0.0.0.0',
      proxy: {
        // http
        '/api': {
          target: `http://${HOST}`,
          changeOrigin: true,
          ws: true,
          rewrite: (path) => path.replace(/^\/api/, ''),
          timeout: 10 * 60 * 1000
        }
      }
    },
    plugins: [
      vue(),
      vueJsx(),
      AutoImport({
        //安装两行后你会发现在组件中不用再导入ref，reactive等
        imports: ['vue', 'vue-router', 'pinia'],
        dts: 'src/d.ts/auto-import.d.ts',
        eslintrc: {
          enabled: true,
          filepath: '.eslintrc-auto-import.json',
          globalsPropValue: true
        },

        //ant-design-vue
        resolvers: [AntDesignVueResolver()]
      }),
      AutoImport({
        dirs: ['src/d.ts/global'],
        dts: 'src/d.ts/auto-global-import.d.ts',
        eslintrc: {
          enabled: true,
          filepath: '.eslintrc-global-import.json',
          globalsPropValue: true
        }
      }),
      Components({
        dts: 'src/d.ts/components.d.ts',
        //ant-design-vue
        resolvers: [AntDesignVueResolver({ importStyle: false, resolveIcons: true })]
      }),
      createHtmlPlugin({
        minify: true,
        viteNext: true,
        inject: {
          data: {
            title: env.JPOM_APP_TITLE,
            base_url: env.JPOM_BASE_URL,
            build: new Date().getTime(),
            env: process.env.NODE_ENV,
            buildVersion: process.env.npm_package_version
          }
        }
      }),
      visualizer({
        emitFile: false,
        // file: 'states.html',
        open: true
      }),
      {
        name: 'vite-plugin-skip-empty-css',
        async transform(code, id) {
          if (/(\.css|\.scss|\.less)$/.test(id)) {
            if (code === '') return ''
            const { root } = await postcss([
              {
                postcssPlugin: 'check-empty-or-comments-only'
              }
            ]).process(code, { from: id })
            if (
              root.nodes.length === 0 ||
              root.nodes.every((node) => {
                return node.type === 'comment'
              })
            ) {
              return ''
            }
            return code
          }
          return code
        }
      }
    ]
  }
})
