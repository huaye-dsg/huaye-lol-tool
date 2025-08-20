import { requestClient } from '#/api/request';

export interface MatchRecord {
  id: string;
  date: string;
  opponent: string;
  result: 'draw' | 'lose' | 'win';
  score: string;
  duration: string;
}

export interface MatchHistoryParams {
  keyword?: string;
  page: number;
  pageSize: number;
}

export interface MatchHistoryResponse {
  records: MatchRecord[];
  total: number;
  page: number;
  pageSize: number;
}

/**
 * 获取战绩查询数据
 * @param params 查询参数
 */
export function getMatchHistoryApi(params: MatchHistoryParams) {
  return requestClient.get<MatchHistoryResponse>('/match-history', { params });
}
