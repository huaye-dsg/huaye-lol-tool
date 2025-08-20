<script lang="ts" setup>
import {ElButton, ElCard, ElTable, ElTableColumn} from 'element-plus';
import {computed} from 'vue';

interface KdaDetail {
  queueGame: string;
  win: boolean;
  imageUrl: string;
  kills: number;
  deaths: number;
  assists: number;
}

interface TableRow {
  horse: string;
  score: number;
  rank: string;
  summonerName: string;
  currKDA: KdaDetail[];
}

interface Props {
  title: string;
  tableData: TableRow[];
  onRefresh: () => void;
  onSummonerClick: (row: TableRow) => void;
}

const props = defineProps<Props>();

// 计算 KDA 列的最大长度
const kdaColumnsCount = computed(() => {
  if (!props.tableData.length) return 0;
  return Math.max(...props.tableData.map(row => row.currKDA.length));
});

// 格式化游戏模式名称，限制为6个字符
const formatQueueGame = (queueGame: string) => {
  return queueGame.length > 4 ? queueGame.slice(0, 4) : queueGame.padEnd(4, ' ');
};
</script>

<template>
  <ElCard class="rounded-xl shadow-lg">
    <template #header>
      <div class="flex justify-between items-center">
        <span class="text-xl font-bold text-foreground">{{ title }}</span>
        <ElButton
            type="primary"
            @click="onRefresh"
            class="bg-primary hover:bg-primary/90"
        >
          刷新数据
        </ElButton>
      </div>
    </template>

    <ElTable
        :data="tableData"
        style="width: 100%"
        class="rounded-lg"
        stripe
        border
    >
      <ElTableColumn prop="horse" label="马匹" width="100" align="center"/>
      <ElTableColumn prop="score" label="分数" width="120" align="center"/>
      <ElTableColumn prop="rank" label="段位" width="120" align="center"/>
      <ElTableColumn prop="summonerName" label="召唤师名称" width="180" align="center">
        <template #default="{ row }">
          <span
              class="text-primary cursor-pointer hover:underline"
              @click="onSummonerClick(row)"
          >
            {{ row.summonerName }}
          </span>
        </template>
      </ElTableColumn>
      <template v-for="index in kdaColumnsCount" :key="index">
        <ElTableColumn :label="`对局 ${index}`">
          <template #default="{ row }">
            <div v-if="row.currKDA[index - 1]" class="flex items-center gap-6 whitespace-nowrap">
              <span
                  class="inline-block font-mono min-w-[4.5rem] text-center text-blue-800 bg-blue-50 rounded px-2 py-0.5">{{ formatQueueGame(row.currKDA[index - 1].queueGame) }}</span>
              <div class="flex items-center gap-3">
                <span :class="[
                  'px-2 py-0.5 rounded font-medium text-base',
                  row.currKDA[index - 1].win ? 'text-green-700 bg-green-100' : 'text-red-700 bg-red-100'
                ]">
                  {{ row.currKDA[index - 1].win ? '胜' : '负' }}
                </span>
                <img
                    v-if="row.currKDA[index - 1].imageUrl"
                    :src="row.currKDA[index - 1].imageUrl"
                    :alt="`KDA ${index} avatar`"
                    class="w-10 h-10"
                />
                <span class="font-mono flex items-center space-x-1 bg-gray-50 px-3 py-1 rounded-lg shadow-sm">
                  <span class="text-blue-600 font-semibold">{{ row.currKDA[index - 1].kills }}</span>
                  <span class="text-gray-400">/</span>
                  <span class="text-red-600 font-semibold">{{ row.currKDA[index - 1].deaths }}</span>
                  <span class="text-gray-400">/</span>
                  <span class="text-green-600 font-semibold">{{ row.currKDA[index - 1].assists }}</span>
               </span>
              </div>
            </div>
            <span v-else>-</span>
          </template>
        </ElTableColumn>
      </template>
    </ElTable>
  </ElCard>
</template>

<style scoped>
:deep(.el-card__header) {
  @apply p-5 bg-card border-b border-border;
}

:deep(.el-table th) {
  @apply bg-muted font-bold text-muted-foreground;
}

:deep(.el-table .el-table__row:hover) {
  @apply bg-muted/50;
}

:deep(.el-button) {
  @apply rounded-lg px-4 py-2.5 font-medium;
}

.font-mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}
</style>