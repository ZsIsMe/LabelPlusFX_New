package ink.meodinger.lpfx.component

import ink.meodinger.lpfx.NOT_FOUND
import ink.meodinger.lpfx.type.TransGroup
import ink.meodinger.lpfx.util.component.vgrow
import ink.meodinger.lpfx.util.property.getValue
import ink.meodinger.lpfx.util.property.setValue
import ink.meodinger.lpfx.util.property.onNew

import javafx.beans.property.*
import javafx.collections.*
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.scene.paint.Color


/**
 * Author: Meodinger
 * Date: 2021/9/30
 * Have fun with my code!
 */

/**
 * A GridPane to display groups for quick move functionality in 3 columns with series-based layout
 */
class CRightGroupBar : GridPane() {

    companion object {
        private const val COLUMNS = 3  // 固定3列佈局
        private const val FIRST_COLUMN_WIDTH = 180.0  // 第一列固定寬度，適合顯示基礎分組名
        private const val FIXED_COLUMN_WIDTH = 60.0  // 第二、第三列固定寬度，夠放4個字符
    }

    // region Properties

    private val groupsProperty: ListProperty<TransGroup> = SimpleListProperty(FXCollections.emptyObservableList())
    /**
     * The TransGroups to display
     */
    fun groupsProperty(): ListProperty<TransGroup> = groupsProperty
    /**
     * @see groupsProperty
     */
    var groups: ObservableList<TransGroup> by groupsProperty

    private val onGroupMoveProperty: ObjectProperty<EventHandler<ActionEvent>> = SimpleObjectProperty(EventHandler {})
    /**
     * How to handle GroupMove (Click on group to move selected labels)
     */
    fun onGroupMoveProperty(): ObjectProperty<EventHandler<ActionEvent>> = onGroupMoveProperty
    /**
     * @see onGroupMoveProperty
     */
    val onGroupMove: EventHandler<ActionEvent> by onGroupMoveProperty
    /**
     * @see onGroupMoveProperty
     */
    fun setOnGroupMove(handler: EventHandler<ActionEvent>) = onGroupMoveProperty.set(handler)

    // endregion

    init {
        // 設置右側分組欄樣式：3列網格佈局，合理控制總寬度
        style = "-fx-background-color: lightgray; -fx-border-color: gray; -fx-border-width: 1px; -fx-padding: 8px;"
        prefWidth = 320.0  // 180 + 60 + 60 + 間距，適中的寬度
        maxWidth = 320.0
        minWidth = 320.0
        hgap = 8.0  // 列間距
        vgap = 8.0  // 行間距
        
        // 設置列寬：所有列都使用固定寬度
        columnConstraints.clear()
        // 第一列：固定寬度
        columnConstraints.add(javafx.scene.layout.ColumnConstraints().apply {
            prefWidth = FIRST_COLUMN_WIDTH
            minWidth = FIRST_COLUMN_WIDTH
            maxWidth = FIRST_COLUMN_WIDTH
        })
        // 第二列：固定寬度
        columnConstraints.add(javafx.scene.layout.ColumnConstraints().apply {
            prefWidth = FIXED_COLUMN_WIDTH
            minWidth = FIXED_COLUMN_WIDTH
            maxWidth = FIXED_COLUMN_WIDTH
        })
        // 第三列：固定寬度
        columnConstraints.add(javafx.scene.layout.ColumnConstraints().apply {
            prefWidth = FIXED_COLUMN_WIDTH
            minWidth = FIXED_COLUMN_WIDTH
            maxWidth = FIXED_COLUMN_WIDTH
        })
        
        groupsProperty.addListener(ListChangeListener {
            while (it.next()) {
                if (it.wasPermutated()) {
                    // will not happen
                    throw IllegalStateException("Permuted: $it")
                } else if (it.wasUpdated()) {
                    // Ignore, TransGroup's Property changed,
                } else {
                    if (it.wasRemoved()) {
                        // 完全重新佈局
                        relayoutAllGroups()
                    }
                    if (it.wasAdded()) {
                        // 完全重新佈局
                        relayoutAllGroups()
                    }
                }
            }
        })
    }

    /**
     * 提取分組的基礎名稱（去掉+2、+4等後綴）
     */
    private fun getBaseName(groupName: String): String {
        return groupName.replace(Regex("\\+\\d+$"), "")
    }

    /**
     * 獲取分組名稱的排序權重（基礎名稱權重為0，+2為1，+4為2，以此類推）
     */
    private fun getSortWeight(groupName: String): Int {
        val match = Regex("\\+(\\d+)$").find(groupName)
        return if (match != null) {
            match.groupValues[1].toInt() / 2  // +2->1, +4->2, +6->3...
        } else {
            0  // 基礎名稱權重最低
        }
    }

    /**
     * 獲取簡化顯示的文本（同系列第二個之後只顯示後綴）
     */
    private fun getDisplayText(groupName: String, isFirstInSeries: Boolean): String {
        return if (isFirstInSeries) {
            groupName  // 系列第一個顯示完整名稱
        } else {
            val match = Regex("\\+(\\d+)$").find(groupName)
            if (match != null) {
                "+${match.groupValues[1]}"  // 後續顯示只顯示後綴
            } else {
                groupName  // 沒有後綴的保持原樣
            }
        }
    }

    /**
     * 按系列重新佈局所有分組
     */
    private fun relayoutAllGroups() {
        // 清除所有現有節點
        children.clear()
        
        if (groups.isEmpty()) return
        
        // 按基礎名稱分組
        val seriesMap = groups.groupBy { getBaseName(it.name) }
        
        // 對每個系列內部排序（基礎名稱在前，+2、+4等在後）
        val sortedSeries = seriesMap.mapValues { (_, seriesGroups) ->
            seriesGroups.sortedBy { getSortWeight(it.name) }
        }
        
        // 按系列的第一個分組在原列表中的位置排序，保持原有順序
        val seriesOrder = sortedSeries.keys.sortedBy { baseName ->
            groups.indexOfFirst { getBaseName(it.name) == baseName }
        }
        
        var currentRow = 0
        
        // 為每個系列分配行
        for (baseName in seriesOrder) {
            val seriesGroups = sortedSeries[baseName] ?: continue
            
            // 為當前系列的所有分組創建按鈕並佈局
            seriesGroups.forEachIndexed { index, group ->
                val column = index % COLUMNS
                val row = currentRow + (index / COLUMNS)
                val isFirstInSeries = (index == 0)
                
                createGroupItem(group, column, row, isFirstInSeries)
            }
            
            // 計算當前系列佔用的行數
            val seriesRows = (seriesGroups.size + COLUMNS - 1) / COLUMNS
            currentRow += seriesRows
        }
    }

    private fun createGroupItem(transGroup: TransGroup, column: Int, row: Int, isFirstInSeries: Boolean) {
        val node = CGroup().apply {
            // 顏色綁定保持原來的邏輯
            colorProperty().bind(transGroup.colorProperty())
            
            // 設置顯示文本：第一個顯示完整名稱，後續只顯示後綴
            val displayText = getDisplayText(transGroup.name, isFirstInSeries)
            name = displayText
            
            setOnAction { 
                // Create ActionEvent with TransGroup as source
                val event = ActionEvent(transGroup, it.target)
                onGroupMove.handle(event)
                isSelected = false
            }
            
            // 保持原來的按鈕樣式：大字體，40px高度，單行顯示
            isLargeFont = true
            isRightGroupBar = true
            
            // 設置按鈕的固定尺寸，保持原來的樣式
            minHeight = 40.0
            prefHeight = 40.0
            maxHeight = 40.0
            maxWidth = Double.MAX_VALUE  // 允許按鈕填滿列寬
        }
        
        add(node, column, row)
    }
    
    private fun removeGroupItem(transGroup: TransGroup) {
        val node = children.filterIsInstance(CGroup::class.java).firstOrNull { it.name == transGroup.name }
        if (node != null) {
            node.nameProperty().unbind()
            node.colorProperty().unbind()
            children.remove(node)
        }
    }

} 