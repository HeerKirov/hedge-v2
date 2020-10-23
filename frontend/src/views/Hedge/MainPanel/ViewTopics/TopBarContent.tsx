import { defineComponent } from "vue"
import MiddleLayout from "../../TopBar/MiddleLayout"
import QueryBox from "../../TopBar/QueryBox"

export default defineComponent({
    setup() {
        return () => <MiddleLayout>
            {{
                default: () => <div class="field is-grouped">
                    <QueryBox class="mr-1" placeholder="使用hedge QL查询主题…" icon="hashtag"/>
                </div>,
                right: () => <>
                    <p class="control mr-1">
                        <button class="button no-drag is-small">
                            <span class="icon"><i class="fa fa-plus"/></span>
                        </button>
                    </p>
                </>
            }}
        </MiddleLayout>
    }
})