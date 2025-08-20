/**
 * 该文件可自行根据业务逻辑进行调整
 */
import type { RequestClientOptions } from '@vben/request';

import { useAppConfig } from '@vben/hooks';
import { preferences } from '@vben/preferences';
import {
  defaultResponseInterceptor,
  errorMessageResponseInterceptor,
  RequestClient,
} from '@vben/request';

import { ElMessage } from 'element-plus';

const { apiURL } = useAppConfig(import.meta.env, import.meta.env.PROD);

function createRequestClient(baseURL: string, options?: RequestClientOptions) {
  const client = new RequestClient({
    ...options,
    baseURL,
  });

  // 请求头处理 - 简化版，只添加语言设置
  client.addRequestInterceptor({
    fulfilled: async (config) => {
      config.headers['Accept-Language'] = preferences.app.locale;
      return config;
    },
  });

  // 处理返回的响应数据格式
  client.addResponseInterceptor(
    defaultResponseInterceptor({
      codeField: 'code',
      dataField: 'data',
      successCode: 0,
    }),
  );

  // 错误消息处理
  client.addResponseInterceptor(
    errorMessageResponseInterceptor((msg: string) => {
      ElMessage.error(msg);
    }),
  );

  return client;
}

const requestClient = createRequestClient(apiURL);


export { requestClient };