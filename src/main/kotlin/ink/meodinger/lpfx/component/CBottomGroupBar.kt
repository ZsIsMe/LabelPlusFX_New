package ink.meodinger.lpfx.component

import ink.meodinger.lpfx.NOT_FOUND
import ink.meodinger.lpfx.type.TransGroup
import ink.meodinger.lpfx.util.component.hgrow
import ink.meodinger.lpfx.util.property.getValue
import ink.meodinger.lpfx.util.property.setValue
import ink.meodinger.lpfx.util.property.onNew

import javafx.beans.property.*
import javafx.collections.*
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.paint.Color


/**
 * Author: Meodinger
 * Date: 2021/9/30
 * Have fun with my code!
 */

/**
 * A ToolBar to display groups for quick move functionality
 */
class CBottomGroupBar : HBox() {

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

    private val holder = HBox().apply {
        hgrow = Priority.ALWAYS
    }

    // endregion

    init {
        // 設置樣式以便看到組件邊界
        style = "-fx-background-color: lightgray; -fx-border-color: gray; -fx-border-width: 1px; -fx-padding: 4px;"
        prefHeight = 40.0
        
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