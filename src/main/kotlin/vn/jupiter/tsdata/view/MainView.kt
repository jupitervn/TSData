package vn.jupiter.tsdata.view

import javafx.scene.control.TabPane
import vn.jupiter.tsdata.app.Styles
import tornadofx.*
import tornadofx.plusAssign
import vn.jupiter.tsdata.controller.*
import vn.jupiter.tsdata.data.Item

class MainView : View("Hello TornadoFX Application") {
    override val root = tabpane {

    }
    val dataSet = mapOf(
            "Item" to ItemTabView(ItemTabController(ItemInfoDataRepo())),
            "NPC" to ItemTabView(ItemTabController(NpcInfoDataRepo(), NpcInfoDataRepo())),
            "Talk" to ItemTabView(ItemTabController(TalkDataRepo())),
            "Skill" to ItemTabView(ItemTabController(SkillDataRepo())),
            "Scene" to ItemTabView(ItemTabController(SceneSkillDataRepo()))
    )

    init {
        with(root) {
            dataSet.keys.forEach {
                tab(it, dataSet[it]!!.root)
            }
        }

    }
}