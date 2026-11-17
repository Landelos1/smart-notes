<!-- 订单管理页面 -->
<template>
  <div class="order-page art-full-height">
    <ArtSearchBar v-model="searchParams" :items="searchFormItems" @search="handleSearch" @reset="handleReset" />

    <div class="order-stats">
      <div class="stat-card">
        <div class="stat-label">符合筛选的订单</div>
        <div class="stat-value">{{ summary.total }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">本页已支付</div>
        <div class="stat-value">{{ summary.paid }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">本页待支付</div>
        <div class="stat-value">{{ summary.pending }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">本页实付金额</div>
        <div class="stat-value">¥ {{ summary.totalAmount }}</div>
      </div>
    </div>

    <ElCard class="art-table-card" shadow="never">
      <ArtTableHeader v-model:columns="columnChecks" :loading="loading" @refresh="refreshData">
        <template #left>
          <ElSpace wrap>
            <ElButton @click="refreshData" v-ripple>刷新列表</ElButton>
          </ElSpace>
        </template>
      </ArtTableHeader>

      <ArtTable
        :loading="loading"
        :data="data"
        :columns="columns"
        :pagination="pagination"
        @pagination:size-change="handleSizeChange"
        @pagination:current-change="handleCurrentChange"
      >
        <template #amount="{ row }">
          <span class="amount">¥ {{ formatAmount(row.amount) }}</span>
        </template>

        <template #payMethod="{ row }">
          <ElTag :type="getPayMethodConfig(row.payMethod).type" effect="plain">
            {{ getPayMethodConfig(row.payMethod).text }}
          </ElTag>
        </template>

        <template #status="{ row }">
          <ElTag :type="getStatusConfig(row.status).type" effect="plain">
            {{ row.statusName || getStatusConfig(row.status).text }}
          </ElTag>
        </template>

        <template #payTime="{ row }">
          <span class="whitespace-nowrap">{{ formatDateTime(row.payTime).slice(0, 16) }}</span>
        </template>

        <template #createTime="{ row }">
          <span class="whitespace-nowrap">{{ formatDateTime(row.createTime).slice(0, 16) }}</span>
        </template>

        <template #operation="{ row }">
          <div class="flex">
            <ArtButtonTable type="view" @click="handleView(row)" />
            <ElButton v-if="row.status === 0" link type="warning" @click="handleCancel(row)">取消</ElButton>
            <ArtButtonTable type="delete" @click="handleDelete(row)" />
          </div>
        </template>
      </ArtTable>
    </ElCard>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref } from 'vue'
  import { ElMessage, ElMessageBox, ElTag } from 'element-plus'
  import ArtButtonTable from '@/components/core/forms/art-button-table/index.vue'
  import ArtSearchBar from '@/components/core/forms/art-search-bar/index.vue'
  import ArtTable from '@/components/core/tables/art-table/index.vue'
  import ArtTableHeader from '@/components/core/tables/art-table-header/index.vue'
  import { useTable } from '@/hooks/core/useTable'
  import {
    fetchOrderList,
    fetchOrderStatus,
    fetchCancelOrder,
    fetchDeleteOrder
  } from '@/api/payment'

  defineOptions({ name: 'OrderManagement' })

  type OrderRow = Api.Payment.OrderVO

  const searchFormItems = computed(() => [
    {
      label: '订单号',
      key: 'orderNo',
      type: 'input',
      props: { clearable: true, placeholder: '输入订单号' }
    },
    {
      label: '套餐名称',
      key: 'planName',
      type: 'input',
      props: { clearable: true, placeholder: '输入套餐名称' }
    },
    {
      label: '支付方式',
      key: 'payMethod',
      type: 'select',
      props: {
        clearable: true,
        placeholder: '选择支付方式',
        options: [
          { label: '微信', value: 1 },
          { label: '支付宝', value: 2 }
        ]
      }
    },
    {
      label: '状态',
      key: 'status',
      type: 'select',
      props: {
        clearable: true,
        placeholder: '选择订单状态',
        options: [
          { label: '待支付', value: 0 },
          { label: '已支付', value: 1 },
          { label: '已取消', value: 2 }
        ]
      }
    },
    {
      label: '创建时间',
      key: 'dateRange',
      type: 'daterange',
      props: {
        clearable: true,
        valueFormat: 'YYYY-MM-DD',
        startPlaceholder: '开始日期',
        endPlaceholder: '结束日期'
      }
    }
  ])

  const {
    columns,
    columnChecks,
    data,
    loading,
    pagination,
    searchParams,
    getData,
    resetSearchParams,
    handleSizeChange,
    handleCurrentChange,
    refreshData,
    refreshUpdate
  } = useTable({
    core: {
      apiFn: (params) => {
        const { dateRange, ...rest } = params as any
        const [startTime, endTime] = Array.isArray(dateRange) ? dateRange : []
        return fetchOrderList({
          ...(rest || {}),
          startTime,
          endTime
        })
      },
      apiParams: {
        current: 1,
        size: 20,
        orderNo: undefined,
        planName: undefined,
        status: undefined,
        payMethod: undefined,
        dateRange: undefined
      } as any,
      columnsFactory: () => [
        { type: 'globalIndex', width: 54, label: '序号' },
        { prop: 'orderNo', label: '订单号', minWidth: 165, showOverflowTooltip: true },
        { prop: 'planName', label: '套餐名称', minWidth: 140, showOverflowTooltip: true },
        { prop: 'amount', label: '金额', width: 100, useSlot: true },
        { prop: 'payMethod', label: '支付方式', width: 100, useSlot: true },
        { prop: 'status', label: '状态', width: 100, useSlot: true },
        { prop: 'payTime', label: '支付时间', minWidth: 150, useSlot: true },
        { prop: 'createTime', label: '创建时间', minWidth: 150, useSlot: true },
        { prop: 'operation', label: '操作', width: 150, useSlot: true }
      ]
    },
    hooks: {
      onError: (err) => {
        ElMessage.error(err.message)
      }
    }
  })

  const summary = computed(() => {
    const total = pagination.total || data.value.length
    const paid = data.value.filter((item) => item.status === 1).length
    const pending = data.value.filter((item) => item.status === 0).length
    const totalAmount = data.value
      .filter((item) => item.status === 1)
      .reduce((acc, item) => acc + Number(item.amount || 0), 0)
    return {
      total,
      paid,
      pending,
      totalAmount: totalAmount.toFixed(2)
    }
  })

  const STATUS_CONFIG = {
    0: { type: 'warning' as const, text: '待支付' },
    1: { type: 'success' as const, text: '已支付' },
    2: { type: 'info' as const, text: '已取消' }
  } as const

  const PAY_METHOD_CONFIG = {
    1: { type: 'success' as const, text: '微信' },
    2: { type: 'primary' as const, text: '支付宝' }
  } as const

  const getStatusConfig = (status?: number) =>
    STATUS_CONFIG[status as keyof typeof STATUS_CONFIG] || { type: 'info' as const, text: '未知' }

  const getPayMethodConfig = (method?: number) =>
    PAY_METHOD_CONFIG[method as keyof typeof PAY_METHOD_CONFIG] || { type: 'info' as const, text: '未知' }

  const formatAmount = (value?: number) => {
    if (value === null || value === undefined) return '-'
    const num = Number(value)
    if (Number.isNaN(num)) return String(value)
    return num.toFixed(2)
  }

  const formatDateTime = (value?: string) => {
    if (!value) return '-'
    const d = new Date(value)
    if (Number.isNaN(d.getTime())) return value
    const pad2 = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(
      d.getMinutes()
    )}:${pad2(d.getSeconds())}`
  }

  const handleSearch = () => {
    getData()
  }

  const handleReset = () => {
    resetSearchParams()
  }

  const handleView = async (row: OrderRow) => {
    const detail = await fetchOrderStatus(row.orderNo)
    await ElMessageBox.alert(
      `订单号：${detail.orderNo}\n套餐：${detail.planName || '-'}\n金额：¥ ${formatAmount(detail.amount)}\n状态：${
        detail.statusName || getStatusConfig(detail.status).text
      }\n创建时间：${formatDateTime(detail.createTime)}\n支付时间：${formatDateTime(detail.payTime)}`,
      '订单详情',
      { confirmButtonText: '关闭' }
    )
  }

  const handleCancel = async (row: OrderRow) => {
    try {
      await ElMessageBox.confirm('确定要取消该订单吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await fetchCancelOrder(row.orderNo)
      ElMessage.success('订单已取消')
      await refreshUpdate()
    } catch (err: any) {
      if (err !== 'cancel') {
        ElMessage.error(err.message || '取消订单失败')
      }
    }
  }

  const handleDelete = async (row: OrderRow) => {
    try {
      await ElMessageBox.confirm('确定要删除该订单吗？此操作不可恢复。', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await fetchDeleteOrder(row.orderNo)
      ElMessage.success('订单已删除')
      await refreshUpdate()
    } catch (err: any) {
      if (err !== 'cancel') {
        ElMessage.error(err.message || '删除订单失败')
      }
    }
  }
</script>

<style lang="scss">
  .order-page {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .order-stats {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 12px;

    @media (max-width: 1024px) {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    @media (max-width: 640px) {
      grid-template-columns: minmax(0, 1fr);
    }
  }

  .stat-card {
    background: var(--el-bg-color);
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 12px;
    padding: 14px 16px;
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .stat-label {
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }

  .stat-value {
    font-size: 20px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  .amount {
    font-weight: 600;
    color: var(--el-color-primary);
  }
</style>
