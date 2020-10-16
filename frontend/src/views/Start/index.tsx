import { defineComponent } from "vue"
import { RouterView } from "vue-router"
import "./style.scss"

export default defineComponent({
    setup() {
        return () => <div id="start">
            <div class="title-bar has-text-centered">
                <span>HEDGE</span>
            </div>
            <RouterView/>
        </div>
    }
})