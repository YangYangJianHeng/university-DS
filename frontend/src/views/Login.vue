<template>
  <div class="login-page">
    <!-- Floating balls background -->
    <div class="floating-balls">
      <div class="ball ball-1" />
      <div class="ball ball-2" />
      <div class="ball ball-3" />
      <div class="ball ball-4" />
      <div class="ball ball-5" />
      <div class="ball ball-6" />
    </div>

    <div class="login-container">
      <!-- Brand Side -->
      <div class="login-brand">
        <div class="brand-content">
          <div class="brand-icon-wrap">
            <img src="/icon.png" width="48" height="48" alt="青创协同" />
          </div>
          <h1 class="brand-title">青创协同</h1>
          <p class="brand-desc">连接志同道合的伙伴<br />一起征战各类学科竞赛</p>
          <div class="brand-features">
            <div class="bf-item" v-for="item in features" :key="item">
              <div class="bf-dot" />
              <span>{{ item }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Form Side -->
      <div class="login-form-wrap">
        <div class="form-card">
          <div class="form-header">
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
          <div class="form-footer">
            <span>还没有账号？</span>
            <router-link to="/register" class="link">立即注册</router-link>
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
const features = ['寻找优秀队友，组队参加竞赛', '展示项目成果，积累竞赛经验', '对接专业导师，获取竞赛指导']

const rules: FormRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 位', trigger: 'blur' },
  ],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

const loading = ref(false)

async function refreshCaptcha() {
  try {
    const res = await getCaptcha()
    form.captchaKey = res.data.captchaKey
    captchaImage.value = res.data.captchaImage
    form.captchaCode = ''
  } catch { /* handled by interceptor */ }
}

onMounted(() => { refreshCaptcha() })

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
  background: #f2f3f5;
  position: relative;
  overflow: hidden;
}

/* ---------- Floating balls ---------- */
.floating-balls {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}
.ball {
  position: absolute;
  border-radius: 50%;
  opacity: 0.35;
}
.ball-1 {
  width: 420px; height: 420px;
  background: radial-gradient(circle at 40% 40%, #e8e9eb, #d5d7db);
  top: -120px; left: -80px;
  animation: drift1 18s ease-in-out infinite;
}
.ball-2 {
  width: 260px; height: 260px;
  background: radial-gradient(circle at 50% 50%, #e0e2e6, #cccfd4);
  bottom: -60px; right: -40px;
  animation: drift2 22s ease-in-out infinite;
}
.ball-3 {
  width: 180px; height: 180px;
  background: radial-gradient(circle at 30% 60%, #e5e7eb, #d0d3d8);
  top: 50%; left: 50%;
  animation: drift3 15s ease-in-out infinite;
}
.ball-4 {
  width: 140px; height: 140px;
  background: radial-gradient(circle at 60% 30%, #edeeef, #d8dade);
  top: 15%; right: 20%;
  animation: drift4 20s ease-in-out infinite;
}
.ball-5 {
  width: 100px; height: 100px;
  background: radial-gradient(circle at 50% 50%, #e2e4e8, #c8cbd0);
  bottom: 25%; left: 15%;
  animation: drift5 16s ease-in-out infinite;
}
.ball-6 {
  width: 200px; height: 200px;
  background: radial-gradient(circle at 45% 55%, #e6e8ec, #cdd0d5);
  top: 10%; left: 35%;
  animation: drift6 24s ease-in-out infinite;
}

@keyframes drift1 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(60px, 40px) scale(1.05); }
  50% { transform: translate(20px, 80px) scale(0.95); }
  75% { transform: translate(-30px, 20px) scale(1.03); }
}
@keyframes drift2 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(-50px, -30px) scale(1.06); }
  50% { transform: translate(-80px, -60px) scale(0.94); }
  75% { transform: translate(-20px, -20px) scale(1.02); }
}
@keyframes drift3 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(40px, -50px) scale(1.04); }
  66% { transform: translate(-40px, -20px) scale(0.96); }
}
@keyframes drift4 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(-30px, 50px) scale(0.97); }
  50% { transform: translate(-60px, 20px) scale(1.05); }
  75% { transform: translate(-20px, 30px) scale(0.98); }
}
@keyframes drift5 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(50px, -40px) scale(1.08); }
}
@keyframes drift6 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(-40px, -30px) scale(1.03); }
  50% { transform: translate(20px, -60px) scale(0.95); }
  75% { transform: translate(50px, -20px) scale(1.04); }
}

.login-container {
  display: flex;
  width: 900px;
  max-width: 95vw;
  min-height: 560px;
  background: var(--color-card);
  border-radius: var(--radius-2xl);
  box-shadow: var(--shadow-xl);
  overflow: hidden;
  z-index: 1;
}

/* Brand Side */
.login-brand {
  flex: 1;
  background: #e8eaed;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 56px 48px;
  position: relative;
  overflow: hidden;
}
.login-brand::before {
  content: '';
  position: absolute;
  width: 300px; height: 300px;
  border-radius: 50%;
  background: rgba(0,0,0,0.02);
  top: -80px; right: -80px;
}
.login-brand::after {
  content: '';
  position: absolute;
  width: 200px; height: 200px;
  border-radius: 50%;
  background: rgba(0,0,0,0.02);
  bottom: -40px; left: -40px;
}
.brand-content {
  color: var(--color-text-primary);
  z-index: 1;
}
.brand-icon-wrap {
  width: 64px; height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0,0,0,0.06);
  color: var(--color-text-secondary);
  border-radius: var(--radius-md);
  margin-bottom: 24px;
}
.brand-title {
  font-size: 30px;
  font-weight: 800;
  margin-bottom: 12px;
  letter-spacing: -0.5px;
}
.brand-desc {
  font-size: 15px;
  color: var(--color-text-muted);
  line-height: 1.7;
  margin-bottom: 36px;
}
.brand-features {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.bf-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: var(--color-text-secondary);
}
.bf-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: var(--color-text-placeholder);
  flex-shrink: 0;
}

/* Form Side */
.login-form-wrap {
  width: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
}
.form-card { width: 100%; }
.form-header { margin-bottom: 36px; }
.form-header h2 {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: 6px;
}
.form-header p {
  font-size: 14px;
  color: var(--color-text-muted);
}

.captcha-row {
  display: flex;
  gap: 10px;
  align-items: center;
}
.captcha-input { flex: 1; }
.captcha-img {
  height: 40px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
  cursor: pointer;
  flex-shrink: 0;
  transition: opacity var(--transition-fast);
}
.captcha-img:hover { opacity: 0.85; }

.login-btn {
  width: 100%;
  height: 46px;
  font-size: 16px;
  font-weight: 600;
  border-radius: var(--radius-sm);
  letter-spacing: 4px;
}

.form-footer {
  text-align: center;
  margin-top: 28px;
  font-size: 14px;
  color: var(--color-text-muted);
}
.link {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: 600;
  margin-left: 4px;
}
.link:hover { text-decoration: underline; }

@media (max-width: 768px) {
  .login-container { flex-direction: column; min-height: auto; }
  .login-brand { padding: 32px 24px; }
  .login-form-wrap { width: 100%; padding: 32px 24px; }
  .brand-features { display: none; }
  .brand-desc { margin-bottom: 0; }
  .brand-title { font-size: 22px; }
}
</style>
