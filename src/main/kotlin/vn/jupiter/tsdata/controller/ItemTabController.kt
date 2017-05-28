package vn.jupiter.tsdata.controller

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import tornadofx.*
import vn.jupiter.tsdata.data.*
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.charset.Charset

/**
 * Created by jupiter on 5/25/17.
 */
val AUTO = 0
abstract class DataRepo<T : TSModel>(val headerSize:Int = AUTO, var itemSize:Int = AUTO) {
    var headerData:ByteArray = ByteArray(headerSize)

    fun getItems(filePath: String): List<T> {
        val itemList = mutableListOf<T>()
        if (File(filePath).exists()) {
            val randomAccessFile = RandomAccessFile(filePath, "r")
            randomAccessFile.seek(0)
            if (headerSize != AUTO) {
                headerData = ByteArray(headerSize)
                randomAccessFile.readFully(headerData)
            }
            if (itemSize == AUTO) {
                var itemByteArraySize = 0
                var currentByte: Byte
                do {
                    currentByte = randomAccessFile.readByte()
                    itemByteArraySize++
                } while (currentByte == 0x0.toByte() || currentByte == 0x2.toByte())
                itemSize = itemByteArraySize - 1
                if (itemSize < 0) {
                    throw RuntimeException("Cannot identify item size")
                }
                randomAccessFile.seek(headerSize.toLong())
            }
            println("Identify item size $itemSize")
            var charSet = Charset.forName("Big5")
            if (filePath.contains("VH")) {
                charSet = Charset.forName("windows-1258")
            }
            val itemArray = ByteArray(itemSize)
            while (randomAccessFile.filePointer + itemSize <= randomAccessFile.length()) {
                randomAccessFile.readFully(itemArray)
                val byteBuffer = ByteBuffer.allocate(itemSize)
                byteBuffer.put(itemArray)
                val item = createNewItem(byteBuffer, itemSize, charSet)
                itemList += item
//                println("Read ${randomAccessFile.filePointer} ${item}")
            }
            println("Read ${itemList.size} items ${randomAccessFile.filePointer}/${randomAccessFile.length()}")
            randomAccessFile.close()
        }
        return itemList
    }

    fun saveItems(outputFile: String, data: List<T>) {
        val targetFile = File(outputFile)
        if (targetFile.exists()) {
            targetFile.delete()
        }
        val randomAccessFile = RandomAccessFile(outputFile, "rw")
        randomAccessFile.write(headerData)
        val byteArray = ByteArray(itemSize)
        data.forEach { value ->
            value.byteData.position(0)
            value.byteData.get(byteArray, 0, itemSize)
            randomAccessFile.write(byteArray)
            println("Write ${value}")
        }
        println("Write ${data.size} items")
        randomAccessFile.close()
    }

    abstract fun createNewItem(byteBuffer: ByteBuffer, itemSize: Int, charSet: Charset): T
}

class ItemInfoDataRepo : DataRepo<Item>(headerSize = 370, itemSize = 370) {
    override fun createNewItem(byteBuffer: ByteBuffer, itemSize: Int, charSet: Charset): Item = Item(byteBuffer, itemSize, charSet)
}

class NpcInfoDataRepo : DataRepo<NPC>() {
    override fun createNewItem(byteBuffer: ByteBuffer, itemSize: Int, charSet: Charset): NPC = NPC(byteBuffer, itemSize, charSet)
}

class TalkDataRepo : DataRepo<Talk>() {
    override fun createNewItem(byteBuffer: ByteBuffer, itemSize: Int, charSet: Charset): Talk = Talk(byteBuffer, itemSize, charSet)
}

class SkillDataRepo : DataRepo<Skill>() {
    override fun createNewItem(byteBuffer: ByteBuffer, itemSize: Int, charSet: Charset): Skill = Skill(byteBuffer, itemSize, charSet)
}

class SceneSkillDataRepo : DataRepo<Scene>(headerSize = 134, itemSize = 134) {
    override fun createNewItem(byteBuffer: ByteBuffer, itemSize: Int, charSet: Charset): Scene = Scene(byteBuffer, itemSize, charSet)
}


class ItemTabController<T : TSModel>(val leftRepo: DataRepo<T>, val rightRepo: DataRepo<T> = leftRepo) : Controller() {
    var isFiltered: Boolean = false
    var leftData = mutableListOf<T>()
    var rightData = mutableListOf<T>()
    val observableList = FXCollections.observableArrayList<Pair<T?, T?>>()
    fun loadItems(originalPath: String, vhPath: String): ObservableList<Pair<T?, T?>> {
        observableList.clear()
        leftData.clear()
        rightData.clear()
        leftData.addAll(leftRepo.getItems(originalPath))
        rightData.addAll(rightRepo.getItems(vhPath))
        val backingMap = buildItems()
        observableList.addAll(backingMap.values)
        observableList.sortBy { pair ->
            (pair.first ?: pair.second)?.id
        }
        return observableList
    }

    private fun buildItems(): MutableMap<Int, Pair<T?, T?>> {
        val backingMap = mutableMapOf<Int, Pair<T?, T?>>()

        leftData.forEach {item ->
            val id = item.id
            val existingPair = backingMap[id]
            if (existingPair != null) {
                backingMap[id] = Pair(item, existingPair.second)
            } else {
                backingMap[id] = Pair(item, null)
            }
        }

        rightData.forEach { item ->
            val id = item.id
            val existingPair = backingMap[id]
            if (existingPair != null) {
                backingMap[id] = Pair(existingPair.first, item)
            } else {
                backingMap[id] = Pair(null, item)
            }
        }
        return backingMap
    }

    fun fillAllRightToLeft() {
        observableList.forEachIndexed { idx, (first, second) ->
            if (first != null) {
                val secondName = second?.name ?: ""
                if (!(secondName.isNumber() || secondName.hasChineseCharacter() || secondName.isEmpty())) {
                    first.name = second!!.getNameKD()
                    first.description = second.description
                    observableList[idx] = Pair(first, second)
                }
            } else {
                if (second != null) {
                    leftData.add(second)
                    observableList[idx] = Pair(second, second)
                }
            }
        }
    }

    fun saveChineseData(outputFile: String) {
        leftRepo.saveItems(outputFile, leftData)
    }

    fun mergeData() {

    }

    fun swapNameRightToLeft(selectedItems: Sequence<Pair<T?, T?>>) {
        val notifyIds = mutableMapOf<Int, Pair<T?, T?>>()
        selectedItems.forEach {
            val leftItem = it.first
            val rightItem = it.second
            if (rightItem != null) {
                if (leftItem != null) {
                    leftItem.name = rightItem.getNameKD()
                } else {
                    leftData.add(rightItem)
                }
                val notifyIdx = observableList.indexOfFirst { pair ->
                    pair.first?.id == rightItem.id || pair.second?.id == rightItem.id
                }
                if (notifyIdx > -1) {
                    notifyIds[notifyIdx] = Pair(leftItem ?: rightItem, rightItem)
                }
            }
        }
        notifyIds.forEach { idx, pairItems ->
            observableList[idx] = pairItems
        }
    }

    fun swapDescRightToLeft(selectedItems: Sequence<Pair<T?, T?>>) {
        val notifyIds = mutableMapOf<Int, Pair<T?, T?>>()
        selectedItems.forEach {
            val leftItem = it.first
            val rightItem = it.second
            if (rightItem != null) {
                if (leftItem != null) {
                    leftItem.description = rightItem.getDesKD()
                } else {
                    leftData.add(rightItem)
                }
                val notifyIdx = observableList.indexOfFirst { pair ->
                    pair.first?.id == rightItem.id || pair.second?.id == rightItem.id
                }
                if (notifyIdx > -1) {
                    notifyIds[notifyIdx] = Pair(leftItem ?: rightItem, rightItem)
                }
            }
        }
        notifyIds.forEach { idx, pairItems ->
            observableList[idx] = pairItems
        }
    }



    fun toggleFilter() {
        observableList.clear()
        isFiltered = !isFiltered
        val backingMap = buildItems()
        val values = backingMap.values
        if (isFiltered) {
            values.removeIf({ pair ->
                val first = pair.first
                val second = pair.second
                !(first == null || second == null || first.hasStrangeName() || second.hasStrangeName())
            })
        }
        observableList.addAll(values)
        observableList.sortBy { pair ->
            (pair.first ?: pair.second)?.id
        }

    }

    fun convertNameCDKD(selectedItems: Sequence<Pair<T?, T?>>) {
        val notifyIds = mutableMapOf<Int, Pair<T?, T?>>()
        selectedItems.forEach {
            val leftItem = it.first
            val rightItem = it.second
            if (rightItem != null) {
                rightItem.name = rightItem.getNameKD()
                val notifyIdx = observableList.indexOfFirst { pair ->
                    pair.second?.id == rightItem.id
                }
                if (notifyIdx > -1) {
                    notifyIds[notifyIdx] = Pair(leftItem, rightItem)
                }
            }
        }
        notifyIds.forEach { idx, pairItems ->
            observableList[idx] = pairItems
        }
    }

    fun deleteRightItems(selectedItems: Sequence<Pair<T?, T?>>) {
        val notifyIds = mutableMapOf<Int, Pair<T?, T?>>()
        selectedItems.forEach {
            val leftItem = it.first
            val rightItem = it.second
            if (rightItem != null) {
                val notifyIdx = observableList.indexOfFirst { pair ->
                    pair.second?.id == rightItem.id
                }
                if (notifyIdx > -1) {
                    notifyIds[notifyIdx] = Pair(leftItem, null)
                }
            }
        }
        notifyIds.forEach { idx, pairItems ->
            observableList[idx] = pairItems
        }
    }
}

fun String.isNumber(): Boolean = toIntOrNull() != null

