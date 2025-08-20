import { verifyAccessToken } from '~/utils/jwt-utils';
import { unAuthorizedResponse } from '~/utils/response';

export default eventHandler(async () => {
  // 直接返回完整的菜单数据
  return useResponseSuccess(MOCK_MENUS[0].menus);
});