import { systemPreferences } from "electron"
import { AppDataDriver, AppDataStatus } from "../appdata"
import { ResourceManager, ResourceStatus } from "../resource"
import { ServerManager } from "../server"
import { ClientException, panic } from "../../exceptions"

/**
 * 对客户端app的状态进行管理。处理从加载到登录的一系列状态。
 */
export interface StateManager {
    /**
     * 加载此模块，也开启app的加载流程。加载流程是用户态的，因此不会异步等待。
     * 如果app已经初始化，就执行加载流程。
     */
    load(): void

    /**
     * 执行初始化。该方法没有回执，需要通过init change event获得初始化过程变化。
     * 只能在NOT_INIT状态下调用，否则调用是无效的。
     */
    init(config: InitConfig): Promise<InitState>

    /**
     * 查看当前state状态。
     */
    state(): State

    /**
     * 使用密码登录。
     * 只能在NOT_LOGIN状态下调用，否则总是返回false。
     * @param password
     */
    login(password?: string): {ok: boolean, state?: State}

    /**
     * 使用touchID登录。
     * 只能在NOT_LOGIN状态下调用，否则总是返回false。
     * 只能在touchID可用时调用，否则总是返回false。
     */
    loginByTouchID(): Promise<{ok: boolean, state?: State}>

    /**
     * 注册一个事件，该事件在state发生改变时触发。
     * @param event
     */
    onStateChanged(event: (state: State) => void): void

    /**
     * 注册一个事件，该事件在init过程中发送初始化过程变化。
     * @param event
     */
    onInitChanged(event: (state: InitState, errorCode?: string, errorMessage?: string) => void): void
}

export enum State {
    NOT_INIT = "NOT_INIT",  //(稳定态)app未初始化
    LOADING = "LOADING",                    //(瞬间态)加载中，还不知道要做什么
    LOADING_RESOURCE = "LOADING_RESOURCE",  //加载中，正在处理资源升级
    LOADING_SERVER = "LOADING_SERVER",      //加载中，正在处理核心服务连接
    NOT_LOGIN = "NOT_LOGIN",    //(稳定态)app已加载完成，但是需要登录
    LOADED = "LOADED"           //(稳定态)app已加载完成，且不需要登录，是已经可用了的状态
}

export enum InitState {
    INITIALIZING = "INITIALIZING",
    INITIALIZING_APPDATA = "INITIALIZING_APPDATA",
    INITIALIZING_RESOURCE = "INITIALIZING_RESOURCE",
    INITIALIZING_SERVER = "INITIALIZING_SERVER",
    INITIALIZING_SERVER_DATABASE = "INITIALIZING_SERVER_DATABASE",
    FINISH = "FINISH",
    ERROR = "ERROR"
}

export interface InitConfig {
    password: string | null
    dbPath: string
}

export interface StateManagerOptions {
    debugMode: boolean
}

export function createStateManager(appdata: AppDataDriver, resource: ResourceManager, server: ServerManager, options: StateManagerOptions): StateManager {
    const stateChangedEvents: ((state: State) => void)[] = []
    const initChangedEvents: ((state: InitState, errorCode?: string, errorMessage?: string) => void)[] = []

    let state: State = State.NOT_INIT

    function setState(newState: State) {
        state = newState
        for (let event of stateChangedEvents) {
            event(newState)
        }
    }

    function setInitState(newState: InitState, errorCode?: string, errorMessage?: string) {
        for (let event of initChangedEvents) {
            event(newState, errorCode, errorMessage)
        }
    }

    async function asyncLoad() {
        if(resource.status() == ResourceStatus.NEED_UPDATE) {
            //resource需要升级
            setState(State.LOADING_RESOURCE)
            await resource.update()
        }
        if(appdata.getAppData().loginOption.password == null) {
            //没有密码

            //启动server
            setState(State.LOADING_SERVER)
            await server.startConnection()

            setState(State.LOADED)
        }else{
            if(appdata.getAppData().loginOption.fastboot) {
                //fastboot模式下，server启动在login检查之前，且前启动过程没有状态信息
                server.startConnection().then(() => {
                    //启动完成后，检查state
                    if(state == State.LOADING_SERVER) {
                        //如果是loading server的状态，说明已经登录，且正在等待启动完成
                        setState(State.LOADED)
                    }
                    //否则认为还没有登录，什么也不做
                })
            }

            setState(State.NOT_LOGIN)
        }
    }

    function asyncLogin(): State {
        if(appdata.getAppData().loginOption.fastboot) {
            //fastboot模式下，检查是否存在server的连接信息
            if(server.connectionInfo() != null) {
                return state = State.LOADED
            }else{
                return state = State.LOADING_SERVER
            }
        }else{
            //非fastboot模式下，启动server
            try {
                return state = State.LOADING_SERVER
            } finally {
                async function connect() {
                    await server.startConnection()
                    setState(State.LOADED)
                }
                connect().catch(e => panic(e, options.debugMode))
            }
        }
    }

    async function asyncInit(config: InitConfig) {
        setInitState(InitState.INITIALIZING_APPDATA)
        await appdata.init()
        await appdata.saveAppData(d => d.loginOption.password = config.password)

        setInitState(InitState.INITIALIZING_RESOURCE)
        await resource.update()

        setInitState(InitState.INITIALIZING_SERVER)
        await server.startConnection()

        setInitState(InitState.INITIALIZING_SERVER_DATABASE)
        await server.initializeRemoteServer(config.dbPath)

        setState(State.LOADED)
        setInitState(InitState.FINISH)
    }

    return {
        load() {
            if(appdata.status() == AppDataStatus.LOADED) {
                setState(State.LOADING)
                asyncLoad().catch(e => panic(e, options.debugMode))
            }
        },
        async init(config: InitConfig): Promise<InitState> {
            if(state == State.NOT_INIT) {
                asyncInit(config).catch(e => console.error(e))
                return InitState.INITIALIZING
            }else{
                throw new ClientException("ALREADY_INIT")
            }
        },
        state() {
            return state
        },
        login(password?: string): {ok: boolean, state?: State} {
            if(state == State.NOT_LOGIN) {
                const truePassword = appdata.getAppData().loginOption.password
                if(truePassword == null || password === truePassword) {
                    const state = asyncLogin()
                    return { ok: true, state }
                }
            }
            return { ok: false }
        },
        async loginByTouchID(): Promise<{ok: boolean, state?: State}> {
            if(state == State.NOT_LOGIN && systemPreferences.canPromptTouchID()) {
                try {
                    await systemPreferences.promptTouchID("进行登录认证")
                }catch (e) {
                    return { ok: false }
                }
                const state = asyncLogin()
                return { ok: true, state }
            }else{
                return { ok: false }
            }
        },
        onStateChanged(event: (state: State) => void) {
            stateChangedEvents.push(event)
        },
        onInitChanged(event: (state: InitState, errorCode?: string, errorMessage?: string) => void) {
            initChangedEvents.push(event)
        }
    }
}
