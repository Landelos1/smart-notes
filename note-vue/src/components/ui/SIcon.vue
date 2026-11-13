<script setup lang="ts">
import { computed } from 'vue'

/**
 * SIcon —— 站内自绘图标集
 *
 * 设计约定：
 * 1. 所有路径写在本文件的 PATHS 表里，不依赖任何图标库、不发网络请求，纯本地渲染。
 * 2. 统一 24×24 viewBox、线性描边风格，颜色跟随 currentColor。
 * 3. 新增图标：在 PATHS 里加一条，key 用短横线命名，值是一组 <path> 的 d。
 *
 * 这样做的原因：图标库要么在线拉取（断网就是空白），要么整套打包（只用到十几个却带进上千个）。
 * 这里只维护本项目真正用到的图标，体积极小且离线可用。
 */
const props = withDefaults(
  defineProps<{
    /** 图标名，对应 PATHS 的 key */
    name: string
    /** 尺寸（px），宽高一致 */
    size?: number | string
    /** 描边粗细，默认 1.7 偏细，适合侧栏与正文混排 */
    thickness?: number | string
  }>(),
  {
    size: 16,
    thickness: 1.7
  }
)

/** 线稿点：配合 round linecap，渲染成一个实心小圆点 */
const DOT = (x: number, y: number) => `M${x} ${y}h.01`

const PATHS: Record<string, string[]> = {
  /* ---------- 内容 ---------- */
  note: [
    'M7 3.5h6.5L18 8v12.5H7z',
    'M13.5 3.5V8H18',
    'M10 12.5h5',
    'M10 16h3.5'
  ],
  files: ['M9 3.5h7L19 6.5V17H9z', 'M16 3.5v3h3', 'M6 7v13.5h9'],
  layers: [
    'M12 3.2l8.5 4.6-8.5 4.6-8.5-4.6z',
    'M3.5 12.4L12 17l8.5-4.6',
    'M3.5 16.6L12 21.2l8.5-4.6'
  ],
  book: [
    'M12 7C10 5.3 7 5 4 5.5v12.7c3-.5 6-.2 8 1.4 2-1.6 5-1.9 8-1.4V5.5c-3-.5-6-.2-8 1.5z',
    'M12 7v12.6'
  ],
  archive: ['M3.5 6.5h17v12.2h-17z', 'M3.5 10.6h17', 'M9.2 6.5V3.3h5.6v3.2'],

  /* ---------- 导航 ---------- */
  compass: [
    'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z',
    'M15.6 8.4l-2.1 5.1-5.1 2.1 2.1-5.1z'
  ],
  users: [
    'M9.5 11.5a3.25 3.25 0 1 0 0-6.5 3.25 3.25 0 0 0 0 6.5z',
    'M3.5 20c0-3.2 2.7-5.5 6-5.5s6 2.3 6 5.5',
    'M16.5 5.5a3 3 0 0 1 0 5.8',
    'M18 14.8c1.7.7 2.8 2.3 2.8 4.4'
  ],
  bookmark: ['M7 4h10v16l-5-3.6L7 20z'],
  heart: [
    'M12 20.2c-1.2-.9-7-4.8-7-9.3A3.85 3.85 0 0 1 12 8.3a3.85 3.85 0 0 1 7 2.6c0 4.5-5.8 8.4-7 9.3z'
  ],
  sparkle: [
    'M11.5 3.2l1.75 4.9 4.9 1.75-4.9 1.75-1.75 4.9-1.75-4.9-4.9-1.75 4.9-1.75z',
    'M18.2 15.4l.85 2.35 2.35.85-2.35.85-.85 2.35-.85-2.35-2.35-.85 2.35-.85z'
  ],
  share: [
    'M12 11.4a3.3 3.3 0 1 0 0-6.6 3.3 3.3 0 0 0 0 6.6z',
    'M6 20.8a3.3 3.3 0 1 0 0-6.6 3.3 3.3 0 0 0 0 6.6z',
    'M18 20.8a3.3 3.3 0 1 0 0-6.6 3.3 3.3 0 0 0 0 6.6z',
    'M10.4 11.6L7.6 14.3',
    'M13.6 11.6l2.8 2.7'
  ],
  layout: ['M4 4h16v16H4z', 'M4 11h16', 'M11 4v16'],
  trash: [
    'M4 7h16',
    'M9.5 7V4.2h5V7',
    'M6.2 7l1 13.2h9.6L17.8 7',
    'M10.2 11v5.6',
    'M13.8 11v5.6'
  ],

  /* ---------- 功能 ---------- */
  plus: ['M12 5v14', 'M5 12h14'],
  search: ['M11 18.2a7.2 7.2 0 1 0 0-14.4 7.2 7.2 0 0 0 0 14.4z', 'M16.4 16.4L21 21'],
  close: ['M6 6l12 12', 'M18 6L6 18'],
  'chevron-down': ['M6.5 9.5l5.5 5.5 5.5-5.5'],
  'chevron-right': ['M9.5 6.5l5.5 5.5-5.5 5.5'],
  pin: ['M8.5 3.5h7l-1.1 6 3.1 3.2H6.5l3.1-3.2z', 'M12 12.7V20.5'],
  download: ['M12 4v10.5', 'M8 11l4 4 4-4', 'M5 19.5h14'],
  settings: [
    'M12 19.2a7.2 7.2 0 1 0 0-14.4 7.2 7.2 0 0 0 0 14.4z',
    'M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z',
    'M12 2.6v2.2',
    'M12 19.2v2.2',
    'M2.6 12h2.2',
    'M19.2 12h2.2',
    'M6.9 6.9L5.4 5.4',
    'M17.1 6.9l1.5-1.5',
    'M6.9 17.1L5.4 18.6',
    'M17.1 17.1l1.5 1.5'
  ],
  crown: ['M3.5 8.5l4.2 4 4.3-6.5 4.3 6.5 4.2-4-2 11H5.5z'],
  logout: ['M9.5 20.5H5.2V3.5h4.3', 'M15.5 16.5l4.5-4.5-4.5-4.5', 'M20 12H9.5'],
  grip: [
    DOT(9.5, 6),
    DOT(9.5, 12),
    DOT(9.5, 18),
    DOT(14.5, 6),
    DOT(14.5, 12),
    DOT(14.5, 18)
  ],
  chat: ['M4.5 4.8h15v10.6H9.8l-3.6 3.1V15.4H4.5z']
  ,
  'check-square': [
    'M5 4.5h14a1.5 1.5 0 0 1 1.5 1.5v12a1.5 1.5 0 0 1-1.5 1.5H5A1.5 1.5 0 0 1 3.5 18V6A1.5 1.5 0 0 1 5 4.5z',
    'M7.5 12.2l2.8 2.8 6.2-6.2'
  ]
}

const d = computed<string[]>(() => PATHS[props.name] || [])
</script>

<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    :stroke-width="thickness"
    stroke-linecap="round"
    stroke-linejoin="round"
    class="sicon"
    aria-hidden="true"
    focusable="false"
  >
    <path v-for="(p, i) in d" :key="i" :d="p" />
  </svg>
</template>

<style scoped>
.sicon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
