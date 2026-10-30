<template>
  <ArtSearchBar
    ref="searchBarRef"
    v-model="formData"
    :items="formItems"
    :rules="rules"
    @reset="handleReset"
    @search="handleSearch"
  >
  </ArtSearchBar>
</template>

<script setup lang="ts">
  interface Props {
    modelValue: Record<string, any>
  }

  interface Emits {
    (e: 'update:modelValue', value: Record<string, any>): void
    (e: 'search', params: Record<string, any>): void
    (e: 'reset'): void
  }

  const props = defineProps<Props>()
  const emit = defineEmits<Emits>()

  const searchBarRef = ref()
  const formData = computed({
    get: () => props.modelValue,
    set: (val) => emit('update:modelValue', val)
  })

  const rules = {}

  const categoryOptions = ref<{ label: string; value: string }[]>([
    { label: '全部', value: 'all' },
    { label: '工作汇报', value: 'work' },
    { label: '学习笔记', value: 'study' },
    { label: '会议记录', value: 'meeting' },
    { label: '项目管理', value: 'project' }
  ])

  const statusOptions = ref<{ label: string; value: number }[]>([
    { label: '草稿', value: 0 },
    { label: '已发布', value: 1 },
    { label: '已下架', value: 2 }
  ])

  const formItems = computed(() => [
    {
      label: '关键词',
      key: 'keyword',
      type: 'input',
      placeholder: '请输入模板标题或描述',
      clearable: true
    },
    {
      label: '分类',
      key: 'category',
      type: 'select',
      props: {
        placeholder: '请选择分类',
        options: categoryOptions.value
      }
    },
    {
      label: '状态',
      key: 'status',
      type: 'select',
      props: {
        placeholder: '请选择状态',
        options: statusOptions.value
      }
    }
  ])

  function handleReset() {
    emit('reset')
  }

  async function handleSearch() {
    await searchBarRef.value.validate()
    emit('search', formData.value)
  }
</script>
