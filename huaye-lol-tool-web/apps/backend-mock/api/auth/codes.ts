import { verifyAccessToken } from '~/utils/jwt-utils';
import { unAuthorizedResponse } from '~/utils/response';

export default eventHandler(() => {
  // 直接返回超级管理员的权限码
  const codes = MOCK_CODES.find((item) => item.username === 'vben')?.codes ?? [];
  return useResponseSuccess(codes);
});