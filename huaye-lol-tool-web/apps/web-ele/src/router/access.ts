import type {
  GenerateMenuAndRoutesOptions,
} from '@vben/types';

async function generateAccess(options: GenerateMenuAndRoutesOptions) {
  // 直接返回静态的菜单和路由配置，不再调用API
  const staticMenus = [
    {
      id: 'dashboard',
      name: '概览',
      path: '/dashboard',
      meta: {
        icon: 'lucide:layout-dashboard',
        order: -1,
        title: '概览',
      },
      children: [
        {
          id: 'analytics',
          name: '主页',
          path: '/dashboard/analytics',
          component: 'views/dashboard/analytics/index.vue',
          meta: {
            affixTab: true,
            icon: 'lucide:area-chart',
            title: '主页',
          },
        },
        {
          id: 'workspace',
          name: '实时对局',
          path: '/dashboard/workspace',
          component: 'views/dashboard/workspace/index.vue',
          meta: {
            icon: 'carbon:workspace',
            title: '实时对局',
          },
        },
        {
          id: 'match-history',
          name: '战绩查询',
          path: '/dashboard/match-history',
          component: 'views/dashboard/match-history/index.vue',
          meta: {
            icon: 'lucide:history',
            title: '战绩查询',
          },
        },
      ],
    },
  ];

  return {
    accessibleMenus: staticMenus,
    accessibleRoutes: options.routes || [],
  };
}

export { generateAccess };