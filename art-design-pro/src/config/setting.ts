/**
 * 系统设置默认值配置
 *
 * 统一管理系统设置的所有默认值
 *
 * ## 主要功能
 *
 * - 菜单相关默认配置
 * - 主题相关默认配置
 * - 界面显示默认配置
 * - 功能开关默认配置
 * - 样式相关默认配置
 *
 * ## 注意事项
 *
 * 1. 修改此文件的配置项时，需要同步更新以下文件：
 *    - src/components/core/layouts/art-settings-panel/widget/SettingActions.vue（复制配置和重置配置逻辑）
 *    - src/store/modules/setting.ts（Store 状态定义）
 * 2. 可以通过设置面板的"复制配置"按钮快速生成配置代码
 * 3. 枚举类型的值需要与 src/enums/appEnum.ts 中的定义保持一致
 */

import AppConfig from '@/config'
import { SystemThemeEnum, MenuThemeEnum, MenuTypeEnum, ContainerWidthEnum } from '@/enums/appEnum'

/**
 * 系统设置默认值配置
 */
export const SETTING_DEFAULT_CONFIG = {
  /** 菜单类型 */
  // 从 LEFT 改为 TOP：左侧整条 210px 侧栏去掉，菜单并入顶栏，内容区吃满整个宽度。
  // 本系统只有 7 个一级菜单、没有二级，横排在 1366 以上屏幕都放得下，不需要侧栏。
  menuType: MenuTypeEnum.TOP,
  /** 菜单展开宽度 */
  // 原来是 230，内容区只剩约 1300px，列表页的操作列差点放不下（上一批列宽就是为此重排的）。
  // 收窄到 210 换回 20px，跟用户端窄侧栏的比例也更接近。可选区间 180~320。
  menuOpenWidth: 210,
  /** 菜单是否展开 */
  menuOpen: true,
  /** 双菜单是否显示文本 */
  dualMenuShowText: false,
  /** 系统主题类型 */
  // 默认锁亮色，与用户端（note-vue）的奶油亮色统一。
  // 原来是 AUTO，会跟随 Windows 深色模式整片变近黑，两个前端放一起像两个项目。
  systemThemeType: SystemThemeEnum.LIGHT,
  /** 系统主题模式 */
  systemThemeMode: SystemThemeEnum.LIGHT,
  /** 菜单风格 */
  menuThemeType: MenuThemeEnum.DESIGN,
  /** 系统主题颜色 */
  systemThemeColor: AppConfig.systemMainColor[0],
  /** 是否显示菜单按钮 */
  showMenuButton: true,
  /** 是否显示快速入口 */
  // 答辩场景用不到，顶栏右侧每多一个图标就越像模板 Demo，关掉。
  showFastEnter: false,
  /** 是否显示刷新按钮 */
  showRefreshButton: true,
  /** 是否显示面包屑 */
  showCrumbs: true,
  /** 是否显示工作台标签 */
  // 工作标签页固定占 40px 高度，本系统的页面切换靠左侧菜单已经足够，
  // 关掉后内容区整体上移，与用户端「单页大留白」的观感更统一。
  showWorkTab: false,
  /** 是否显示语言切换 */
  // 纯中文系统，顶栏留一个 EN 按钮只会让人误点。
  showLanguage: false,
  /** 是否显示进度条 */
  showNprogress: false,
  /** 是否显示设置引导 */
  // 那个「点击这里查看主题风格、开启顶栏菜单等更多配置」的气泡正好压在内容区上，
  // 答辩投屏时像贴了张广告，关掉。
  showSettingGuide: false,
  /** 是否显示节日文本 */
  showFestivalText: false,
  /** 是否显示水印 */
  watermarkVisible: false,
  /** 是否自动关闭 */
  autoClose: false,
  /** 是否唯一展开 */
  uniqueOpened: true,
  /** 是否色弱模式 */
  colorWeak: false,
  /** 是否刷新 */
  refresh: false,
  /** 是否加载节日烟花 */
  holidayFireworksLoaded: false,
  /** 边框模式 */
  boxBorderMode: true,
  /** 页面过渡效果 */
  pageTransition: 'slide-left',
  /** 标签页样式 */
  tabStyle: 'tab-default',
  /** 自定义圆角 */
  // 面板档位只到 '1'（1rem = 16px），已是可选最大值，比用户端的 14px 还略大，无需再调。
  customRadius: '1',
  /** 容器宽度 */
  containerWidth: ContainerWidthEnum.FULL,
  /** 节日日期 */
  festivalDate: ''
}

/**
 * 获取设置默认值
 * @returns 设置默认值对象
 */
export function getSettingDefaults() {
  return { ...SETTING_DEFAULT_CONFIG }
}

/**
 * 重置为默认设置
 * @param currentSettings 当前设置对象
 */
export function resetToDefaults(currentSettings: Record<string, any>) {
  const defaults = getSettingDefaults()
  Object.keys(defaults).forEach((key) => {
    if (key in currentSettings) {
      currentSettings[key] = defaults[key as keyof typeof defaults]
    }
  })
}
