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
import javafx.scene.layout.VBox
import javafx.scene.layout.Priority
import javafx.scene.paint.Color


/**
 * Author: Meodinger
 * Date: 2021/9/30
 * Have fun with my code!
 */

/**
 * A VBox to display groups for quick move functionality vertically
 */
class CRightGroupBar : VBox() {

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

    // region Instance

    private val holder = VBox().apply {
        vgrow = Priority.ALWAYS
    }

    // endregion

    init {
        // 設置右側分組欄樣式：固定寬度，垂直排列
        style = "-fx-background-color: lightgray; -fx-border-color: gray; -fx-border-width: 1px; -fx-padding: 8px;"
        prefWidth = 200.0
        maxWidth = 200.0
        minWidth = 200.0
        spacing = 8.0  // 添加按鈕間距
        
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
        if (children.isEmpty()) {
            children.add(holder)
        }

        val node = CGroup().apply {
            nameProperty().bind(transGroup.nameProperty())
            colorProperty().bind(transGroup.colorProperty())
            setOnAction { 
                // Create ActionEvent with TransGroup as source
                val event = ActionEvent(transGroup, it.target)
                onGroupMove.handle(event)
                isSelected = false
            }
            
            // 右側分組欄按鈕樣式：固定寬度，單行顯示，大字體
            isLargeFont = true
            isRightGroupBar = true
            
            // 設置按鈕的固定尺寸
            minHeight = 40.0
            prefHeight = 40.0
            maxHeight = 40.0
            // 移除固定寬度設置，讓CGroup動態計算寬度
            // minWidth = 180.0
            // prefWidth = 180.0
            // maxWidth = 180.0
        }
        children.add(groupId, node)
    }
    
    private fun removeGroupItem(transGroup: TransGroup) {
        if (children.size == 2) {
            children.removeLast()
        }

        val node = children.filterIsInstance(CGroup::class.java).firstOrNull { it.name == transGroup.name }
        if (node != null) {
            node.nameProperty().unbind()
            node.colorProperty().unbind()
            children.remove(node)
        }
    }

} 