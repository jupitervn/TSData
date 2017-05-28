package vn.jupiter.tsdata.view

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.scene.control.SelectionMode
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import javafx.util.Callback
import tornadofx.*
import vn.jupiter.tsdata.controller.ItemTabController
import vn.jupiter.tsdata.data.TSModel


/**
 * Created by jupiter on 5/25/17.
 */
class ItemTabView<T : TSModel>(val controller: ItemTabController<T>) : View() {
    var sourceFileTF : TextField by singleAssign()
    var destFileTF: TextField by singleAssign()
    override val root = vbox {
        prefWidth = 800.0
        prefHeight = 600.0
        hbox {
            sourceFileTF = textfield {
                hboxConstraints {
                    marginLeftRight(20.0)
                    hGrow = Priority.ALWAYS
                }
            }

            button("Choose...") {
                action {
                    val fileList = chooseFile("Open Chinese File", arrayOf(FileChooser.ExtensionFilter("Only .dat", "*.dat"))) {

                    }
                    if (fileList.isNotEmpty()) {
                        sourceFileTF.text = fileList[0].absolutePath
                    }
                }
            }

            destFileTF = textfield {
                hboxConstraints {
                    marginLeftRight(20.0)
                    hGrow = Priority.ALWAYS
                }
            }

            button("Choose...") {
                action {
                    val fileList = chooseFile("Open VN File", arrayOf(FileChooser.ExtensionFilter("Only .dat", "*.Dat")))
                    if (fileList.isNotEmpty()) {
                        destFileTF.text = fileList[0].absolutePath
                    }
                }
            }
        }
        hbox {
            button("Load data") {
                action {
                    runAsync {
//                        controller.loadItems("/Users/jupiter/Parallels/Game Tools/VH/Item.Dat", "/Users/jupiter/Parallels/Game Tools/VHCD/Item.Dat")
//                        controller.loadItems("/Users/jupiter/Parallels/Game Tools/Original/Item.Dat", "")
                        controller.loadItems(sourceFileTF.text, destFileTF.text)

                    } ui { items ->
                        displayItems(items)
                    }
                }
            }

            button("L<--R") {
                action {
                    runAsync {
                        fillData()
                    } ui {
                        dataTableView?.requestResize()
                    }
                }
            }
            button("<-->") {
                action {
//                    controller.loadItems("/Users/jupiter/Parallels/Game Tools/Item_new.Dat", "")
                }
            }

            button("Filter") {
                action {
                    controller.toggleFilter()
                }
            }


            button("Save data") {
                action {
                    controller.saveChineseData(sourceFileTF.text + "_new")
                }
            }
        }
    }

    var dataTableView: TableView<Pair<T?, T?>>? = null


    init {
        dataTableView = tableview(FXCollections.emptyObservableList()) {  }
        root += dataTableView as TableView<Pair<T?, T?>>
    }

    private fun displayItems(items: ObservableList<Pair<T?, T?>>) {
        if (dataTableView != null) {
            root.children -= dataTableView
        }
        val dataView = tableview(items) {
            vboxConstraints {
                vgrow = Priority.ALWAYS
            }
            column<Pair<T?, T?>, Int>("ID", valueProvider = { feature ->
                (feature.value.first ?: feature.value.second)?.id.toProperty()
            })
            val chineseNameColumn = column<Pair<T?, T?>, String>("Name", valueProvider = { feature ->
                feature.value.first?.name.toProperty()
            }).makeEditable()
            val chineseDescColumn = column<Pair<T?, T?>, String>("Description", valueProvider = { feature ->
                feature.value.first?.description.toProperty()
            }).makeEditable()
            chineseNameColumn.onEditCommit = EventHandler { event ->
                event.rowValue.first?.name = event.newValue
            }
            val existingFactory = chineseNameColumn.cellFactory
            chineseNameColumn.cellFactory = Callback {
                val cell = existingFactory.call(it)
                cell.itemProperty().onChange {
                    if (it != null) {
                        val (first, second) = cell.rowItem
                        if (first == null || second == null || first.hasStrangeName() || second.hasStrangeName()
                                || !first.name.contentEquals(second.name) || !first.description.contentEquals(second.description)) {
                            cell.background = Background(BackgroundFill(Color.RED, null, null))
                        } else {
                            cell.background = Background.EMPTY
                        }
                    }
                }
                cell
            }

            chineseDescColumn.onEditCommit = EventHandler { event ->
                event.rowValue.first?.description = event.newValue
            }
            column<Pair<T?, T?>, String>("NameVH", valueProvider = { feature ->
                feature.value.second?.name.toProperty()
            })

            column<Pair<T?, T?>, String>("DescVH", valueProvider = { feature ->
                feature.value.second?.description.toProperty()
            })
            enableCellEditing()
            columnResizePolicy = SmartResize.POLICY
            selectionModel.selectionMode = SelectionMode.MULTIPLE

            contextmenu {
                item("<<<") {
                    action {
                        if (selectionModel.selectedCells.count() > 0) {
                            val firstItem = selectionModel.selectedCells.get(0)
                            val selectedItems = selectionModel.selectedItems.asSequence()
                            val columnCount = firstItem.tableView.columns.size
                            when {
                                firstItem.column == columnCount - 1 -> controller.swapDescRightToLeft(selectedItems)
                                firstItem.column == columnCount - 2 -> controller.swapNameRightToLeft(selectedItems)
                            }
                        }
                    }
                }

                item("CD -> KD") {
                    action {
                        if (selectionModel.selectedCells.count() > 0) {
                            val firstItem = selectionModel.selectedCells.get(0)
                            val selectedItems = selectionModel.selectedItems.asSequence()
                            val columnCount = firstItem.tableView.columns.size
                            when {
//                                firstItem.column == columnCount - 1 -> controller.convertCDKD(selectedItems)
                                firstItem.column == columnCount - 2 -> controller.convertNameCDKD(selectedItems)
                            }
                        }
                    }
                }

                item("Delete") {
                    action {
                        if (selectionModel.selectedCells.count() > 0) {
                            val selectedItems = selectionModel.selectedItems.asSequence()
                            controller.deleteRightItems(selectedItems)

                        }
                    }
                }
            }
        }
        root += dataView
        dataView.contextMenu
        dataTableView = dataView
    }

    private fun fillData() {
        controller.fillAllRightToLeft()
    }
}