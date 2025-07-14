package ink.meodinger.lpfx.component

import ink.meodinger.lpfx.I18N
import ink.meodinger.lpfx.get
import ink.meodinger.lpfx.State
import ink.meodinger.lpfx.action.ActionType
import ink.meodinger.lpfx.action.FunctionAction
import ink.meodinger.lpfx.action.LabelAction
import ink.meodinger.lpfx.options.Logger
import ink.meodinger.lpfx.options.Settings
import ink.meodinger.lpfx.type.TransLabel
import ink.meodinger.lpfx.util.property.onChange
import javafx.event.EventHandler
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.input.Clipboard
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent

class CTextMenu(
    private val textField: TextInputControl,
    private val state: State? = null,
    private val view: CTreeView? = null
) : ContextMenu() {

    //    add default menu items
    private val undoMI = MenuItem(I18N["input.menu.undo"]).apply {
        onAction = EventHandler { textField.undo() }
    }
    private val redoMI = MenuItem(I18N["input.menu.redo"]).apply {
        onAction = EventHandler { textField.redo() }
    }
    private val cutMI = MenuItem(I18N["input.menu.cut"]).apply {
        onAction = EventHandler { textField.cut() }
    }
    private val copyMI = MenuItem(I18N["input.menu.copy"]).apply {
        onAction = EventHandler { textField.copy() }
    }
    private val pasteMI = MenuItem(I18N["input.menu.paste"]).apply {
        onAction = EventHandler { textField.paste() }
    }
    private val deleteMI = MenuItem(I18N["input.menu.delete_selection"]).apply {
        onAction = EventHandler { deleteSelectedText(textField) }
    }
    private val selectAllMI = MenuItem(I18N["input.menu.select_all"]).apply {
        onAction = EventHandler { textField.selectAll() }
    }

    //    add custom menu items
    private val quickInput = Menu(I18N["input.menu.quick_input"])
    
    // 分割選中文本菜單項
    private val splitSelectedTextItem = MenuItem(I18N["input.menu.split_selected_text"]).apply {
        onAction = EventHandler { splitSelectedText() }
        accelerator = KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN)
    }


    init {
        textField.undoableProperty()
            .addListener { _, _, newValue ->
                undoMI.isDisable =
                    !newValue!!
            }
        textField.redoableProperty()
            .addListener { _, _, newValue ->
                redoMI.isDisable =
                    !newValue!!
            }
        textField.selectionProperty()
            .addListener { _, _, newValue ->
                cutMI.isDisable =
                    newValue.length == 0
                copyMI.isDisable = newValue.length == 0
                deleteMI.isDisable = newValue.length == 0
                selectAllMI.isDisable = newValue.length == newValue.end
                
                // 只有在有選中文本且state和view不為null時才啟用分割選中文本菜單項
                if (state != null && view != null) {
                    splitSelectedTextItem.isDisable = newValue.length == 0
                }
            }
            
        // 添加 Command+P 快捷鍵支持
        if (state != null && view != null) {
            textField.addEventFilter(KeyEvent.KEY_PRESSED) { event ->
                if ((event.isMetaDown || event.isControlDown) && event.code == KeyCode.P) {
                    // 檢查是否有選中文本
                    if (textField.selection.length > 0) {
                        event.consume() // 消費事件避免其他處理
                        splitSelectedText()
                    }
                }
            }
        }

        //init QuickInputItems
        initQuickInputItems(quickInput)
        
        // 根據是否有state和view決定是否顯示分割選中文本菜單項
        if (state != null && view != null) {
            textField.contextMenu = ContextMenu(
                undoMI, redoMI, cutMI, copyMI, pasteMI, deleteMI, SeparatorMenuItem(), selectAllMI, quickInput, SeparatorMenuItem(), splitSelectedTextItem
            )
        } else {
            textField.contextMenu = ContextMenu(
                undoMI, redoMI, cutMI, copyMI, pasteMI, deleteMI, SeparatorMenuItem(), selectAllMI, quickInput
            )
        }
    }

    private fun deleteSelectedText(t: TextInputControl) {
        val range = t.selection
        if (range.length == 0) {
            return
        }
        val text = t.text
        val newText = text.substring(0, range.start) + text.substring(range.end)
        t.text = newText
        t.positionCaret(range.start)
    }

    private fun initQuickInputItems(menu: Menu) {
        menu.items.clear()
        menu.items.addAll(getQuickInputItems(textField))
        Settings.quickInputTextsProperty.addListener( onChange {
            Logger.info("refresh the quick input items", "CTextMenu")
            menu.items.clear()
            menu.items.addAll(getQuickInputItems(textField))
        })
    }

    private fun getQuickInputItems(t: TextInputControl): List<MenuItem> {
        return Settings.quickInputTexts.map {
            MenuItem(it).apply {
                onAction =  EventHandler { t.insertText(t.caretPosition, text) }
            }
        }
    }

    /**
     * 分割選中文本功能實現
     */
    private fun splitSelectedText() {
        if (state == null || view == null) return
        
        // 檢查是否有開啟的檔案
        if (!state.isOpened) return
        
        // 檢查是否有選中的文本
        val selection = textField.selection
        if (selection.length == 0) return
        
        // 獲取當前選中的標籤
        val selectedItems = view.selectionModel.selectedItems
            .filterIsInstance<CTreeLabelItem>()
        
        if (selectedItems.isEmpty()) return
        
        // 獲取選中的文本和位置
        val selectedText = textField.selectedText
        val selectionStart = selection.start
        val selectionEnd = selection.end
        
        // 檢查選中文本前面是否有換行符
        val fullText = textField.text
        val hasNewlineBefore = selectionStart > 0 && 
            (fullText[selectionStart - 1] == '\n' || fullText[selectionStart - 1] == '\r')
        
        // 如果前面有換行符，擴展選中範圍包含換行符
        val actualStart = if (hasNewlineBefore) {
            // 檢查是否是\r\n的情況
            if (selectionStart > 1 && fullText[selectionStart - 2] == '\r' && fullText[selectionStart - 1] == '\n') {
                selectionStart - 2
            } else {
                selectionStart - 1
            }
        } else {
            selectionStart
        }
        
        // 手動選中包含換行符的範圍並剪切
        textField.selectRange(actualStart, selectionEnd)
        textField.cut()
        
        // 去掉剪切文本前的換行符（確保新標籤文本乾淨）
        val clipboardText = selectedText.trimStart('\n', '\r')
        
        // 獲取最後一個（最右側）選中的標籤
        val lastSelectedLabel = selectedItems.maxByOrNull { it.transLabel.x }
            ?: return
        
        // 計算新標籤的位置
        // 在最後一個標籤左邊大概1個Label寬度的位置
        val labelRadius = Settings.labelRadius
        val labelWidth = labelRadius * 2
        
        // 從State中獲取當前圖片的尺寸信息
        val currentPicFile = state.getPicFileNow()
        if (!currentPicFile.exists()) return
        
        val currentImage = Image(currentPicFile.toURI().toString())
        val imageWidth = currentImage.width
        
        val offset = labelWidth * 1 / imageWidth
        
        val newX = (lastSelectedLabel.transLabel.x - offset).coerceIn(0.0, 1.0)
        val newY = lastSelectedLabel.transLabel.y // 相同高度
        
        // 使用與最後選中標籤相同的分組
        val groupId = lastSelectedLabel.transLabel.groupId
        
        // 計算新的標籤索引：找到選中標籤中最大的索引，然後加1
        val maxSelectedIndex = selectedItems.maxOfOrNull { it.transLabel.index } ?: 0
        val newIndex = maxSelectedIndex + 1
        
        // 創建新的標籤
        val newLabel = TransLabel(newIndex, groupId, newX, newY, clipboardText)
        
        // 執行添加標籤的動作
        val labelAction = LabelAction(
            ActionType.ADD, state,
            state.currentPicName,
            newLabel
        )
        
        // 創建包含選中新標籤的複合動作
        val createAction = FunctionAction(
            { 
                labelAction.commit()
                // 選中新創建的標籤
                view.selectLabel(newIndex, clear = true, scrollTo = true)
            },
            { 
                labelAction.revert()
            }
        )
        
        state.doAction(createAction)
    }

}
