import { defineComponent, inject } from "vue"
import { useRouter } from "vue-router"
import { InitState } from "@/functions/adapter-ipc"
import { useAppInfo, useInitController } from "@/functions/service"
import ProgressFlag from "@/components/ProgressFlag"
import { InitContextInjection } from "./inject"
import style from "./style.module.scss"

export default defineComponent({
    setup() {
        const router = useRouter()
        const appInfo = useAppInfo()
        const context = inject(InitContextInjection)!
        const { state, initializeApp } = useInitController()

        const finish = () => router.push({name: "MainIndex"})
        const initialize = () => {
            if(appInfo.clientMode) {
                initializeApp({password: context.password.value, dbPath: `${appInfo.userDataPath}/appdata/channel/${appInfo.channel}/database/${context.db.folderInAppData}`})
            }
        }

        return () => state.value === undefined ? <>
            <h2 class="is-size-4 mb-2">完成</h2>
            <p>必要的配置已选择。</p>
            <div class="block py-2 px-3 mt-2 mb-1">
                口令: {context.password.hasPassword ? "已使用口令" : "未使用口令"}
            </div>
            <div class="block py-2 px-3 mb-3">
                数据库: {context.db.custom ? <code>{context.db.customFolderPath}</code> : context.db.folderInAppData}
            </div>
            <p>接下来将:</p>
            <p>1. 初始化App数据文档和数据库。</p>
            <p>2. 部署App核心服务资源。这会稍微多花一点时间。</p>

            <div class={style.bottom}>
                <button class="button is-medium is-link is-light absolute left-bottom" onClick={context.page.prev}><span class="icon"><i class="fa fa-arrow-left"/></span><span>上一步</span></button>
                <button class="button is-medium is-link absolute right-bottom" onClick={initialize}><span>确定</span><span class="icon"><i class="fa fa-check"/></span></button>
            </div>
        </> : state.value === InitState.FINISH ? <>
            <h2 class="is-size-4 mb-2">完成</h2>
            <div class="has-text-centered absolute center">
                <i class="fa fa-3x fa-check mb-4"/>
                <div>初始化已完成。点击继续开始使用。</div>
            </div>
            <div class={style.bottom}>
                <button class="button is-medium is-link absolute right-bottom" onClick={finish}>继续<span class="icon"><i class="fa fa-check"/></span></button>
            </div>
        </> : state.value === InitState.ERROR ? <>
            <h2 class="is-size-4 mb-2">错误</h2>
            <p>初始化过程失败。</p>
            {/* TODO 错误信息 */}
            <div class={style.bottom}>
                <button class="button is-medium is-link is-light absolute left-bottom" onClick={context.page.prev}><span class="icon"><i class="fa fa-arrow-left"/></span><span>上一步</span></button>
            </div>
        </> : <div class="has-text-centered absolute center">
            <span class="icon"><i class="fa fa-3x fa-code-branch"/></span>
            <p class="mt-4">{loadingMessage[state.value]}</p>
            <ProgressFlag class="mt-2"/>
        </div>
    }
})

const loadingMessage = {
    [InitState.INITIALIZING_APPDATA]: "正在构建数据……",
    [InitState.INITIALIZING_RESOURCE]: "正在部署资源……",
    [InitState.INITIALIZING_SERVER]: "正在启动服务……",
    [InitState.INITIALIZING_SERVER_DATABASE]: "正在构建数据库……"
}