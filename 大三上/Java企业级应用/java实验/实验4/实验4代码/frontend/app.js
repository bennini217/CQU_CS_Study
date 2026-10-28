const { createApp, reactive, ref, computed, onMounted, nextTick, watch } = Vue;
const { ElMessage, ElMessageBox } = ElementPlus;

// API基础URL
const API_BASE = 'http://localhost:8080/api';

// API请求工具函数
const api = {
    async get(url) {
        try {
            const response = await fetch(API_BASE + url, {
                method: 'GET',
                headers: { 'Content-Type': 'application/json' }
            });
            
            if (!response.ok) {
                throw new Error(`HTTP错误: ${response.status}`);
            }
            
            const result = await response.json();
            if (result.code === 200) {
                return result.data;
            } else {
                throw new Error(result.msg || '请求失败');
            }
        } catch (error) {
            if (error.name === 'TypeError' && error.message.includes('fetch')) {
                throw new Error('无法连接到服务器，请检查后端服务是否启动');
            }
            throw error;
        }
    },
    async post(url, data) {
        try {
            const response = await fetch(API_BASE + url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });
            
            if (!response.ok) {
                throw new Error(`HTTP错误: ${response.status}`);
            }
            
            const result = await response.json();
            if (result.code === 200) {
                return result.data;
            } else {
                throw new Error(result.msg || '请求失败');
            }
        } catch (error) {
            if (error.name === 'TypeError' && error.message.includes('fetch')) {
                throw new Error('无法连接到服务器，请检查后端服务是否启动');
            }
            throw error;
        }
    }
};

// ==========================================
// 1. 创新组件：AI 智能助教 (AI Assistant)
// ==========================================
const AIAssistant = {
    props: ['user', 'db'],
    template: `
        <div>
            <div class="ai-float-btn" @click="visible = true" title="呼叫 AI 助教">
                <el-icon class="is-loading" v-if="thinking"><Loading /></el-icon>
                <el-icon v-else><Microphone /></el-icon>
            </div>

            <el-dialog v-model="visible" :title="'🤖 AI 智能助教 - 服务对象: ' + user.name" width="450px" custom-class="ai-dialog">
                <div class="chat-box" ref="chatBoxRef">
                    <div v-for="(msg, index) in messages" :key="index" :class="['message', msg.role]">
                        <div class="avatar">{{ msg.role === 'ai' ? '🤖' : '👤' }}</div>
                        <div class="content">{{ msg.text }}</div>
                    </div>
                </div>
                <div class="input-area">
                    <el-input v-model="input" placeholder="输入问题 (如: 挂科了吗)" @keyup.enter="sendMessage">
                        <template #append><el-button @click="sendMessage">发送</el-button></template>
                    </el-input>
                </div>
                <div class="quick-tags" style="margin-top:10px;">
                    <el-tag size="small" @click="quickAsk('我的平均分是多少？')">平均分</el-tag>
                    <el-tag size="small" @click="quickAsk('我这学期挂科了吗？')">挂科查询</el-tag>
                    <el-tag size="small" @click="quickAsk('帮我生成学习建议')">学习建议</el-tag>
                </div>
            </el-dialog>
        </div>
    `,
    setup(props) {
        const visible = ref(false);
        const input = ref('');
        const thinking = ref(false);
        const chatBoxRef = ref(null);
        const messages = reactive([{ role: 'ai', text: `你好，${props.user.name}！我是你的专属 AI 助教。` }]);

        const scrollToBottom = () => nextTick(() => { if (chatBoxRef.value) chatBoxRef.value.scrollTop = chatBoxRef.value.scrollHeight; });
        const quickAsk = (text) => { input.value = text; sendMessage(); };

        const sendMessage = async () => {
            if (!input.value.trim()) return;
            const text = input.value;
            messages.push({ role: 'user', text });
            input.value = ''; thinking.value = true; scrollToBottom();

            try {
                const reply = await api.post('/ai/chat', {
                    question: text,
                    studentId: props.user.id
                });
                messages.push({ role: 'ai', text: reply });
            } catch (error) {
                messages.push({ role: 'ai', text: '抱歉，我遇到了一些问题，请稍后再试。' });
            } finally {
                thinking.value = false; scrollToBottom();
            }
        };
        return { visible, input, messages, sendMessage, thinking, chatBoxRef, quickAsk };
    }
};

// ==========================================
// 2. 核心组件：仪表盘 (Dashboard)
// ==========================================
const Dashboard = {
    props: ['user', 'db'],
    template: `
        <div>
            <div style="margin-bottom: 20px; display:flex; justify-content:space-between; align-items:center;">
                <div>
                    <h2 style="margin:0;">早安，{{ user.name }} {{ roleText }}！☀️</h2>
                    <p style="color:#909399; margin:5px 0;">今天是 {{ currentDate }}，祝你度过愉快的一天。</p>
                </div>
                <el-button type="primary" plain size="small" @click="toggleFullScreen">
                    <el-icon><FullScreen /></el-icon> 沉浸模式
                </el-button>
            </div>

            <el-row :gutter="20">
                <el-col :span="6" v-for="card in statsCards" :key="card.title">
                    <el-card shadow="hover" class="stat-card">
                        <div class="stat-icon" :style="{ background: card.color }">
                            <el-icon style="color:white; font-size:24px;"><component :is="card.icon" /></el-icon>
                        </div>
                        <div class="stat-info">
                            <div class="stat-value">{{ card.value }}</div>
                            <div class="stat-label">{{ card.title }}</div>
                        </div>
                    </el-card>
                </el-col>
            </el-row>

            <el-row :gutter="20" style="margin-top: 20px;">
                <el-col :span="14">
                    <el-card shadow="always">
                        <template #header><span>{{ roleText === '同学' ? '成绩走势分析' : '教学班级及格率' }}</span></template>
                        <div id="mainChart" style="height: 350px;"></div>
                    </el-card>
                </el-col>
                <el-col :span="10">
                    <el-card shadow="always">
                        <template #header><span>系统公告</span></template>
                        <el-timeline style="padding-left: 0;">
                            <el-timeline-item timestamp="2025/11/01" placement="top" type="primary">
                                <el-card><h4>期中考试成绩已发布</h4><p>请各位同学及时查询。</p></el-card>
                            </el-timeline-item>
                            <el-timeline-item timestamp="2025/10/28" placement="top" color="#0bbd87">
                                <el-card><h4>AI 助教上线</h4><p>点击右下角悬浮球体验智能问答。</p></el-card>
                            </el-timeline-item>
                        </el-timeline>
                    </el-card>
                </el-col>
            </el-row>
        </div>
    `,
    setup(props) {
        const currentDate = new Date().toLocaleDateString();
        const roleText = computed(() => props.user.id.startsWith('S') ? '同学' : '老师');
        
        const statsCards = ref([]);
        
        const loadDashboardData = async () => {
            try {
                if (roleText.value === '同学') {
                    const data = await api.get(`/dashboard/student?studentId=${props.user.id}`);
                    statsCards.value = [
                        { title: '已修课程', value: data.totalCourses, icon: 'Notebook', color: '#409EFF' },
                        { title: '平均成绩', value: data.avgScore, icon: 'DataLine', color: '#67C23A' },
                        { title: '挂科预警', value: data.failCount, icon: 'Warning', color: '#F56C6C' },
                        { title: `全校排名 (前${data.percentage}%)`, value: `第 ${data.rank} 名`, icon: 'Trophy', color: '#E6A23C' }
                    ];
                } else {
                    const data = await api.get(`/dashboard/teacher?teacherId=${props.user.id}`);
                    statsCards.value = [
                        { title: '执教班级', value: data.classCount, icon: 'School', color: '#409EFF' },
                        { title: '学生总数', value: data.studentCount, icon: 'UserFilled', color: '#67C23A' },
                        { title: '待批改', value: data.pendingCount, icon: 'EditPen', color: '#E6A23C' },
                        { title: '不及格人次', value: data.failCount, icon: 'Warning', color: '#F56C6C' }
                    ];
                }
            } catch (error) {
                console.error('加载仪表盘数据失败:', error);
                ElMessage.error('加载数据失败');
            }
        };

        const chartData = ref({ semesterNames: [], avgScores: [] });
        const teacherChartData = ref({ excellent: 0, good: 0, pass: 0, fail: 0 });
        
        const loadChartData = async () => {
            try {
                if (roleText.value === '同学') {
                    const data = await api.get(`/dashboard/student/trend?studentId=${props.user.id}`);
                    chartData.value = data || { semesterNames: [], avgScores: [] };
                } else {
                    const data = await api.get(`/dashboard/teacher/pass-rate?teacherId=${props.user.id}`);
                    teacherChartData.value = data || { excellent: 0, good: 0, pass: 0, fail: 0 };
                }
            } catch (error) {
                console.error('加载图表数据失败:', error);
            }
        };

        const initChart = () => {
            const chartDom = document.getElementById('mainChart');
            if (!chartDom) return;
            echarts.dispose(chartDom);
            const myChart = echarts.init(chartDom);
            
            let option;
            if (roleText.value === '同学') {
                // 使用实际数据 - 学期平均成绩
                const semesterNames = chartData.value.semesterNames || [];
                const avgScores = chartData.value.avgScores || [];
                
                // 如果没有数据，显示提示
                if (semesterNames.length === 0) {
                    option = {
                        title: {
                            text: '暂无成绩数据',
                            left: 'center',
                            top: 'center',
                            textStyle: { color: '#999', fontSize: 16 }
                        }
                    };
                } else {
                    option = {
                        tooltip: { 
                            trigger: 'axis',
                            formatter: function(params) {
                                return params[0].name + '<br/>' + 
                                       params[0].seriesName + ': ' + params[0].value.toFixed(2) + '分';
                            }
                        },
                        xAxis: { 
                            type: 'category', 
                            data: semesterNames,
                            name: '学期'
                        },
                        yAxis: { 
                            type: 'value', 
                            min: 0, 
                            max: 100,
                            name: '平均成绩',
                            axisLabel: {
                                formatter: '{value}分'
                            }
                        },
                        series: [{
                            name: '学期平均成绩',
                            data: avgScores,
                            type: 'line', 
                            smooth: true, 
                            areaStyle: { opacity: 0.2 }, 
                            itemStyle: { color: '#409EFF' },
                            label: {
                                show: true,
                                position: 'top',
                                formatter: '{c}分'
                            },
                            markLine: {
                                data: [
                                    { type: 'average', name: '平均值' }
                                ]
                            }
                        }]
                    };
                }
            } else {
                // 教师端：使用实际数据
                const { excellent, good, pass, fail } = teacherChartData.value;
                const total = excellent + good + pass + fail;
                
                if (total === 0) {
                    option = {
                        title: {
                            text: '暂无成绩数据',
                            left: 'center',
                            top: 'center',
                            textStyle: { color: '#999', fontSize: 16 }
                        }
                    };
                } else {
                    option = {
                        tooltip: { 
                            trigger: 'item',
                            formatter: '{b}: {c} ({d}%)'
                        },
                        legend: { top: '5%', left: 'center' },
                        series: [{
                            name: '成绩分布', 
                            type: 'pie', 
                            radius: ['40%', '70%'],
                            itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
                            data: [
                                { value: excellent, name: '优秀 (90+)', itemStyle: { color: '#67C23A' } },
                                { value: good, name: '良好 (80-89)', itemStyle: { color: '#409EFF' } },
                                { value: pass, name: '及格 (60-79)', itemStyle: { color: '#E6A23C' } },
                                { value: fail, name: '不及格 (<60)', itemStyle: { color: '#F56C6C' } }
                            ]
                        }]
                    };
                }
            }
            myChart.setOption(option);
            window.addEventListener('resize', () => myChart.resize());
        };

        const toggleFullScreen = () => !document.fullscreenElement ? document.documentElement.requestFullscreen() : document.exitFullscreen();

        onMounted(async () => { 
            await loadDashboardData();
            await loadChartData();
            nextTick(() => { initChart(); }); 
        });
        
        // 监听数据变化，重新渲染图表
        watch([chartData, teacherChartData], () => {
            nextTick(() => { initChart(); });
        }, { deep: true });
        
        return { user: props.user, currentDate, roleText, statsCards, toggleFullScreen };
    }
};

// ==========================================
// 3. 核心业务组件：个人信息中心 (User Profile)
// ==========================================
const UserProfile = {
    props: ['user', 'db'],
    template: `
        <el-card class="content-card">
            <template #header>
                <div style="display:flex; justify-content:space-between; align-items:center;">
                    <span>个人信息卡片</span>
                    <el-button type="primary" size="small" plain @click="editMode = !editMode">
                        {{ editMode ? '保存信息' : '编辑资料' }}
                    </el-button>
                </div>
            </template>
            <div style="display: flex; align-items: flex-start;">
                <div style="margin-right: 40px; text-align: center;">
                    <div style="width: 100px; height: 100px; background: #eee; border-radius: 50%; margin-bottom: 10px; line-height: 100px; font-size: 40px; color: #999;">
                        {{ user.name[0] }}
                    </div>
                    <el-tag>{{ user.id.startsWith('S') ? '学生' : '教师' }}</el-tag>
                </div>
                <el-descriptions :column="2" border style="flex: 1">
                    <el-descriptions-item label="用户ID">{{ user.id }}</el-descriptions-item>
                    <el-descriptions-item label="姓名">{{ user.name }}</el-descriptions-item>
                    <el-descriptions-item label="性别">
                        <span v-if="!editMode">{{ user.gender || '未知' }}</span>
                        <el-radio-group v-else v-model="user.gender"><el-radio label="男">男</el-radio><el-radio label="女">女</el-radio></el-radio-group>
                    </el-descriptions-item>
                    <el-descriptions-item label="所属学院">计算机科学与技术学院</el-descriptions-item>
                    <el-descriptions-item label="联系电话">
                         <span v-if="!editMode">138****0000</span><el-input v-else size="small" placeholder="请输入电话" />
                    </el-descriptions-item>
                    <el-descriptions-item label="电子邮箱">
                        <span v-if="!editMode">{{ user.id }}@school.edu.cn</span><el-input v-else size="small" placeholder="请输入邮箱" />
                    </el-descriptions-item>
                    <el-descriptions-item label="状态"><el-tag type="success">正常</el-tag></el-descriptions-item>
                </el-descriptions>
            </div>
        </el-card>
    `,
    setup(props) {
        const editMode = ref(false);
        return { editMode };
    }
};

// 4. 核心业务组件：学生成绩单 (增强版 + 智能预警)
const StudentGradeTable = {
    props: ['user', 'db'],
    template: `
        <el-card class="content-card">
            <template #header>
                <div style="display:flex; justify-content:space-between;">
                    <span>我的成绩单</span>
                    <div>
                        <el-input v-model="searchKey" placeholder="搜索课程名称" style="width: 200px; margin-right: 10px;" prefix-icon="Search" />
                        <el-button type="success" plain @click="exportExcel">导出 Excel</el-button>
                    </div>
                </div>
            </template>
            
            <el-table :data="filteredGrades" stripe style="width: 100%">
                <el-table-column prop="courseName" label="课程名称" width="180"></el-table-column>
                <el-table-column prop="scores.normal" label="平时">
                    <template #default="scope">{{ scope.row.scores?.normal || '-' }}</template>
                </el-table-column>
                <el-table-column prop="scores.midterm" label="期中">
                    <template #default="scope">{{ scope.row.scores?.midterm || '-' }}</template>
                </el-table-column>
                <el-table-column prop="scores.lab" label="实验">
                    <template #default="scope">{{ scope.row.scores?.lab || '-' }}</template>
                </el-table-column>
                <el-table-column prop="scores.finalExam" label="期末">
                    <template #default="scope">{{ scope.row.scores?.finalExam || '-' }}</template>
                </el-table-column>
                <el-table-column prop="scores.comprehensive" label="综合成绩" sortable>
                    <template #default="scope">
                        <el-tag :type="getScoreTag(scope.row.scores)">
                            {{ scope.row.scores?.comprehensive || '-' }}
                        </el-tag>
                    </template>
                </el-table-column>
            </el-table>

            <div style="margin-top: 20px; border-top: 1px dashed #ebeef5; padding-top: 20px;">
                <h4 style="margin: 0 0 15px 0; color: #303133;">
                    <el-icon style="vertical-align: middle; margin-right: 5px;"><Aim /></el-icon> 
                    智能学业分析报告
                </h4>
                <el-alert
                    :title="analysis.title"
                    :type="analysis.type"
                    :description="analysis.content"
                    :show-icon="true"
                    :closable="false"
                    style="margin-bottom: 10px;">
                </el-alert>
                
                <div v-if="analysis.details.length > 0" style="background: #fdf6ec; padding: 10px 15px; border-radius: 4px; font-size: 13px; color: #e6a23c;">
                    <div v-for="(tip, index) in analysis.details" :key="index" style="margin-bottom: 5px;">
                        • {{ tip }}
                    </div>
                </div>
            </div>
        </el-card>
    `,
    setup(props) {
        const searchKey = ref('');
        
        // 获取我的所有成绩
        const myGrades = ref([]);
        const filteredGrades = computed(() => myGrades.value.filter(g => g.courseName.includes(searchKey.value)));
        
        const loadGrades = async () => {
            try {
                const grades = await api.get(`/grade/list?studentId=${props.user.id}`);
                myGrades.value = grades.map(g => ({
                    ...g,
                    courseName: g.courseName || '未知课程'
                }));
            } catch (error) {
                console.error('加载成绩失败:', error);
                ElMessage.error('加载成绩失败');
            }
        };
        
        onMounted(() => {
            loadGrades();
        });

        // 🧠 智能分析逻辑
        const analysis = computed(() => {
            const grades = myGrades.value;
            if (grades.length === 0) return { title: '暂无数据', type: 'info', content: '暂无成绩记录。', details: [] };

            const fails = grades.filter(g => {
                const score = typeof g.scores === 'object' ? g.scores.comprehensive : g.scores;
                return (typeof score === 'number' ? score : parseFloat(score)) < 60;
            });
            const avg = grades.length > 0 ? grades.reduce((acc, cur) => {
                const score = typeof cur.scores === 'object' ? cur.scores.comprehensive : cur.scores;
                return acc + (typeof score === 'number' ? score : parseFloat(score));
            }, 0) / grades.length : 0;
            let details = [];

            // 1. 生成具体建议
            if (grades.some(g => {
                const score = typeof g.scores === 'object' ? g.scores.finalExam : 0;
                return (typeof score === 'number' ? score : parseFloat(score)) < 60;
            })) details.push("部分课程期末卷面成绩不及格，建议加强考前复习深度。");
            if (grades.some(g => {
                const score = typeof g.scores === 'object' ? g.scores.lab : 0;
                return (typeof score === 'number' ? score : parseFloat(score)) < 70;
            })) details.push("实验成绩有提升空间，建议多动手实践，提高实验报告质量。");
            if (grades.some(g => {
                const score = typeof g.scores === 'object' ? g.scores.normal : 0;
                return (typeof score === 'number' ? score : parseFloat(score)) < 80;
            })) details.push("平时成绩偏低，请注意出勤率和作业完成情况。");

            // 2. 生成主标题和类型
            if (fails.length > 0) {
                return {
                    title: `⚠️ 学业预警：你有 ${fails.length} 门课程不及格`,
                    type: 'error',
                    content: `不及格课程：${fails.map(g => g.courseName || '未知课程').join('、')}。请务必联系老师了解重修政策，制定复习计划。`,
                    details: details
                };
            } else if (avg >= 85) {
                return {
                    title: `🏆 表现优异：平均分 ${avg.toFixed(1)}`,
                    type: 'success',
                    content: '你的成绩非常出色，稳居班级前列！建议尝试参加学科竞赛或科研项目。',
                    details: [] // 优秀就不显示啰嗦的建议了
                };
            } else if (avg < 70) {
                return {
                    title: `🔔 成绩预警：平均分 ${avg.toFixed(1)} 偏低`,
                    type: 'warning',
                    content: '虽然没有挂科，但整体成绩处于危险边缘。建议调整学习方法，增加自习时间。',
                    details: details
                };
            } else {
                return {
                    title: `✨ 表现良好：平均分 ${avg.toFixed(1)}`,
                    type: 'info',
                    content: '成绩整体稳定，继续保持。建议针对薄弱科目进行专项突破。',
                    details: details
                };
            }
        });

        const getScoreTag = (s) => {
            const score = typeof s === 'object' && s.comprehensive !== undefined ? s.comprehensive : s;
            const num = typeof score === 'number' ? score : parseFloat(score);
            return num < 60 ? 'danger' : (num >= 90 ? 'success' : 'primary');
        };
        const exportExcel = () => ElMessage.success('正在下载成绩单... (模拟)');

        return { filteredGrades, getScoreTag, searchKey, exportExcel, analysis };
    }
};

// ==========================================
// 5. 核心业务组件：可视化课表 (列表/周视图)
// ==========================================
const StudentCourseTable = {
    props: ['user', 'db'],
    template: `
        <el-card class="content-card">
            <template #header>
                <div style="display:flex; justify-content:space-between; align-items:center;">
                    <span>我的课程表</span>
                    <el-radio-group v-model="viewMode" size="small">
                        <el-radio-button label="list">列表视图</el-radio-button>
                        <el-radio-button label="table">周课表视图</el-radio-button>
                    </el-radio-group>
                </div>
            </template>
            <el-table v-if="viewMode === 'list'" :data="myCourses" style="width: 100%">
                <el-table-column prop="id" label="班级编号" width="120"></el-table-column>
                <el-table-column prop="courseName" label="课程名称"></el-table-column>
                <el-table-column prop="teacherName" label="任课教师"></el-table-column>
                <el-table-column prop="timeStr" label="上课时间">
                     <template #default="scope"><el-tag>{{ scope.row.day }} {{ scope.row.period }}</el-tag></template>
                </el-table-column>
            </el-table>
            <div v-else class="timetable">
                <div class="timetable-header">
                    <div style="width:80px; flex:none;">时间</div>
                    <div v-for="d in days" :key="d">{{ d }}</div>
                </div>
                <div class="timetable-body">
                    <div v-for="p in periods" :key="p" class="timetable-row">
                        <div class="period-label">{{ p }}</div>
                        <div v-for="d in days" :key="d + p" class="timetable-cell">
                            <div v-if="getCourse(d, p)" class="course-block" :style="{background: getCourseColor(getCourse(d, p).courseName)}">
                                <strong>{{ getCourse(d, p).courseName }}</strong>
                                <br/>{{ getCourse(d, p).teacherName }}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </el-card>
    `,
    setup(props) {
        const viewMode = ref('table');
        const days = ['周一', '周二', '周三', '周四', '周五'];
        const periods = ['1-2节', '3-4节', '5-6节', '7-8节'];
        const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#9b59b6'];

        const myCourses = ref([]);
        
        const loadCourses = async () => {
            try {
                const courses = await api.get(`/grade/timetable?studentId=${props.user.id}`);
                myCourses.value = courses.map(tc => ({
                    ...tc,
                    id: tc.classId,
                    timeStr: `${tc.day} ${tc.period}`
                }));
            } catch (error) {
                console.error('加载课程表失败:', error);
                ElMessage.error('加载课程表失败');
            }
        };
        
        onMounted(() => {
            loadCourses();
        });

        const getCourse = (day, period) => myCourses.value.find(c => c.day === day && c.period === period);
        const getCourseColor = (name) => {
            let hash = 0; for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash);
            return colors[Math.abs(hash) % colors.length];
        };
        return { viewMode, myCourses, days, periods, getCourse, getCourseColor };
    }
};

// ==========================================
// 6. 核心业务组件：教师端功能
// ==========================================
const TeacherClassManager = {
    props: ['user', 'db'],
    template: `
        <el-card class="content-card">
            <template #header>
                <div style="display: flex; justify-content: space-between; align-items: center;">
                    <span>班级成绩管理</span>
                    <div>
                         <el-select v-model="selectedClassId" placeholder="请选择班级" style="width: 180px; margin-right: 10px;">
                            <el-option v-for="tc in myClasses" :key="tc.id" :label="tc.id + ' - ' + tc.courseName" :value="tc.id" />
                        </el-select>
                        <el-button type="primary" @click="exportExcel" :disabled="!selectedClassId">导出成绩单</el-button>
                    </div>
                </div>
            </template>
            <el-table v-if="selectedClassId" :data="classGrades" style="width: 100%" height="400">
                <el-table-column prop="studentId" label="学号" sortable width="120"></el-table-column>
                <el-table-column prop="studentName" label="姓名" width="120"></el-table-column>
                <el-table-column prop="scores.normal" label="平时">
                    <template #default="scope">{{ scope.row.scores?.normal || scope.row.scores?.normalScore || '-' }}</template>
                </el-table-column>
                <el-table-column prop="scores.midterm" label="期中">
                    <template #default="scope">{{ scope.row.scores?.midterm || scope.row.scores?.midtermScore || '-' }}</template>
                </el-table-column>
                <el-table-column prop="scores.lab" label="实验">
                    <template #default="scope">{{ scope.row.scores?.lab || scope.row.scores?.labScore || '-' }}</template>
                </el-table-column>
                <el-table-column prop="scores.finalExam" label="期末">
                    <template #default="scope">{{ scope.row.scores?.finalExam || scope.row.scores?.finalScore || '-' }}</template>
                </el-table-column>
                <el-table-column prop="scores.comprehensive" label="综合" sortable>
                     <template #default="scope">
                        <span :style="{color: (scope.row.scores?.comprehensive || scope.row.scores?.totalScore || 0) < 60 ? 'red' : 'black', fontWeight: 'bold'}">
                            {{ scope.row.scores?.comprehensive || scope.row.scores?.totalScore || '-' }}
                        </span>
                    </template>
                </el-table-column>
            </el-table>
            <div v-else style="text-align:center; padding:50px; color:#999;">请先选择班级</div>
        </el-card>
    `,
    setup(props) {
        const selectedClassId = ref('');
        const myClasses = ref([]);
        const classGrades = ref([]);
        
        const loadMyClasses = async () => {
            try {
                const classes = await api.get(`/grade/my-classes?teacherId=${props.user.id}`);
                myClasses.value = classes;
            } catch (error) {
                console.error('加载班级列表失败:', error);
                ElMessage.error('加载班级列表失败');
            }
        };
        
        const loadClassGrades = async () => {
            if (!selectedClassId.value) {
                classGrades.value = [];
                return;
            }
            try {
                const grades = await api.get(`/grade/class?classId=${selectedClassId.value}`);
                classGrades.value = grades;
            } catch (error) {
                console.error('加载班级成绩失败:', error);
                ElMessage.error('加载班级成绩失败');
            }
        };
        
        watch(selectedClassId, () => {
            loadClassGrades();
        });
        
        const exportExcel = () => ElMessage.success('成绩单导出成功！');
        
        onMounted(() => {
            loadMyClasses();
        });
        
        return { selectedClassId, myClasses, classGrades, exportExcel };
    }
};

const TeacherStatistics = {
    props: ['user', 'db'],
    template: `<el-card class="content-card"><div id="scoreChart" style="height:400px;"></div></el-card>`,
    setup(props) {
        const loadStats = async () => {
            try {
                const stats = await api.get('/teacher/stats');
                const dom = document.getElementById('scoreChart');
                if(dom) {
                    echarts.dispose(dom);
                    const chart = echarts.init(dom);
                    chart.setOption({
                        title: { text: '全校成绩分布统计' }, tooltip: {},
                        xAxis: { data: stats.map(s => s.name) }, yAxis: {},
                        series: [{ 
                            type: 'bar', 
                            data: stats.map(s => s.value), 
                            itemStyle: { color: '#409EFF' } 
                        }]
                    });
                }
            } catch (error) {
                console.error('加载统计数据失败:', error);
                ElMessage.error('加载统计数据失败');
            }
        };
        
        onMounted(() => {
            loadStats();
        });
        return {};
    }
};

const TeacherRankings = {
    props: ['user', 'db'],
    template: `
        <el-card class="content-card">
            <template #header>
                <div style="display: flex; justify-content: space-between; align-items: center;">
                    <span>全校排名 (Top 50)</span>
                </div>
            </template>
            <el-table v-loading="loading" :data="list" style="width: 100%" height="400" stripe>
                <el-table-column type="index" label="排名" width="80"></el-table-column>
                <el-table-column prop="id" label="学号" sortable width="120"></el-table-column>
                <el-table-column prop="name" label="姓名" width="120"></el-table-column>
                <el-table-column prop="total" label="总分" sortable>
                    <template #default="scope">
                        <span style="font-weight: bold;">
                            {{ scope.row.total != null ? Number(scope.row.total).toFixed(2) : '-' }}
                        </span>
                    </template>
                </el-table-column>
                <el-table-column prop="avg" label="平均分" sortable>
                    <template #default="scope">
                        {{ scope.row.avg != null ? Number(scope.row.avg).toFixed(2) : '-' }}
                    </template>
                </el-table-column>
                <el-table-column prop="courseCount" label="课程数"></el-table-column>
            </el-table>
        </el-card>
    `,
    setup(props) {
        const list = ref([]);
        const loading = ref(false);
        
        const loadRankings = async () => {
            if (loading.value) return; // 防止重复加载
            loading.value = true;
            try {
                const rankings = await api.get('/teacher/rank');
                // 保持数据为数字类型，不要在这里转换
                list.value = rankings || [];
            } catch (error) {
                console.error('加载排名失败:', error);
                ElMessage.error('加载排名失败: ' + (error.message || '未知错误'));
                list.value = []; // 出错时清空列表
            } finally {
                loading.value = false;
            }
        };
        
        onMounted(() => {
            loadRankings();
        });
        
        return { list, loading };
    }
};

const TeacherWarning = {
    props: ['user', 'db'],
    template: `
        <el-card class="content-card" style="border-top: 3px solid #F56C6C">
            <template #header><span style="color:#F56C6C; font-weight:bold;">学业预警中心</span></template>
            <el-table :data="list">
                <el-table-column prop="studentId" label="学号"></el-table-column>
                <el-table-column prop="studentName" label="姓名"></el-table-column>
                <el-table-column prop="courseName" label="挂科课程"></el-table-column>
                <el-table-column prop="score" label="分数"><template #default="s"><el-tag type="danger">{{s.row.score}}</el-tag></template></el-table-column>
            </el-table>
        </el-card>
    `,
    setup(props) {
        const list = ref([]);
        
        const loadWarnings = async () => {
            try {
                const warnings = await api.get('/teacher/warning');
                list.value = warnings.map(w => ({
                    ...w,
                    score: typeof w.score === 'number' ? w.score.toFixed(2) : w.score
                }));
            } catch (error) {
                console.error('加载学业预警失败:', error);
                ElMessage.error('加载学业预警失败');
            }
        };
        
        onMounted(() => {
            loadWarnings();
        });
        
        return { list };
    }
};

// ==========================================
// 7. 主程序 (Main App)
// ==========================================
const app = createApp({
    setup() {
        const db = reactive({ students: [], teachers: [], courses: [], teachingClasses: [], grades: [] });
        const isLoggedIn = ref(false);
        const currentUser = ref(null);
        const userRole = ref('');
        const currentMenu = ref('dashboard');
        const loginForm = reactive({ id: '', password: '' });
        const loading = ref(false);
        const loginFormRef = ref(null);
        const passwordDialogVisible = ref(false);
        const passwordForm = reactive({ oldPass: '', newPass: '', confirmPass: '' });
        const passwordFormRef = ref(null);

        // 登录表单验证规则
        const loginRules = {
            id: [
                { required: true, message: '用户名或学号/工号不能为空', trigger: 'blur' },
                { 
                    pattern: /^[ST]\d{3}$/i, 
                    message: '格式错误，应为S或T开头后跟3位数字（如：S001、T001）', 
                    trigger: 'blur' 
                }
            ],
            password: [
                { required: true, message: '密码不能为空', trigger: 'blur' },
                { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
            ]
        };

        // 密码修改表单验证规则
        const passwordRules = {
            oldPass: [
                { required: true, message: '旧密码不能为空', trigger: 'blur' }
            ],
            newPass: [
                { required: true, message: '新密码不能为空', trigger: 'blur' },
                { min: 6, message: '新密码长度不能小于6位', trigger: 'blur' }
            ],
            confirmPass: [
                { required: true, message: '确认密码不能为空', trigger: 'blur' },
                {
                    validator: (rule, value, callback) => {
                        if (value !== passwordForm.newPass) {
                            callback(new Error('两次输入的密码不一致'));
                        } else {
                            callback();
                        }
                    },
                    trigger: 'blur'
                }
            ]
        };

        const openPasswordDialog = () => { 
            passwordDialogVisible.value = true;
            // 重置表单验证状态
            nextTick(() => {
                if (passwordFormRef.value) {
                    passwordFormRef.value.clearValidate();
                }
            });
        };
        
        const submitPasswordChange = async () => {
            // 表单验证：校验不通过时阻止请求发送
            if (!passwordFormRef.value) return;
            
            try {
                const valid = await passwordFormRef.value.validate();
                if (!valid) {
                    ElMessage.error('请检查表单输入');
                    return;
                }
                
                // 一致性校验（双重保障）
                if (passwordForm.newPass !== passwordForm.confirmPass) {
                    ElMessage.error('两次输入的密码不一致');
                    return;
                }
                
                // 前端基础校验（双重保障）
                if (!passwordForm.oldPass || !passwordForm.newPass || !passwordForm.confirmPass) {
                    ElMessage.error('所有字段不能为空');
                    return;
                }
                
                if (passwordForm.newPass.length < 6) {
                    ElMessage.error('新密码长度不能小于6位');
                    return;
                }
                
                await api.post('/change-password', {
                    id: currentUser.value.id,
                    oldPass: passwordForm.oldPass,
                    newPass: passwordForm.newPass
                });
                ElMessage.success('密码修改成功');
                passwordDialogVisible.value = false;
                passwordForm.oldPass = '';
                passwordForm.newPass = '';
                passwordForm.confirmPass = '';
            } catch (error) {
                // 如果是验证错误，不显示错误消息（Element Plus会自动显示）
                if (error && error.fields) {
                    return;
                }
                ElMessage.error(error.message || '密码修改失败');
            }
        };

        const handleLogin = async () => {
            // 表单验证：校验不通过时阻止请求发送
            if (!loginFormRef.value) return;
            
            try {
                const valid = await loginFormRef.value.validate();
                if (!valid) {
                    ElMessage.error('请检查表单输入');
                    return;
                }
            } catch (error) {
                // 如果是验证错误，不显示错误消息（Element Plus会自动显示）
                if (error && error.fields) {
                    return;
                }
            }
            
            loading.value = true;
            try {
                const { id, password } = loginForm;
                
                // 前端基础校验（双重保障）
                if (!id || !password) {
                    ElMessage.error('用户名和密码不能为空');
                    loading.value = false;
                    return;
                }
                
                if (!/^[ST]\d{3}$/i.test(id)) {
                    ElMessage.error('用户名格式错误，应为S或T开头后跟3位数字');
                    loading.value = false;
                    return;
                }
                
                if (password.length < 6) {
                    ElMessage.error('密码长度不能小于6位');
                    loading.value = false;
                    return;
                }
                
                const data = await api.post('/login', {
                    username: id.toUpperCase(),
                    password: password
                });
                
                currentUser.value = {
                    id: data.id,
                    name: data.name,
                    role: data.role
                };
                userRole.value = data.role;
                isLoggedIn.value = true;
                currentMenu.value = 'dashboard';
                ElMessage.success('登录成功');
            } catch (error) {
                ElMessage.error(error.message || '登录失败');
            } finally {
                loading.value = false;
            }
        };
        const logout = () => { isLoggedIn.value = false; currentUser.value = null; };
        
        // 关键修复：菜单切换
        const handleMenuSelect = (index) => { currentMenu.value = index; };

        return { 
            db, isLoggedIn, currentUser, userRole, currentMenu, loginForm, loading, loginFormRef,
            loginRules, passwordRules,
            handleLogin, logout, handleMenuSelect,
            passwordDialogVisible, passwordForm, passwordFormRef, openPasswordDialog, submitPasswordChange
        };
    }
});

// 注册所有组件
app.component('dashboard', Dashboard);
app.component('user-profile', UserProfile);
app.component('my-grades', StudentGradeTable);
app.component('my-courses', StudentCourseTable);
app.component('ai-assistant', AIAssistant);
app.component('class-grades', TeacherClassManager);
app.component('statistics', TeacherStatistics);
app.component('student-rank', TeacherRankings);
app.component('grade-warning', TeacherWarning);

// 注册图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component);
}

app.use(ElementPlus);
app.mount('#app');