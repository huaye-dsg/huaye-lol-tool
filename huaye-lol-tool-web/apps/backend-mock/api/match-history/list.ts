import { faker } from '@faker-js/faker';
import { eventHandler, getQuery } from 'h3';
import { verifyAccessToken } from '~/utils/jwt-utils';
import { unAuthorizedResponse, usePageResponseSuccess } from '~/utils/response';
import { sleep } from '~/utils/response';

// 生成模拟战绩数据
function generateMockMatchHistory(count: number) {
  const dataList = [];

  for (let i = 0; i < count; i++) {
    const results = ['win', 'lose', 'draw'];
    const result = faker.helpers.arrayElement(results);

    const dataItem = {
      id: faker.string.uuid(),
      date: faker.date.past().toISOString().split('T')[0],
      opponent: `${faker.person.firstName()} ${faker.person.lastName()}`,
      result,
      score: `${faker.number.int({ min: 0, max: 5 })}:${faker.number.int({ min: 0, max: 5 })}`,
      duration: `${faker.number.int({ min: 20, max: 60 })}:${faker.number.int({ min: 0, max: 59 }).toString().padStart(2, '0')}`,
    };

    dataList.push(dataItem);
  }

  return dataList;
}

const mockData = generateMockMatchHistory(100);

export default eventHandler(async (event) => {
  const userinfo = verifyAccessToken(event);
  if (!userinfo) {
    return unAuthorizedResponse(event);
  }

  await sleep(600);

  const { page, pageSize, keyword } = getQuery(event);

  // 根据关键词过滤数据
  let filteredData = mockData;
  if (keyword) {
    const keywordStr = keyword as string;
    filteredData = mockData.filter((item) =>
      item.opponent.toLowerCase().includes(keywordStr.toLowerCase()),
    );
  }

  return usePageResponseSuccess(
    (page as string) || '1',
    (pageSize as string) || '10',
    filteredData,
  );
});
