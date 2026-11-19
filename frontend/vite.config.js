import fs from 'node:fs'
import { fileURLToPath } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'

const kebab = (name) =>
  name
    .replace(/([a-z0-9])([A-Z])/g, '$1-$2')
    .replace(/([A-Z])([A-Z][a-z])/g, '$1-$2')
    .toLowerCase()

// 没有独立 js 入口的子组件，必须映射到父组件目录
const SUB_COMPONENTS = {
  'carousel-item': 'carousel',
  'checkbox-group': 'checkbox',
  'checkbox-button': 'checkbox',
  'descriptions-item': 'descriptions',
  'form-item': 'form',
  option: 'select',
  'option-group': 'select',
  'radio-group': 'radio',
  'radio-button': 'radio',
  'tab-pane': 'tabs',
  'table-column': 'table',
  'timeline-item': 'timeline'
}

const componentEntry = (partial) =>
  fileURLToPath(
    new URL(`./node_modules/element-plus/es/components/${partial}/index.mjs`, import.meta.url)
  )

const themeChalkEntry = (partial) =>
  fileURLToPath(
    new URL(`./node_modules/element-plus/theme-chalk/el-${partial}.css`, import.meta.url)
  )

/**
 * 子组件（如 carousel-item）样式常与父组件分离（theme-chalk/el-carousel-item.css），
 * 只引父组件样式会导致子组件布局样式缺失（轮播项不绝对定位、竖向堆叠）。
 * 这里统一补引子组件自己的 theme-chalk 样式。
 */
function sideEffectsFor(partial, folder) {
  const effects = [
    'element-plus/es/components/base/style/css',
    `element-plus/es/components/${folder}/style/css`
  ]
  if (folder !== partial && fs.existsSync(themeChalkEntry(partial))) {
    effects.push(`element-plus/theme-chalk/el-${partial}.css`)
  }
  return effects
}

/**
 * Element Plus 按需引入解析器：直接指向具体组件目录，避免整包打进产物。
 */
function elementPlusResolver() {
  return (name) => {
    if (!/^El[A-Z]/.test(name)) return undefined
    const partial = kebab(name.slice(2))
    const folder = fs.existsSync(componentEntry(partial)) ? partial : SUB_COMPONENTS[partial]
    if (!folder) return undefined
    return {
      name,
      from: `element-plus/es/components/${folder}/index`,
      sideEffects: sideEffectsFor(partial, folder)
    }
  }
}

export default defineConfig({
  plugins: [
    vue(),
    Components({ resolvers: [elementPlusResolver()], dts: false })
  ],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) }
  },
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/ws': {
        target: 'http://localhost:8080',
        ws: true,
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 900,
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined
          if (id.includes('echarts') || id.includes('zrender')) return 'echarts'
          if (id.includes('element-plus') || id.includes('@element-plus')) return 'element-plus'
          return 'vendor'
        }
      }
    }
  }
})
