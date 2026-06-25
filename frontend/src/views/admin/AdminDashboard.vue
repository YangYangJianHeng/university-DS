<template>
  <div class="admin-dashboard">
    <h2 class="page-title">管理后台</h2>

    <!-- Stats Cards -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background:#ecf5ff;">
              <el-icon :size="28" color="#409eff"><UserFilled /></el-icon>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ stats.totalUsers ?? '--' }}</span>
              <span class="stat-label">总用户数</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background:#f0f9eb;">
              <el-icon :size="28" color="#67c23a"><UserFilled /></el-icon>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ stats.totalTeams ?? '--' }}</span>
              <span class="stat-label">总团队数</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background:#fdf6ec;">
              <el-icon :size="28" color="#e6a23c"><FolderOpened /></el-icon>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ stats.totalProjects ?? '--' }}</span>
              <span class="stat-label">总项目数</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background:#fef0f0;">
              <el-icon :size="28" color="#f56c6c"><Trophy /></el-icon>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ stats.totalCompetitions ?? '--' }}</span>
              <span class="stat-label">总竞赛数</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Tabs: 招募审核 | 用户管理 | 审核日志 | 项目管理 | 任务管理 | 团队管理 | 导师管理 -->
    <el-card shadow="never" class="section-card">
      <el-tabs v-model="activeTab">
        <!-- Tab 1: Recruit Audit -->
        <el-tab-pane label="招募审核" name="recruit">
          <div class="section-header">
            <span>待审核招募（{{ pendingRecruits.length }}）</span>
          </div>
          <el-table :data="pendingRecruits" v-loading="recruitLoading" stripe size="small">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
            <el-table-column prop="projectType" label="类型" width="90">
              <template #default="{ row }">
                <el-tag size="small">{{ row.projectType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="发布人" width="100">
              <template #default="{ row }">
                {{ publisherNames[row.id] || '--' }}
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="提交时间" width="130">
              <template #default="{ row }">
                {{ row.createdAt ? dayjs(row.createdAt).format('MM-DD HH:mm') : '--' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="success" @click="handleApproveRecruit(row)">通过</el-button>
                <el-button size="small" type="danger" @click="handleRejectRecruit(row)">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!recruitLoading && pendingRecruits.length === 0" description="暂无待审核招募" />
        </el-tab-pane>

        <!-- Tab 2: User Management -->
        <el-tab-pane label="用户管理" name="users">
          <div class="section-header">
            <span>用户列表</span>
            <el-input
              v-model="userSearch"
              placeholder="搜索用户名"
              clearable
              style="width:240px"
              size="small"
              @input="loadUsers"
            />
          </div>
          <el-table :data="users" v-loading="userLoading" stripe size="small">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="username" label="用户名" width="120" />
            <el-table-column prop="phone" label="手机号" width="120" />
            <el-table-column prop="major" label="专业" min-width="140" show-overflow-tooltip />
            <el-table-column label="角色" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="roleType(row.role)">
                  {{ roleLabel(row.role) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="审核权限" width="120">
              <template #default="{ row }">
                <el-switch
                  v-if="row.role !== 'ADMIN'"
                  :model-value="row.role === 'REVIEWER'"
                  @change="(val: boolean) => handleToggleReviewer(row, val)"
                />
                <span v-else style="color:#999;font-size:12px;">--</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-switch
                  :model-value="row.status === 1"
                  :loading="row._toggleLoading"
                  @change="(val: boolean) => handleToggleUserStatus(row, val)"
                />
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="注册时间" width="120">
              <template #default="{ row }">
                {{ row.createdAt ? dayjs(row.createdAt).format('YYYY-MM-DD') : '--' }}
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Tab 3: Audit Log -->
        <el-tab-pane label="审核日志" name="auditLog">
          <el-tabs v-model="auditTab">
            <el-tab-pane label="待审核" name="pending" />
            <el-tab-pane label="已审核" name="done" />
          </el-tabs>
          <el-table :data="auditList" v-loading="auditListLoading" stripe size="small">
            <el-table-column prop="targetType" label="审核类型" width="100">
              <template #default="{ row }">
                <el-tag size="small">{{ auditTypeLabel(row.targetType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="审核对象ID" width="100">
              <template #default="{ row }">{{ row.targetId || '--' }}</template>
            </el-table-column>
            <el-table-column prop="action" label="操作" width="80">
              <template #default="{ row }">
                <el-tag :type="row.action === 'APPROVE' ? 'success' : 'danger'" size="small">
                  {{ row.action === 'APPROVE' ? '通过' : '驳回' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reason" label="原因" min-width="120" />
            <el-table-column prop="createdAt" label="提交时间" width="120">
              <template #default="{ row }">
                {{ row.createdAt ? dayjs(row.createdAt).format('YYYY-MM-DD') : '--' }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag
                  :type="row.afterStatus === 'APPROVED' ? 'success' : row.afterStatus === 'REJECTED' ? 'danger' : 'warning'"
                  size="small"
                >
                  {{ row.afterStatus === 'APPROVED' ? '已通过' : row.afterStatus === 'REJECTED' ? '已驳回' : '待审核' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="130" fixed="right" v-if="auditTab === 'pending'">
              <template #default="{ row }">
                <el-button size="small" type="success" @click="handleApprove(row)">通过</el-button>
                <el-button size="small" type="danger" @click="handleReject(row)">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Tab 4: Project Management -->
        <el-tab-pane label="项目管理" name="projects">
          <div class="section-header">
            <span>所有项目</span>
            <el-button size="small" type="primary" @click="refreshProjects">刷新</el-button>
          </div>
          <el-table :data="allProjects" v-loading="projectLoading" stripe size="small">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="name" label="项目名称" min-width="140" show-overflow-tooltip />
            <el-table-column prop="teamId" label="所属团队ID" width="100" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="projectStatusType(row.status)" size="small">{{ projectStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="120">
              <template #default="{ row }">
                {{ row.createdAt ? dayjs(row.createdAt).format('YYYY-MM-DD') : '--' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="openEditProject(row)">编辑</el-button>
                <el-popconfirm title="确定删除该项目？关联的任务也会被删除" @confirm="handleDeleteProject(row)">
                  <template #reference>
                    <el-button size="small" type="danger">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-if="projectTotal > 10"
            v-model:current-page="projectPage"
            :total="projectTotal"
            :page-size="10"
            layout="prev, pager, next"
            size="small"
            style="margin-top:12px;justify-content:flex-end;"
            @current-change="loadAllProjects"
          />
        </el-tab-pane>

        <!-- Tab 5: Task Management (按项目分组) -->
        <el-tab-pane label="任务管理" name="tasks">
          <div v-loading="taskProjectLoading" style="display:flex;gap:16px;min-height:400px;">
            <!-- 左侧项目列表 -->
            <div style="width:260px;flex-shrink:0;">
              <div class="section-header" style="margin-bottom:8px;">
                <span>选择项目</span>
                <el-button size="small" text @click="refreshTaskProjects">刷新</el-button>
              </div>
              <div style="max-height:500px;overflow-y:auto;border:1px solid #ebeef5;border-radius:6px;">
                <div
                  v-for="proj in taskProjects"
                  :key="proj.id"
                  @click="selectTaskProject(proj)"
                  :class="['project-select-item', { active: selectedTaskProject?.id === proj.id }]"
                >
                  <div class="project-select-name">{{ proj.name || '未命名项目' }}</div>
                  <div class="project-select-meta">
                    <el-tag :type="projectStatusType(proj.status)" size="small" effect="plain">{{ projectStatusLabel(proj.status) }}</el-tag>
                    <span style="font-size:11px;color:#909399;">ID:{{ proj.id }}</span>
                  </div>
                </div>
                <el-empty v-if="taskProjects.length === 0 && !taskProjectLoading" description="暂无项目" :image-size="60" />
              </div>
            </div>

            <!-- 右侧任务列表 -->
            <div style="flex:1;min-width:0;">
              <div class="section-header">
                <span v-if="selectedTaskProject">{{ selectedTaskProject.name }} · 任务列表（{{ filteredTasks.length }}）</span>
                <span v-else style="color:#909399;">请先在左侧选择一个项目</span>
                <div style="display:flex;gap:8px;" v-if="selectedTaskProject">
                  <el-select v-model="taskFilterStatus" placeholder="状态筛选" size="small" clearable style="width:110px;" @change="applyTaskFilter">
                    <el-option label="待办" value="TODO" />
                    <el-option label="进行中" value="DOING" />
                    <el-option label="已完成" value="DONE" />
                  </el-select>
                  <el-select v-model="taskFilterPriority" placeholder="优先级" size="small" clearable style="width:100px;" @change="applyTaskFilter">
                    <el-option label="高" value="HIGH" />
                    <el-option label="中" value="MEDIUM" />
                    <el-option label="低" value="LOW" />
                  </el-select>
                </div>
              </div>
              <template v-if="selectedTaskProject">
                <el-table :data="filteredTasks" v-loading="taskLoading" stripe size="small" max-height="450">
                  <el-table-column prop="id" label="ID" width="60" />
                  <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
                  <el-table-column label="优先级" width="80">
                    <template #default="{ row }">
                      <el-tag :type="priorityType(row.priority)" size="small">{{ priorityLabel(row.priority) }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="状态" width="90">
                    <template #default="{ row }">
                      <el-tag :type="taskStatusType(row.status)" size="small">{{ taskStatusLabel(row.status) }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="deadline" label="截止时间" width="120">
                    <template #default="{ row }">
                      {{ row.deadline ? dayjs(row.deadline).format('MM-DD HH:mm') : '--' }}
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="160" fixed="right">
                    <template #default="{ row }">
                      <el-button size="small" type="primary" @click="openEditTask(row)">编辑</el-button>
                      <el-popconfirm title="确定删除该任务？" @confirm="handleDeleteTask(row)">
                        <template #reference>
                          <el-button size="small" type="danger">删除</el-button>
                        </template>
                      </el-popconfirm>
                    </template>
                  </el-table-column>
                </el-table>
                <el-empty v-if="filteredTasks.length === 0 && !taskLoading" description="该项目暂无任务" :image-size="80" />
              </template>
              <el-empty v-else description="请选择左侧项目查看任务" :image-size="80" />
            </div>
          </div>
        </el-tab-pane>

        <!-- Tab 6: Team Management -->
        <el-tab-pane label="团队管理" name="teams">
          <div class="section-header">
            <span>所有团队</span>
            <el-button size="small" type="primary" @click="refreshTeams">刷新</el-button>
          </div>
          <el-table :data="allTeams" v-loading="teamLoading" stripe size="small">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="name" label="团队名称" min-width="140" show-overflow-tooltip />
            <el-table-column prop="leaderId" label="队长ID" width="80" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">{{ row.status === 'ACTIVE' ? '正常' : '已解散' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="120">
              <template #default="{ row }">
                {{ row.createdAt ? dayjs(row.createdAt).format('YYYY-MM-DD') : '--' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="openEditTeam(row)">编辑</el-button>
                <el-popconfirm title="确定删除该团队？关联的项目也会被删除" @confirm="handleDeleteTeam(row)">
                  <template #reference>
                    <el-button size="small" type="danger">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-if="teamTotal > 10"
            v-model:current-page="teamPage"
            :total="teamTotal"
            :page-size="10"
            layout="prev, pager, next"
            size="small"
            style="margin-top:12px;justify-content:flex-end;"
            @current-change="loadAllTeams"
          />
        </el-tab-pane>

        <!-- Tab 7: Competition Management -->
        <el-tab-pane label="竞赛管理" name="competitions">
          <div class="section-header">
            <span>所有竞赛</span>
            <el-button size="small" type="primary" @click="refreshCompetitions">刷新</el-button>
          </div>
          <el-table :data="allCompetitions" v-loading="competitionLoading" stripe size="small">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="name" label="竞赛名称" min-width="160" show-overflow-tooltip />
            <el-table-column prop="level" label="级别" width="90">
              <template #default="{ row }">
                <el-tag size="small">{{ row.level || '--' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="category" label="类别" width="100" show-overflow-tooltip />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="competitionStatusType(row.status)" size="small">{{ competitionStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="organizer" label="主办方" min-width="120" show-overflow-tooltip />
            <el-table-column label="报名时间" width="180">
              <template #default="{ row }">
                <span v-if="row.registerStart">{{ row.registerStart }} ~ {{ row.registerEnd }}</span>
                <span v-else style="color:#999">--</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="openEditCompetition(row)">编辑</el-button>
                <el-popconfirm title="确定删除该竞赛？" @confirm="handleDeleteCompetition(row)">
                  <template #reference>
                    <el-button size="small" type="danger">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-if="competitionTotal > 10"
            v-model:current-page="competitionPage"
            :total="competitionTotal"
            :page-size="10"
            layout="prev, pager, next"
            size="small"
            style="margin-top:12px;justify-content:flex-end;"
            @current-change="loadAllCompetitions"
          />
        </el-tab-pane>

        <!-- Tab 8: Teacher Management -->
        <el-tab-pane label="导师管理" name="teachers">
          <div class="section-header">
            <span>导师库</span>
            <el-button size="small" type="primary" @click="refreshTeachers">刷新</el-button>
          </div>
          <el-table :data="allTeachers" v-loading="teacherLoading" stripe size="small">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="department" label="院系" min-width="120" show-overflow-tooltip />
            <el-table-column prop="title" label="职称" width="100" />
            <el-table-column prop="researchFields" label="研究方向" min-width="140" show-overflow-tooltip />
            <el-table-column prop="currentTeams" label="指导团队数" width="100" />
            <el-table-column label="认证状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.isVerified === 1 ? 'success' : 'warning'" size="small">{{ row.isVerified === 1 ? '已认证' : '未认证' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="openEditTeacher(row)">编辑</el-button>
                <el-popconfirm title="确定删除该导师？" @confirm="handleDeleteTeacher(row)">
                  <template #reference>
                    <el-button size="small" type="danger">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-if="teacherTotal > 10"
            v-model:current-page="teacherPage"
            :total="teacherTotal"
            :page-size="10"
            layout="prev, pager, next"
            size="small"
            style="margin-top:12px;justify-content:flex-end;"
            @current-change="loadAllTeachers"
          />
        </el-tab-pane>

        <!-- Tab 9: Achievement Management -->
        <el-tab-pane label="成果展品管理" name="achievements">
          <div class="section-header">
            <span>所有成果</span>
            <el-button size="small" type="primary" @click="refreshAchievements">刷新</el-button>
          </div>
          <el-table :data="allAchievements" v-loading="achievementLoading" stripe size="small">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
            <el-table-column label="类型" width="100">
              <template #default="{ row }">
                <el-tag size="small">{{ achievementTypeLabel(row.type) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="获奖级别" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.awardLevel" :type="awardLevelType(row.awardLevel)" size="small">{{ awardLevelLabel(row.awardLevel) }}</el-tag>
                <span v-else style="color:#999">--</span>
              </template>
            </el-table-column>
            <el-table-column prop="projectId" label="项目ID" width="80" />
            <el-table-column prop="teamId" label="团队ID" width="80" />
            <el-table-column label="公开" width="70">
              <template #default="{ row }">
                <el-tag :type="row.isPublic === 1 ? 'success' : 'info'" size="small">{{ row.isPublic === 1 ? '是' : '否' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="精选" width="70">
              <template #default="{ row }">
                <el-switch
                  :model-value="row.isFeatured === 1"
                  @change="(val: boolean) => handleToggleAchievementFeatured(row, val)"
                  size="small"
                />
              </template>
            </el-table-column>
            <el-table-column label="浏览/点赞" width="100">
              <template #default="{ row }">
                {{ row.viewCount || 0 }} / {{ row.likeCount || 0 }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="openEditAchievement(row)">编辑</el-button>
                <el-popconfirm title="确定删除该成果？" @confirm="handleDeleteAchievement(row)">
                  <template #reference>
                    <el-button size="small" type="danger">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-if="achievementTotal > 10"
            v-model:current-page="achievementPage"
            :total="achievementTotal"
            :page-size="10"
            layout="prev, pager, next"
            size="small"
            style="margin-top:12px;justify-content:flex-end;"
            @current-change="loadAllAchievements"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- ===== Edit Dialogs ===== -->

    <!-- Project Edit Dialog -->
    <el-dialog v-model="projectEditVisible" title="编辑项目" width="500px">
      <el-form :model="projectEditForm" label-width="90px">
        <el-form-item label="项目名称">
          <el-input v-model="projectEditForm.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="projectEditForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="projectEditForm.status">
            <el-option label="规划中" value="PLANNING" />
            <el-option label="进行中" value="IN_PROGRESS" />
            <el-option label="审核中" value="REVIEW" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已归档" value="ARCHIVED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="projectEditVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProjectEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- Task Edit Dialog -->
    <el-dialog v-model="taskEditVisible" title="编辑任务" width="500px">
      <el-form :model="taskEditForm" label-width="90px">
        <el-form-item label="标题">
          <el-input v-model="taskEditForm.title" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="taskEditForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="taskEditForm.priority">
            <el-option label="高" value="HIGH" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="低" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="taskEditForm.status">
            <el-option label="待办" value="TODO" />
            <el-option label="进行中" value="DOING" />
            <el-option label="已完成" value="DONE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskEditVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTaskEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- Team Edit Dialog -->
    <el-dialog v-model="teamEditVisible" title="编辑团队" width="500px">
      <el-form :model="teamEditForm" label-width="90px">
        <el-form-item label="团队名称">
          <el-input v-model="teamEditForm.name" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="teamEditForm.status">
            <el-option label="正常" value="ACTIVE" />
            <el-option label="已解散" value="DISBANDED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="teamEditVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTeamEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- Competition Edit Dialog -->
    <el-dialog v-model="competitionEditVisible" title="编辑竞赛" width="600px">
      <el-form :model="competitionEditForm" label-width="90px">
        <el-form-item label="竞赛名称">
          <el-input v-model="competitionEditForm.name" />
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="competitionEditForm.level">
            <el-option label="国家级" value="国家级" />
            <el-option label="省级" value="省级" />
            <el-option label="市级" value="市级" />
            <el-option label="校级" value="校级" />
          </el-select>
        </el-form-item>
        <el-form-item label="类别">
          <el-input v-model="competitionEditForm.category" />
        </el-form-item>
        <el-form-item label="主办方">
          <el-input v-model="competitionEditForm.organizer" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="competitionEditForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="competitionEditForm.status">
            <el-option label="报名中" value="REGISTERING" />
            <el-option label="进行中" value="ONGOING" />
            <el-option label="已结束" value="ENDED" />
            <el-option label="已关闭" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item label="官网链接">
          <el-input v-model="competitionEditForm.officialUrl" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="competitionEditVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCompetitionEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- Teacher Edit Dialog -->
    <el-dialog v-model="teacherEditVisible" title="编辑导师" width="500px">
      <el-form :model="teacherEditForm" label-width="90px">
        <el-form-item label="院系">
          <el-input v-model="teacherEditForm.department" />
        </el-form-item>
        <el-form-item label="职称">
          <el-input v-model="teacherEditForm.title" />
        </el-form-item>
        <el-form-item label="研究方向">
          <el-input v-model="teacherEditForm.researchFields" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="认证状态">
          <el-switch
            :model-value="teacherEditForm.isVerified === 1"
            @change="(val: boolean) => teacherEditForm.isVerified = val ? 1 : 0"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="teacherEditVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTeacherEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- Achievement Edit Dialog -->
    <el-dialog v-model="achievementEditVisible" title="编辑成果" width="500px">
      <el-form :model="achievementEditForm" label-width="90px">
        <el-form-item label="标题">
          <el-input v-model="achievementEditForm.title" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="achievementEditForm.type">
            <el-option label="论文" value="PAPER" />
            <el-option label="专利" value="PATENT" />
            <el-option label="竞赛获奖" value="AWARD" />
            <el-option label="软件著作" value="SOFTWARE" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="获奖级别">
          <el-select v-model="achievementEditForm.awardLevel" clearable placeholder="无">
            <el-option label="国家级" value="NATIONAL" />
            <el-option label="省级" value="PROVINCIAL" />
            <el-option label="校级" value="SCHOOL" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="achievementEditForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="公开">
          <el-switch
            :model-value="achievementEditForm.isPublic === 1"
            @change="(val: boolean) => achievementEditForm.isPublic = val ? 1 : 0"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="achievementEditVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAchievementEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import request from '@/api/request'
import { adminListCompetitions, adminUpdateCompetition, adminDeleteCompetition } from '@/api/competition'
import { adminListAchievements, adminUpdateAchievement, adminDeleteAchievement } from '@/api/achievement'

const activeTab = ref('recruit')

// Stats
const stats = ref<Record<string, number>>({})

// Users
const users = ref<any[]>([])
const userLoading = ref(false)
const userSearch = ref('')

// Recruit Audit
const pendingRecruits = ref<any[]>([])
const recruitLoading = ref(false)
const publisherNames = ref<Record<number, string>>({})

// Audit Log
const auditTab = ref('pending')
const auditList = ref<any[]>([])
const auditListLoading = ref(false)

// Projects
const allProjects = ref<any[]>([])
const projectLoading = ref(false)
const projectPage = ref(1)
const projectTotal = ref(0)

// Tasks (按项目分组管理)
const taskProjects = ref<any[]>([])
const taskProjectLoading = ref(false)
const selectedTaskProject = ref<any>(null)
const allTasks = ref<any[]>([])
const filteredTasks = ref<any[]>([])
const taskLoading = ref(false)
const taskFilterStatus = ref('')
const taskFilterPriority = ref('')

// Teams
const allTeams = ref<any[]>([])
const teamLoading = ref(false)
const teamPage = ref(1)
const teamTotal = ref(0)

// Teachers
const allTeachers = ref<any[]>([])
const teacherLoading = ref(false)
const teacherPage = ref(1)
const teacherTotal = ref(0)

// Competitions
const allCompetitions = ref<any[]>([])
const competitionLoading = ref(false)
const competitionPage = ref(1)
const competitionTotal = ref(0)

// Achievements
const allAchievements = ref<any[]>([])
const achievementLoading = ref(false)
const achievementPage = ref(1)
const achievementTotal = ref(0)

// --- Edit Dialog States ---

// Project Edit
const projectEditVisible = ref(false)
const projectEditForm = ref<any>({})
const editingProjectId = ref<number | null>(null)

// Task Edit
const taskEditVisible = ref(false)
const taskEditForm = ref<any>({})
const editingTaskId = ref<number | null>(null)

// Team Edit
const teamEditVisible = ref(false)
const teamEditForm = ref<any>({})
const editingTeamId = ref<number | null>(null)

// Competition Edit
const competitionEditVisible = ref(false)
const competitionEditForm = ref<any>({})
const editingCompetitionId = ref<number | null>(null)

// Teacher Edit
const teacherEditVisible = ref(false)
const teacherEditForm = ref<any>({})
const editingTeacherId = ref<number | null>(null)

// Achievement Edit
const achievementEditVisible = ref(false)
const achievementEditForm = ref<any>({})
const editingAchievementId = ref<number | null>(null)

onMounted(async () => {
  await loadStats()
  await loadPendingRecruits()
  await loadUsers()
  await loadAuditList()
})

watch(activeTab, async (tab) => {
  if (tab === 'recruit') await loadPendingRecruits()
  if (tab === 'users') await loadUsers()
  if (tab === 'auditLog') await loadAuditList()
  if (tab === 'projects') await loadAllProjects()
  if (tab === 'tasks') await loadTaskProjects()
  if (tab === 'teams') await loadAllTeams()
  if (tab === 'competitions') await loadAllCompetitions()
  if (tab === 'teachers') await loadAllTeachers()
  if (tab === 'achievements') await loadAllAchievements()
})

watch(auditTab, () => {
  loadAuditList()
})

async function loadStats() {
  try {
    const res = await request.get('/admin/dashboard/stats')
    stats.value = res.data || {}
  } catch (_) {
    stats.value = {}
  }
}

// ---- Recruit Audit ----

async function loadPendingRecruits() {
  recruitLoading.value = true
  try {
    const res = await request.get('/admin/recruits/pending', { params: { page: 1, size: 20 } })
    const data = res.data
    const records = data?.records || []
    pendingRecruits.value = records

    // Fetch publisher names
    if (records.length > 0) {
      const names: Record<number, string> = {}
      await Promise.all(records.map(async (r: any) => {
        try {
          const userRes = await request.get(`/user/profile/${r.publisherId}`)
          names[r.id] = userRes.data?.username || '--'
        } catch {
          names[r.id] = '--'
        }
      }))
      publisherNames.value = names
    }
  } catch (_) {
    pendingRecruits.value = []
  } finally {
    recruitLoading.value = false
  }
}

async function handleApproveRecruit(row: any) {
  try {
    await request.put(`/admin/recruits/${row.id}/audit`, null, { params: { action: 'APPROVE' } })
    ElMessage.success('招募已通过审核')
    await loadPendingRecruits()
    await loadStats()
  } catch (_) {}
}

async function handleRejectRecruit(row: any) {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入驳回原因', '驳回招募', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputValidator: (val: string) => !!val || '请填写驳回原因',
    })
    await request.put(`/admin/recruits/${row.id}/audit`, null, { params: { action: 'REJECT', reason } })
    ElMessage.success('招募已驳回')
    await loadPendingRecruits()
    await loadStats()
  } catch (_) {}
}

// ---- User Management ----

async function loadUsers() {
  userLoading.value = true
  try {
    const params: any = { page: 1, size: 50 }
    if (userSearch.value) params.keyword = userSearch.value
    const res = await request.get('/admin/users', { params })
    const data = res.data
    users.value = data?.records || []
  } catch (_) {
    users.value = []
  } finally {
    userLoading.value = false
  }
}

async function handleToggleUserStatus(row: any, val: boolean) {
  row._toggleLoading = true
  try {
    await request.put(`/admin/users/${row.id}/status`, null, { params: { status: val ? 1 : 0 } })
    row.status = val ? 1 : 0
    ElMessage.success('状态已更新')
  } catch (_) {
  } finally {
    row._toggleLoading = false
  }
}

async function handleToggleReviewer(row: any, val: boolean) {
  try {
    const role = val ? 'REVIEWER' : 'USER'
    await request.put(`/admin/users/${row.id}/role`, null, { params: { role } })
    row.role = role
    ElMessage.success(val ? '已授予审核权限' : '已撤销审核权限')
  } catch (_) {}
}

function roleType(role: string) {
  if (role === 'ADMIN') return 'danger'
  if (role === 'REVIEWER') return 'warning'
  if (role === 'TEACHER') return 'success'
  return 'primary'
}

function auditTypeLabel(type: string) {
  const map: Record<string, string> = {
    RECRUIT: '招募审核',
    TEAM: '团队审核',
    ACHIEVEMENT: '成果审核',
  }
  return map[type] || type || '--'
}

function roleLabel(role: string) {
  const map: Record<string, string> = {
    ADMIN: '管理员',
    REVIEWER: '审核员',
    TEACHER: '导师',
    USER: '学生',
  }
  return map[role] || role
}

// ---- Audit Log ----

async function loadAuditList() {
  auditListLoading.value = true
  try {
    const params: any = {}
    if (auditTab.value === 'done') params.status = 'APPROVED,REJECTED'
    else params.status = 'PENDING'
    const res = await request.get('/admin/audit/list', { params })
    const data = res.data
    auditList.value = data?.records || []
  } catch (_) {
    auditList.value = []
  } finally {
    auditListLoading.value = false
  }
}

async function handleApprove(row: any) {
  try {
    await request.put(`/admin/audit/${row.id}`, null, { params: { action: 'APPROVE' } })
    ElMessage.success('已通过')
    await loadAuditList()
    await loadStats()
  } catch (_) {}
}

async function handleReject(row: any) {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入驳回原因', '驳回', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
    })
    await request.put(`/admin/audit/${row.id}`, null, { params: { action: 'REJECT', reason } })
    ElMessage.success('已驳回')
    await loadAuditList()
    await loadStats()
  } catch (_) {}
}

// ---- Project Management ----

async function loadAllProjects(page = 1) {
  projectLoading.value = true
  try {
    const res = await request.get('/admin/projects', { params: { page, size: 10 } })
    const data = res.data
    allProjects.value = data?.records || []
    projectTotal.value = data?.total || 0
    projectPage.value = page
  } catch (_) {
    allProjects.value = []
  } finally {
    projectLoading.value = false
  }
}

function projectStatusType(s: string) {
  const map: Record<string, string> = { PLANNING: 'info', IN_PROGRESS: 'primary', REVIEW: 'warning', COMPLETED: 'success', ARCHIVED: 'info' }
  return map[s] || 'info'
}
function projectStatusLabel(s: string) {
  const map: Record<string, string> = { PLANNING: '规划中', IN_PROGRESS: '进行中', REVIEW: '审核中', COMPLETED: '已完成', ARCHIVED: '已归档' }
  return map[s] || s
}

function openEditProject(row: any) {
  editingProjectId.value = row.id
  projectEditForm.value = {
    name: row.name || '',
    description: row.description || '',
    status: row.status || 'PLANNING',
  }
  projectEditVisible.value = true
}

async function saveProjectEdit() {
  try {
    await request.put(`/admin/projects/${editingProjectId.value}`, projectEditForm.value)
    ElMessage.success('项目已更新')
    projectEditVisible.value = false
    await loadAllProjects(projectPage.value)
  } catch (_) {}
}

async function handleDeleteProject(row: any) {
  try {
    await request.delete(`/admin/projects/${row.id}`)
    ElMessage.success('项目已删除')
    await loadAllProjects(projectPage.value)
    await loadStats()
  } catch (_) {}
}

function refreshProjects() {
  loadAllProjects(1)
}

// ---- Task Management (按项目分组) ----

async function loadTaskProjects() {
  taskProjectLoading.value = true
  try {
    const res = await request.get('/admin/projects', { params: { page: 1, size: 200 } })
    const data = res.data
    taskProjects.value = data?.records || []
  } catch (_) {
    taskProjects.value = []
  } finally {
    taskProjectLoading.value = false
  }
}

async function selectTaskProject(proj: any) {
  selectedTaskProject.value = proj
  taskFilterStatus.value = ''
  taskFilterPriority.value = ''
  taskLoading.value = true
  try {
    // 使用 /api/task/list?projectId=X 按项目获取任务
    const res = await request.get('/task/list', { params: { projectId: proj.id } })
    allTasks.value = res.data || []
    applyTaskFilter()
  } catch (_) {
    allTasks.value = []
    filteredTasks.value = []
  } finally {
    taskLoading.value = false
  }
}

function applyTaskFilter() {
  let list = [...allTasks.value]
  if (taskFilterStatus.value) {
    list = list.filter((t: any) => t.status === taskFilterStatus.value)
  }
  if (taskFilterPriority.value) {
    list = list.filter((t: any) => t.priority === taskFilterPriority.value)
  }
  filteredTasks.value = list
}

function refreshTaskProjects() {
  selectedTaskProject.value = null
  allTasks.value = []
  filteredTasks.value = []
  loadTaskProjects()
}

function priorityType(p: string) {
  const map: Record<string, string> = { HIGH: 'danger', MEDIUM: 'warning', LOW: 'info' }
  return map[p] || 'info'
}
function priorityLabel(p: string) {
  const map: Record<string, string> = { HIGH: '高', MEDIUM: '中', LOW: '低' }
  return map[p] || p
}
function taskStatusType(s: string) {
  const map: Record<string, string> = { TODO: 'info', DOING: 'warning', DONE: 'success' }
  return map[s] || 'info'
}
function taskStatusLabel(s: string) {
  const map: Record<string, string> = { TODO: '待办', DOING: '进行中', DONE: '已完成' }
  return map[s] || s
}

function openEditTask(row: any) {
  editingTaskId.value = row.id
  taskEditForm.value = {
    title: row.title || '',
    description: row.description || '',
    priority: row.priority || 'MEDIUM',
    status: row.status || 'TODO',
  }
  taskEditVisible.value = true
}

async function saveTaskEdit() {
  try {
    await request.put(`/admin/tasks/${editingTaskId.value}`, taskEditForm.value)
    ElMessage.success('任务已更新')
    taskEditVisible.value = false
    // 刷新当前选中项目的任务
    if (selectedTaskProject.value) {
      await selectTaskProject(selectedTaskProject.value)
    }
  } catch (_) {}
}

async function handleDeleteTask(row: any) {
  try {
    await request.delete(`/admin/tasks/${row.id}`)
    ElMessage.success('任务已删除')
    if (selectedTaskProject.value) {
      await selectTaskProject(selectedTaskProject.value)
    }
  } catch (_) {}
}

// ---- Team Management ----

async function loadAllTeams(page = 1) {
  teamLoading.value = true
  try {
    const res = await request.get('/admin/teams', { params: { page, size: 10 } })
    const data = res.data
    allTeams.value = data?.records || []
    teamTotal.value = data?.total || 0
    teamPage.value = page
  } catch (_) {
    allTeams.value = []
  } finally {
    teamLoading.value = false
  }
}

function openEditTeam(row: any) {
  editingTeamId.value = row.id
  teamEditForm.value = {
    name: row.name || '',
    status: row.status || 'ACTIVE',
  }
  teamEditVisible.value = true
}

async function saveTeamEdit() {
  try {
    await request.put(`/admin/teams/${editingTeamId.value}`, teamEditForm.value)
    ElMessage.success('团队已更新')
    teamEditVisible.value = false
    await loadAllTeams(teamPage.value)
  } catch (_) {}
}

async function handleDeleteTeam(row: any) {
  try {
    await request.delete(`/admin/teams/${row.id}`)
    ElMessage.success('团队已删除')
    await loadAllTeams(teamPage.value)
    await loadStats()
  } catch (_) {}
}

function refreshTeams() {
  loadAllTeams(1)
}

// ---- Teacher Management ----

async function loadAllTeachers(page = 1) {
  teacherLoading.value = true
  try {
    const res = await request.get('/admin/teachers', { params: { page, size: 10 } })
    const data = res.data
    allTeachers.value = data?.records || []
    teacherTotal.value = data?.total || 0
    teacherPage.value = page
  } catch (_) {
    allTeachers.value = []
  } finally {
    teacherLoading.value = false
  }
}

function openEditTeacher(row: any) {
  editingTeacherId.value = row.id
  teacherEditForm.value = {
    department: row.department || '',
    title: row.title || '',
    researchFields: row.researchFields || '',
    isVerified: row.isVerified ?? 0,
  }
  teacherEditVisible.value = true
}

async function saveTeacherEdit() {
  try {
    await request.put(`/admin/teachers/${editingTeacherId.value}`, teacherEditForm.value)
    ElMessage.success('导师已更新')
    teacherEditVisible.value = false
    await loadAllTeachers(teacherPage.value)
  } catch (_) {}
}

async function handleDeleteTeacher(row: any) {
  try {
    await request.delete(`/admin/teachers/${row.id}`)
    ElMessage.success('导师已删除')
    await loadAllTeachers(teacherPage.value)
  } catch (_) {}
}

function refreshTeachers() {
  loadAllTeachers(1)
}

// ---- Competition Management ----

async function loadAllCompetitions(page = 1) {
  competitionLoading.value = true
  try {
    const res = await adminListCompetitions({ page, size: 10 })
    const data = res.data
    allCompetitions.value = data?.records || []
    competitionTotal.value = data?.total || 0
    competitionPage.value = page
  } catch (_) {
    allCompetitions.value = []
  } finally {
    competitionLoading.value = false
  }
}

function competitionStatusType(s: string) {
  const map: Record<string, string> = { REGISTERING: 'success', ONGOING: 'primary', ENDED: 'info', CLOSED: 'danger' }
  return map[s] || 'info'
}
function competitionStatusLabel(s: string) {
  const map: Record<string, string> = { REGISTERING: '报名中', ONGOING: '进行中', ENDED: '已结束', CLOSED: '已关闭' }
  return map[s] || s
}

function openEditCompetition(row: any) {
  editingCompetitionId.value = row.id
  competitionEditForm.value = {
    name: row.name || '',
    level: row.level || '',
    category: row.category || '',
    organizer: row.organizer || '',
    description: row.description || '',
    status: row.status || 'REGISTERING',
    officialUrl: row.officialUrl || '',
  }
  competitionEditVisible.value = true
}

async function saveCompetitionEdit() {
  try {
    await adminUpdateCompetition(editingCompetitionId.value!, competitionEditForm.value)
    ElMessage.success('竞赛已更新')
    competitionEditVisible.value = false
    await loadAllCompetitions(competitionPage.value)
  } catch (_) {}
}

async function handleDeleteCompetition(row: any) {
  try {
    await adminDeleteCompetition(row.id)
    ElMessage.success('竞赛已删除')
    await loadAllCompetitions(competitionPage.value)
    await loadStats()
  } catch (_) {}
}

function refreshCompetitions() {
  loadAllCompetitions(1)
}

// ---- Achievement Management ----

async function loadAllAchievements(page = 1) {
  achievementLoading.value = true
  try {
    const res = await adminListAchievements({ page, size: 10 })
    const data = res.data
    allAchievements.value = data?.records || []
    achievementTotal.value = data?.total || 0
    achievementPage.value = page
  } catch (_) {
    allAchievements.value = []
  } finally {
    achievementLoading.value = false
  }
}

function achievementTypeLabel(t: string) {
  const map: Record<string, string> = { PAPER: '论文', PATENT: '专利', AWARD: '竞赛获奖', SOFTWARE: '软件著作', OTHER: '其他', SHARE: '分享' }
  return map[t] || t || '--'
}

function awardLevelType(l: string) {
  const map: Record<string, string> = { NATIONAL: 'danger', PROVINCIAL: 'warning', SCHOOL: 'primary' }
  return map[l] || 'info'
}
function awardLevelLabel(l: string) {
  const map: Record<string, string> = { NATIONAL: '国家级', PROVINCIAL: '省级', SCHOOL: '校级' }
  return map[l] || l
}

async function handleToggleAchievementFeatured(row: any, val: boolean) {
  try {
    await adminUpdateAchievement(row.id, { isFeatured: val ? 1 : 0 })
    row.isFeatured = val ? 1 : 0
    ElMessage.success(val ? '已设为精选' : '已取消精选')
  } catch (_) {}
}

function openEditAchievement(row: any) {
  editingAchievementId.value = row.id
  achievementEditForm.value = {
    title: row.title || '',
    type: row.type || '',
    awardLevel: row.awardLevel || '',
    description: row.description || '',
    isPublic: row.isPublic ?? 1,
  }
  achievementEditVisible.value = true
}

async function saveAchievementEdit() {
  try {
    await adminUpdateAchievement(editingAchievementId.value!, achievementEditForm.value)
    ElMessage.success('成果已更新')
    achievementEditVisible.value = false
    await loadAllAchievements(achievementPage.value)
  } catch (_) {}
}

async function handleDeleteAchievement(row: any) {
  try {
    await adminDeleteAchievement(row.id)
    ElMessage.success('成果已删除')
    await loadAllAchievements(achievementPage.value)
    await loadStats()
  } catch (_) {}
}

function refreshAchievements() {
  loadAllAchievements(1)
}
</script>

<style scoped>
.admin-dashboard {
  padding: 0;
}
.page-title {
  font-size: 20px;
  color: #303133;
  margin-bottom: 20px;
}
.stats-row {
  margin-bottom: 16px;
}
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
}
.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-info {
  display: flex;
  flex-direction: column;
}
.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 2px;
}
.section-card {
  margin-bottom: 16px;
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

/* 任务管理 - 项目选择器 */
.project-select-item {
  padding: 12px 14px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background 0.2s;
}
.project-select-item:hover {
  background: #f5f7fa;
}
.project-select-item.active {
  background: #ecf5ff;
  border-left: 3px solid #409eff;
}
.project-select-name {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.project-select-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
