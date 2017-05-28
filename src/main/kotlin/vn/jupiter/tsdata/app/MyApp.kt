package vn.jupiter.tsdata.app

import tornadofx.*
import vn.jupiter.tsdata.view.MainView
import java.nio.charset.Charset

class MyApp: App(MainView::class, Styles::class) {
    override fun onBeforeShow(view: UIComponent) {
        super.onBeforeShow(view)
        println(Charset.availableCharsets())
    }
}