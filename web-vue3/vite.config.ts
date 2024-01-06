/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
import path from 'node:path'
import { ConfigEnv, defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import { createHtmlPlugin } from 'vite-plugin-html'

//自动导入vue中hook reactive ref等
import AutoImport from 'unplugin-auto-import/vite'
//自动导入ui-组件 比如说ant-design-vue  element-plus等
import Components from 'unplugin-vue-components/vite'
//ant-design-vue
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers'

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
      outDir: '../modules/server/src/main/resources/dist2'
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
        //ant-design-vue
        resolvers: [AntDesignVueResolver()]
      }),
      AutoImport({
        dirs: ['src/d.ts/global'],
        dts: 'src/d.ts/auto-global-import.d.ts'
      }),
      Components({
        dts: 'src/d.ts/components.d.ts',
        //ant-design-vue   importStyle = false 样式就没了
        resolvers: [AntDesignVueResolver({ importStyle: false, resolveIcons: true })]
      }),

      createHtmlPlugin({
        minify: true,
        inject: {
          data: {
            title: env.JPOM_APP_TITLE,
            base_url: env.JPOM_BASE_URL,
            build: new Date().getTime(),
            env: process.env.NODE_ENV,
            buildVersion: process.env.npm_package_version
          }
        }
      })
    ]
  }
})
