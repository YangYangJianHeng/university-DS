// 全局声明 Element Plus 图标组件类型
// 图标在 main.ts 中通过 app.component() 全局注册
// 使用 Vue 内置 Component 类型，避免导入 @element-plus/icons-vue 造成的性能问题
import type { Component } from 'vue'

declare module 'vue' {
  export interface GlobalComponents {
    // 导航/操作
    ArrowLeft: Component
    ArrowDown: Component
    ArrowUp: Component
    Plus: Component
    Delete: Component
    Search: Component
    Edit: Component
    EditPen: Component
    Close: Component
    Menu: Component
    MoreFilled: Component
    // 用户/头像
    User: Component
    UserFilled: Component
    // 视图
    Grid: Component
    List: Component
    // 时间
    Clock: Component
    Timer: Component
    // 文件/上传
    Upload: Component
    UploadFilled: Component
    Download: Component
    Document: Component
    FolderOpened: Component
    Files: Component
    // 查看/预览
    View: Component
    Hide: Component
    Link: Component
    // 消息/聊天
    ChatDotRound: Component
    ChatLineSquare: Component
    Message: Component
    Bell: Component
    // 状态
    Warning: Component
    WarningFilled: Component
    InfoFilled: Component
    CircleCheckFilled: Component
    CircleCloseFilled: Component
    Check: Component
    // 其他功能
    Setting: Component
    Refresh: Component
    Switch: Component
    SwitchButton: Component
    Reading: Component
    Star: Component
    StarFilled: Component
    Collection: Component
    Trophy: Component
    Medal: Component
    Tickets: Component
    DataAnalysis: Component
    TrendCharts: Component
    Filter: Component
    Rank: Component
    Money: Component
    Promotion: Component
    Aim: Component
    VideoPlay: Component
    Connection: Component
    HomeFilled: Component
    Tools: Component
    // 后台管理
    Picture: Component
    Share: Component
    // 成果展厅
    Calendar: Component
  }
}
