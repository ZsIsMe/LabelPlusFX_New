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
import javafx.scene.control.ToggleButton
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

    private val onToggleTranslationProperty: ObjectProperty<EventHandler<ActionEvent>> = SimpleObjectProperty(EventHandler {})
    /**
     * How to handle translation orientation toggle
     */
    fun onToggleTranslationProperty(): ObjectProperty<EventHandler<ActionEvent>> = onToggleTranslationProperty
    fun setOnToggleTranslation(handler: EventHandler<ActionEvent>) = onToggleTranslationProperty.set(handler)
    val onToggleTranslation: EventHandler<ActionEvent> by onToggleTranslationProperty


    // endregion

    // region Instance

    private val holder = HBox().apply {
        hgrow = Priority.ALWAYS
    }
    
    val toggleTranslationButton = ToggleButton("垂直翻譯").apply {
        minHeight = 50.0
        prefHeight = 50.0
        maxHeight = 50.0
        setOnAction { event ->
            // Pass the button's selected state as the source
            val actionEvent = ActionEvent(this, event.target)
            onToggleTranslation.handle(actionEvent)
        }
    }

    // endregion

    init {
        // 設置樣式以便看到組件邊界
        style = "-fx-background-color: lightgray; -fx-border-color: gray; -fx-border-width: 1px; -fx-padding: 4px;"
        prefHeight = 60.0
        maxHeight = 60.0
        spacing = 8.0  // 添加按鈕間距
        
        children.add(toggleTranslationButton)

        
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
            
            // 啟用大字體模式：2倍字體大小、加粗、可換行
            isLargeFont = true
            
            // 設置按鈕的最小和最大尺寸，確保不會超出容器
            minHeight = 50.0
            prefHeight = 50.0
            maxHeight = 50.0
            minWidth = 80.0
            prefWidth = 120.0
            maxWidth = 140.0
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