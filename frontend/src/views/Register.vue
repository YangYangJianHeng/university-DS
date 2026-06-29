<template>
  <div class="register-page">
    <!-- Floating balls background -->
    <div class="floating-balls">
      <div class="ball ball-1" />
      <div class="ball ball-2" />
      <div class="ball ball-3" />
      <div class="ball ball-4" />
      <div class="ball ball-5" />
      <div class="ball ball-6" />
    </div>
    <div class="register-container">
      <!-- Brand Side -->
      <div class="register-brand">
        <div class="brand-content">
          <div class="brand-icon-wrap">
            <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M12 2L2 7l10 5 10-5-10-5z"/>
              <path d="M2 17l10 5 10-5"/>
              <path d="M2 12l10 5 10-5"/>
            </svg>
          </div>
          <h2 class="brand-title">加入我们</h2>
          <p class="brand-desc">开启你的竞赛之旅<br />与优秀的人一起成长</p>
        </div>
      </div>

      <!-- Form Side -->
      <div class="register-form-wrap">
        <div class="form-card">
          <div class="form-header">
            <h2>创建账号</h2>
            <p>填写以下信息完成注册</p>
          </div>
          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            size="large"
            class="register-form"
            @keyup.enter="handleRegister"
          >
            <el-row :gutter="14">
              <el-col :span="12">
                <el-form-item prop="phone">
                  <el-input v-model="form.phone" placeholder="手机号" :prefix-icon="Iphone" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item prop="username">
                  <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="14">
              <el-col :span="12">
                <el-form-item prop="password">
                  <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item prop="confirmPassword">
                  <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" :prefix-icon="Lock" show-password />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="14">
              <el-col :span="12">
                <el-form-item prop="studentId">
                  <el-input v-model="form.studentId" placeholder="学号" :prefix-icon="Reading" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item prop="major">
                  <el-input v-model="form.major" placeholder="专业" :prefix-icon="School" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item prop="grade">
              <el-select v-model="form.grade" placeholder="选择年级" style="width:100%">
                <el-option label="大一" value="大一" />
                <el-option label="大二" value="大二" />
                <el-option label="大三" value="大三" />
                <el-option label="大四" value="大四" />
                <el-option label="研一" value="研一" />
                <el-option label="研二" value="研二" />
                <el-option label="研三" value="研三" />
              </el-select>
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
              <el-button type="primary" class="register-btn" :loading="loading" @click="handleRegister">
                注 册
              </el-button>
            </el-form-item>
          </el-form>
          <div class="form-footer">
            <span>已有账号？</span>
            <router-link to="/login" class="link">返回登录</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { register, getCaptcha } from '@/api/user'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Iphone, Lock, User, Reading, School, PictureFilled } from '@element-plus/icons-vue'

const router = useRouter()
const formRef = ref<FormInstance>()

const form = reactive({
  phone: '',
  password: '',
  confirmPassword: '',
  username: '',
  studentId: '',
  major: '',
  grade: '',
  captchaKey: '',
  captchaCode: '',
})

const captchaImage = ref('')

const validatePassword = (_rule: any, value: string, callback: any) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validatePassword, trigger: 'blur' },
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度为 2-20 位', trigger: 'blur' },
  ],
  studentId: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  major: [{ required: true, message: '请输入专业', trigger: 'blur' }],
  grade: [{ required: true, message: '请选择年级', trigger: 'change' }],
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

async function handleRegister() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const { confirmPassword, ...submitData } = form
    await register(submitData)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch {
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
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
  width: 400px; height: 400px;
  background: radial-gradient(circle at 40% 40%, #e8e9eb, #d5d7db);
  top: -100px; right: -60px;
  animation: drift1 18s ease-in-out infinite;
}
.ball-2 {
  width: 280px; height: 280px;
  background: radial-gradient(circle at 50% 50%, #e0e2e6, #cccfd4);
  bottom: -80px; left: -50px;
  animation: drift2 22s ease-in-out infinite;
}
.ball-3 {
  width: 160px; height: 160px;
  background: radial-gradient(circle at 30% 60%, #e5e7eb, #d0d3d8);
  top: 45%; left: 55%;
  animation: drift3 15s ease-in-out infinite;
}
.ball-4 {
  width: 120px; height: 120px;
  background: radial-gradient(circle at 60% 30%, #edeeef, #d8dade);
  top: 10%; left: 25%;
  animation: drift4 20s ease-in-out infinite;
}
.ball-5 {
  width: 90px; height: 90px;
  background: radial-gradient(circle at 50% 50%, #e2e4e8, #c8cbd0);
  bottom: 20%; right: 25%;
  animation: drift5 16s ease-in-out infinite;
}
.ball-6 {
  width: 180px; height: 180px;
  background: radial-gradient(circle at 45% 55%, #e6e8ec, #cdd0d5);
  top: 55%; left: 15%;
  animation: drift6 24s ease-in-out infinite;
}

@keyframes drift1 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(-50px, 30px) scale(1.05); }
  50% { transform: translate(-30px, -40px) scale(0.95); }
  75% { transform: translate(20px, -10px) scale(1.03); }
}
@keyframes drift2 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(40px, -20px) scale(1.06); }
  50% { transform: translate(60px, 30px) scale(0.94); }
  75% { transform: translate(20px, 40px) scale(1.02); }
}
@keyframes drift3 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(-30px, -40px) scale(1.04); }
  66% { transform: translate(50px, 20px) scale(0.96); }
}
@keyframes drift4 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(40px, -30px) scale(0.97); }
  50% { transform: translate(20px, -60px) scale(1.05); }
  75% { transform: translate(-20px, -20px) scale(0.98); }
}
@keyframes drift5 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(-40px, 30px) scale(1.08); }
}
@keyframes drift6 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(30px, 20px) scale(1.03); }
  50% { transform: translate(-20px, 40px) scale(0.95); }
  75% { transform: translate(-50px, -10px) scale(1.04); }
}

.register-container {
  display: flex;
  width: 860px;
  max-width: 95vw;
  min-height: 600px;
  background: var(--color-card);
  border-radius: var(--radius-2xl);
  box-shadow: var(--shadow-xl);
  overflow: hidden;
  z-index: 1;
}

/* Brand Side */
.register-brand {
  width: 260px;
  background: #e8eaed;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  position: relative;
  overflow: hidden;
  flex-shrink: 0;
}
.register-brand::before {
  content: '';
  position: absolute;
  width: 200px; height: 200px;
  border-radius: 50%;
  background: rgba(0,0,0,0.02);
  top: -60px; right: -60px;
}
.register-brand::after {
  content: '';
  position: absolute;
  width: 150px; height: 150px;
  border-radius: 50%;
  background: rgba(0,0,0,0.02);
  bottom: -30px; left: -30px;
}
.brand-content {
  color: var(--color-text-primary);
  text-align: center;
  z-index: 1;
}
.brand-icon-wrap {
  width: 56px; height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0,0,0,0.06);
  color: var(--color-text-secondary);
  border-radius: var(--radius-md);
  margin: 0 auto 20px;
}
.brand-title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 8px;
}
.brand-desc {
  font-size: 13px;
  color: var(--color-text-muted);
  line-height: 1.5;
}

/* Form Side */
.register-form-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}
.form-card { width: 100%; max-width: 480px; }
.form-header { margin-bottom: 32px; }
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

.register-btn {
  width: 100%;
  height: 46px;
  font-size: 16px;
  font-weight: 600;
  border-radius: var(--radius-sm);
  letter-spacing: 4px;
}

.form-footer {
  text-align: center;
  margin-top: 24px;
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
  .register-container { flex-direction: column; min-height: auto; }
  .register-brand { width: 100%; padding: 24px; }
  .register-form-wrap { padding: 24px 16px; }
  .brand-desc { display: none; }
}
</style>
