<template>
  <div class="login-page">
    <div class="login-bg">
      <div class="bg-shape bg-shape-1"></div>
      <div class="bg-shape bg-shape-2"></div>
      <div class="bg-shape bg-shape-3"></div>
    </div>
    <div class="login-container">
      <div class="login-left">
        <div class="left-content">
          <div class="brand-icon-box">
            <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M12 2L2 7l10 5 10-5-10-5z"/>
              <path d="M2 17l10 5 10-5"/>
              <path d="M2 12l10 5 10-5"/>
            </svg>
          </div>
          <h1 class="left-title">竞赛组队平台</h1>
          <p class="left-desc">连接志同道合的伙伴，一起征战各类学科竞赛</p>
          <div class="left-features">
            <div class="feature-item">
              <div class="feature-dot"></div>
              <span>寻找优秀队友，组队参加竞赛</span>
            </div>
            <div class="feature-item">
              <div class="feature-dot"></div>
              <span>展示项目成果，积累竞赛经验</span>
            </div>
            <div class="feature-item">
              <div class="feature-dot"></div>
              <span>对接专业导师，获取竞赛指导</span>
            </div>
          </div>
        </div>
      </div>
      <div class="login-right">
        <div class="login-card">
          <div class="card-header">
            <h2>欢迎回来</h2>
            <p>请登录您的账号</p>
          </div>
          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            size="large"
            class="login-form"
            @keyup.enter="handleLogin"
          >
            <el-form-item prop="phone">
              <el-input
                v-model="form.phone"
                placeholder="手机号"
                :prefix-icon="Iphone"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="密码"
                :prefix-icon="Lock"
                show-password
                autocomplete="current-password"
              />
            </el-form-item>
            <el-form-item prop="captchaCode">
              <div class="captcha-row">
                <el-input
                  v-model="form.captchaCode"
                  placeholder="验证码"
                  :prefix-icon="PictureFilled"
                  class="captcha-input"
                />
                <img
                  :src="captchaImage"
                  class="captcha-img"
                  alt="验证码"
                  title="点击刷新验证码"
                  @click="refreshCaptcha"
                />
              </div>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                class="login-btn"
                :loading="loading"
                @click="handleLogin"
              >
                登 录
              </el-button>
            </el-form-item>
          </el-form>
          <div class="card-footer">
            <span>还没有账号？</span>
            <router-link to="/register" class="register-link">立即注册</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { login, getCaptcha } from '@/api/user'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Iphone, Lock, PictureFilled } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()

const form = reactive({
  phone: '',
  password: '',
  captchaKey: '',
  captchaCode: '',
})

const captchaImage = ref('')

const rules: FormRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 位', trigger: 'blur' },
  ],
  captchaCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
  ],
}

const loading = ref(false)

async function refreshCaptcha() {
  try {
    const res = await getCaptcha()
    form.captchaKey = res.data.captchaKey
    captchaImage.value = res.data.captchaImage
    form.captchaCode = ''
  } catch {
    // error handled by interceptor
  }
}

onMounted(() => {
  refreshCaptcha()
})

async function handleLogin() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login(form)
    userStore.setToken(res.data.token)
    userStore.setUserInfo(res.data.userInfo)
    ElMessage.success('登录成功')
    // 先导航到Dashboard（确保路由守卫能读到token），
    // 再异步拉取profile（login响应已包含完整userInfo，profile调用非必需）
    router.push('/dashboard')
    setTimeout(() => userStore.fetchProfile().catch(() => {}), 300)
  } catch {
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eef1f5 0%, #dfe3e8 100%);
  position: relative;
  overflow: hidden;
}

.login-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.bg-shape {
  position: absolute;
  border-radius: 50%;
}

.bg-shape-1 {
  width: 600px;
  height: 600px;
  background: linear-gradient(135deg, rgba(91, 127, 149, 0.06) 0%, rgba(122, 143, 166, 0.04) 100%);
  top: -200px;
  right: -100px;
}

.bg-shape-2 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, rgba(91, 127, 149, 0.04) 0%, rgba(176, 190, 197, 0.06) 100%);
  bottom: -100px;
  left: -100px;
}

.bg-shape-3 {
  width: 200px;
  height: 200px;
  background: linear-gradient(135deg, rgba(107, 158, 133, 0.05) 0%, rgba(184, 160, 110, 0.05) 100%);
  top: 30%;
  left: 10%;
}

.login-container {
  display: flex;
  width: 900px;
  max-width: 95vw;
  min-height: 560px;
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  z-index: 1;
}

.login-left {
  flex: 1;
  background: var(--gradient-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  position: relative;
  overflow: hidden;
}

.login-left::before {
  content: '';
  position: absolute;
  width: 300px;
  height: 300px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.05);
  top: -80px;
  right: -80px;
}

.login-left::after {
  content: '';
  position: absolute;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.05);
  bottom: -40px;
  left: -40px;
}

.left-content {
  color: #fff;
  z-index: 1;
}

.brand-icon-box {
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 16px;
  margin-bottom: 28px;
  backdrop-filter: blur(4px);
}

.left-title {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 12px;
}

.left-desc {
  font-size: 15px;
  opacity: 0.85;
  line-height: 1.6;
  margin-bottom: 32px;
}

.left-features {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  opacity: 0.9;
}

.feature-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.6);
  flex-shrink: 0;
}

.login-right {
  width: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
}

.login-card {
  width: 100%;
}

.card-header {
  margin-bottom: 32px;
}

.card-header h2 {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: 8px;
}

.card-header p {
  font-size: 14px;
  color: var(--color-text-muted);
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 10px;
  padding: 0 12px;
  box-shadow: 0 0 0 1px #e4e7ed inset;
  transition: box-shadow 0.2s;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--color-primary) inset;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 22px;
}

.login-btn {
  width: 100%;
  height: 46px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 8px;
  background: var(--color-primary);
  border: none;
  letter-spacing: 4px;
  transition: all 0.3s;
}

.login-btn:hover {
  box-shadow: 0 4px 20px rgba(91, 127, 149, 0.35);
  transform: translateY(-1px);
}

.card-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: var(--color-text-muted);
}

.captcha-row {
  display: flex;
  gap: 10px;
  align-items: center;
}
.captcha-input {
  flex: 1;
}
.captcha-img {
  height: 42px;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  cursor: pointer;
  flex-shrink: 0;
  transition: opacity 0.2s;
}
.captcha-img:hover {
  opacity: 0.85;
}

.register-link {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: 600;
  margin-left: 4px;
}

.register-link:hover {
  text-decoration: underline;
}

/* Responsive */
@media (max-width: 768px) {
  .login-container {
    flex-direction: column;
    max-width: 95vw;
    min-height: auto;
  }
  .login-left {
    padding: 32px 24px;
  }
  .login-right {
    width: 100%;
    padding: 32px 24px;
  }
  .left-title {
    font-size: 22px;
  }
  .left-features {
    display: none;
  }
  .left-desc {
    margin-bottom: 0;
  }
}
</style>
