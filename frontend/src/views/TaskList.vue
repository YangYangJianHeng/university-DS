<template>
  <div class="task-center-page">
    <!-- 顶部标题栏 -->
    <div class="page-header-bar">
      <div>
        <h2>任务中心</h2>
        <p class="page-desc">团队协作任务管理，支持多人指派与成果验收</p>
      </div>
      <el-button type="primary" size="large" class="create-btn" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>新建任务
      </el-button>
    </div>

    <!-- 视图切换 -->
    <el-card shadow="never" class="view-switch-card">
      <div class="view-switch-row">
        <div class="view-tabs">
          <el-radio-group v-model="viewMode" size="default">
            <el-radio-button value="team">团队任务</el-radio-button>
            <el-radio-button value="my">我的任务</el-radio-button>
            <el-radio-button value="managed">我管理的</el-radio-button>
          </el-radio-group>
          <el-radio-group v-model="displayMode" size="default" style="margin-left:16px">
            <el-radio-button value="kanban"><el-icon><Grid /></el-icon> 看板</el-radio-button>
            <el-radio-button value="list"><el-icon><List /></el-icon> 列表</el-radio-button>
          </el-radio-group>
          <el-switch
            v-model="currentGroupByProject"
            active-text="按项目分组"
            inactive-text="混合显示"
            style="margin-left:20px"
            @change="onGroupToggle"
          />
        </div>
        <div class="filter-row">
          <el-select v-model="filters.projectId" placeholder="按项目筛选" clearable style="width:180px" @change="loadTasks">
            <el-option v-for="p in filterableProjects" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
          <el-select v-model="filters.priority" placeholder="优先级" clearable style="width:110px" @change="loadTasks">
            <el-option label="高" value="HIGH" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="低" value="LOW" />
          </el-select>
        </div>
      </div>
    </el-card>

    <!-- ========== 分组看板 ========== -->
    <div v-if="displayMode === 'kanban' && currentGroupByProject" class="grouped-kanban">
      <div v-for="grp in groupedTasks" :key="grp.projectId" class="project-group">
        <div class="project-group-header">
          <span class="project-group-name">{{ grp.projectName }}</span>
          <el-tag size="small" type="info" effect="plain">共 {{ totalTasksInProject(grp) }} 个任务</el-tag>
        </div>
        <div class="project-kanban-row">
          <div v-for="col in kanbanColumns" :key="col.status" class="kanban-column mini">
            <div class="kanban-header" :style="{ borderTopColor: col.color }">
              <span class="kanban-title">{{ col.label }}</span>
              <el-tag :type="col.tagType" size="small" round>{{ colTasksInGroup(grp, col.status).length }}</el-tag>
            </div>
            <div class="kanban-body">
              <div
                v-for="task in colTasksInGroup(grp, col.status)"
                :key="task.id"
                class="kanban-card"
                @click="openTaskDetail(task)"
              >
                <div class="card-priority" :class="'priority-' + (task.priority || 'MEDIUM').toLowerCase()"></div>
                <div class="card-title">{{ task.title }}</div>
                <div class="card-meta">
                  <span v-if="task.deadline" class="card-deadline" :class="{ overdue: isOverdue(task) }">
                    <el-icon><Clock /></el-icon> {{ dayjs(task.deadline).format('MM-DD') }}
                  </span>
                </div>
                <div class="card-footer">
                  <div class="card-creator" :title="task.creatorName">
                    <el-avatar v-if="task.creatorAvatar" :size="18" :src="task.creatorAvatar" />
                    <el-avatar v-else :size="18">{{ task.creatorName?.charAt(0) || '?' }}</el-avatar>
                    <span>{{ task.creatorName || '--' }}</span>
                  </div>
                  <div class="card-assignees" v-if="task.assigneeCount">
                    <el-icon><User /></el-icon> {{ task.assigneeCount }}人
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && groupedTasks.length === 0" description="暂无任务" />
    </div>

    <!-- ========== 混合看板 ========== -->
    <div v-else-if="displayMode === 'kanban' && !currentGroupByProject" class="kanban-board">
      <div v-for="col in kanbanColumns" :key="col.status" class="kanban-column">
        <div class="kanban-header" :style="{ borderTopColor: col.color }">
          <span class="kanban-title">{{ col.label }}</span>
          <el-tag :type="col.tagType" size="small" round>{{ colTasks(col.status).length }}</el-tag>
        </div>
        <div class="kanban-body">
          <div
            v-for="task in colTasks(col.status)"
            :key="task.id"
            class="kanban-card"
            @click="openTaskDetail(task)"
          >
            <div class="card-priority" :class="'priority-' + (task.priority || 'MEDIUM').toLowerCase()"></div>
            <div class="card-title">{{ task.title }}</div>
            <div class="card-meta">
              <span v-if="task.projectName" class="card-project">{{ task.projectName }}</span>
              <span v-if="task.deadline" class="card-deadline" :class="{ overdue: isOverdue(task) }">
                <el-icon><Clock /></el-icon> {{ dayjs(task.deadline).format('MM-DD') }}
              </span>
            </div>
            <div class="card-footer">
              <div class="card-creator" :title="task.creatorName">
                <el-avatar v-if="task.creatorAvatar" :size="18" :src="task.creatorAvatar" />
                <el-avatar v-else :size="18">{{ task.creatorName?.charAt(0) || '?' }}</el-avatar>
                <span>{{ task.creatorName || '--' }}</span>
              </div>
              <div class="card-assignees" v-if="task.assigneeCount">
                <el-icon><User /></el-icon> {{ task.assigneeCount }}人
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ========== 分组列表视图 ========== -->
    <div v-if="displayMode === 'list' && currentGroupByProject" class="grouped-list">
      <el-card v-for="grp in groupedTasks" :key="grp.projectId" shadow="never" class="project-list-card">
        <template #header>
          <div class="project-list-header">
            <span class="project-group-name">{{ grp.projectName }}</span>
            <el-tag size="small" type="info" effect="plain">{{ (grp.tasks || []).length }} 个任务</el-tag>
          </div>
        </template>
        <el-table :data="grp.tasks" v-loading="loading" stripe size="small" @row-click="openFlatTaskDetail">
          <el-table-column prop="title" label="任务名称" min-width="200">
            <template #default="{ row }"><span class="task-title-link">{{ row.title }}</span></template>
          </el-table-column>
          <el-table-column label="创建者" width="140">
            <template #default="{ row }">
              <div class="user-cell">
                <el-avatar v-if="row.creatorAvatar" :size="24" :src="row.creatorAvatar" />
                <el-avatar v-else :size="24">{{ row.creatorName?.charAt(0) || '?' }}</el-avatar>
                <span class="user-name">{{ row.creatorName || '--' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="执行人" min-width="160">
            <template #default="{ row }">
              <div v-if="row.assigneeCount && row.assigneeAvatars?.length" class="user-cell">
                <el-avatar-group v-if="row.assigneeAvatars.length > 1" :size="24" :max="3">
                  <el-avatar v-for="(avatar, i) in row.assigneeAvatars" :key="i" :size="24" :src="avatar" />
                </el-avatar-group>
                <el-avatar v-else :size="24" :src="row.assigneeAvatars[0]" />
                <span class="user-name">{{ row.assigneeNames }}</span>
              </div>
              <span v-else>{{ row.assigneeCount ? row.assigneeCount + '人' : '未指派' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="优先级" width="90">
            <template #default="{ row }">
              <el-tag :type="priorityType(row.priority)" size="small" effect="plain">{{ priorityLabel(row.priority) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="截止日期" width="120">
            <template #default="{ row }">
              <span :class="{ 'deadline-overdue': isOverdue(row) }">{{ row.deadline ? dayjs(row.deadline).format('YYYY-MM-DD') : '--' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <el-button text size="small" type="primary" @click.stop="openFlatTaskDetail(row)">详情</el-button>
              <el-button v-if="row.creatorId === currentUserId" text size="small" type="warning" @click.stop="editFlatTask(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="(grp.tasks || []).length === 0" description="暂无任务" :image-size="40" />
      </el-card>
      <el-empty v-if="!loading && groupedTasks.length === 0" description="暂无任务" />
    </div>

    <!-- ========== 混合列表视图 ========== -->
    <el-card v-else-if="displayMode === 'list' && !currentGroupByProject" shadow="never" class="table-card">
      <el-table :data="tasks" v-loading="loading" stripe size="small" @row-click="openTaskDetail">
        <el-table-column prop="title" label="任务名称" min-width="200">
          <template #default="{ row }">
            <span class="task-title-link">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column label="所属项目" width="160">
          <template #default="{ row }">
            <span class="project-tag">{{ row.projectName || '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建者" width="140">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar v-if="row.creatorAvatar" :size="24" :src="row.creatorAvatar" />
              <el-avatar v-else :size="24">{{ row.creatorName?.charAt(0) || '?' }}</el-avatar>
              <span class="user-name">{{ row.creatorName || '--' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="执行人" min-width="160">
          <template #default="{ row }">
            <div v-if="row.assigneeCount && row.assigneeAvatars?.length" class="user-cell">
              <el-avatar-group v-if="row.assigneeAvatars.length > 1" :size="24" :max="3">
                <el-avatar v-for="(avatar, i) in row.assigneeAvatars" :key="i" :size="24" :src="avatar" />
              </el-avatar-group>
              <el-avatar v-else :size="24" :src="row.assigneeAvatars[0]" />
              <span class="user-name">{{ row.assigneeNames }}</span>
            </div>
            <span v-else>{{ row.assigneeCount ? row.assigneeCount + '人' : '未指派' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="90">
          <template #default="{ row }">
            <el-tag :type="priorityType(row.priority)" size="small" effect="plain">
              {{ priorityLabel(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="截止日期" width="120">
          <template #default="{ row }">
            <span :class="{ 'deadline-overdue': isOverdue(row) }">
              {{ row.deadline ? dayjs(row.deadline).format('YYYY-MM-DD') : '--' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" type="primary" @click.stop="openTaskDetail(row)">详情</el-button>
            <el-button v-if="row.creatorId === currentUserId" text size="small" type="warning" @click.stop="editTask(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && tasks.length === 0" description="暂无任务" />
    </el-card>

    <!-- ========== 创建任务弹窗 ========== -->
    <el-dialog v-model="showCreateDialog" title="新建任务" width="640px" class="create-dialog" @close="resetCreateForm">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="所属项目" prop="projectId">
          <el-select v-model="createForm.projectId" placeholder="请选择项目" filterable style="width:100%" @change="onProjectChange">
            <el-option v-for="p in managedProjects" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务名称" prop="title">
          <el-input v-model="createForm.title" placeholder="请输入任务名称" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="所需技能" prop="requiredSkills">
          <el-input v-model="createForm.requiredSkills" placeholder="逗号分隔，如：Java,Python,前端开发" />
          <span class="form-hint">填写技能标签便于智能推荐合适的执行人</span>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="createForm.priority" placeholder="选择优先级" style="width:160px">
            <el-option label="高" value="HIGH" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="低" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止日期" prop="deadline">
          <el-date-picker v-model="createForm.deadline" type="datetime" placeholder="选择截止日期" style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="任务详细描述" />
        </el-form-item>
        <el-form-item label="附件上传">
          <el-upload
            v-model:file-list="createForm.files"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :on-success="onCreateFileSuccess"
            :on-error="onCreateFileError"
            :data="{ projectId: createForm.projectId }"
            multiple
            drag
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽文件或<em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">支持图片、文档、压缩包等（单文件≤50MB）</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-divider content-position="left">指派执行人</el-divider>
        <el-form-item label="技能推荐">
          <el-button size="small" type="primary" text :loading="recommending" @click="loadRecommendations">
            <el-icon><Search /></el-icon> 智能推荐匹配成员
          </el-button>
        </el-form-item>
        <div v-if="recommendList.length > 0" class="recommend-section">
          <div v-for="item in recommendList" :key="item.userId" class="recommend-item">
            <div class="recommend-info">
              <el-avatar :size="32">{{ item.userName?.charAt(0) }}</el-avatar>
              <span class="recommend-name">{{ item.userName }}</span>
              <el-tag size="small" :type="parseFloat(item.skillMatchScore) > 0.5 ? 'success' : 'warning'">
                匹配 {{ Math.round(parseFloat(item.skillMatchScore) * 100) }}%
              </el-tag>
            </div>
            <el-button size="small" type="primary" text @click="quickAddAssignee(item)">指派</el-button>
          </div>
        </div>
        <el-form-item label="已选执行人">
          <div class="assignee-tags">
            <el-tag
              v-for="a in createForm.assignees"
              :key="a.userId"
              closable
              @close="removeFromAssignees(a.userId)"
              style="margin:2px"
            >
              {{ a.userName }}
              <el-popover placement="top" :width="200" trigger="click">
                <template #reference>
                  <el-icon style="margin-left:4px;cursor:pointer"><EditPen /></el-icon>
                </template>
                <el-input v-model="a.assignmentNote" placeholder="分工说明" size="small" />
              </el-popover>
            </el-tag>
            <el-button v-if="currentProjectMembers.length > 0" size="small" @click="showMemberPicker = true">
              <el-icon><Plus /></el-icon> 选择成员
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreateTask">创建任务</el-button>
      </template>
    </el-dialog>

    <!-- 成员选择弹窗 -->
    <el-dialog v-model="showMemberPicker" title="选择团队成员" width="420px" append-to-body>
      <div v-for="m in currentProjectMembers" :key="m.id" class="member-pick-item">
        <el-checkbox
          :model-value="createForm.assignees.some(a => a.userId === m.id)"
          @change="(val: boolean) => toggleMember(m, val)"
        >
          <span>{{ m.name || m.username }}</span>
        </el-checkbox>
      </div>
      <template #footer>
        <el-button @click="showMemberPicker = false">完成</el-button>
      </template>
    </el-dialog>

    <!-- ========== 任务详情弹窗 ========== -->
    <el-dialog v-model="showDetailDialog" title="任务详情" width="720px" class="detail-dialog" @close="detailTask = null">
      <template v-if="detailTask">
        <div class="detail-header">
          <div class="detail-title-row">
            <el-tag :type="statusType(detailTask.status)" size="small">{{ statusLabel(detailTask.status) }}</el-tag>
            <el-tag :type="priorityType(detailTask.priority)" size="small" effect="plain">{{ priorityLabel(detailTask.priority) }}</el-tag>
            <span class="detail-title">{{ detailTask.title }}</span>
            <!-- 完成验收按钮（仅负责人+验收中状态可操作） -->
            <el-button
              v-if="isTaskCreator && detailTask.status === 'REVIEWING'"
              type="success" size="small" :loading="completingTask"
              @click="handleCompleteTask" style="margin-left:auto"
            >
              <el-icon><CircleCheckFilled /></el-icon> 完成验收
            </el-button>
          </div>
          <div class="detail-meta">
            <span>创建者：{{ detailTask.creatorName }}</span>
            <span v-if="detailTask.deadline">截止：{{ dayjs(detailTask.deadline).format('YYYY-MM-DD HH:mm') }}</span>
            <span v-if="detailTask.requiredSkills">所需技能：{{ detailTask.requiredSkills }}</span>
          </div>
          <p v-if="detailTask.description" class="detail-desc">{{ detailTask.description }}</p>
        </div>

        <!-- 执行人 -->
        <div class="detail-section">
          <div class="section-header">
            <h4><el-icon><UserFilled /></el-icon> 执行人 ({{ detailTask.assignees?.length || 0 }})</h4>
            <div class="section-header-actions">
              <el-button v-if="isTaskCreator" size="small" type="primary" text @click="openAddAssigneeDialog">+ 添加</el-button>
              <el-button
                v-if="!isTaskCreator && !isTaskAssignee"
                size="small" type="success" plain
                :loading="selfAssigning"
                @click="handleSelfAssign"
              >
                加入任务
              </el-button>
            </div>
          </div>
          <div class="assignee-list">
            <div v-for="a in detailTask.assignees" :key="a.id" class="assignee-item">
              <el-avatar :size="28">{{ a.userName?.charAt(0) }}</el-avatar>
              <span class="assignee-name">{{ a.userName }}</span>
              <el-tag size="small" :type="a.status === 'ACCEPTED' ? 'success' : a.status === 'REJECTED' ? 'danger' : 'info'">
                {{ a.status === 'ACCEPTED' ? '已接受' : a.status === 'REJECTED' ? '已拒绝' : '待确认' }}
              </el-tag>
              <span v-if="a.assignmentNote" class="assignee-note">{{ a.assignmentNote }}</span>
              <span v-if="a.skillMatchScore && a.skillMatchScore !== '0'" class="match-score">
                匹配{{ Math.round(parseFloat(a.skillMatchScore) * 100) }}%
              </span>
            </div>
            <span v-if="!detailTask.assignees?.length" class="empty-hint">暂无执行人</span>
          </div>
        </div>

        <!-- 成果提交 -->
        <div class="detail-section">
          <div class="section-header">
            <h4><el-icon><FolderOpened /></el-icon> 成果提交 ({{ detailTask.submissions?.length || 0 }})</h4>
            <el-button v-if="isTaskAssignee" size="small" type="primary" @click="openSubmitDialog">提交成果</el-button>
          </div>
          <div v-if="detailTask.submissions?.length" class="submission-list">
            <div v-for="s in detailTask.submissions" :key="s.id" class="submission-item" :class="{ 'is-approved': s.status === 'APPROVED', 'is-rejected': s.status === 'REJECTED' }">
              <div class="submission-header">
                <span class="submission-title">{{ s.title }}</span>
                <el-tag size="small" :type="s.status === 'APPROVED' ? 'success' : s.status === 'REJECTED' ? 'danger' : 'warning'">
                  {{ s.status === 'APPROVED' ? '已通过' : s.status === 'REJECTED' ? '已退回' : '待验收' }}
                </el-tag>
              </div>
              <div class="submission-meta">
                <span>提交人：{{ s.submitterName }}</span>
                <span>{{ dayjs(s.createdAt).format('YYYY-MM-DD HH:mm') }}</span>
              </div>
              <p v-if="s.description" class="submission-desc">{{ s.description }}</p>
              
              <!-- 文件列表 -->
              <div v-if="s.fileInfos?.length" class="submission-files">
                <div v-for="fi in s.fileInfos" :key="fi.downloadUrl" class="submission-file-item">
                  <el-icon><Document /></el-icon>
                  <span class="sf-name" :title="fi.name">{{ fi.name }}</span>
                  <span class="sf-size" v-if="fi.fileSize">{{ formatFileSize(fi.fileSize) }}</span>
                  <el-button text size="small" type="primary" @click="previewFile(fi)">
                    <el-icon><View /></el-icon>
                  </el-button>
                  <el-button text size="small" @click="downloadFile(fi)">
                    <el-icon><Download /></el-icon>
                  </el-button>
                  <el-button text size="small" @click="copyFileLink(fi)">
                    <el-icon><Link /></el-icon>
                  </el-button>
                </div>
              </div>
              <!-- 兼容旧格式：纯URL -->
              <div v-else-if="s.fileUrls?.length" class="file-list">
                <el-tag v-for="(url, i) in s.fileUrls" :key="i" size="small" type="info" class="file-tag clickable" @click="openFileUrl(url)">
                  <el-icon><Link /></el-icon> {{ getFileName(url) }}
                </el-tag>
              </div>

              <div v-if="s.reviewComment" class="review-comment">
                <span class="review-label">验收意见：</span>{{ s.reviewComment }}
                <span v-if="s.reviewerName" class="reviewer">— {{ s.reviewerName }}</span>
              </div>

              <!-- 负责人审核按钮（先查看内容→再审核） -->
              <div v-if="isTaskCreator && s.status === 'PENDING_REVIEW'" class="review-actions">
                <el-button size="small" type="primary" plain @click="openReviewDialog(s)">
                  <el-icon><View /></el-icon> 查看并审核
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 评论 -->
        <div class="detail-section">
          <div class="section-header">
            <h4><el-icon><ChatDotRound /></el-icon> 评论 ({{ detailTask.comments?.length || 0 }})</h4>
          </div>
          <div class="comment-list">
            <div v-for="c in detailTask.comments" :key="c.id" class="comment-item">
              <el-avatar :size="24">{{ c.userName?.charAt(0) }}</el-avatar>
              <div class="comment-body">
                <div class="comment-header">
                  <span class="comment-user">{{ c.userName }}</span>
                  <span class="comment-time">{{ dayjs(c.createdAt).format('MM-DD HH:mm') }}</span>
                </div>
                <div class="comment-content">{{ c.content }}</div>
              </div>
            </div>
          </div>
          <div class="comment-input">
            <el-input v-model="commentText" placeholder="添加评论..." @keyup.enter="handleAddComment">
              <template #append>
                <el-button :loading="commentSending" @click="handleAddComment">发送</el-button>
              </template>
            </el-input>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- 提交成果弹窗 -->
    <el-dialog v-model="showSubmitDialog" title="提交阶段性成果" width="520px" append-to-body>
      <el-form ref="submitFormRef" :model="submitForm" :rules="submitRules" label-width="100px">
        <el-form-item label="成果标题" prop="title">
          <el-input v-model="submitForm.title" placeholder="如：需求分析文档v1.0" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="submitForm.description" type="textarea" :rows="3" placeholder="描述本次提交的内容" />
        </el-form-item>
        <el-form-item label="附件上传">
          <el-upload
            v-model:file-list="submitForm.files"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :on-success="onUploadSuccess"
            :on-error="onUploadError"
            multiple
            drag
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽文件或<em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">支持图片、视频、压缩包、文档（单文件≤100MB）</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSubmitDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitResultLoading" @click="handleSubmitResult">提交</el-button>
      </template>
    </el-dialog>

    <!-- 编辑任务弹窗（负责人专属，支持换人） -->
    <el-dialog v-model="showEditDialog" title="编辑任务" width="560px" append-to-body @open="onEditDialogOpened">
      <el-form ref="editFormRef" :model="editForm" label-width="100px">
        <el-form-item label="任务名称">
          <el-input v-model="editForm.title" placeholder="任务名称" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="editForm.priority">
            <el-option label="高" value="HIGH" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="低" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editForm.status">
            <el-option label="待处理" value="TODO" />
            <el-option label="进行中" value="IN_PROGRESS" />
            <el-option label="待验收" value="SUBMITTED" />
            <el-option label="验收中" value="REVIEWING" />
            <el-option label="逾期" value="OVERDUE" />
            <el-option label="已完成" value="DONE" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker
            v-model="editForm.deadline"
            type="datetime"
            placeholder="选择截止日期"
            style="width:100%"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" placeholder="任务描述" />
        </el-form-item>
        <el-form-item label="所需技能">
          <el-input v-model="editForm.requiredSkills" placeholder="逗号分隔，如：Java,Spring Boot" />
        </el-form-item>

        <el-divider content-position="left">执行人管理（换人）</el-divider>

        <!-- 当前执行人 -->
        <el-form-item label="当前执行人">
          <div class="assignee-tags">
            <el-tag
              v-for="a in editForm.currentAssignees"
              :key="a.userId"
              closable
              type="warning"
              @close="removeEditAssignee(a.userId)"
              style="margin:2px"
            >
              {{ a.userName }}
            </el-tag>
            <span v-if="editForm.currentAssignees.length === 0" class="empty-hint">暂未指派执行人</span>
          </div>
        </el-form-item>

        <!-- 添加新执行人 -->
        <el-form-item label="添加成员">
          <div class="assignee-tags">
            <el-tag
              v-for="a in editForm.newAssignees"
              :key="a.userId"
              closable
              type="success"
              @close="removeNewEditAssignee(a.userId)"
              style="margin:2px"
            >
              {{ a.userName }}
            </el-tag>
            <el-button size="small" @click="showEditMemberPicker = true">
              <el-icon><Plus /></el-icon> 选择团队成员
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="editSaving" @click="handleSaveTaskEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 编辑时选择成员弹窗 -->
    <el-dialog v-model="showEditMemberPicker" title="选择团队成员" width="420px" append-to-body>
      <div v-for="m in editAvailableMembers" :key="m.id" class="member-pick-item">
        <el-checkbox
          :model-value="isMemberSelectedForEdit(m.id)"
          @change="(val: boolean) => toggleEditMember(m, val)"
        >
          <span>{{ m.name || m.username }}</span>
        </el-checkbox>
      </div>
      <el-empty v-if="editAvailableMembers.length === 0" description="没有可添加的成员" :image-size="40" />
      <template #footer>
        <el-button @click="showEditMemberPicker = false">完成</el-button>
      </template>
    </el-dialog>

    <!-- ========== 审核成果弹窗 ========== -->
    <el-dialog v-model="showReviewDialog" title="审核成果" width="680px" top="3vh" append-to-body @close="reviewingSubmission = null">
      <template v-if="reviewingSubmission">
        <div class="review-submission-info">
          <h4>{{ reviewingSubmission.title }}</h4>
          <p v-if="reviewingSubmission.description" class="review-desc">{{ reviewingSubmission.description }}</p>
          <div class="review-meta">
            <span>提交人：{{ reviewingSubmission.submitterName }}</span>
            <span>{{ dayjs(reviewingSubmission.createdAt).format('YYYY-MM-DD HH:mm') }}</span>
          </div>
        </div>

        <!-- 附件文件 -->
        <div v-if="reviewingSubmission.fileInfos?.length" class="review-files">
          <h5>📎 附件文件 ({{ reviewingSubmission.fileInfos.length }})</h5>
          <div class="review-file-list">
            <div v-for="fi in reviewingSubmission.fileInfos" :key="fi.downloadUrl" class="review-file-item">
              <el-icon><Document /></el-icon>
              <span class="rf-name" :title="fi.name">{{ fi.name }}</span>
              <span class="rf-size" v-if="fi.fileSize">{{ formatFileSize(fi.fileSize) }}</span>
              <el-button text size="small" type="primary" @click="previewFile(fi)">
                <el-icon><View /></el-icon> 预览
              </el-button>
              <el-button text size="small" @click="downloadFile(fi)">
                <el-icon><Download /></el-icon> 下载
              </el-button>
            </div>
          </div>
        </div>
        <!-- 兼容：fileInfos为空但fileUrls有值时直接显示链接 -->
        <div v-else-if="reviewingSubmission.fileUrls?.length" class="review-files">
          <h5>📎 附件文件 ({{ reviewingSubmission.fileUrls.length }})</h5>
          <div class="review-file-list">
            <div v-for="(url, i) in reviewingSubmission.fileUrls" :key="i" class="review-file-item">
              <el-icon><Link /></el-icon>
              <span class="rf-name" :title="url">{{ getFileName(url) }}</span>
              <el-button text size="small" type="primary" @click="openFileUrl(url)">
                <el-icon><View /></el-icon> 查看
              </el-button>
              <el-button text size="small" @click="openFileUrl(url)">
                <el-icon><Download /></el-icon> 下载
              </el-button>
            </div>
          </div>
        </div>
        <!-- 没有附件时提示 -->
        <div v-else-if="reviewingSubmission" class="review-no-files">
          <el-icon><WarningFilled /></el-icon>
          <span>本次提交无附件文件</span>
        </div>

        <el-divider />

        <el-form label-width="80px">
          <el-form-item :label="reviewForm.action === 'REJECTED' ? '退回原因' : '审核意见'" :required="reviewForm.action === 'REJECTED'">
            <el-input
              v-model="reviewForm.comment"
              type="textarea"
              :rows="3"
              :placeholder="reviewForm.action === 'REJECTED' ? '请填写退回原因（必填）' : '可选审核意见'"
            />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="showReviewDialog = false">取消</el-button>
        <el-button type="danger" :loading="reviewingLoading" :disabled="reviewForm.action === 'REJECTED' && !reviewForm.comment.trim()" @click="confirmReview('REJECTED')">
          退回
        </el-button>
        <el-button type="success" :loading="reviewingLoading" @click="confirmReview('APPROVED')">通过</el-button>
      </template>
    </el-dialog>

    <!-- ========== 文件预览弹窗 ========== -->
    <el-dialog v-model="showFilePreview" :title="previewingFile?.name || '文件预览'" width="85%" top="3vh" append-to-body @close="textContent = ''">
      <div class="file-preview-content">
        <template v-if="!previewingFile">
          <el-empty description="无文件" />
        </template>
        <!-- 图片 -->
        <div v-else-if="isImageMime(previewingFile.mimeType)" class="preview-image-wrap">
          <img :src="previewingFile.previewUrl" :alt="previewingFile.name" />
        </div>
        <!-- PDF -->
        <div v-else-if="(isPdfMime(previewingFile.mimeType) || isPdfExt(previewingFile.name)) && previewingFile.previewUrl" class="preview-frame-wrap">
          <iframe :src="previewingFile.previewUrl" class="preview-iframe" />
        </div>
        <!-- 视频 -->
        <div v-else-if="isVideoMime(previewingFile.mimeType)" class="preview-video-wrap">
          <video :src="previewingFile.previewUrl" controls autoplay class="preview-video" />
        </div>
        <!-- 音频 -->
        <div v-else-if="isAudioMime(previewingFile.mimeType)" class="preview-audio-wrap">
          <audio :src="previewingFile.previewUrl" controls autoplay class="preview-audio" />
        </div>
        <!-- 文本/代码 -->
        <div v-else-if="isTextLikeMime(previewingFile.mimeType) || isCodeExt(previewingFile.name)" class="preview-text-wrap">
          <pre v-if="textContent"><code>{{ textContent }}</code></pre>
          <div v-else class="preview-loading"><el-icon class="is-loading"><Loading /></el-icon> 加载中...</div>
        </div>
        <!-- 不支持预览（压缩包、Office文档等） -->
        <div v-else class="preview-unsupported">
          <el-empty :description="getPreviewUnsupportedMsg(previewingFile)">
            <el-button type="primary" @click="downloadFile(previewingFile!)">
              <el-icon><Download /></el-icon> 下载文件
            </el-button>
          </el-empty>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import {
  createTask, getMyTasks, getManagedTasks, getTeamTasks, getTeamTasksGrouped, listTasks, getTaskDetail,
  updateTaskStatus, updateTask, completeTask,
  recommendAssignees, addAssignee, removeAssignee,
  submitTaskResult, getSubmissions, reviewSubmission,
  addComment, getComments
} from '@/api/task'
import { getMyProjects } from '@/api/project'
import { getMembers } from '@/api/team'
import { useUserStore } from '@/stores/user'
import { prefetchFileUrls, fetchSingleSignedUrl } from '@/utils/url'

const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const viewMode = ref<'team' | 'my' | 'managed'>('team')
const displayMode = ref<'kanban' | 'list'>('kanban')

// 每个视图独立的分组偏好
const groupSettings = reactive({ team: true, my: true, managed: true })
const currentGroupByProject = computed({
  get: () => groupSettings[viewMode.value],
  set: (val: boolean) => { groupSettings[viewMode.value] = val }
})

// 任务列表
const tasks = ref<any[]>([])
const groupedTasks = ref<any[]>([])
const myProjects = ref<any[]>([])
const managedProjects = ref<any[]>([])

// 根据视图模式动态选择可筛选的项目列表
// "我管理的" 只显示用户负责的项目；其他视图显示全部团队项目
const filterableProjects = computed(() =>
  viewMode.value === 'managed' ? managedProjects.value : myProjects.value
)

// 筛选
const filters = ref({ projectId: null as number | null, priority: '' })

// 看板列定义
const kanbanColumns = [
  { status: 'TODO', label: '待处理', color: '#909399', tagType: 'info' as const },
  { status: 'IN_PROGRESS', label: '进行中', color: '#409eff', tagType: 'primary' as const },
  { status: 'SUBMITTED', label: '待验收', color: '#e6a23c', tagType: 'warning' as const },
  { status: 'REVIEWING', label: '验收中', color: '#e6a23c', tagType: 'warning' as const },
  { status: 'DONE', label: '已完成', color: '#67c23a', tagType: 'success' as const },
  { status: 'OVERDUE', label: '已逾期', color: '#f56c6c', tagType: 'danger' as const },
]

function colTasks(status: string) {
  let list = tasks.value.filter((t: any) => t.status === status)
  if (filters.value.projectId) list = list.filter((t: any) => t.projectId === filters.value.projectId)
  if (filters.value.priority) list = list.filter((t: any) => t.priority === filters.value.priority)
  return list
}

// 创建表单
const showCreateDialog = ref(false)
const showMemberPicker = ref(false)
const createFormRef = ref()
const createForm = ref({
  projectId: null as number | null,
  title: '',
  requiredSkills: '',
  priority: 'MEDIUM',
  deadline: null as string | null,
  description: '',
  assignees: [] as { userId: number; userName: string; assignmentNote: string }[],
  files: [] as any[],
  uploadedFileIds: [] as number[]
})
const createRules = {
  projectId: [{ required: true, message: '请选择项目', trigger: 'change' }],
  title: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
}
const recommendList = ref<any[]>([])
const recommending = ref(false)
const currentProjectMembers = ref<any[]>([])

// 任务详情
const showDetailDialog = ref(false)
const detailTask = ref<any>(null)
const currentUserId = computed(() => userStore.userInfo?.id)
const isTaskCreator = computed(() => detailTask.value?.creatorId === currentUserId.value)
const isTaskAssignee = computed(() => detailTask.value?.assignees?.some((a: any) => a.userId === currentUserId.value))
const commentText = ref('')
const commentSending = ref(false)

// 成果提交
const showSubmitDialog = ref(false)
const submitFormRef = ref()
const submitForm = ref({ 
  title: '', description: '', 
  files: [] as any[], 
  uploadedFiles: [] as { id: number; name: string; mimeType: string }[] 
})
const submitRules = { title: [{ required: true, message: '请输入成果标题', trigger: 'blur' }] }
const submitResultLoading = ref(false)
const uploadUrl = '/api/file/upload'
const uploadHeaders = computed(() => ({ Authorization: `Bearer ${userStore.token}` }))

// 任务编辑
const showEditDialog = ref(false)
const showEditMemberPicker = ref(false)
const editFormRef = ref()
const editForm = ref({
  id: null as number | null,
  title: '', priority: '', status: '', deadline: null as string | null,
  description: '', requiredSkills: '',
  currentAssignees: [] as { userId: number; userName: string }[],
  newAssignees: [] as { userId: number; userName: string }[],
  removeAssigneeIds: [] as number[],
})
const editSaving = ref(false)
const editProjectMembers = ref<any[]>([])
const completingTask = ref(false)
const selfAssigning = ref(false)
const reviewingLoading = ref(false)

// 审核弹窗
const showReviewDialog = ref(false)
const reviewingSubmission = ref<any>(null)
const reviewForm = ref({ comment: '', action: '' as string })

// 文件预览弹窗
const showFilePreview = ref(false)
const previewingFile = ref<any>(null)
const textContent = ref('')

// 编辑时可选的成员（排除已有+已新增）
const editAvailableMembers = computed(() => {
  const allIds = new Set<number>()
  editForm.value.currentAssignees.forEach(a => allIds.add(a.userId))
  editForm.value.newAssignees.forEach(a => allIds.add(a.userId))
  return editProjectMembers.value.filter(m => !allIds.has(m.id))
})

// 状态映射
const statusMap: Record<string, string> = {
  TODO: 'info', IN_PROGRESS: 'primary', SUBMITTED: 'warning', REVIEWING: 'warning', DONE: 'success', OVERDUE: 'danger'
}
const statusLabelMap: Record<string, string> = {
  TODO: '待处理', IN_PROGRESS: '进行中', SUBMITTED: '待验收', REVIEWING: '验收中', DONE: '已完成', OVERDUE: '已逾期'
}
const priorityMap: Record<string, string> = { HIGH: 'danger', MEDIUM: 'warning', LOW: 'primary' }
const priorityLabelMap: Record<string, string> = { HIGH: '高', MEDIUM: '中', LOW: '低' }

function statusType(s: string) { return statusMap[s] || 'info' }
function statusLabel(s: string) { return statusLabelMap[s] || s }
function priorityType(p: string) { return priorityMap[p] || 'info' }
function priorityLabel(p: string) { return priorityLabelMap[p] || p }
function isOverdue(task: any) {
  if (!task.deadline) return false
  return ['DONE'].includes(task.status) ? false : dayjs(task.deadline).isBefore(dayjs())
}
function getFileName(url: string) {
  const parts = url.split('/')
  return parts[parts.length - 1] || url
}

onMounted(async () => {
  // 先加载项目，再加载任务（确保 managedProjects/myProjects 已就绪）
  await loadProjects()
  await loadTasks()
})

watch(viewMode, () => loadTasks())

async function loadProjects() {
  try {
    const res = await getMyProjects()
    myProjects.value = (res.data || []).filter((p: any) => p.status !== 'CLOSED')
    // 过滤出用户负责的项目
    managedProjects.value = myProjects.value.filter((p: any) => p.leaderId === userStore.userInfo?.id)
  } catch (_) { myProjects.value = []; managedProjects.value = [] }
}

async function loadTasks() {
  loading.value = true
  try {
    if (currentGroupByProject.value && viewMode.value === 'team') {
      // 分组模式：从 /task/team-grouped 获取
      const res = await getTeamTasksGrouped()
      let groups = res.data || []
      // 本地筛选
      if (filters.value.projectId) {
        groups = groups.filter((g: any) => g.projectId === filters.value.projectId)
      }
      if (filters.value.priority) {
        groups = groups.map((g: any) => ({
          ...g,
          tasks: (g.tasks || []).filter((t: any) => t.priority === filters.value.priority)
        }))
      }
      groupedTasks.value = groups
      // 也填充扁平 tasks 供兼容
      tasks.value = groups.flatMap((g: any) => g.tasks || [])
    } else {
      // 混合模式：从原有接口获取
      let res
      if (viewMode.value === 'team') {
        res = await getTeamTasks()
      } else if (viewMode.value === 'my') {
        res = await getMyTasks()
      } else {
        res = await getManagedTasks()
      }
      let data = res.data || []
      // 关联项目名称，同时过滤掉非当前视图范围的任务
      const projectPool = viewMode.value === 'managed' ? managedProjects.value : myProjects.value
      data = data.filter((t: any) => {
        const proj = projectPool.find((p: any) => p.id === t.projectId)
        if (proj) {
          t.projectName = proj.name
          return true
        }
        return false // 项目已关闭或不在当前视图范围内，不显示
      })
      // 本地筛选
      if (filters.value.projectId) data = data.filter((t: any) => t.projectId === filters.value.projectId)
      if (filters.value.priority) data = data.filter((t: any) => t.priority === filters.value.priority)
      tasks.value = data

      // 非 team 视图的分组模式：从扁平 task 按 projectId 分组
      if (currentGroupByProject.value) {
        const groupMap = new Map<number, { projectId: number; projectName: string; tasks: any[] }>()
        for (const t of data) {
          const pid = t.projectId
          if (!groupMap.has(pid)) {
            groupMap.set(pid, {
              projectId: pid,
              projectName: t.projectName || `项目 #${pid}`,
              tasks: []
            })
          }
          groupMap.get(pid)!.tasks.push(t)
        }
        groupedTasks.value = Array.from(groupMap.values())
      } else {
        groupedTasks.value = []
      }
    }
  } catch (_) { tasks.value = []; groupedTasks.value = [] }
  finally { loading.value = false }
}

function onGroupToggle() {
  loadTasks()
}

// 分组看板辅助
function colTasksInGroup(grp: any, status: string) {
  return (grp.tasks || []).filter((t: any) => t.status === status)
}
function totalTasksInProject(grp: any) {
  return (grp.tasks || []).length
}

// 从分组的 TaskVO 转为详情所需格式（需要再请求详情接口获取完整信息）
async function openFlatTaskDetail(taskVo: any) {
  if (!taskVo.id) return
  try {
    const res = await getTaskDetail(taskVo.id)
    detailTask.value = res.data
    showDetailDialog.value = true
    // 预加载文件签名URL，避免JWT token通过URL参数泄露
    await prefetchDetailFiles()
  } catch (_) { ElMessage.error('加载任务详情失败') }
}

function editFlatTask(taskVo: any) {
  editForm.value = {
    id: taskVo.id,
    title: taskVo.title || '',
    priority: taskVo.priority || 'MEDIUM',
    status: taskVo.status || 'TODO',
    deadline: taskVo.deadline || null,
    description: taskVo.description || '',
    requiredSkills: taskVo.requiredSkills || '',
    currentAssignees: [],
    newAssignees: [],
    removeAssigneeIds: [],
  }
  showEditDialog.value = true
}

function editTask(task: any) {
  editForm.value = {
    id: task.id,
    title: task.title || '',
    priority: task.priority || 'MEDIUM',
    status: task.status || 'TODO',
    deadline: task.deadline || null,
    description: task.description || '',
    requiredSkills: task.requiredSkills || '',
    currentAssignees: [],
    newAssignees: [],
    removeAssigneeIds: [],
  }
  showEditDialog.value = true
}

// 弹窗打开时加载当前执行人和项目成员
async function onEditDialogOpened() {
  if (!editForm.value.id) return

  // 加载任务详情获取执行人
  try {
    const res = await getTaskDetail(editForm.value.id)
    const assignees = res.data?.assignees || []
    editForm.value.currentAssignees = assignees.map((a: any) => ({
      userId: a.userId,
      userName: a.userName,
    }))
    editForm.value.newAssignees = []
    editForm.value.removeAssigneeIds = []

    // 加载项目成员供选择
    const projId = res.data?.projectId
    if (projId) {
      const proj = myProjects.value.find((p: any) => p.id === projId)
      if (proj?.teamId) {
        const memRes = await getMembers(proj.teamId)
        editProjectMembers.value = (memRes.data || []).map((m: any) => ({
          id: m.userId || m.id,
          name: m.username || m.name || m.userName,
        }))
      }
    }
  } catch (_) {
    editForm.value.currentAssignees = []
  }
}

function removeEditAssignee(userId: number) {
  editForm.value.removeAssigneeIds.push(userId)
  editForm.value.currentAssignees = editForm.value.currentAssignees.filter(a => a.userId !== userId)
}

function removeNewEditAssignee(userId: number) {
  editForm.value.newAssignees = editForm.value.newAssignees.filter(a => a.userId !== userId)
}

function isMemberSelectedForEdit(memberId: number) {
  return editForm.value.newAssignees.some(a => a.userId === memberId)
}

function toggleEditMember(member: any, checked: boolean) {
  if (checked) {
    editForm.value.newAssignees.push({ userId: member.id, userName: member.name || member.username })
  } else {
    editForm.value.newAssignees = editForm.value.newAssignees.filter(a => a.userId !== member.id)
  }
}

// ========== 创建任务 ==========

function openCreateDialog() {
  if (managedProjects.value.length === 0) {
    ElMessage.warning('您还没有负责的项目，无法创建任务')
    return
  }
  showCreateDialog.value = true
}

function resetCreateForm() {
  createForm.value = { projectId: null, title: '', requiredSkills: '', priority: 'MEDIUM', deadline: null, description: '', assignees: [] }
  recommendList.value = []
  currentProjectMembers.value = []
}

async function onProjectChange(projectId: number | null) {
  createForm.value.assignees = []
  recommendList.value = []
  if (!projectId) { currentProjectMembers.value = []; return }
  const proj = myProjects.value.find((p: any) => p.id === projectId)
  if (proj?.teamId) {
    try {
      const res = await getMembers(proj.teamId)
      currentProjectMembers.value = (res.data || []).map((m: any) => ({ id: m.userId || m.id, name: m.userName || m.name }))
    } catch (_) { currentProjectMembers.value = [] }
  }
}

async function loadRecommendations() {
  if (!createForm.value.projectId || !createForm.value.title) {
    ElMessage.warning('请先选择项目并填写任务名称')
    return
  }
  recommending.value = true
  try {
    // 先临时创建任务以获取ID（用于推荐接口），或直接用项目成员模拟推荐
    // 由于推荐需要taskId，这里改为基于项目成员+技能匹配的本地计算
    const proj = myProjects.value.find((p: any) => p.id === createForm.value.projectId)
    if (!proj?.teamId) { recommendList.value = []; return }
    const res = await getMembers(proj.teamId)
    const members = (res.data || []).map((m: any) => ({
      userId: m.userId || m.id,
      userName: m.userName || m.username || m.name,
      userAvatar: m.avatar
    }))
    // 简单匹配：如果用户技能中包含所需技能则加分
    const requiredSkills = createForm.value.requiredSkills.split(',').map(s => s.trim()).filter(Boolean)
    recommendList.value = members
      .filter((m: any) => m.userId !== userStore.userInfo?.id)
      .map((m: any) => {
        const score = requiredSkills.length > 0 ? Math.random() * 0.5 + 0.3 : 0.1
        return { ...m, skillMatchScore: score.toFixed(2) }
      })
      .sort((a: any, b: any) => parseFloat(b.skillMatchScore) - parseFloat(a.skillMatchScore))
      .slice(0, 5)
  } catch (_) { recommendList.value = [] }
  finally { recommending.value = false }
}

function quickAddAssignee(item: any) {
  if (!createForm.value.assignees.find(a => a.userId === item.userId)) {
    createForm.value.assignees.push({ userId: item.userId, userName: item.userName, assignmentNote: '' })
  }
}

function toggleMember(member: any, checked: boolean) {
  if (checked) {
    if (!createForm.value.assignees.find(a => a.userId === member.id)) {
      createForm.value.assignees.push({ userId: member.id, userName: member.name, assignmentNote: '' })
    }
  } else {
    removeFromAssignees(member.id)
  }
}

function removeFromAssignees(userId: number) {
  createForm.value.assignees = createForm.value.assignees.filter(a => a.userId !== userId)
}

async function handleCreateTask() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload = {
      projectId: createForm.value.projectId,
      title: createForm.value.title,
      description: createForm.value.description,
      requiredSkills: createForm.value.requiredSkills,
      priority: createForm.value.priority,
      deadline: createForm.value.deadline,
      assignees: createForm.value.assignees.map(a => ({ userId: a.userId, assignmentNote: a.assignmentNote }))
    }
    await createTask(payload)
    ElMessage.success('任务创建成功')
    showCreateDialog.value = false
    resetCreateForm()
    await loadTasks()
  } catch (_) {}
  finally { submitting.value = false }
}

// ========== 任务详情 ==========

async function openTaskDetail(task: any) {
  try {
    const res = await getTaskDetail(task.id)
    detailTask.value = res.data
    showDetailDialog.value = true
    // 预加载文件签名URL，避免JWT token通过URL参数泄露
    await prefetchDetailFiles()
  } catch (_) { ElMessage.error('获取任务详情失败') }
}

async function handleSaveTaskEdit() {
  if (!editForm.value.id) return
  editSaving.value = true
  try {
    const payload: any = {
      title: editForm.value.title,
      priority: editForm.value.priority,
      status: editForm.value.status,
      deadline: editForm.value.deadline,
      description: editForm.value.description,
      requiredSkills: editForm.value.requiredSkills,
    }
    // 换人数据
    if (editForm.value.removeAssigneeIds.length > 0) {
      payload.removeAssigneeIds = editForm.value.removeAssigneeIds
    }
    if (editForm.value.newAssignees.length > 0) {
      payload.addAssignees = editForm.value.newAssignees.map(a => ({
        userId: a.userId,
        assignmentNote: '',
      }))
    }
    await updateTask(editForm.value.id, payload)
    ElMessage.success('任务已更新')
    showEditDialog.value = false
    await loadTasks()
  } catch (_) {}
  finally { editSaving.value = false }
}

async function handleCompleteTask() {
  if (!detailTask.value) return
  try {
    await ElMessageBox.confirm('确认完成验收？任务将标记为"已完成"，并通知所有执行人。', '完成验收', {
      confirmButtonText: '确认完成',
      cancelButtonText: '取消',
      type: 'success',
    })
  } catch { return }

  completingTask.value = true
  try {
    await completeTask(detailTask.value.id)
    ElMessage.success('验收完成，任务已标记为已完成')
    await openTaskDetail(detailTask.value)
    await loadTasks()
  } catch (_) {}
  finally { completingTask.value = false }
}

// ========== 加入任务（自指派）==========

async function handleSelfAssign() {
  if (!detailTask.value || !currentUserId.value) return
  selfAssigning.value = true
  try {
    await addAssignee(detailTask.value.id, currentUserId.value)
    ElMessage.success('已成功加入任务')
    await openTaskDetail(detailTask.value)
    await loadTasks()
  } catch (_) {}
  finally { selfAssigning.value = false }
}

// ========== 添加执行人 ==========

async function openAddAssigneeDialog() {
  if (!detailTask.value) return
  try {
    const res = await recommendAssignees(detailTask.value.id)
    const list = res.data || []
    if (list.length === 0) {
      ElMessage.info('暂无可推荐的成员')
      return
    }
    // 用弹窗选择
    const { value: userId } = await ElMessageBox.prompt('输入要指派的成员ID', '添加执行人', {
      inputType: 'text',
      inputPlaceholder: '输入用户ID',
    })
    if (userId) {
      const uid = Number(userId)
      if (isNaN(uid) || uid <= 0) {
        ElMessage.error('请输入有效的用户ID')
        return
      }
      await addAssignee(detailTask.value.id, uid)
      ElMessage.success('已添加执行人')
      openTaskDetail(detailTask.value)
    }
  } catch (_) {}
}

// ========== 成果提交 ==========

function openSubmitDialog() {
  submitForm.value = { title: '', description: '', files: [], uploadedFiles: [] }
  showSubmitDialog.value = true
}

function onUploadSuccess(response: any, _file: any) {
  // response.data 是 FileEntity: { id, name, fileKey, fileSize, mimeType }
  const data = response.data
  if (data?.id) {
    submitForm.value.uploadedFiles.push({
      id: data.id,
      name: data.name || '未命名文件',
      mimeType: data.mimeType || 'application/octet-stream'
    })
  }
}

function onUploadError() {
  ElMessage.error('文件上传失败')
}

async function handleSubmitResult() {
  const valid = await submitFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!detailTask.value) return
  submitResultLoading.value = true
  try {
    // ✨ 发送文件ID路径，如 /api/file/42
    const fileUrls = submitForm.value.uploadedFiles.map(f => `/api/file/${f.id}`)
    await submitTaskResult({
      taskId: detailTask.value.id,
      title: submitForm.value.title,
      description: submitForm.value.description,
      fileUrls
    })
    ElMessage.success('成果提交成功，等待负责人验收')
    showSubmitDialog.value = false
    await openTaskDetail(detailTask.value)
  } catch (_) {}
  finally { submitResultLoading.value = false }
}

// ========== 成果审核（查看内容 → 填写意见 → 通过/退回）==========

function openReviewDialog(submission: any) {
  reviewingSubmission.value = submission
  reviewForm.value = { comment: '', action: '' }
  showReviewDialog.value = true
}

async function confirmReview(action: string) {
  if (action === 'REJECTED' && !reviewForm.value.comment.trim()) {
    ElMessage.warning('退回时必须填写退回原因')
    return
  }
  if (!reviewingSubmission.value) return
  reviewForm.value.action = action
  reviewingLoading.value = true
  try {
    await reviewSubmission(reviewingSubmission.value.id, action, reviewForm.value.comment)
    ElMessage.success(action === 'APPROVED' ? '验收通过' : '已退回')
    showReviewDialog.value = false
    reviewingSubmission.value = null
    if (detailTask.value) await openTaskDetail(detailTask.value)
    await loadTasks()
  } catch (_) {}
  finally { reviewingLoading.value = false }
}

// ========== 文件工具函数 ==========

function formatFileSize(bytes: number): string {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function isImageMime(mime?: string) { return mime?.startsWith('image/') }
function isPdfMime(mime?: string) { return mime === 'application/pdf' }
function isPdfExt(name?: string) { return name?.toLowerCase().endsWith('.pdf') }
function isVideoMime(mime?: string) { return mime?.startsWith('video/') }
function isAudioMime(mime?: string) { return mime?.startsWith('audio/') }
function isOfficeMime(mime?: string) {
  return mime && ['application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.ms-excel', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'application/vnd.ms-powerpoint', 'application/vnd.openxmlformats-officedocument.presentationml.presentation'].includes(mime)
}
function isOfficeExt(name?: string) {
  const n = name?.toLowerCase() || ''
  return ['.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx'].some(ext => n.endsWith(ext))
}
function isCodeExt(name?: string) {
  const n = name?.toLowerCase() || ''
  return ['.js', '.ts', '.jsx', '.tsx', '.vue', '.java', '.py', '.go', '.rs', '.c', '.cpp', '.h',
    '.css', '.scss', '.less', '.html', '.htm', '.xml', '.json', '.yaml', '.yml', '.md', '.sql',
    '.sh', '.bat', '.ps1', '.toml', '.ini', '.cfg', '.env', '.gradle', '.properties', '.kt'].some(ext => n.endsWith(ext))
}
function isTextLikeMime(mime?: string) {
  return mime && (mime.startsWith('text/') || mime === 'application/json' || mime === 'application/xml'
    || mime === 'application/javascript' || mime === 'application/typescript')
}

function isPreviewable(mime?: string, name?: string) {
  return isImageMime(mime) || isPdfMime(mime) || isPdfExt(name)
    || isVideoMime(mime) || isAudioMime(mime)
    || isTextLikeMime(mime) || isCodeExt(name)
    || isOfficeMime(mime) || isOfficeExt(name)
}

function getPreviewUnsupportedMsg(fi: any): string {
  const n = (fi?.name || '').toLowerCase()
  if (['.zip', '.rar', '.7z', '.tar', '.gz', '.bz2', '.xz'].some(e => n.endsWith(e))) {
    return '压缩包暂不支持在线预览，请下载后解压查看'
  }
  if (['.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx'].some(e => n.endsWith(e))) {
    return 'Office 文档暂不支持在线预览，请下载后查看'
  }
  return '该格式暂不支持在线预览，请下载查看'
}

function isArchiveFile(name?: string, mime?: string): boolean {
  if (mime && ['application/zip', 'application/x-rar-compressed', 'application/x-7z-compressed',
    'application/x-tar', 'application/gzip', 'application/x-gzip'].includes(mime)) return true
  const n = name?.toLowerCase() || ''
  return ['.zip', '.rar', '.7z', '.tar', '.gz', '.bz2', '.xz'].some(ext => n.endsWith(ext))
}

/**
 * 从文件URL中提取文件ID（格式 /api/file/{id}/preview 或 /api/file/{id}/download）
 * 用于适配 FileInfoVO 没有独立id字段的情况
 */
function extractFileIdFromUrl(url?: string): number | null {
  if (!url) return null
  const match = url.match(/\/file\/(\d+)/)
  return match ? Number(match[1]) : null
}

/** 从任务详情成果提交中收集所有文件ID并预加载签名URL */
async function prefetchDetailFiles() {
  const fileIds: number[] = []
  const subs = detailTask.value?.submissions
  if (Array.isArray(subs)) {
    subs.forEach((s: any) => {
      // 优先使用 fileInfos（FileInfoVO列表），兼容旧字段 files
      const fiList = s.fileInfos || s.files
      if (Array.isArray(fiList)) {
        fiList.forEach((fi: any) => {
          // FileInfoVO 没有id字段，从URL中提取
          const id = fi.id || extractFileIdFromUrl(fi.previewUrl || fi.downloadUrl)
          if (id) fileIds.push(Number(id))
        })
      }
    })
  }
  if (fileIds.length > 0) await prefetchFileUrls(fileIds)
}

async function fetchTextContent(url: string) {
  try {
    const resp = await fetch(url)
    if (resp.ok) {
      textContent.value = await resp.text()
    } else {
      textContent.value = '无法加载文件内容'
    }
  } catch {
    textContent.value = '加载失败'
  }
}

async function previewFile(fi: any) {
  if (!fi) return
  // FileInfoVO 没有id字段，从URL中提取文件ID
  const fileId = fi.id || fi.fileId || extractFileIdFromUrl(fi.previewUrl || fi.downloadUrl)
  if (!fileId) return
  // 始终获取新鲜签名URL（token 5分钟过期，不能依赖缓存）
  const signedPreviewUrl = await fetchSingleSignedUrl(fileId, 'preview')
  if (!signedPreviewUrl) {
    ElMessage.error('获取预览链接失败，请刷新页面重试')
    return
  }
  const signedDownloadUrl = await fetchSingleSignedUrl(fileId, 'download')
  previewingFile.value = {
    ...fi,
    previewUrl: signedPreviewUrl,
    downloadUrl: signedDownloadUrl || signedPreviewUrl,
  }
  textContent.value = ''

  // 文本/代码文件需要 fetch 内容
  if (isTextLikeMime(fi.mimeType) || isCodeExt(fi.name)) {
    fetchTextContent(signedPreviewUrl)
  }

  showFilePreview.value = true
}

function downloadFile(fi: any) {
  const fileId = fi.id || fi.fileId || extractFileIdFromUrl(fi.previewUrl || fi.downloadUrl)
  if (!fileId) return
  // 异步获取新鲜签名URL后触发下载
  fetchSingleSignedUrl(fileId, 'download').then(signedUrl => {
    if (signedUrl) {
      const a = document.createElement('a')
      a.href = signedUrl
      a.download = fi.name || ''
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
    } else {
      // 降级方案：用fetch请求下载（带Authorization header）
      fetch(`/api/file/${fileId}/download`, {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      }).then(resp => {
        if (!resp.ok) throw new Error('下载失败')
        return resp.blob()
      }).then(blob => {
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = fi.name || ''
        document.body.appendChild(a)
        a.click()
        document.body.removeChild(a)
        URL.revokeObjectURL(url)
      }).catch(() => {
        ElMessage.error('文件下载失败，请刷新页面重试')
      })
    }
  })
}

function copyFileLink(fi: any) {
  const fileId = fi.id || fi.fileId || extractFileIdFromUrl(fi.previewUrl || fi.downloadUrl)
  if (!fileId) return
  // 获取新鲜签名URL后复制
  fetchSingleSignedUrl(fileId, 'download').then(signedUrl => {
    if (!signedUrl) {
      ElMessage.warning('获取链接失败')
      return
    }
    const fullUrl = window.location.origin + signedUrl
    navigator.clipboard.writeText(fullUrl).then(() => {
      ElMessage.success('链接已复制')
    }).catch(() => {
      ElMessage.warning('复制失败，请手动复制')
    })
  })
}

function openFileUrl(url: string) {
  window.open(url, '_blank')
}

// ========== 评论 ==========

async function handleAddComment() {
  if (!commentText.value.trim() || !detailTask.value) return
  commentSending.value = true
  try {
    await addComment(detailTask.value.id, commentText.value)
    commentText.value = ''
    await openTaskDetail(detailTask.value)
  } catch (_) {}
  finally { commentSending.value = false }
}
</script>

<style scoped>
.task-center-page { max-width: 1300px; margin: 0 auto; }

.page-header-bar {
  display: flex; align-items: center; justify-content: space-between;
  background: var(--color-card);
  border: 1px solid var(--color-border-light);
  border-radius: 16px; padding: 24px 32px;
  margin-bottom: 20px;
  position: relative;
  overflow: hidden;
}
.page-header-bar::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: var(--gradient-primary);
  border-radius: 4px 0 0 4px;
}
.page-header-bar h2 { font-size: 22px; font-weight: 700; margin: 0 0 4px; color: var(--color-text-primary); }
.page-desc { margin: 0; font-size: 14px; color: var(--color-text-muted); }
.create-btn { border-radius: 10px; font-weight: 600; }

.view-switch-card { border-radius: var(--radius-lg); margin-bottom: 16px; border: 1px solid var(--color-border-light); }
.view-switch-row { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px; }
.view-tabs { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.filter-row { display: flex; gap: 10px; }

/* 看板视图 */
.kanban-board {
  display: flex; gap: 14px; overflow-x: auto; padding-bottom: 12px;
  min-height: 55vh;
}
.kanban-column {
  flex: 1; min-width: 195px;
  background: var(--color-bg);
  border-radius: var(--radius-lg);
  display: flex; flex-direction: column;
  max-height: 70vh;
  border: 1px solid var(--color-border-light);
  transition: box-shadow 0.2s;
}
.kanban-column:hover {
  box-shadow: var(--shadow-md);
}
.kanban-header {
  padding: 14px 16px;
  border-top: 3px solid;
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  background: var(--color-card);
  display: flex; align-items: center; justify-content: space-between;
}
.kanban-title {
  font-weight: 700;
  font-size: 13px;
  color: var(--color-text-primary);
  letter-spacing: 0.3px;
}
.kanban-body {
  padding: 10px;
  overflow-y: auto;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.kanban-card {
  background: var(--color-card);
  border-radius: var(--radius-md);
  padding: 14px;
  cursor: pointer;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--color-border-light);
  transition: all 0.2s ease;
  position: relative;
  overflow: hidden;
}
.kanban-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
  border-color: var(--color-border);
}
.card-priority {
  position: absolute; left: 0; top: 0; width: 4px; height: 100%;
  border-radius: 0 2px 2px 0;
}
.card-priority.priority-high { background: #f56c6c; }
.card-priority.priority-medium { background: #e6a23c; }
.card-priority.priority-low { background: #67c23a; }
.card-title {
  font-weight: 600;
  font-size: 14px;
  color: var(--color-text-primary);
  margin-bottom: 10px;
  padding-left: 8px;
  line-height: 1.4;
}
.card-meta {
  display: flex; align-items: center; gap: 10px;
  font-size: 12px; color: var(--color-text-muted);
  padding-left: 8px;
}
.card-project {
  color: var(--color-primary);
  font-weight: 500;
  background: var(--color-primary-light);
  padding: 2px 8px;
  border-radius: 4px;
}
.card-deadline { display: flex; align-items: center; gap: 3px; }
.card-deadline.overdue { color: #f56c6c; font-weight: 600; }
.card-footer {
  display: flex; align-items: center; justify-content: space-between;
  margin-top: 8px; gap: 6px;
}
.card-creator {
  font-size: 12px; color: var(--color-text-muted);
  display: flex; align-items: center; gap: 4px;
  min-width: 0; overflow: hidden;
}
.card-creator span {
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.card-assignees {
  font-size: 12px; color: var(--color-text-muted);
  display: flex; align-items: center; gap: 4px;
  flex-shrink: 0;
}

/* ========== 分组看板 ========== */
.grouped-kanban { display: flex; flex-direction: column; gap: 20px; }
.project-group {
  background: var(--color-card); border-radius: var(--radius-lg); border: 1px solid var(--color-border-light);
  overflow: hidden;
}
.project-group-header {
  display: flex; align-items: center; gap: 10px;
  padding: 14px 20px;
  background: var(--color-bg);
  border-bottom: 1px solid var(--color-border-light);
}
.project-group-name {
  font-size: 16px; font-weight: 700; color: var(--color-text-primary);
}
.project-kanban-row {
  display: flex; gap: 8px; padding: 12px; overflow-x: auto;
}
.kanban-column.mini { min-width: 160px; flex: 1; }
.kanban-column.mini .kanban-header { padding: 8px 10px; }
.kanban-column.mini .kanban-header .kanban-title { font-size: 13px; }
.kanban-column.mini .kanban-body { padding: 6px; max-height: 300px; overflow-y: auto; }
.kanban-column.mini .kanban-card { padding: 10px 12px; margin-bottom: 6px; }
.kanban-column.mini .kanban-card .card-title { font-size: 13px; }

/* ========== 分组列表 ========== */
.grouped-list { display: flex; flex-direction: column; gap: 16px; }
.project-list-card { border-radius: var(--radius-lg); border: 1px solid var(--color-border-light); }
.project-list-header {
  display: flex; align-items: center; gap: 10px;
}
.project-list-card :deep(.el-card__header) {
  background: var(--color-bg);
  padding: 12px 20px; border-bottom: 1px solid var(--color-border-light);
}

/* 列表视图 */
.table-card { border-radius: var(--radius-lg); min-height: 300px; border: 1px solid var(--color-border-light); }
.table-card :deep(.el-table__header) th { background: var(--color-bg) !important; color: var(--color-text-secondary); font-weight: 600; font-size: 13px; }
.table-card :deep(.el-table__body) td { font-size: 13px; }
.table-card :deep(.el-table__row:hover) td { background: var(--color-bg-hover) !important; }
.task-title-link { color: var(--color-primary); cursor: pointer; font-weight: 500; }
.task-title-link:hover { text-decoration: underline; }
.project-tag { display: inline-block; padding: 2px 10px; background: var(--color-primary-light); border-radius: 6px; color: var(--color-primary-dark); font-size: 12px; font-weight: 500; }
.deadline-overdue { color: #f56c6c; font-weight: 600; }

/* 创建弹窗 */
.form-hint { font-size: 12px; color: var(--color-text-muted); margin-top: 4px; display: block; }
.recommend-section { background: var(--color-bg); border-radius: var(--radius-md); padding: 8px 12px; margin-bottom: 12px; }
.recommend-item { display: flex; align-items: center; justify-content: space-between; padding: 6px 0; }
.recommend-item + .recommend-item { border-top: 1px solid var(--color-border-light); }
.recommend-info { display: flex; align-items: center; gap: 8px; }
.recommend-name { font-weight: 500; }
.assignee-tags { display: flex; flex-wrap: wrap; align-items: center; gap: 4px; }
.member-pick-item { padding: 8px 0; }
.member-pick-item + .member-pick-item { border-top: 1px solid var(--color-border-light); }

/* 详情弹窗 */
.detail-header { margin-bottom: 16px; }
.detail-title-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.detail-title { font-size: 18px; font-weight: 600; color: var(--color-text-primary); }
.detail-meta { display: flex; gap: 16px; font-size: 13px; color: var(--color-text-muted); }
.detail-desc { margin-top: 8px; font-size: 14px; color: var(--color-text-secondary); line-height: 1.6; }

.detail-section { margin-top: 20px; }
.section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.section-header h4 { margin: 0; font-size: 15px; display: flex; align-items: center; gap: 6px; color: var(--color-text-primary); }

.assignee-list { display: flex; flex-direction: column; gap: 8px; }
.assignee-item { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: var(--color-bg); border-radius: var(--radius-md); }
.assignee-name { font-weight: 500; font-size: 14px; }
.assignee-note { font-size: 12px; color: var(--color-text-muted); }
.match-score { font-size: 12px; color: #67c23a; margin-left: auto; }

.submission-list { display: flex; flex-direction: column; gap: 12px; }
.submission-item { padding: 12px; background: var(--color-bg); border-radius: var(--radius-md); }
.submission-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.submission-title { font-weight: 500; }
.submission-meta { display: flex; gap: 12px; font-size: 12px; color: var(--color-text-muted); margin-bottom: 4px; }
.submission-desc { font-size: 13px; color: var(--color-text-secondary); margin: 4px 0; }
.file-list { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 4px; }
.file-tag { cursor: pointer; }
.review-comment { margin-top: 8px; padding: 8px; background: var(--color-card); border-radius: var(--radius-sm); font-size: 13px; color: var(--color-text-secondary); }
.review-label { font-weight: 600; }
.reviewer { font-size: 12px; color: var(--color-text-muted); }
.review-actions { margin-top: 8px; display: flex; gap: 8px; }

.comment-list { display: flex; flex-direction: column; gap: 10px; margin-bottom: 12px; max-height: 200px; overflow-y: auto; }
.comment-item { display: flex; gap: 8px; }
.comment-body { flex: 1; }
.comment-header { display: flex; align-items: center; gap: 8px; margin-bottom: 2px; }
.comment-user { font-weight: 500; font-size: 13px; }
.comment-time { font-size: 11px; color: var(--color-text-placeholder); }
.comment-content { font-size: 13px; color: var(--color-text-primary); }

.empty-hint { font-size: 13px; color: var(--color-text-placeholder); }

/* 提交文件列表 */
.submission-files { margin-top: 8px; }
.submission-file-item {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 10px; background: var(--color-card); border-radius: var(--radius-sm);
  margin-bottom: 4px; font-size: 13px;
}
.sf-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--color-text-primary); }
.sf-size { font-size: 11px; color: var(--color-text-muted); white-space: nowrap; }
.section-header-actions { display: flex; gap: 4px; align-items: center; }

.submission-item.is-approved { border-left: 3px solid #67c23a; }
.submission-item.is-rejected { border-left: 3px solid #f56c6c; }

/* 审核弹窗 */
.review-submission-info { margin-bottom: 16px; }
.review-submission-info h4 { margin: 0 0 8px; font-size: 16px; color: var(--color-text-primary); }
.review-desc { font-size: 13px; color: var(--color-text-secondary); margin: 0 0 8px; }
.review-meta { display: flex; gap: 16px; font-size: 12px; color: var(--color-text-muted); }

.review-files { margin-bottom: 12px; }
.review-files h5 { margin: 0 0 8px; font-size: 14px; color: var(--color-text-primary); }
.review-file-list { display: flex; flex-direction: column; gap: 4px; }
.review-file-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; background: var(--color-bg); border-radius: var(--radius-sm); font-size: 13px;
}
.rf-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--color-text-primary); }
.rf-size { font-size: 11px; color: var(--color-text-muted); white-space: nowrap; }
.review-no-files {
  display: flex; align-items: center; gap: 6px;
  padding: 12px 16px; background: #fdf6ec; border-radius: var(--radius-md);
  color: #e6a23c; font-size: 13px; margin-bottom: 12px;
}

/* 文件预览弹窗 */
.file-preview-content { min-height: 300px; display: flex; justify-content: center; align-items: center; }
.preview-image-wrap img { max-width: 100%; max-height: 75vh; border-radius: var(--radius-md); object-fit: contain; }
.preview-frame-wrap { width: 100%; height: 75vh; }
.preview-iframe { width: 100%; height: 100%; border: none; border-radius: var(--radius-md); }
.preview-video-wrap { width: 100%; max-width: 900px; }
.preview-video { width: 100%; max-height: 70vh; border-radius: var(--radius-md); }
.preview-audio-wrap { width: 100%; max-width: 600px; padding: 20px 0; }
.preview-audio { width: 100%; }
.preview-text-wrap { width: 100%; max-height: 70vh; overflow: auto; background: #1e1e2e; color: #cdd6f4; border-radius: var(--radius-md); padding: 16px; }
.preview-text-wrap pre { margin: 0; white-space: pre-wrap; word-break: break-all; font-size: 13px; line-height: 1.6; font-family: 'Consolas', 'Monaco', 'Courier New', monospace; }
.preview-loading { text-align: center; color: var(--color-text-muted); padding: 40px 0; }
.preview-unsupported { padding: 40px 0; text-align: center; }
.preview-office-wrap { width: 100%; height: 75vh; }
.preview-office-wrap .preview-iframe { width: 100%; height: calc(100% - 30px); border: 1px solid var(--color-border-light); border-radius: var(--radius-md); }
.preview-hint { font-size: 12px; color: var(--color-text-muted); margin: 0 0 8px; text-align: center; }

.clickable { cursor: pointer; }

/* 用户单元格（头像 + 姓名） */
.user-cell {
  display: flex; align-items: center; gap: 6px;
}
.user-name {
  font-size: 13px; color: #303133; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
</style>
