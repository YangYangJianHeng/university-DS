/**
 * 青创协同 微信小程序 - 应用入口
 * 对标 Web 端认证逻辑
 */
const { getToken, setToken, clearToken } = require('./utils/request');
const { userApi } = require('./utils/api');

App({
  globalData: {
    userInfo: null,
    token: '',
    isLoggedIn: false,
    isAdmin: false,
    unreadCount: 0,
    systemInfo: null
  },

  onLaunch() {
    // 获取系统信息
    const sysInfo = wx.getSystemInfoSync();
    this.globalData.systemInfo = sysInfo;
    this.globalData.statusBarHeight = sysInfo.statusBarHeight;
    this.globalData.navBarHeight = sysInfo.platform === 'android' ? 48 : 44;
    this.globalData.totalNavHeight = sysInfo.statusBarHeight + this.globalData.navBarHeight;

    // 检查登录状态
    this.checkLoginStatus();

    // 检查更新
    this.checkUpdate();
  },

  onShow() {
    // 每次显示时刷新未读消息数
    if (this.globalData.isLoggedIn) {
      this.fetchUnreadCount();
    }
  },

  /** 检查登录状态 */
  checkLoginStatus() {
    const token = getToken();
    if (token) {
      this.globalData.token = token;
      this.globalData.isLoggedIn = true;
      // 静默获取用户信息
      this.fetchUserInfo();
    }
  },

  /** 获取用户信息 */
  async fetchUserInfo() {
    try {
      const user = await userApi.getProfile();
      this.globalData.userInfo = user;
      this.globalData.isAdmin = user.role === 'ADMIN';

      // 缓存
      wx.setStorageSync('userInfo', user);

      // 广播用户信息更新
      this.emit('userInfoUpdate', user);
    } catch (err) {
      // 静默失败，可能是token过期
      if (err.message === '未授权') {
        this.logout(true);
      }
    }
  },

  /** 获取未读消息数 */
  async fetchUnreadCount() {
    try {
      const count = await require('./utils/api').notificationApi.getUnreadCount();
      this.globalData.unreadCount = count || 0;
      // 更新tabBar红点
      if (count > 0) {
        wx.setTabBarBadge({ index: 3, text: String(count > 99 ? '99+' : count) });
      } else {
        wx.removeTabBarBadge({ index: 3 });
      }
      this.emit('unreadUpdate', count);
    } catch (e) {
      // 静默失败
    }
  },

  /** 登录 */
  async login(phone, password, captchaKey, captchaCode) {
    const res = await userApi.login({ phone, password, captchaKey, captchaCode });
    if (res && res.token) {
      setToken(res.token);
      this.globalData.token = res.token;
      this.globalData.isLoggedIn = true;

      // 获取用户信息
      await this.fetchUserInfo();

      return res;
    }
    throw new Error('登录失败');
  },

  /** 注册 */
  async register(data) {
    return await userApi.register(data);
  },

  /** 登出 */
  logout(silent = false) {
    clearToken();
    this.globalData.token = '';
    this.globalData.userInfo = null;
    this.globalData.isLoggedIn = false;
    this.globalData.isAdmin = false;
    this.globalData.unreadCount = 0;
    wx.removeTabBarBadge({ index: 3 });

    if (!silent) {
      // 跳转到登录页
      wx.reLaunch({ url: '/pages/login/login' });
    }
  },

  /** 检查更新 */
  checkUpdate() {
    if (wx.canIUse('getUpdateManager')) {
      const updateManager = wx.getUpdateManager();
      updateManager.onCheckForUpdate(res => {
        if (res.hasUpdate) {
          updateManager.onUpdateReady(() => {
            wx.showModal({
              title: '更新提示',
              content: '新版本已就绪，是否重启应用？',
              success(res) {
                if (res.confirm) {
                  updateManager.applyUpdate();
                }
              }
            });
          });
          updateManager.onUpdateFailed(() => {
            wx.showToast({ title: '新版本下载失败', icon: 'none' });
          });
        }
      });
    }
  },

  // --- 简易事件系统 ---
  _events: {},

  on(event, callback) {
    if (!this._events[event]) this._events[event] = [];
    this._events[event].push(callback);
  },

  off(event, callback) {
    if (!this._events[event]) return;
    this._events[event] = this._events[event].filter(cb => cb !== callback);
  },

  emit(event, data) {
    if (!this._events[event]) return;
    this._events[event].forEach(cb => cb(data));
  }
});
