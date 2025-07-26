package ink.meodinger.lpfx.component.tools

import ink.meodinger.lpfx.*
import ink.meodinger.lpfx.action.ActionType
import ink.meodinger.lpfx.action.ComplexAction
import ink.meodinger.lpfx.action.LabelAction
import ink.meodinger.lpfx.util.component.add
import ink.meodinger.lpfx.util.component.closeOnEscape
import ink.meodinger.lpfx.util.component.gridHAlign
import ink.meodinger.lpfx.util.component.withContent
import ink.meodinger.lpfx.component.dialog.showConfirm
import ink.meodinger.lpfx.component.dialog.showInfo
import ink.meodinger.lpfx.util.property.getValue
import ink.meodinger.lpfx.util.property.setValue
import ink.meodinger.lpfx.util.string.emptyString

import javafx.beans.property.*
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.stage.Stage

/**
 * Author: Meodinger
 * Date: 2022/3/24
 * Have fun with my code!
 */
class SearchReplace(private val state: State) : Stage() {

    private data class FindResult(val picName: String, val labelIndex: Int, val range: IntRange)

    companion object {
        private const val BUTTON_WIDTH: Double = 96.0
    }

    private val searchTextProperty: StringProperty = SimpleStringProperty(emptyString())
    private var searchText: String by searchTextProperty

    private val replaceTextProperty: StringProperty = SimpleStringProperty(emptyString())
    private var replaceText: String by replaceTextProperty

    private val wrapFindProperty: BooleanProperty = SimpleBooleanProperty(true)
    private var wrapFind: Boolean by wrapFindProperty

    private val ignoreCaseProperty: BooleanProperty = SimpleBooleanProperty(false)
    private var ignoreCase: Boolean by ignoreCaseProperty

    init {
        icons.add(ICON)
        title = I18N["snr.title"]
        width = 500.0
        height = 280.0
        isResizable = false
        scene = Scene(StackPane().withContent(GridPane()) {
            padding = Insets(16.0)
            hgap = 16.0
            vgap = 8.0
            alignment = Pos.CENTER

            //    0        1            2
            // 0    Find: |          | FindNext
            // 1 Replace: |          | Replace
            // 2 O Backward            ReplaceAll
            // 3 O CaseInsensitive     Cancel
            add(Label(I18N["snr.text_search"]), 0, 0) {
                gridHAlign = HPos.RIGHT
            }
            add(Label(I18N["snr.text_replace"]), 0, 1) {
                gridHAlign = HPos.RIGHT
            }
            add(TextField(), 1, 0) {
                prefColumnCount = 20
                searchTextProperty.bind(textProperty())
            }
            add(TextField(), 1, 1) {
                prefColumnCount = 20
                replaceTextProperty.bind(textProperty())
            }
            add(CheckBox(I18N["snr.wrap_find"]), 0, 2, 2, 1) {
                isSelected = true
                wrapFindProperty.bind(selectedProperty())
            }
            add(CheckBox(I18N["snr.ignore_case"]), 0, 3, 2, 1) {
                isSelected = false
                ignoreCaseProperty.bind(selectedProperty())
            }

            add(VBox(8.0).apply {
                alignment = Pos.TOP_CENTER
                children.add(Button(I18N["snr.find_next"]).apply {
                    prefWidth = BUTTON_WIDTH
                    disableProperty().bind(searchTextProperty.isEmpty)
                    setOnAction { handleFindNext() }
                })
                children.add(Button(I18N["snr.replace"]).apply {
                    prefWidth = BUTTON_WIDTH
                    disableProperty().bind(searchTextProperty.isEmpty)
                    setOnAction { handleReplace() }
                })
                children.add(Button(I18N["snr.replace_all"]).apply {
                    prefWidth = BUTTON_WIDTH
                    disableProperty().bind(searchTextProperty.isEmpty)
                    setOnAction { handleReplaceAll() }
                })
                children.add(Button("修正波浪號").apply {
                    prefWidth = BUTTON_WIDTH
                    setOnAction { handleFixTildes() }
                })
                children.add(Button("全部’～->~").apply {
                    prefWidth = BUTTON_WIDTH
                    setOnAction { handleReplaceFullWidthTilde() }
                })
                children.add(Button("!->！").apply {
                    prefWidth = BUTTON_WIDTH
                    setOnAction { handleReplaceExclamation() }
                })
                children.add(Button(I18N["common.cancel"]).apply {
                    prefWidth = BUTTON_WIDTH
                    setOnAction { hide() }
                })
            }, 2, 0, 1, 4)
        })

        closeOnEscape()
    }

    private fun findNext(wrap: Boolean): FindResult? {
        // If in current label
        val currentLabel = state.transFile.getTransLabel(state.currentPicName, state.currentLabelIndex)
        val currentCaret = state.view.cTransArea.caretPosition
        val currentIndex = currentLabel.text.indexOf(searchText, currentCaret, ignoreCase)
        if (currentIndex != NOT_FOUND) {
            return FindResult(state.currentPicName, state.currentLabelIndex, currentIndex..(currentIndex + searchText.length))
        }

        // If in current picture
        val labelList = state.transFile.getTransList(state.currentPicName)
        for (i in (state.currentLabelIndex + 1) until labelList.size) {
            val index = labelList[i].text.indexOf(searchText, 0, ignoreCase)
            if (index != NOT_FOUND) {
                return FindResult(state.currentPicName, labelList[i].index, index..(index + searchText.length))
            }
        }

        // Find in the rest
        val picNames = state.transFile.sortedPicNames
        val picIndex = picNames.indexOf(state.currentPicName)
        for (i in (picIndex until picNames.size).let { if (wrap) it + (0 until picIndex) else it }) {
            for (label in state.transFile.getTransList(picNames[i])) {
                val index = label.text.indexOf(searchText, 0, ignoreCase)
                if (index != NOT_FOUND) {
                    return FindResult(picNames[i], label.index, index..(index + searchText.length))
                }
            }
        }

        return null
    }

    private fun handleFindNext() {
        var findResult = findNext(wrapFind)
        if (findResult == null) {
            val result = showConfirm(this, I18N["snr.not_found_re"])
            if (!(result.isPresent && result.get() == ButtonType.YES)) return

            findResult = findNext(true)
            if (findResult == null) {
                showInfo(this@SearchReplace, I18N["snr.not_found"])
                return
            }
        }

        state.currentPicName = findResult.picName
        state.currentLabelIndex = findResult.labelIndex
        state.view.cTransArea.selectRange(findResult.range.first, findResult.range.last)
    }
    private fun handleReplace() {
        if (state.view.cTransArea.selectedText == searchText) {
            state.view.cTransArea.replaceSelection(replaceText)
        } else {
            handleFindNext()
        }
    }
    private fun handleReplaceAll() {
        var count = 0
        val actions = ArrayList<LabelAction>()
        for (picName in state.transFile.sortedPicNames) {
            for (label in state.transFile.getTransList(picName)) {
                var index = label.text.indexOf(searchText, 0, ignoreCase)
                if (index == NOT_FOUND) continue

                var text = label.text
                while (index != NOT_FOUND) {
                    count++
                    text = text.replaceRange(index, index + searchText.length, replaceText)
                    index = text.indexOf(searchText, index, ignoreCase)
                }
                actions.add(LabelAction(ActionType.CHANGE, state, picName, label, newText = text))
            }
        }

        if (count == 0) {
            showInfo(this@SearchReplace, I18N["snr.not_found"])
        } else {
            state.doAction(ComplexAction(actions))
            showInfo(this@SearchReplace, String.format(I18N["snr.replace_count.i"], count))
        }
    }

    private fun handleFixTildes() {
        var count = 0
        val actions = ArrayList<LabelAction>()
        val regex = Regex("~+")

        for (picName in state.transFile.sortedPicNames) {
            for (label in state.transFile.getTransList(picName)) {
                val originalText = label.text
                if (!originalText.contains("~")) continue

                val newText = regex.replace(originalText) { matchResult ->
                    val tildes = matchResult.value
                    if (tildes.length % 2 != 0) {
                        "$tildes~"
                    } else {
                        tildes
                    }
                }

                if (originalText != newText) {
                    count++
                    actions.add(LabelAction(ActionType.CHANGE, state, picName, label, newText = newText))
                }
            }
        }

        if (count == 0) {
            showInfo(this@SearchReplace, "沒有找到需要修正的波浪號。")
        } else {
            state.doAction(ComplexAction(actions))
            showInfo(this@SearchReplace, "已修正 $count 個標籤中的波浪號。")
        }
    }

    private fun handleReplaceFullWidthTilde() {
        var count = 0
        val actions = ArrayList<LabelAction>()

        for (picName in state.transFile.sortedPicNames) {
            for (label in state.transFile.getTransList(picName)) {
                val originalText = label.text
                if (!originalText.contains("～")) continue

                val newText = originalText.replace("～", "~")

                if (originalText != newText) {
                    count++
                    actions.add(LabelAction(ActionType.CHANGE, state, picName, label, newText = newText))
                }
            }
        }

        if (count == 0) {
            showInfo(this@SearchReplace, "沒有找到需要替換的全形波浪號。")
        } else {
            state.doAction(ComplexAction(actions))
            showInfo(this@SearchReplace, "已替換 $count 個標籤中的全形波浪號。")
        }
    }

    private fun handleReplaceExclamation() {
        var count = 0
        val actions = ArrayList<LabelAction>()

        for (picName in state.transFile.sortedPicNames) {
            for (label in state.transFile.getTransList(picName)) {
                val originalText = label.text
                if (!originalText.contains("!")) continue

                val newText = originalText.replace("!", "！")

                if (originalText != newText) {
                    count++
                    actions.add(LabelAction(ActionType.CHANGE, state, picName, label, newText = newText))
                }
            }
        }

        if (count == 0) {
            showInfo(this@SearchReplace, "沒有找到需要替換的半形驚嘆號。")
        } else {
            state.doAction(ComplexAction(actions))
            showInfo(this@SearchReplace, "已替換 $count 個標籤中的半形驚嘆號。")
        }
    }

}
