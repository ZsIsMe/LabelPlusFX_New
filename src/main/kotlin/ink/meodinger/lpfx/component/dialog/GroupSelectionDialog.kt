package ink.meodinger.lpfx.component.dialog

import ink.meodinger.lpfx.I18N
import ink.meodinger.lpfx.component.CGroup
import ink.meodinger.lpfx.type.TransGroup
import ink.meodinger.lpfx.get
import javafx.beans.property.SimpleObjectProperty
import javafx.event.ActionEvent
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.layout.FlowPane
import javafx.scene.layout.VBox
import javafx.scene.text.Text

/**
 * 分組選擇對話框
 * 顯示所有分組，用戶點擊任何分組即可直接選擇，無需確認
 */
class GroupSelectionDialog(
    private val groups: List<TransGroup>,
    private val dialogTitle: String = I18N["context.move_to.dialog.title"],
    private val headerText: String = I18N["context.move_to.dialog.header"]
) : Dialog<TransGroup>() {

    private val selectedGroupProperty = SimpleObjectProperty<TransGroup>(null)
    
    init {
        initUI()
        setupEventHandlers()
    }

    private fun initUI() {
        this.title = dialogTitle
        this.isResizable = false
        
        // 創建頭部文本
        val headerLabel = Text(headerText).apply {
            wrappingWidth = 300.0
            style = "-fx-font-size: 14px;"
        }
        
        // 創建分組選擇面板
        val groupsPane = FlowPane().apply {
            hgap = 8.0
            vgap = 8.0
            alignment = Pos.CENTER
            prefWrapLength = 300.0
        }
        
        // 為每個分組創建選擇按鈕
        groups.forEach { group ->
            val groupButton = CGroup().apply {
                nameProperty().bind(group.nameProperty())
                colorProperty().bind(group.colorProperty())
                
                // 點擊時直接選擇該分組並關閉對話框
                setOnAction { _ ->
                    selectedGroupProperty.set(group)
                    result = group
                    close()
                }
            }
            groupsPane.children.add(groupButton)
        }
        
        // 主內容面板
        val content = VBox(10.0).apply {
            padding = Insets(20.0)
            alignment = Pos.CENTER
            children.addAll(headerLabel, groupsPane)
        }
        
        dialogPane.content = content
        dialogPane.buttonTypes.add(ButtonType.CANCEL)
    }
    
    private fun setupEventHandlers() {
        // 設置結果轉換器
        setResultConverter { buttonType ->
            if (buttonType == ButtonType.CANCEL) null else selectedGroupProperty.get()
        }
    }
} 