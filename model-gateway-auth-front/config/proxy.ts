/**
 * 开发环境代理：把所有 /model-gateway-auth 转发到本地 9080 后端。
 */
export default {
  dev: {
    '/model-gateway-auth': {
      target: 'http://localhost:9080/model-gateway-auth',
      changeOrigin: true,
       pathRewrite: { '^/model-gateway-auth': '' },
    },
  },
};
