/** @type {import('tailwindcss').Config} */

// ============================================================
// 暖调奶油（Warm Cream）主题令牌
// 设计原则：中性色偏暖（米→褐），强调色用陶土橘，辅色用橄榄绿 / 姜黄 / 砖红。
// 关键决策：沿用原有的色阶名称（gray / blue / red ...），只替换色值。
//   —— 这样全站 1500+ 处已写好的工具类无需改动即可换肤，
//      同时 tailwind-merge 的默认配置依然能正确去重（换成新名字会失效）。
// ============================================================

// 注意：本项目 package.json 是 "type": "module"，配置文件走 ESM。
// 这里**必须**用 import，写成 plugins: [require('@tailwindcss/typography')]
// 会在加载配置时直接抛 "require is not defined"，Tailwind 整个不工作。
import typography from '@tailwindcss/typography'

/** 暖中性色：替代 gray / slate / zinc / neutral / stone */
const cream = {
  50: '#FBF8F2',
  100: '#F6F0E5',
  200: '#EDE4D4',
  300: '#DFD2BD',
  400: '#9C8770',
  500: '#8B745C',
  600: '#6D5742',
  700: '#513F30',
  800: '#362A20',
  900: '#241B14',
  950: '#17110C',
}

/** 陶土橘：替代 blue / indigo / sky，作为全站主强调色 */
const clay = {
  50: '#FDF4EA',
  100: '#FAE7D3',
  200: '#F4CEA9',
  300: '#EBAE79',
  400: '#E08E4D',
  500: '#D2742E',
  600: '#B25F22',
  700: '#8E4A1B',
  800: '#6B3814',
  900: '#48250E',
  950: '#2E1708',
}

/** 橄榄绿：替代 green / emerald / teal / lime，用于成功态 */
const olive = {
  50: '#F4F6EC',
  100: '#E6EBD5',
  200: '#CDD8B0',
  300: '#AEBD86',
  400: '#8D9F62',
  500: '#74874D',
  600: '#5C6E3B',
  700: '#48562F',
  800: '#364124',
  900: '#242C18',
  950: '#161C0F',
}

/** 姜黄：替代 amber / orange / yellow，用于警告态 */
const ginger = {
  50: '#FDF6E8',
  100: '#F9E9C6',
  200: '#F2D394',
  300: '#E8B95F',
  400: '#DFA648',
  500: '#C98A2E',
  600: '#A66F21',
  700: '#82551A',
  800: '#5F3E14',
  900: '#42290E',
  950: '#2A1A08',
}

/** 砖红：替代 red / rose，用于危险态 */
const brick = {
  50: '#FCF1EF',
  100: '#F7DED9',
  200: '#EFBDB3',
  300: '#E2948A',
  400: '#D06E62',
  500: '#C0564A',
  600: '#A34238',
  700: '#82332C',
  800: '#5F2620',
  900: '#401A16',
  950: '#2A100D',
}

/** 藕荷：替代 purple / violet / fuchsia / pink，用于标签与次级强调 */
const mauve = {
  50: '#F8F1F4',
  100: '#F0E1E7',
  200: '#E0C4D0',
  300: '#CBA3B5',
  400: '#B1829A',
  500: '#996883',
  600: '#7D5369',
  700: '#634253',
  800: '#48303D',
  900: '#2F2028',
  950: '#1E1419',
}

export default {
  content: [
    './index.html',
    './src/**/*.{vue,js,ts,jsx,tsx}',
  ],
  theme: {
    extend: {
      // prose 类（@tailwindcss/typography）的暖色主题。
      // 插件自带的是冷灰系：正文 #374151、链接蓝色、引用条青灰、表格冷灰边。
      // 直接用在奶油底上会像贴错了一张文件。这里把它的 --tw-prose-* 变量
      // 全部换回站内 token，prose 与 .markdown-preview 保持在同一个色系里。
      // 只配 DEFAULT：prose-sm 只改字号行高，颜色变量从 .prose 继承下来。
      typography: {
        DEFAULT: {
          css: {
            '--tw-prose-body': cream[700],
            '--tw-prose-headings': cream[900],
            '--tw-prose-lead': cream[600],
            '--tw-prose-links': clay[600],
            '--tw-prose-bold': cream[900],
            '--tw-prose-counters': cream[500],
            '--tw-prose-bullets': cream[300],
            '--tw-prose-hr': cream[200],
            '--tw-prose-quotes': cream[800],
            '--tw-prose-quote-borders': clay[200],
            '--tw-prose-captions': cream[500],
            '--tw-prose-kbd': cream[900],
            '--tw-prose-code': clay[700],
            '--tw-prose-pre-code': cream[100],
            '--tw-prose-pre-bg': cream[800],
            '--tw-prose-th-borders': cream[300],
            '--tw-prose-td-borders': cream[200],
          },
        },
      },
      colors: {
        gray: cream,
        slate: cream,
        zinc: cream,
        neutral: cream,
        stone: cream,

        blue: clay,
        indigo: clay,
        sky: clay,

        green: olive,
        emerald: olive,
        teal: olive,
        lime: olive,

        amber: ginger,
        orange: ginger,
        yellow: ginger,

        red: brick,
        rose: brick,

        purple: mauve,
        violet: mauve,
        fuchsia: mauve,
        pink: mauve,

        // 语义别名，供新写的组件直接引用
        cream: cream,
        clay: clay,
        olive: olive,
        ginger: ginger,
        brick: brick,
        mauve: mauve,
      },
      fontFamily: {
        sans: [
          'Inter',
          '-apple-system',
          'BlinkMacSystemFont',
          'Segoe UI',
          'PingFang SC',
          'Hiragino Sans GB',
          'Microsoft YaHei',
          'sans-serif',
        ],
        serif: [
          'Noto Serif SC',
          'Source Han Serif SC',
          'Songti SC',
          'STSong',
          'Georgia',
          'serif',
        ],
        mono: ['JetBrains Mono', 'Consolas', 'Monaco', 'Courier New', 'monospace'],
      },
      borderRadius: {
        sm: '8px',
        DEFAULT: '10px',
        md: '12px',
        lg: '16px',
        xl: '20px',
        '2xl': '24px',
        '3xl': '30px',
      },
      boxShadow: {
        sm: '0 1px 2px 0 rgba(88, 64, 40, 0.04)',
        DEFAULT: '0 2px 6px -1px rgba(88, 64, 40, 0.06), 0 1px 3px rgba(88, 64, 40, 0.04)',
        md: '0 4px 14px -2px rgba(88, 64, 40, 0.08)',
        lg: '0 10px 30px -6px rgba(88, 64, 40, 0.10)',
        xl: '0 18px 44px -10px rgba(88, 64, 40, 0.13)',
        '2xl': '0 26px 60px -14px rgba(88, 64, 40, 0.16)',
        inner: 'inset 0 1px 2px rgba(88, 64, 40, 0.06)',
        none: 'none',
      },
      letterSpacing: {
        tightest: '-0.035em',
      },
      transitionTimingFunction: {
        cream: 'cubic-bezier(0.22, 0.61, 0.36, 1)',
      },
    },
  },
  plugins: [typography],
}
