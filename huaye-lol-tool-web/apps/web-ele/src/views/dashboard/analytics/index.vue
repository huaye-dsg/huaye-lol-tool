<script lang="ts" setup>
import {onMounted, reactive, ref, watch} from 'vue';
import heroData from '../../../constants/heroes.json';
import {
  ElButton,
  ElCard,
  ElMessage,
  ElOption,
  ElSelect,
  ElSwitch,
} from 'element-plus';

// === 类型定义 ===
interface UserInfo {
  name: string;
  rank: string;
  uuid: string;
  privacy: string;
  summonerId: string;
}

interface ConfigState {
  autoAcceptGame: boolean;
  autoPickChamp: boolean;
  autoPickChampID: number;
  autoBanChampID: number;
  autoBanChamp: boolean;
}

interface ChampOption {
  label: string;
  value: number;
}

// === API 请求封装 ===
const API_BASE_URL = 'http://localhost:9527/api';

const api = {
  async request<T = any>(url: string, method = 'GET', data?: any): Promise<T> {
    try {
      const body = data ? JSON.stringify(data) : undefined;
      const response = await fetch(`${API_BASE_URL}${url}`, {
        method,
        headers: {
          'Content-Type': 'application/json',
        },
        body,
      });

      const text = await response.text();
      if (!text) {
        ElMessage.warning('服务器返回空响应');
        return {} as T;
      }

      let result;
      try {
        result = JSON.parse(text);
      } catch (parseError) {
        console.warn('响应不是有效的JSON格式:', text);
        ElMessage.warning('服务器响应格式错误');
        return {} as T;
      }

      if (!response.ok) {
        const error = new Error(`HTTP错误! 状态码: ${response.status}`);
        error.name = 'HttpError';
        throw error;
      }

      return result;
    } catch (error) {
      console.error(`请求失败: ${url}`, error);
      ElMessage.error('请求失败，请检查网络或服务状态');
      // 创建新的错误对象，保留原始错误的信息
      const apiError = new Error(error instanceof Error ? error.message : String(error));
      apiError.name = 'ApiError';
      throw apiError;
    }
  },

  async getConfig() {
    return this.request<{ code: number; message: string; data: Partial<ConfigState> }>('/global/config');
  },

  async setBanChampion(config: { autoBanChamp: boolean; championId: number }) {
    return this.request('/set/ban/champion', 'POST', config);
  },

  async setAutoAcceptGame(config: { autoAcceptGame: boolean }) {
    return this.request('/set/auto/accept/game', 'POST', config);
  },

  async reconnect() {
    return this.request('/reconnect', 'POST');
  },

  async getUserInfo() {
    return this.request<{ code: number; message: string; data: any }>('/custom/info');
  },
};

// === 状态管理 ===
const configState = reactive<ConfigState>({
  autoAcceptGame: true,
  autoPickChamp: false,
  autoPickChampID: 1,
  autoBanChampID: 104,
  autoBanChamp: true,
});

const userInfo = reactive<UserInfo>({
  name: '张三',
  rank: '黄金',
  uuid: '123e4567-e89b-12d3-a456-426614174000',
  privacy: 'PRIVATE',
  summonerId: '123456',
});

// === 英雄选项 ===
const champOptions = ref<ChampOption[]>(
    heroData.map(hero => ({
      label: `${hero.alias} (${hero.heroId})`,
      value: Number(hero.heroId)
    }))
);

const selectedPickChamp = ref<number>(1);
const selectedBanChamp = ref<number>(100);

// === 监听器 ===
watch(
    () => configState.autoPickChampID,
    (newVal) => selectedPickChamp.value = Number(newVal)
);

watch(
    () => configState.autoBanChampID,
    (newVal) => selectedBanChamp.value = Number(newVal)
);

// === 事件处理 ===
const handleAutoAcceptChange = async (val: boolean | string | number) => {
  const enabled = Boolean(val);
  configState.autoAcceptGame = enabled;
  await api.setAutoAcceptGame({autoAcceptGame: enabled});
};

const handleAutoBanToggle = async (val: boolean | string | number) => {
  const enabled = Boolean(val);
  configState.autoBanChamp = enabled;

  if (!enabled) {
    configState.autoBanChampID = 0;
    selectedBanChamp.value = 0;
    // 只有关闭时发送请求
    await api.setBanChampion({
      autoBanChamp: false,
      championId: 0,
    });
    ElMessage.success('已关闭自动禁用');
  }
};

const handleAutoPickToggle = async (val: boolean | string | number) => {
  const enabled = Boolean(val);
  configState.autoPickChamp = enabled;

  if (!enabled) {
    configState.autoPickChampID = 0;
    selectedPickChamp.value = 0;
    // 只有关闭时发送请求
    await api.setBanChampion({
      autoBanChamp: false,
      championId: 0,
    });
    ElMessage.success('已关闭自动选择');
  }
};

const handleAutoPickChange = async (val: number | string | boolean) => {
  const heroId = Number(val);
  configState.autoPickChampID = heroId;
  selectedPickChamp.value = heroId;

  // 选择英雄时发送请求
  await api.setBanChampion({
    autoBanChamp: true,
    championId: heroId,
  });
  ElMessage.success('已设置自动选择英雄');
};

const handleAutoBanChampChange = async (val: number | string | boolean) => {
  const heroId = Number(val);
  configState.autoBanChampID = heroId;
  selectedBanChamp.value = heroId;

  // 选择英雄时发送请求
  await api.setBanChampion({
    autoBanChamp: true,
    championId: heroId,
  });
  ElMessage.success('已设置自动禁用英雄');
};

const handleReconnect = async () => {
  try {
    await api.reconnect();
    ElMessage.success('重新连接成功');
  } catch (error) {
    console.error('重新连接失败:', error);
    ElMessage.error('重新连接失败');
  }
};

const handleRefreshUserInfo = async () => {
  try {
    const result = await api.getUserInfo();

    if (result.code !== 200) {
      throw new Error(result.message || `请求失败，状态码: ${result.code}`);
    }

    if (!result.data) {
      ElMessage.warning('未找到用户信息');
      return;
    }

    const {gameName, name, rank, puuid, uuid, privacy, summonerId} = result.data;

    if (!gameName && !name) {
      throw new Error('返回数据缺少必要字段');
    }

    Object.assign(userInfo, {
      name: gameName || name || userInfo.name,
      rank: rank || userInfo.rank,
      uuid: puuid || uuid || userInfo.uuid,
      privacy: privacy || userInfo.privacy,
      summonerId: summonerId || userInfo.summonerId,
    });

    ElMessage.success('已刷新用户信息');
  } catch (error: any) {
    console.error('获取用户信息失败:', error);
    ElMessage.error(`获取用户信息失败: ${error.message}`);
  }
};

const handleRefreshConfig = async () => {
  try {
    const {code, message, data} = await api.getConfig();

    if (code !== 200) {
      throw new Error(message || `请求失败，状态码: ${code}`);
    }

    if (!data) {
      ElMessage.warning('未找到配置信息');
      return;
    }

    // 更新配置状态
    Object.assign(configState, {
      autoPickChampID: data.autoPickChampID ?? configState.autoPickChampID,
      autoBanChampID: data.autoBanChampID ?? configState.autoBanChampID,
      autoAcceptGame: data.autoAcceptGame ?? configState.autoAcceptGame,
      autoBanChamp: data.autoBanChamp ?? configState.autoBanChamp,
      autoPickChamp: data.autoPickChamp ?? configState.autoPickChamp,
    });

    // 更新选中值
    selectedPickChamp.value = Number(configState.autoPickChampID);
    selectedBanChamp.value = Number(configState.autoBanChampID);

    ElMessage.success('配置信息已刷新');
  } catch (error: any) {
    console.error('刷新配置失败:', error);
    ElMessage.error(`配置刷新失败: ${error.message}`);
  }
};

// === 初始化 ===
onMounted(async () => {
  await handleRefreshConfig();
  await handleRefreshUserInfo();
});
</script>

<template>
  <div class="p-6 bg-background min-h-screen">
    <div class="container mx-auto">
      <div class="mb-8 flex items-center justify-between">
        <div>
          <h1 class="text-3xl font-bold text-foreground mb-2">游戏配置面板</h1>
          <p class="text-muted-foreground">管理和配置您的游戏设置</p>
        </div>
        <ElButton
            type="success"
            size="large"
            @click="handleReconnect"
            class="hover:shadow-md transition-all duration-300 transform hover:scale-105 !px-6"
        >
          <i class="fas fa-plug mr-2"></i>
          重新连接
        </ElButton>
      </div>

      <ElCard class="mb-8 shadow-md rounded-xl hover:shadow-lg transition-all duration-300">
        <template #header>
          <div class="flex items-center justify-between py-2">
            <div>
              <h2 class="text-xl font-semibold text-foreground">配置管理</h2>
              <p class="text-sm text-muted-foreground">自定义您的游戏行为</p>
            </div>
            <ElButton type="primary" size="large" @click="handleRefreshConfig" plain
                      class="hover:shadow-md transition-all duration-300">
              <i class="fas fa-sync-alt mr-2"></i>
              刷新配置
            </ElButton>
          </div>
        </template>

        <div class="flex flex-col gap-6 p-4">
          <!-- 自动接受 -->
          <div
              class="flex items-center justify-between p-6 bg-card rounded-xl border border-border hover:shadow-md transition-all duration-300">
            <div>
              <h3 class="font-medium text-foreground mb-1">自动接受</h3>
              <p class="text-sm text-muted-foreground">自动接受游戏邀请</p>
            </div>
            <ElSwitch
                v-model="configState.autoAcceptGame"
                size="large"
                @change="handleAutoAcceptChange"
                class="!m-0 transform hover:scale-105 transition-transform duration-300"
            />
          </div>

          <div class="grid grid-cols-2 gap-6">
            <!-- 自动禁用 -->
            <div
                class="flex items-center justify-between p-6 bg-card rounded-xl border border-border hover:shadow-md transition-all duration-300">
              <div class="flex-shrink-0">
                <h3 class="font-medium text-foreground mb-1">自动禁用</h3>
                <p class="text-sm text-muted-foreground">自动禁用选定英雄</p>
              </div>
              <div class="flex items-center gap-4 flex-shrink-0">
                <ElSwitch
                    size="large"
                    v-model="configState.autoBanChamp"
                    @change="handleAutoBanToggle"
                    class="!m-0 transform hover:scale-105 transition-transform duration-300"
                />
                <div class="w-48">
                  <ElSelect
                      v-if="configState.autoBanChamp"
                      v-model="selectedBanChamp"
                      placeholder="请选择英雄"
                      class="w-full !shadow-sm hover:!shadow-md transition-shadow duration-300"
                      size="default"
                      filterable
                      @change="handleAutoBanChampChange"
                  >
                    <ElOption
                        v-for="item in champOptions"
                        :key="item.value"
                        :label="item.label"
                        :value="Number(item.value)"
                    />
                  </ElSelect>
                  <div v-else-if="selectedBanChamp && !configState.autoBanChamp"
                       class="text-sm text-muted-foreground truncate px-3 py-2 border rounded-lg bg-muted">
                    {{ champOptions.find(opt => opt.value === selectedBanChamp)?.label }}
                  </div>
                </div>
              </div>
            </div>

            <!-- 自动选择 -->
            <div
                class="flex items-center justify-between p-6 bg-card rounded-xl border border-border hover:shadow-md transition-all duration-300">
              <div class="flex-shrink-0">
                <h3 class="font-medium text-foreground mb-1">自动选择</h3>
                <p class="text-sm text-muted-foreground">自动选择选定英雄</p>
              </div>
              <div class="flex items-center gap-4 flex-shrink-0">
                <ElSwitch
                    v-model="configState.autoPickChamp"
                    @change="handleAutoPickToggle"
                    size="large"
                    class="!m-0 transform hover:scale-105 transition-transform duration-300"
                />
                <div class="w-48">
                  <ElSelect
                      v-if="configState.autoPickChamp"
                      v-model="selectedPickChamp"
                      placeholder="请选择英雄"
                      class="w-full !shadow-sm hover:!shadow-md transition-shadow duration-300"
                      size="default"
                      filterable
                      @change="handleAutoPickChange"
                  >
                    <ElOption
                        v-for="item in champOptions"
                        :key="item.value"
                        :label="item.label"
                        :value="Number(item.value)"
                    />
                  </ElSelect>
                  <div v-else-if="selectedPickChamp && !configState.autoPickChamp"
                       class="text-sm text-muted-foreground truncate px-3 py-2 border rounded-lg bg-muted">
                    {{ champOptions.find(opt => opt.value === selectedPickChamp)?.label }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </ElCard>

      <ElCard class="shadow-md rounded-xl hover:shadow-lg transition-all duration-300">
        <template #header>
          <div class="flex items-center justify-between py-2">
            <div>
              <h2 class="text-xl font-semibold text-foreground">召唤师信息</h2>
            </div>
            <ElButton
                type="primary"
                size="default"
                @click="handleRefreshUserInfo"
                plain
                class="hover:shadow-md transition-all duration-300"
            >
              <i class="fas fa-sync-alt mr-1"></i>
              刷新
            </ElButton>
          </div>
        </template>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4 p-4">
          <div
              class="flex flex-col p-6 bg-card rounded-xl border border-border hover:shadow-md transition-all duration-300">
            <span class="text-sm text-muted-foreground mb-2">名称</span>
            <span class="text-base font-medium text-foreground">{{ userInfo.name }}</span>
          </div>
          <div
              class="flex flex-col p-6 bg-card rounded-xl border border-border hover:shadow-md transition-all duration-300">
            <span class="text-sm text-muted-foreground mb-2">段位</span>
            <span class="text-base font-medium text-foreground">{{ userInfo.rank }}</span>
          </div>
          <div
              class="flex flex-col p-6 bg-card rounded-xl border border-border hover:shadow-md transition-all duration-300">
            <span class="text-sm text-muted-foreground mb-2">UUID</span>
            <span class="text-base font-medium text-foreground truncate">{{ userInfo.uuid }}</span>
          </div>
          <div
              class="flex flex-col p-6 bg-card rounded-xl border border-border hover:shadow-md transition-all duration-300">
            <span class="text-sm text-muted-foreground mb-2">隐私设置</span>
            <span class="text-base font-medium text-foreground">{{ userInfo.privacy }}</span>
          </div>
          <div
              class="flex flex-col p-6 bg-card rounded-xl border border-border hover:shadow-md transition-all duration-300">
            <span class="text-sm text-muted-foreground mb-2">召唤师ID</span>
            <span class="text-base font-medium text-foreground truncate">{{ userInfo.summonerId }}</span>
          </div>
        </div>
      </ElCard>
    </div>
  </div>
</template>