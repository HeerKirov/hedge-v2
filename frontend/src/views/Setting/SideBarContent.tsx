import { defineComponent, toRef } from "vue"
import { useRoute, useRouter } from "vue-router"
import style from "./style.module.scss"

/**
 * 主页面的侧栏内容的框架模块。只提供布局框架，以及基本功能，比如折叠按钮，以及底栏的基本功能按钮。
 */
export default defineComponent({
    setup() {
        const route = useRoute()
        const router = useRouter()

        const routeName = toRef(route, 'name')

        return () => <>
            <p class="is-size-4">设置</p>
            <aside class="menu deep">
                {settings.map(scope => <>
                    <p class="menu-label">{scope.label}</p>
                    <ul class="menu-list">
                        {scope.list.map(item => <li>
                            <a class={{"is-active": routeName.value === item.route}} onClick={() => router.push({name: item.route})}>
                                <span class="icon"><i class={`fa fa-${item.icon}`}/></span><span>{item.name}</span>
                            </a>
                        </li>)}
                    </ul>
                </>)}
            </aside>
        </>
    }
})

interface SettingScope {
    label: string,
    list: SettingItem[]
}

interface SettingItem {
    route: string
    name: string
    icon: string
}

const settings: SettingScope[] = [
    {
        label: "应用程序",
        list: [
            {route: "SettingAppSecurity", name: "认证", icon: "key"},
            {route: "SettingAppearance", name: "外观", icon: "cloud-sun"},
            {route: "SettingWebAccess", name: "局域网访问", icon: "network-wired"},
            {route: "SettingBackup", name: "备份与还原", icon: "sync-alt"}
        ]
    },
    {
        label: "数据库事务",
        list: [
            {route: "SettingDBMeta", name: "元数据选项", icon: "coffee"},
            {route: "SettingDBQuery", name: "查询选项", icon: "search"},
            {route: "SettingDBImport", name: "导入选项", icon: "plus-square"},
            {route: "SettingDBOrigin", name: "原始数据选项", icon: "file-invoice"}
        ]
    },
    {
        label: "高级选项",
        list: [
            {route: "SettingServer", name: "核心服务", icon: "server"},
            {route: "SettingCli", name: "命令行工具", icon: "terminal"},
            {route: "SettingProxy", name: "代理", icon: "globe"},
            {route: "SettingChannel", name: "频道", icon: "coins"},
        ]
    }
]