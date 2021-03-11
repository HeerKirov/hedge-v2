import { defineComponent, inject, InjectionKey, onUnmounted, ref, Ref } from "vue"
import style from "./style.module.scss"
import SideBar from "./SideBar"
import TopBar from "./TopBar"
import MainContent from "./MainContent"

export { SideBar, TopBar, MainContent }

/**
 * 控制sideLayout的侧边栏开关。是可选的依赖属性，如果该属性不存在，就会在sideLayout内维护此属性。
 */
export const sideBarSwitchInjection: InjectionKey<Ref<boolean>> = Symbol()

/**
 * 控制sideLayout的侧边栏当前宽度。是可选的依赖属性，如果该属性不存在，就会在sideLayout内维护此属性。
 */
export const sideBarWidthInjection: InjectionKey<Ref<number>> = Symbol()

/**
 * 主要页面的侧栏分栏结构。
 * 只提供了一个分栏结构，不包括任何内容，通过slot.side导入侧栏，slot.default导入主要内容区域。
 * 通过inject响应sideBarWidth/sideBarSwitch属性，以控制侧栏开关。
 * 这个组件可以在主要页面以及详情页面中复用，由于通过inject注入属性，因此可以实现全局共享侧栏状态。
 */
export default defineComponent({
    props: {
        defaultWidth: {type: Number, default: 225},
        attachRange: {type: Number, default: 10},
        maxWidth: {type: Number, default: 400},
        minWidth: {type: Number, default: 150}
    },
    setup(props, { slots }) {
        const sideBarWidth = inject(sideBarWidthInjection, () => ref(props.defaultWidth), true)
        const sideBarSwitch = inject(sideBarSwitchInjection, () => ref(true), true)

        const { resizeAreaMouseDown } = useResizeBar(sideBarWidth, {
            defaultWidth: props.defaultWidth,
            maxWidth: props.maxWidth,
            minWidth: props.minWidth,
            attachRange: props.attachRange
        })

        return () => <div class={style.sideLayout}>
            <div class={style.content} style={{"left": `${sideBarSwitch.value ? sideBarWidth.value : 0}px`}}>
                {slots.default?.()}
            </div>
            <div class={style.sideContent} style={{"width": `${sideBarWidth.value}px`, "transform": `translateX(${sideBarSwitch.value ? '0' : '-100%'})`}}>
                {slots.side?.()}
            </div>
            {sideBarSwitch.value && <div class={style.resizeContent} style={{"left": `${sideBarWidth.value}px`}} onMousedown={resizeAreaMouseDown}/>}
        </div>
    }
})

function useResizeBar(sizeBarWidth: Ref<number>, configure: {defaultWidth: number, maxWidth: number, minWidth: number, attachRange: number}) {
    const { defaultWidth, maxWidth, minWidth, attachRange } = configure

    const resizeAreaMouseDown = () => {
        document.addEventListener('mousemove', mouseMove)
        document.addEventListener('mouseup', mouseUp)
        return false
    }

    const mouseMove = (e: MouseEvent) => {
        const newWidth = e.pageX
        sizeBarWidth.value 
            = newWidth > maxWidth ? maxWidth 
            : newWidth < minWidth ? minWidth 
            : Math.abs(newWidth - defaultWidth) <= attachRange ? defaultWidth 
            : newWidth
    }

    const mouseUp = () => {
        document.removeEventListener('mousemove', mouseMove)
        document.addEventListener('mouseup', mouseUp)
    }

    onUnmounted(() => {
        document.removeEventListener('mousemove', mouseMove)
        document.addEventListener('mouseup', mouseUp)
    })

    return {resizeAreaMouseDown}
}