<template>
  <div class="create-recruit-container">
    <!-- Header -->
    <div class="page-header-bar">
      <div>
        <h2>发布招募</h2>
        <p class="page-desc">填写项目信息，寻找志同道合的队友</p>
      </div>
    </div>

    <el-card shadow="never" class="form-card">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="110px"
        class="create-form"
      >
        <el-form-item label="招募标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入招募标题，例如：2024 挑战杯参赛团队招募" maxlength="100" show-word-limit />
        </el-form-item>

        <el-form-item label="项目名称" prop="projectName">
          <el-input v-model="form.projectName" placeholder="请输入项目名称" maxlength="100" />
        </el-form-item>

        <el-form-item label="项目图标">
          <div class="icon-upload-area">
            <div class="icon-preview" v-if="form.projectIcon">
              <el-avatar :size="64" :src="form.projectIcon" shape="square" />
              <el-button text type="danger" size="small" @click="form.projectIcon = ''">移除</el-button>
            </div>
            <div class="icon-placeholder" v-else>
              <el-icon :size="32" color="#c0c4cc"><Picture /></el-icon>
              <span class="icon-hint">设置项目图标（可选）</span>
            </div>
            <el-input v-model="form.projectIcon" placeholder="输入图标URL，或使用下方上传" size="small" style="margin-top:8px" />
            <div class="icon-upload-btn">
              <input ref="iconInputRef" type="file" accept="image/*" style="display:none" @change="handleIconUpload" />
              <el-button size="small" :loading="iconUploading" @click="iconInputRef?.click()">
                <el-icon><Upload /></el-icon>上传图标
              </el-button>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="项目类型" prop="projectType">
          <el-select v-model="form.projectType" placeholder="请选择项目类型" style="width:100%">
            <el-option label="竞赛" value="竞赛" />
            <el-option label="科研" value="科研" />
            <el-option label="创新创业" value="创新创业" />
          </el-select>
        </el-form-item>

        <el-form-item label="项目描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="6"
            placeholder="请详细描述项目背景、目标、当前进展、招募要求等信息"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="所需技能" prop="requiredSkills">
          <div class="skills-wrapper">
            <div class="skills-tags" v-if="form.requiredSkills.length > 0">
              <el-tag
                v-for="(skill, index) in form.requiredSkills"
                :key="index"
                closable
                type="info"
                @close="removeSkill(index)"
              >
                {{ skill }}
              </el-tag>
            </div>
            <div class="skills-input-row">
              <el-input
                v-model="newSkill"
                placeholder="输入技能名称，按回车添加"
                size="default"
                @keyup.enter="addSkill"
              />
              <el-button type="primary" @click="addSkill" :disabled="!newSkill.trim()">添加</el-button>
            </div>
          </div>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="最大人数" prop="maxMembers">
              <el-input-number v-model="form.maxMembers" :min="1" :max="50" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="截止日期" prop="deadline">
              <el-date-picker
                v-model="form.deadline"
                type="date"
                placeholder="选择截止日期"
                :disabled-date="disabledDate"
                value-format="YYYY-MM-DD"
                style="width:100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item>
          <el-button type="primary" :loading="submitting" class="submit-btn" @click="handleSubmit">
            发布招募
          </el-button>
          <el-button @click="router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createRecruit } from '@/api/recruit'
import { uploadFile } from '@/api/file'
import { withToken } from '@/utils/url'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const newSkill = ref('')

const form = reactive({
  title: '',
  projectName: '',
  projectIcon: '',
  description: '',
  projectType: '',
  requiredSkills: [] as string[],
  maxMembers: 5,
  deadline: '',
})

// 图标上传
const iconInputRef = ref<HTMLInputElement>()
const iconUploading = ref(false)

async function handleIconUpload(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  iconUploading.value = true
  try {
    const res = await uploadFile(file, 0) // teamId=0 表示临时上传
    // 使用预览URL作为图标路径
    const fileData = res.data
    form.projectIcon = withToken(`/api/file/${fileData.id}/preview`)
    ElMessage.success('图标上传成功')
  } catch (_) {} finally {
    iconUploading.value = false
    if (iconInputRef.value) iconInputRef.value.value = ''
  }
}

const rules: FormRules = {
  title: [
    { required: true, message: '请输入招募标题', trigger: 'blur' },
    { min: 2, max: 100, message: '标题长度在 2 到 100 个字符', trigger: 'blur' },
  ],
  description: [{ required: true, message: '请输入项目描述', trigger: 'blur' }],
  projectType: [{ required: true, message: '请选择项目类型', trigger: 'change' }],
  maxMembers: [{ required: true, message: '请设置最大人数', trigger: 'blur' }],
  deadline: [{ required: true, message: '请选择截止日期', trigger: 'change' }],
}

function disabledDate(time: Date) {
  // 禁用今天之前的日期（保留今天可选）
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return time.getTime() < today.getTime()
}

function addSkill() {
  const skill = newSkill.value.trim()
  if (!skill) return
  if (form.requiredSkills.includes(skill)) {
    ElMessage.warning('该技能已添加')
    return
  }
  form.requiredSkills.push(skill)
  newSkill.value = ''
}

function removeSkill(index: number) {
  form.requiredSkills.splice(index, 1)
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

    submitting.value = true
  try {
    // deadline 需要转换为 LocalDateTime 格式 (yyyy-MM-ddTHH:mm:ss)
    const deadlineStr = form.deadline ? form.deadline + 'T23:59:59' : null
    const res = await createRecruit({
      title: form.title,
      projectName: form.projectName,
      projectIcon: form.projectIcon,
      description: form.description,
      projectType: form.projectType,
      requiredSkills: form.requiredSkills,
      maxMembers: form.maxMembers,
      deadline: deadlineStr,
    })
    ElMessage.success('招募发布成功')
    router.push(`/recruit/${res.data?.id || ''}`)
  } catch (_) {} finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.create-recruit-container { max-width: 800px; margin: 0 auto; }

.page-header-bar {
  background: var(--gradient-primary);
  border-radius: 16px;
  padding: 24px 32px;
  margin-bottom: 20px;
  color: #fff;
}

.page-header-bar h2 {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 4px;
}

.page-desc { margin: 0; font-size: 14px; opacity: 0.8; }

.form-card {
  border-radius: 14px;
}

.create-form {
  max-width: 620px;
  margin: 0 auto;
  padding: 20px 0;
}

.create-form :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e4e7ed inset;
  transition: box-shadow 0.2s;
}
.create-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--color-primary) inset;
}

.create-form :deep(.el-form-item) {
  margin-bottom: 24px;
}

.skills-wrapper { width: 100%; }

.skills-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.skills-input-row {
  display: flex;
  gap: 8px;
}

.skills-input-row .el-input { flex: 1; }

.submit-btn {
  border-radius: 10px;
  font-weight: 600;
  height: 42px;
  padding: 0 28px;
  background: var(--gradient-primary);
  border: none;
}
.submit-btn:hover {
  box-shadow: 0 4px 20px rgba(102,126,234,0.4);
  transform: translateY(-1px);
}

/* 图标上传 */
.icon-upload-area { width: 100%; }
.icon-upload-area :deep(.el-input__wrapper) { border-radius: 10px; }
.icon-preview { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.icon-preview :deep(.el-avatar) { border-radius: 12px; }
.icon-placeholder {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  padding: 20px; border: 2px dashed #e4e7ed; border-radius: 12px; background: #fafbfc;
}
.icon-hint { font-size: 13px; color: #909399; }
.icon-upload-btn { margin-top: 8px; }
</style>
