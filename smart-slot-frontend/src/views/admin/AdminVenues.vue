<template>
  <div class="admin-venues-page">
    <div class="page-header">
      <div>
        <h2 class="title">场地配置管理</h2>
        <p class="subtitle">配置场馆开放场地、多规格时段收费单价与设施信息</p>
      </div>
      <el-button type="primary" @click="openAddDialog">
        <el-icon><Plus /></el-icon> 新增场地
      </el-button>
    </div>

    <!-- 筛选搜索栏 -->
    <el-card shadow="never" class="filter-card card-shadow">
      <el-form :inline="true" class="demo-form-inline">
        <el-form-item label="分类筛选">
          <el-select v-model="filterCategoryId" placeholder="全部分类" clearable style="width: 160px;">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="场地名称">
          <el-input v-model="filterKeyword" placeholder="输入名称关键词" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchVenues">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 分页表格 (MyBatis-Plus 分页插件) -->
    <el-card shadow="never" class="table-card card-shadow">
      <el-table :data="venueList" v-loading="loading" style="width: 100%" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="封面" width="100">
          <template #default="{ row }">
            <el-image 
              :src="row.coverImage" 
              style="width: 64px; height: 42px; border-radius: 6px;" 
              fit="cover" 
            />
          </template>
        </el-table-column>
        <el-table-column prop="name" label="场地名称" min-width="150" />
        <el-table-column prop="categoryName" label="所属分类" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ row.categoryName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pricePerHour" label="时段单价" width="120">
          <template #default="{ row }">
            <span class="price-text">￥{{ row.pricePerHour }}/h</span>
          </template>
        </el-table-column>
        <el-table-column label="每日营业时段" width="140">
          <template #default="{ row }">
            {{ row.openTime }} ~ {{ row.closeTime }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="开放状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常开放' : '检修维护' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="openEditDialog(row)">编辑</el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页组件 -->
      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[5, 10, 20]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchVenuePage"
          @current-change="fetchVenuePage"
        />
      </div>
    </el-card>

    <!-- 场地新增/编辑表单对话框 (复杂表单交互) -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑场地信息' : '新增场馆场地'"
      width="560px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="所属分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择运动/空间分类" style="width: 100%;">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="场地名称" prop="name">
          <el-input v-model="form.name" placeholder="如: 羽毛球 5 号场 (专业地胶)" />
        </el-form-item>
        <el-form-item label="时段单价" prop="pricePerHour">
          <el-input-number v-model="form.pricePerHour" :precision="2" :step="10" :min="1" />
          <span style="margin-left: 8px; color: #64748b;">元 / 小时</span>
        </el-form-item>
        <el-form-item label="容纳人数" prop="capacity">
          <el-input-number v-model="form.capacity" :min="1" :max="100" />
        </el-form-item>
        <el-form-item label="开放时间" required>
          <el-col :span="11">
            <el-input v-model="form.openTime" placeholder="例如: 09:00" />
          </el-col>
          <el-col :span="2" style="text-align: center;">至</el-col>
          <el-col :span="11">
            <el-input v-model="form.closeTime" placeholder="例如: 22:00" />
          </el-col>
        </el-form-item>
        <el-form-item label="配套设施" prop="facilities">
          <el-input v-model="form.facilities" placeholder="逗号分隔，如: 专业防滑地胶,空调,独立更衣室" />
        </el-form-item>
        <el-form-item label="封面图片URL">
          <el-input v-model="form.coverImage" placeholder="图片 URL 地址" />
        </el-form-item>
        <el-form-item label="场地介绍">
          <el-input v-model="form.description" type="textarea" rows="3" placeholder="场地的特色、灯光或注意事项" />
        </el-form-item>
        <el-form-item label="场地状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常开放预约</el-radio>
            <el-radio :value="0">暂停开放检修</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { getAdminVenuesPage, saveVenue, deleteVenue, getCategories } from '@/api/venue'
import { ElMessage, ElMessageBox } from 'element-plus'

const categories = ref([])
const venueList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const filterCategoryId = ref(null)
const filterKeyword = ref('')
const loading = ref(false)

const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  categoryId: null,
  name: '',
  pricePerHour: 50.00,
  capacity: 4,
  openTime: '09:00',
  closeTime: '22:00',
  facilities: '专业防滑地胶,空调,Wi-Fi',
  coverImage: 'https://images.unsplash.com/photo-1521537634581-0dced2fee2ef?w=800&auto=format&fit=crop&q=60',
  description: '',
  status: 1
})

const rules = {
  categoryId: [{ required: true, message: '请选择场地分类', trigger: 'change' }],
  name: [{ required: true, message: '请输入场地名称', trigger: 'blur' }],
  pricePerHour: [{ required: true, message: '请输入每小时单价', trigger: 'blur' }],
  capacity: [{ required: true, message: '请输入容纳人数', trigger: 'blur' }]
}

async function loadCategories() {
  try {
    categories.value = await getCategories()
  } catch (e) {
    console.error(e)
  }
}

async function fetchVenuePage() {
  loading.value = true
  try {
    const res = await getAdminVenuesPage({
      current: currentPage.value,
      size: pageSize.value,
      categoryId: filterCategoryId.value,
      keyword: filterKeyword.value
    })
    venueList.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function searchVenues() {
  currentPage.value = 1
  fetchVenuePage()
}

function resetSearch() {
  filterCategoryId.value = null
  filterKeyword.value = ''
  searchVenues()
}

function openAddDialog() {
  isEdit.value = false
  form.id = null
  form.categoryId = categories.value[0]?.id || null
  form.name = ''
  form.pricePerHour = 50.00
  form.capacity = 4
  form.openTime = '09:00'
  form.closeTime = '22:00'
  form.facilities = '专业防滑地胶,空调,免费热水'
  form.coverImage = 'https://images.unsplash.com/photo-1521537634581-0dced2fee2ef?w=800&auto=format&fit=crop&q=60'
  form.description = ''
  form.status = 1
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

async function handleSave() {
  if (!formRef.value) return
  await formRef.value.validate()

  saving.value = true
  try {
    await saveVenue(form)
    ElMessage.success('场地信息保存成功！')
    dialogVisible.value = false
    fetchVenuePage()
  } catch (e) {
    console.error(e)
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认要删除场地「${row.name}」吗？此操作不可逆。`, '删除警告', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消'
    })
    await deleteVenue(row.id)
    ElMessage.success('删除成功')
    fetchVenuePage()
  } catch (e) {
    // canceled
  }
}

onMounted(() => {
  loadCategories()
  fetchVenuePage()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.title {
  font-size: 20px;
  font-weight: 800;
  color: #0f172a;
}

.subtitle {
  font-size: 13px;
  color: #64748b;
  margin-top: 4px;
}

.filter-card {
  margin-bottom: 20px;
  border-radius: 12px;
  padding: 8px 16px;
}

.table-card {
  border-radius: 12px;
  padding: 8px 16px 20px;
}

.price-text {
  font-weight: 700;
  color: #4f46e5;
}

.pagination-bar {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
