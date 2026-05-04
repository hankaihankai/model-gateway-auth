import type { Request, Response } from 'express';

/**
 * 53 条假用户数据。
 */
const list = Array.from({ length: 53 }).map((_, i) => ({
  userId: i + 1,
  username: `user_${i + 1}`,
  nickname: `用户${i + 1}`,
  phone: `138****${String(1000 + i).slice(-4)}`,
  email: `user_${i + 1}@example.com`,
  role: i === 0 ? 'ADMIN' : 'USER',
  status: i % 7 === 0 ? 0 : 1,
}));

export default {
  // 'GET /api/admin/users': (req: Request, res: Response) => {
  //   const { pageNo = '1', pageSize = '10', username, role, status } = req.query as Record<string, string | undefined>;
  //   let data = list;
  //   if (username) data = data.filter((x) => x.username.includes(username));
  //   if (role)     data = data.filter((x) => x.role === role);
  //   if (status !== undefined && status !== '') {
  //     data = data.filter((x) => String(x.status) === String(status));
  //   }
  //   const start = (Number(pageNo) - 1) * Number(pageSize);
  //   res.json({
  //     code: 200,
  //     message: 'success',
  //     data: {
  //       list: data.slice(start, start + Number(pageSize)),
  //       total: data.length,
  //       pageNo: Number(pageNo),
  //       pageSize: Number(pageSize),
  //     },
  //   });
  // },
};
