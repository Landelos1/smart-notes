# 知识库管理系统界面 (Vue 版本)

这是一个基于 Vue 3 + TypeScript + Vite + TailwindCSS 的知识库管理系统前端界面。

## 功能特性

- 📝 **笔记管理** - 创建、编辑、删除笔记，支持 Markdown 格式
- 📁 **笔记本分类** - 按笔记本组织和筛选笔记
- 🤖 **AI 对话** - 智能 AI 助手对话功能
- 📚 **共享知识库** - 浏览和加入共享知识库
- 📋 **模板中心** - 使用预设模板快速创建笔记
- ⚙️ **个人设置** - 管理个人信息和 AI 助手设置

## 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **TypeScript** - 类型安全的 JavaScript 超集
- **Vite** - 下一代前端构建工具
- **TailwindCSS** - 实用优先的 CSS 框架
- **Lucide Vue** - 美观的图标库

## 项目结构

```
note-vue/
├── src/
│   ├── components/
│   │   ├── ui/              # UI 基础组件
│   │   │   ├── Button.vue
│   │   │   ├── Input.vue
│   │   │   ├── Textarea.vue
│   │   │   ├── Avatar.vue
│   │   │   ├── Badge.vue
│   │   │   └── utils.ts
│   │   ├── NotebookSidebar.vue
│   │   ├── NoteList.vue
│   │   ├── NoteEditor.vue
│   │   ├── SimpleMarkdownEditor.vue
│   │   ├── AIChatPage.vue
│   │   ├── ChatHistory.vue
│   │   ├── ChatDialog.vue
│   │   ├── SettingsPage.vue
│   │   ├── TemplateCenterPage.vue
│   │   └── SharedKnowledgeBasePage.vue
│   ├── types/
│   │   ├── note.ts
│   │   └── chat.ts
│   ├── utils/
│   │   └── timeFormat.ts
│   ├── App.vue
│   ├── main.ts
│   └── index.css
├── index.html
├── package.json
├── vite.config.ts
├── tsconfig.json
├── tailwind.config.js
└── postcss.config.js
```

## 快速开始

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

### 构建生产版本

```bash
npm run build
```

### 预览生产版本

```bash
npm run preview
```

## 从 React 版本迁移

此项目是从 React 版本重构而来，主要变化：

1. **组件语法** - 使用 Vue 3 Composition API (`<script setup>`)
2. **状态管理** - 使用 Vue 的 `ref` 和 `computed` 替代 React 的 `useState` 和 `useMemo`
3. **事件处理** - 使用 Vue 的 `emit` 替代 React 的回调函数
4. **样式** - 保持使用 TailwindCSS，样式基本一致
5. **图标** - 使用 `lucide-vue-next` 替代 `lucide-react`

## 许可证

MIT
