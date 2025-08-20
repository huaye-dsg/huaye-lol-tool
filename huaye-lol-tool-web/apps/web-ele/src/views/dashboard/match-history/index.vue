<script lang="ts" setup>
import { onMounted, ref } from 'vue';

import {
  ElButton,
  ElCard,
  ElInput,
  ElMessage,
  ElPagination,
  ElTable,
  ElTableColumn,
} from 'element-plus';

// 表格数据类型定义，匹配后端 GameBriefInfo
interface TableRow {
  queueGame: string;    // 游戏模式
  imageUrl: string;     // 英雄图片URL
  win: boolean;         // 胜负
  kills: number;        // 击杀
  deaths: number;       // 死亡
  assists: number;      // 助攻
}

// 默认的召唤师名称
const DEFAULT_SUMMONER_NAME = '中国第一深情#92429';

// 搜索输入框绑定的召唤师名称
const summonerNameInput = ref<string>('');
// 表格数据
const tableData = ref<TableRow[]>([]);

// 分页相关
const pagination = ref({
  currentPage: 1,
  pageSize: 10,
  total: 0,
});

// 加载状态
const loading = ref(false);

// 加载表格数据
const loadData = async (name: string, page = 1, pageSize = 10) => {
  if (!name) {
    summonerNameInput.value = DEFAULT_SUMMONER_NAME;
    name = DEFAULT_SUMMONER_NAME;
  }

  loading.value = true;
  try {
    const response = await fetch(
      `http://127.0.0.1:9527/api/summoner/game/history?name=${encodeURIComponent(name)}&pageNum=${page}&pageSize=${pageSize}`,
    );

    // 检查HTTP响应状态
    if (!response.ok) {
      throw new Error(`网络请求失败，状态码: ${response.status}`);
    }

    // 解析响应数据
    const result = await response.json();

    // 检查业务状态码
    if (result.code !== 200) {
      throw new Error(result.message || `请求失败，状态码: ${result.code}`);
    }

    // 设置表格数据
    tableData.value = result.data || [];
    pagination.value.total = result.data?.length || 0;
  } catch (error: any) {
    ElMessage.error(`请求失败: ${error.message}`);
    console.error('获取召唤师游戏历史失败:', error);
    tableData.value = [];
    pagination.value.total = 0;
  } finally {
    loading.value = false;
  }
};

// 搜索按钮点击事件
const handleSearch = () => {
  pagination.value.currentPage = 1; // 搜索时重置到第一页
  loadData(
    summonerNameInput.value,
    pagination.value.currentPage,
    pagination.value.pageSize,
  );
};

// 页面改变
const handleCurrentChange = (currentPage: number) => {
  pagination.value.currentPage = currentPage;
  loadData(summonerNameInput.value, currentPage, pagination.value.pageSize);
};

// 页面大小改变
const handleSizeChange = (pageSize: number) => {
  pagination.value.pageSize = pageSize;
  pagination.value.currentPage = 1; // 页面大小改变时重置到第一页
  loadData(summonerNameInput.value, 1, pageSize);
};

// 格式化KDA
const formatKDA = (row: TableRow) => {
  return `${row.kills}/${row.deaths}/${row.assists}`;
};

// 页面加载时初始化数据
onMounted(() => {
  summonerNameInput.value = DEFAULT_SUMMONER_NAME;
  loadData(summonerNameInput.value, pagination.value.currentPage, pagination.value.pageSize);
});
</script>

<template>
  <div class="bg-background min-h-screen p-4">
    <ElCard title="召唤师游戏历史">
      <div class="search-section mb-4 flex items-center gap-4">
        <ElInput
          v-model="summonerNameInput"
          placeholder="请输入召唤师名称"
          style="width: 250px"
          clearable
          @keyup.enter="handleSearch"
        />
        <ElButton type="primary" @click="handleSearch">搜索</ElButton>
      </div>

      <ElTable
        v-loading="loading"
        :data="tableData"
        style="width: 100%"
        border
        stripe
      >
        <ElTableColumn
          prop="queueGame"
          label="游戏模式"
          align="center"
          width="200"
        />
        <ElTableColumn label="英雄" align="center" width="120">
          <template #default="{ row }">
            <img
              :src="row.imageUrl"
              alt="英雄"
              style="width: 50px; height: 50px; border-radius: 50%"
            />
          </template>
        </ElTableColumn>
        <ElTableColumn label="胜负" align="center" width="120">
          <template #default="{ row }">
            <span :style="{ color: row.win ? '#67C23A' : '#F56C6C' }">
              {{ row.win ? '胜利' : '失败' }}
            </span>
          </template>
        </ElTableColumn>
        <ElTableColumn label="KDA" align="center" width="200">
          <template #default="{ row }">
            {{ formatKDA(row) }}
          </template>
        </ElTableColumn>
      </ElTable>

      <div class="mt-4 flex justify-end">
        <ElPagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 30, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </ElCard>
  </div>
</template>

<style scoped>
.el-table {
  @apply text-sm;
}

.el-pagination {
  @apply mt-4;
}

.search-section {
  @apply pb-4 mb-4 border-b border-border;
}
</style>