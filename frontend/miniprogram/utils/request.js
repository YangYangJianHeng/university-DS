/**
 * 青创协同 网络请求封装
 * 对标 Web 端 src/api/request.ts
 */

const BASE_URL = 'http://localhost:8080/api';

// 请求队列（用于token刷新）
let isRefreshing = false;
let requestQueue = [];

/** 获取token */
function getToken() {
  try {
    return wx.getStorageSync('token') || '';
  } catch (e) {
    return '';
  }
}

/** 设置token */
function setToken(token) {
  wx.setStorageSync('token', token);
}

/** 清除token */
function clearToken() {
  wx.removeStorageSync('token');
  wx.removeStorageSync('userInfo');
}

/** 跳转登录 */
function goToLogin() {
  const pages = getCurrentPages();
  const currentPage = pages[pages.length - 1];
  if (currentPage && currentPage.route !== 'pages/login/login') {
    wx.reLaunch({ url: '/pages/login/login' });
  }
}

/**
 * 发起请求
 * @param {Object} options - 请求配置
 */
function request(options) {
  const {
    url,
    method = 'GET',
    data = {},
    header = {},
    showLoading = false,
    loadingText = '加载中...',
    skipErrorToast = false
  } = options;

  return new Promise((resolve, reject) => {
    if (showLoading) {
      wx.showLoading({ title: loadingText, mask: true });
    }

    const token = getToken();
    const headers = {
      'Content-Type': 'application/json',
      ...header
    };
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    wx.request({
      url: `${BASE_URL}${url}`,
      method,
      data,
      header: headers,
      timeout: 15000,
      success(res) {
        if (showLoading) wx.hideLoading();

        const { statusCode, data: resData } = res;

        // HTTP 状态码处理
        if (statusCode === 401) {
          clearToken();
          if (!skipErrorToast) {
            wx.showToast({ title: '登录已过期，请重新登录', icon: 'none' });
          }
          goToLogin();
          reject(new Error('未授权'));
          return;
        }

        if (statusCode === 403) {
          if (!skipErrorToast) {
            wx.showToast({ title: '无权限访问', icon: 'none' });
          }
          reject(new Error('无权限'));
          return;
        }

        if (statusCode === 404) {
          if (!skipErrorToast) {
            wx.showToast({ title: '资源不存在', icon: 'none' });
          }
          reject(new Error('资源不存在'));
          return;
        }

        if (statusCode >= 500) {
          if (!skipErrorToast) {
            wx.showToast({ title: '服务器错误', icon: 'none' });
          }
          reject(new Error('服务器错误'));
          return;
        }

        // 业务状态码处理
        if (resData && resData.code !== undefined) {
          if (resData.code === 200) {
            resolve(resData.data);
          } else if (resData.code === 401) {
            clearToken();
            goToLogin();
            if (!skipErrorToast) {
              wx.showToast({ title: resData.message || '未授权', icon: 'none' });
            }
            reject(new Error(resData.message || '未授权'));
          } else {
            if (!skipErrorToast) {
              wx.showToast({ title: resData.message || '请求失败', icon: 'none' });
            }
            reject(new Error(resData.message || '请求失败'));
          }
        } else {
          // 无标准code字段，直接返回数据
          resolve(resData);
        }
      },
      fail(err) {
        if (showLoading) wx.hideLoading();
        console.error(`[请求失败] ${method} ${url}:`, err.errMsg || err);
        if (!skipErrorToast) {
          wx.showToast({ title: '网络连接失败', icon: 'none' });
        }
        reject(err);
      }
    });
  });
}

/** GET 请求 */
function get(url, params = {}, options = {}) {
  const queryString = Object.entries(params)
    .filter(([_, v]) => v !== undefined && v !== null && v !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
    .join('&');
  const fullUrl = queryString ? `${url}?${queryString}` : url;
  return request({ ...options, url: fullUrl, method: 'GET' });
}

/** POST 请求 */
function post(url, data = {}, options = {}) {
  return request({ ...options, url, method: 'POST', data });
}

/** PUT 请求 */
function put(url, data = {}, options = {}) {
  // 支持 query params
  const params = options.params || {};
  const queryString = Object.entries(params)
    .filter(([_, v]) => v !== undefined && v !== null && v !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
    .join('&');
  const fullUrl = queryString ? `${url}?${queryString}` : url;
  return request({ ...options, url: fullUrl, method: 'PUT', data });
}

/** DELETE 请求 */
function del(url, params = {}, options = {}) {
  const queryString = Object.entries(params)
    .filter(([_, v]) => v !== undefined && v !== null && v !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
    .join('&');
  const fullUrl = queryString ? `${url}?${queryString}` : url;
  return request({ ...options, url: fullUrl, method: 'DELETE' });
}

/** 上传文件 */
function upload(url, filePath, formData = {}, options = {}) {
  const token = getToken();

  return new Promise((resolve, reject) => {
    if (options.showLoading) {
      wx.showLoading({ title: options.loadingText || '上传中...', mask: true });
    }

    wx.uploadFile({
      url: `${BASE_URL}${url}`,
      filePath,
      name: 'file',
      formData,
      header: {
        'Authorization': `Bearer ${token}`
      },
      timeout: 30000,
      success(res) {
        if (options.showLoading) wx.hideLoading();
        try {
          const data = JSON.parse(res.data);
          if (data.code === 200) {
            resolve(data.data);
          } else {
            if (!options.skipErrorToast) {
              wx.showToast({ title: data.message || '上传失败', icon: 'none' });
            }
            reject(new Error(data.message));
          }
        } catch (e) {
          resolve(res.data);
        }
      },
      fail(err) {
        if (options.showLoading) wx.hideLoading();
        if (!options.skipErrorToast) {
          wx.showToast({ title: '上传失败', icon: 'none' });
        }
        reject(err);
      }
    });
  });
}

module.exports = {
  request,
  get,
  post,
  put,
  del,
  upload,
  getToken,
  setToken,
  clearToken,
  goToLogin,
  BASE_URL
};
