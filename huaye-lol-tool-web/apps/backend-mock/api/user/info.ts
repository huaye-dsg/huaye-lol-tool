import { verifyAccessToken } from '~/utils/jwt-utils';
import { unAuthorizedResponse } from '~/utils/response';

export default eventHandler(() => {
  // 直接返回超级管理员用户信息
  const userinfo = {
    id: 0,
    realName: 'Vben',
    roles: ['super'],
    username: 'vben',
  };
  return useResponseSuccess(userinfo);
});