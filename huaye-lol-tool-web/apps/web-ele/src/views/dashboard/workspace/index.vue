<script lang="ts" setup>
import {onMounted, ref} from 'vue';
import {
  ElDialog,
  ElMessage,
  ElPagination,
  ElTable,
  ElTableColumn,
} from 'element-plus';
import GameTable from './components/GameTable.vue';

// 表格数据类型定义
interface TableRow {
  horse: string;
  score: number;
  rank: string;
  summonerName: string;
  currKDA: {
    queueGame: string;
    win: boolean;
    imageUrl: string;
    kills: number;
    deaths: number;
    assists: number;
  }[];
}

interface DetailRow {
  queueGame: string;
  horse: string;
  imageUrl: string;
  win: boolean;
  kills: number;
  deaths: number;
  assists: number;
}

interface ApiResponse<T> {
  code: number;
  message?: string;
  data: T;
  total?: number;  // 添加可选的 total 字段
}

interface PaginationState {
  currentPage: number;
  pageSize: number;
  pageSizes: number[];
  total: number;
}

// 表格数据
const table1Data = ref<TableRow[]>([]);
const table2Data = ref<TableRow[]>([]);

// 弹窗相关状态
const dialogVisible = ref(false);
const currentSummoner = ref<TableRow | null>(null);
const summonerDetailData = ref<DetailRow[]>([]);
const loading = ref(false);

// 分页相关状态
const pagination = ref<PaginationState>({
  currentPage: 1,
  pageSize: 10,
  pageSizes: [10, 20, 50, 100],
  total: 0,
});

// 修改 useMockData 的初始值为 false
const useMockData = ref(false);

// 修复 Mock 数据生成函数的类型问题并在 requestApi 中使用它
const generateMockData = (count: number): TableRow[] => {
  const horses = ['战马', '独角兽', '飞马', '天马', '魔马', '幽灵马'] as const;
  const ranks = ['青铜', '白银', '黄金', '铂金', '钻石', '大师', '王者'] as const;
  const queueGames = ['单双排', '灵活组排', '大乱斗', '云顶之弈'] as const;

  return Array.from({length: count}, (_, index) => ({
    horse: horses[Math.floor(Math.random() * horses.length)] as string,
    score: Math.floor(Math.random() * 1000) + 1000,
    rank: ranks[Math.floor(Math.random() * ranks.length)] as string,
    summonerName: `召唤师_${index + 1}`,
    currKDA: Array.from({length: 5}, () => ({
      queueGame: queueGames[Math.floor(Math.random() * queueGames.length)] as string,
      win: Math.random() > 0.5,
      imageUrl: `https://api.dicebear.com/7.x/adventurer/svg?seed=${Math.random()}`,
      kills: Math.floor(Math.random() * 15),
      deaths: Math.floor(Math.random() * 10),
      assists: Math.floor(Math.random() * 20)
    }))
  }));
};

// 修改 requestApi 函数，区分处理不同的返回类型
const requestApi = async <T>(url: string): Promise<T> => {
  // 模拟网络延迟
  await new Promise(resolve => setTimeout(resolve, 500));

  if (!useMockData.value) {
    // 使用真实 API
    try {
      const response = await fetch(url);
      const result = await response.json();
      console.log('API Response:', result);

      // 如果请求的是召唤师详情，直接返回完整响应
      if (url.includes('summoner')) {
        return result as T;
      }

      // 其他请求处理
      if (result.code === 200 && result.data) {
        return result.data as T;
      }
      if (Array.isArray(result)) {
        return result as T;
      }
      return result as T;
    } catch (error) {
      console.error('API 请求失败:', error);
      throw error;
    }
  }

  // Mock 数据逻辑保持不变
  if (url.includes('overview')) {
    return generateMockData(5) as T;
  }

  if (url.includes('summoner')) {
    const queueGames = ['单双排', '灵活组排', '大乱斗', '云顶之弈'] as const;
    const horses = ['战马', '独角兽', '飞马', '天马', '魔马', '幽灵马'] as const;

    const mockData: DetailRow[] = Array.from({length: 10}, () => ({
      queueGame: queueGames[Math.floor(Math.random() * queueGames.length)] as string,
      horse: horses[Math.floor(Math.random() * horses.length)] as string,
      imageUrl: `https://api.dicebear.com/7.x/adventurer/svg?seed=${Math.random()}`,
      win: Math.random() > 0.5,
      kills: Math.floor(Math.random() * 15),
      deaths: Math.floor(Math.random() * 10),
      assists: Math.floor(Math.random() * 20)
    }));

    return {
      code: 200,
      message: 'success',
      data: mockData,
      total: 100
    } as T;
  }

  const error = new Error('未知的 API 请求');
  console.error('请求失败:', error.message);
  ElMessage.error(`请求失败: ${error.message}`);
  throw error;
};

// 修改 fetchTableData 函数，添加数据结构检查
const fetchTableData = async (type: 1 | 2): Promise<TableRow[]> => {
  try {
    const data = await requestApi<TableRow[]>(`http://127.0.0.1:9527/api/game/overview?type=${type}`);
    console.log(`表格${type}数据:`, data); // 添加日志查看处理后的数据
    if (!Array.isArray(data)) {
      console.error(`表格${type}数据格式错误:`, data);
      return [];
    }
    return data;
  } catch (error) {
    console.error(`获取表格${type}数据失败:`, error);
    ElMessage.error(`表格${type}数据加载失败`);
    return [];
  }
};

// 修改 fetchSummonerDetail 的返回类型和实现
const fetchSummonerDetail = async (summonerName: string): Promise<ApiResponse<DetailRow[]>> => {
  loading.value = true;
  try {
    const response = await requestApi<ApiResponse<DetailRow[]>>(
        `http://127.0.0.1:9527/api/summoner/game/history?name=${encodeURIComponent(summonerName)}&page=${pagination.value.currentPage}&size=${pagination.value.pageSize}`
    );
    return response;
  } catch (error) {
    console.error('获取召唤师详情失败:', error);
    throw error;
  } finally {
    loading.value = false;
  }
};

// 修改 loadSummonerDetail 函数中的 total 判断
const loadSummonerDetail = async () => {
  if (!currentSummoner.value) return;

  try {
    const result = await fetchSummonerDetail(currentSummoner.value.summonerName);
    // 添加对响应状态码的判断
    if (result.code === 200) {
      summonerDetailData.value = result.data;
      pagination.value.total = result.total || 50;  // 添加默认值
      ElMessage.success('数据加载成功');
    } else {
      ElMessage.error(result.message || '数据加载失败');
      summonerDetailData.value = [];
      pagination.value.total = 0;
    }
  } catch (error) {
    console.error('加载召唤师详情失败:', error);
    ElMessage.error('数据加载失败');
    summonerDetailData.value = [];
    pagination.value.total = 0;
  }
};

// 分页处理函数
const handlePageChange = async (page: number) => {
  pagination.value.currentPage = page;
  await loadSummonerDetail();
};

// 分页大小处理函数
const handleSizeChange = async (size: number) => {
  pagination.value.pageSize = size;
  pagination.value.currentPage = 1; // 重置为第一页
  await loadSummonerDetail();
};

// 点击召唤师处理函数
const handleSummonerClick = async (row: TableRow) => {
  currentSummoner.value = row;
  dialogVisible.value = true;
  pagination.value.currentPage = 1; // 重置为第一页
  await loadSummonerDetail();
};

// 加载排行榜数据
const loadTable1Data = async () => {
  try {
    const data = await fetchTableData(1);
    table1Data.value = data;
    ElMessage.success('我方数据加载成功');
  } catch (error) {
    ElMessage.error('我方数据加载失败');
  }
};

// 加载竞技场数据
const loadTable2Data = async () => {
  try {
    const data = await fetchTableData(2);
    table2Data.value = data;
    ElMessage.success('敌方数据加载成功');
  } catch (error) {
    ElMessage.error('敌方数据加载失败');
  }
};

// 复制文本到剪贴板
const copyToClipboard = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text);
    ElMessage.success('复制成功');
  } catch (err) {
    console.error('复制失败:', err);
    ElMessage.error('复制失败');
  }
};

// 页面加载时初始化数据
onMounted(async () => {
  try {
    // 并行获取两个表格数据
    await Promise.all([
      loadTable1Data(),
      loadTable2Data()
    ]);
    console.log('页面数据初始化完成');
  } catch (error) {
    console.error('初始化数据失败:', error);
    ElMessage.error('数据加载失败');
  }
});
</script>

<template>
  <div class="p-5 bg-background min-h-screen">
    <!-- 排行榜表格 -->
    <div class="mb-6">
      <GameTable
          title="我方"
          :table-data="table1Data"
          :on-refresh="loadTable1Data"
          :on-summoner-click="handleSummonerClick"
      />
    </div>

    <!-- 竞技场表格 -->
    <GameTable
        title="敌方"
        :table-data="table2Data"
        :on-refresh="loadTable2Data"
        :on-summoner-click="handleSummonerClick"
    />

    <!-- 召唤师详情弹窗 -->
    <ElDialog
        v-model="dialogVisible"
        :title="`战绩查询 - ${currentSummoner?.summonerName || ''}`"
        width="833px"
        destroy-on-close
    >
      <template #title>
        <div class="flex items-center gap-2">
<!--          <span>战绩查询</span>-->
          <span
              class="text-gray-600 cursor-pointer hover:text-blue-500 transition-colors"
              v-if="currentSummoner"
              @click="copyToClipboard(currentSummoner.summonerName)"
          >
            {{ currentSummoner.summonerName }}
          </span>
        </div>
      </template>
      <div class="w-full">
        <ElTable
            v-loading="loading"
            :data="summonerDetailData"
            border
            stripe
            :fit="true"
            class="w-full"
        >
          <ElTableColumn
              prop="queueGame"
              label="游戏模式"
              align="center"
              min-width="200"
          >
            <template #default="{ row }">
              <span class="text-gray-700 font-medium">{{ row.queueGame }}</span>
            </template>
          </ElTableColumn>
          <ElTableColumn label="英雄" align="center" min-width="200">
            <template #default="{ row }">
              <div class="flex justify-center items-center">
                <img
                    :src="row.imageUrl"
                    :alt="row.horse"
                    class="w-12 h-12 rounded-full"
                />
              </div>
            </template>
          </ElTableColumn>
          <ElTableColumn label="胜负" align="center" min-width="200">
            <template #default="{ row }">
                <span :class="[row.win ? 'text-green-500' : 'text-red-600', 'font-bold px-4 py-1 rounded-full bg-opacity-10', row.win ? 'bg-green-100' : 'bg-red-100']">
                  {{ row.win ? '胜' : '负' }}
                </span>
            </template>
          </ElTableColumn>
          <ElTableColumn label="KDA" align="center" min-width="200">
            <template #default="{ row }">
              <span class="font-mono text-gray-800">
                <span class="text-blue-600">{{ row.kills }}</span>
                <span class="text-gray-400">/</span>
                <span class="text-red-600">{{ row.deaths }}</span>
                <span class="text-gray-400">/</span>
                <span class="text-green-600">{{ row.assists }}</span>
              </span>
            </template>
          </ElTableColumn>
        </ElTable>

        <div class="mt-4 flex justify-end">
          <ElPagination
              v-model:current-page="pagination.currentPage"
              :page-sizes="pagination.pageSizes"
              :page-size="pagination.pageSize"
              :total="pagination.total"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleSizeChange"
              @current-change="handlePageChange"
          />
        </div>
      </div>
    </ElDialog>
  </div>
</template>

<style scoped>
/* 移除了重复的样式，因为已经移到了组件中 */
</style>