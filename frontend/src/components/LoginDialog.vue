<template>
  <Teleport to="body">
    <Transition name="dialog-fade">
      <div v-if="visible" class="dialog-overlay" @click.self="handleClose">
        <div class="dialog-panel">
          <!-- Close -->
          <button class="dialog-close" @click="handleClose">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>

          <!-- Header -->
          <div class="dialog-header">
            <div class="dialog-logo">
              <img src="/icon.png" width="32" height="32" alt="青创协同" />
            </div>
            <h2 class="dialog-title">欢迎回来</h2>
            <p class="dialog-sub">登录 青创协同，开启你的竞赛之旅</p>
          </div>

          <!-- Form -->
          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            size="large"
            class="dialog-form"
            @keyup.enter="handleLogin"
          >
            <el-form-item prop="phone">
              <el-input
                v-model="form.phone"
                placeholder="手机号"
                :prefix-icon="Iphone"
                class="custom-input"
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
                class="custom-input"
              />
            </el-form-item>
            <el-form-item prop="captchaCode">
              <div class="captcha-row">
                <el-input
                  v-model="form.captchaCode"
                  placeholder="验证码"
                  :prefix-icon="PictureFilled"
                  class="captcha-input custom-input"
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
                class="submit-btn"
                :loading="loading"
                @click="handleLogin"
              >
                登 录
              </el-button>
            </el-form-item>
          </el-form>

          <div class="dialog-footer">
            <span>还没有账号？</span>
            <a class="register-link" @click="goRegister">立即注册</a>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { login, getCaptcha } from '@/api/user'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Iphone, Lock, PictureFilled } from '@element-plus/icons-vue'

const props = defineProps<{ visible: boolean }>()
const emit = defineEmits<{ (e: 'update:visible', value: boolean): void }>()

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()

const form = reactive({ phone: '', password: '', captchaKey: '', captchaCode: '' })
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

watch(() => props.visible, (val) => {
  if (val) {
    form.phone = ''
    form.password = ''
    form.captchaKey = ''
    form.captchaCode = ''
    formRef.value?.resetFields()
    refreshCaptcha()
  }
})

function handleClose() {
  emit('update:visible', false)
}

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

function goRegister() {
  router.push('/register')
  handleClose()
}
</script>

<style scoped>
.dialog-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.dialog-panel {
  position: relative;
  width: 420px;
  max-width: 92vw;
  padding: 48px 40px 40px;
  background: #fff;
  border-radius: 24px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.12), 0 0 0 1px rgba(0, 0, 0, 0.04);
}

.dialog-close {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f6fa;
  border: 1px solid rgba(0, 0, 0, 0.04);
  border-radius: 10px;
  color: #888;
  cursor: pointer;
  transition: all 0.2s;
}
.dialog-close:hover {
  background: rgba(124, 58, 237, 0.06);
  border-color: rgba(124, 58, 237, 0.15);
  color: #7c3aed;
}

.dialog-header {
  text-align: center;
  margin-bottom: 36px;
}
.dialog-logo {
  margin-bottom: 16px;
  display: inline-flex;
  padding: 14px;
  background: rgba(124, 58, 237, 0.05);
  border-radius: 16px;
  border: 1px solid rgba(124, 58, 237, 0.08);
}
.dialog-title {
  font-family: 'Inter', sans-serif;
  font-size: 26px;
  font-weight: 800;
  color: #0f0f1a;
  margin-bottom: 6px;
  letter-spacing: -0.5px;
}
.dialog-sub {
  font-size: 14px;
  color: #888;
  font-weight: 400;
}

.dialog-form {
  margin-bottom: 4px;
}

.custom-input :deep(.el-input__wrapper) {
  background: #f5f6fa;
  border: 1.5px solid rgba(0, 0, 0, 0.05);
  border-radius: 12px;
  box-shadow: none !important;
  padding: 0 14px;
  transition: all 0.3s;
}
.custom-input :deep(.el-input__wrapper:hover) {
  border-color: rgba(124, 58, 237, 0.2);
}
.custom-input :deep(.el-input__wrapper.is-focus) {
  border-color: #7c3aed;
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.06) !important;
  background: #fff;
}
.custom-input :deep(.el-input__inner) {
  color: #1a1a2e;
}

.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 3px;
  border-radius: 12px;
  background: linear-gradient(135deg, #7c3aed 0%, #a855f7 100%);
  border: none;
  color: #fff;
  cursor: pointer;
  transition: all 0.3s;
  font-family: 'Inter', 'Noto Sans SC', sans-serif;
  box-shadow: 0 4px 16px rgba(124, 58, 237, 0.25);
}
.submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 24px rgba(124, 58, 237, 0.35);
}
.submit-btn:active {
  transform: translateY(0);
}

.dialog-footer {
  text-align: center;
  font-size: 13px;
  color: #999;
  margin-top: 24px;
}
.register-link {
  color: #7c3aed;
  cursor: pointer;
  font-weight: 600;
  margin-left: 4px;
  transition: color 0.2s;
}
.register-link:hover {
  color: #6d28d9;
  text-decoration: underline;
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
  height: 44px;
  border-radius: 10px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.2s;
}
.captcha-img:hover {
  opacity: 0.85;
  border-color: rgba(124, 58, 237, 0.25);
}

/* Transitions */
.dialog-fade-enter-active,
.dialog-fade-leave-active {
  transition: opacity 0.3s ease;
}
.dialog-fade-enter-active .dialog-panel,
.dialog-fade-leave-active .dialog-panel {
  transition: transform 0.35s cubic-bezier(0.25, 0.46, 0.45, 0.94), opacity 0.3s ease;
}
.dialog-fade-enter-from,
.dialog-fade-leave-to {
  opacity: 0;
}
.dialog-fade-enter-from .dialog-panel {
  transform: scale(0.94) translateY(24px);
  opacity: 0;
}
.dialog-fade-leave-to .dialog-panel {
  transform: scale(0.94) translateY(12px);
  opacity: 0;
}
</style>
