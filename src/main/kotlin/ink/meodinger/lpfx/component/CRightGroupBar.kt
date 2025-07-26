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
 * A GridPane to display groups for quick move functionality in 3 columns
 */
class CRightGroupBar : GridPane() {

    companion object {
        private const val COLUMNS = 3  // 固定3列佈局
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
        // 設置右側分組欄樣式：3列網格佈局，總寬度為原來的3倍
        style = "-fx-background-color: lightgray; -fx-border-color: gray; -fx-border-width: 1px; -fx-padding: 8px;"
        prefWidth = 600.0  // 原來200px的3倍
        maxWidth = 600.0
        minWidth = 600.0
        hgap = 8.0  // 列間距
        vgap = 8.0  // 行間距
        
        // 設置列寬自動平分
        repeat(COLUMNS) { columnIndex ->
            columnConstraints.add(javafx.scene.layout.ColumnConstraints().apply {
                percentWidth = 100.0 / COLUMNS
                hgrow = Priority.ALWAYS
            })
        }
        
        groupsProperty.addListener(ListChangeListener {
            while (it.next()) {
                if (it.wasPermutated()) {
                    // will not happen
                    throw IllegalStateException("Permuted: $it")
                } else if (it.wasUpdated()) {
                    // Ignore, TransGroup's Property changed,
                } else {
                    if (it.wasRemoved()) {
                        it.removed.forEach(this::removeGroupItem)
                    }
                    if (it.wasAdded()) {
                        it.addedSubList.forEachIndexed { index, group ->
                            createGroupItem(group, it.from + index)
                        }
                    }
                }
            }
        })
    }

    private fun createGroupItem(transGroup: TransGroup, groupId: Int) {
        val node = CGroup().apply {
            nameProperty().bind(transGroup.nameProperty())
            colorProperty().bind(transGroup.colorProperty())
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
        
        // 計算在網格中的位置
        val column = groupId % COLUMNS
        val row = groupId / COLUMNS
        
        add(node, column, row)
    }
    
    private fun removeGroupItem(transGroup: TransGroup) {
        val node = children.filterIsInstance(CGroup::class.java).firstOrNull { it.name == transGroup.name }
        if (node != null) {
            node.nameProperty().unbind()
            node.colorProperty().unbind()
            children.remove(node)
            
            // 重新排列剩餘的按鈕
            rearrangeItems()
        }
    }
    
    private fun rearrangeItems() {
        val groupNodes = children.filterIsInstance(CGroup::class.java)
        children.clear()
        
        groupNodes.forEachIndexed { index, node ->
            val column = index % COLUMNS
            val row = index / COLUMNS
            add(node, column, row)
        }
    }

} 