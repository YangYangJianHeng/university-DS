<template>
  <div class="register-page">
    <div class="register-bg">
      <div class="bg-shape bg-shape-1"></div>
      <div class="bg-shape bg-shape-2"></div>
    </div>
    <div class="register-container">
      <div class="register-left">
        <div class="left-content">
          <div class="brand-icon-box">
            <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M12 2L2 7l10 5 10-5-10-5z"/>
              <path d="M2 17l10 5 10-5"/>
              <path d="M2 12l10 5 10-5"/>
            </svg>
          </div>
          <h2 class="left-title">加入我们</h2>
          <p class="left-desc">开启你的竞赛之旅，与优秀的人一起成长</p>
        </div>
      </div>
      <div class="register-right">
        <div class="register-card">
          <div class="card-header">
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
            <el-row :gutter="16">
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
            <el-row :gutter="16">
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
            <el-row :gutter="16">
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
              <el-select v-model="form.grade" placeholder="年级" style="width:100%">
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
              <el-button class="register-btn" :loading="loading" @click="handleRegister">
                注 册
              </el-button>
            </el-form-item>
          </el-form>
          <div class="card-footer">
            <span>已有账号？</span>
            <router-link to="/login" class="login-link">返回登录</router-link>
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
  background: linear-gradient(135deg, #eef1f5 0%, #dfe3e8 100%);
  position: relative;
  overflow: hidden;
}

.register-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.bg-shape {
  position: absolute;
  border-radius: 50%;
}

.bg-shape-1 {
  width: 500px;
  height: 500px;
  background: linear-gradient(135deg, rgba(91, 127, 149, 0.06) 0%, rgba(122, 143, 166, 0.04) 100%);
  top: -150px;
  right: -80px;
}

.bg-shape-2 {
  width: 350px;
  height: 350px;
  background: linear-gradient(135deg, rgba(107, 158, 133, 0.06) 0%, rgba(136, 153, 166, 0.06) 100%);
  bottom: -80px;
  left: -60px;
}

.register-container {
  display: flex;
  width: 860px;
  max-width: 95vw;
  min-height: 600px;
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  z-index: 1;
}

.register-left {
  width: 280px;
  background: var(--gradient-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  position: relative;
  overflow: hidden;
  flex-shrink: 0;
}

.register-left::before {
  content: '';
  position: absolute;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.05);
  top: -60px;
  right: -60px;
}

.register-left::after {
  content: '';
  position: absolute;
  width: 150px;
  height: 150px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.05);
  bottom: -30px;
  left: -30px;
}

.left-content {
  color: #fff;
  text-align: center;
  z-index: 1;
}

.brand-icon-box {
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 14px;
  margin: 0 auto 20px;
  backdrop-filter: blur(4px);
}

.left-title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 8px;
}

.left-desc {
  font-size: 13px;
  opacity: 0.85;
  line-height: 1.5;
}

.register-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.register-card {
  width: 100%;
  max-width: 480px;
}

.card-header {
  margin-bottom: 28px;
}

.card-header h2 {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: 6px;
}

.card-header p {
  font-size: 14px;
  color: var(--color-text-muted);
}

.register-form :deep(.el-input__wrapper) {
  border-radius: 10px;
  padding: 0 12px;
  box-shadow: 0 0 0 1px #e4e7ed inset;
  transition: box-shadow 0.2s;
}

.register-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--color-primary) inset;
}

.register-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.register-btn {
  width: 100%;
  height: 46px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 8px;
  background: var(--color-primary);
  border: none;
  color: #fff;
  letter-spacing: 4px;
  transition: all 0.3s;
}

.register-btn:hover {
  box-shadow: 0 4px 20px rgba(91, 127, 149, 0.35);
  transform: translateY(-1px);
  color: #fff;
}

.card-footer {
  text-align: center;
  margin-top: 20px;
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

.login-link {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: 600;
  margin-left: 4px;
}

.login-link:hover {
  text-decoration: underline;
}

/* Responsive */
@media (max-width: 768px) {
  .register-container {
    flex-direction: column;
    max-width: 95vw;
    min-height: auto;
  }
  .register-left {
    width: 100%;
    padding: 24px;
  }
  .register-right {
    padding: 24px 16px;
  }
  .left-desc {
    display: none;
  }
}
</style>
