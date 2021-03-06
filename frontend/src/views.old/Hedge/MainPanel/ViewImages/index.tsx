import { defineComponent } from "vue"
import TopBar from "../../TopBar"
import TopBarContent from "./TopBarContent"
import ImageGrid from "@/layouts/ImageGrid"

import img1 from "@/assets/img1.local.jpg"
import img2 from "@/assets/img2.local.jpg"

export default defineComponent({
    setup() {
        const images = [
            img1, img2
        ]

        return () => <div id="hedge-image">
            <ImageGrid items={images} marginTopBar={true}/>
            <TopBar>
                <TopBarContent/>
            </TopBar>
        </div>
    }
})