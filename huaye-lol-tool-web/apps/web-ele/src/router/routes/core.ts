import type { RouteRecordRaw } from 'vue-router';

import { mergeRouteModules } from '@vben/utils';


const BasicLayout = () => import('#/layouts/basic.vue');

// 导入动态路由
const dynamicRouteFiles = import.meta.glob('./modules/**/*.ts', {
  eager: true,
});

const dynamicRoutes: RouteRecordRaw[] = mergeRouteModules(dynamicRouteFiles);

/** 全局404页面 */
const fallbackNotFoundRoute: RouteRecordRaw = {
  component: () => import('#/views/_core/fallback/not-found.vue'),
  meta: {
    hideInBreadcrumb: true,
    hideInMenu: true,
    hideInTab: true,
    title: '404',
  },
  name: 'FallbackNotFound',
  path: '/:path(.*)*',
};

/** 基本路由，这些路由是必须存在的 */
const coreRoutes: RouteRecordRaw[] = [
  /**
   * 根路由
   * 使用基础布局，作为所有页面的父级容器，子级就不必配置BasicLayout。
   * 此路由必须存在，且不应修改
   */
  {
    component: BasicLayout,
    meta: {
      hideInBreadcrumb: true,
      title: 'Root',
    },
    name: 'Root',
    path: '/',
    redirect: '/dashboard/analytics', // 直接重定向到主页
    children: dynamicRoutes, // 添加动态路由作为子路由
  },
];

export { coreRoutes, fallbackNotFoundRoute };