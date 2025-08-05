package ink.meodinger.lpfx.component

import ink.meodinger.lpfx.Config.MonoFont
import ink.meodinger.lpfx.util.color.opacity
import ink.meodinger.lpfx.util.property.*

import javafx.beans.binding.Bindings
import javafx.beans.property.*
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.VPos
import javafx.scene.Node
import javafx.scene.control.Control
import javafx.scene.control.Skin
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.Shape
import javafx.scene.shape.StrokeType
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import javafx.scene.text.TextBoundsType
import javafx.scene.effect.DropShadow
import javafx.scene.effect.BlurType


/**
 * Author: Meodinger
 * Date: 2021/7/29
 * Have fun with my code!
 */

/**
 * A Control that represents a TransLabel
 */
class CLabel(
    labelIndex:  Int    = -1,
    labelRadius: Double = 24.0,
    labelColor:  Color  = Color.web("66CCFF"),
) : Control() {

    companion object {
        /**
         * The minimal pick radius for CLabel
         */
        const val MIN_PICK_RADIUS: Double = 16.0
    }

    // region Properties

    private val indexProperty: IntegerProperty = SimpleIntegerProperty(labelIndex)
    /**
     * The index to display
     */
    fun indexProperty(): IntegerProperty = indexProperty
    /**
     * @see indexProperty
     */
    var index: Int by indexProperty

    private val colorProperty: ObjectProperty<Color> = SimpleObjectProperty(labelColor)
    /**
     * The color of the CLabel
     */
    fun colorProperty(): ObjectProperty<Color> = colorProperty
    /**
     * @see colorProperty
     */
    var color: Color by colorProperty

    private val radiusProperty: DoubleProperty = SimpleDoubleProperty(labelRadius)
    /**
     * The radius of the CLabel
     */
    fun radiusProperty(): DoubleProperty = radiusProperty
    /**
     * @see radiusProperty
     */
    var radius: Double by radiusProperty

    private val textOpaqueProperty: BooleanProperty = SimpleBooleanProperty(false)
    /**
     * Whether the text of the CLabel could be opaque
     */
    fun textOpaqueProperty(): BooleanProperty = textOpaqueProperty
    /**
     * @see textOpaqueProperty
     */
    var isTextOpaque: Boolean by textOpaqueProperty

    private val selectedStrokeProperty: BooleanProperty = SimpleBooleanProperty(false)
    /**
     * Whether the text of the Selected CLabel has stroke
     */
    fun selectedStrokeProperty(): BooleanProperty = selectedStrokeProperty
    /**
     * @see selectedStrokeProperty
     */
    var isSelectedStroke: Boolean by selectedStrokeProperty

    private val colorOpacityProperty: DoubleProperty = SimpleDoubleProperty(1.0)
    /**
     * The opacity of the CLabel
     */
    fun colorOpacityProperty(): DoubleProperty = colorOpacityProperty
    /**
     * @see colorOpacityProperty
     */
    var colorOpacity: Double by colorOpacityProperty

    private val selectedProperty: BooleanProperty = SimpleBooleanProperty(false)
    /**
     * the state of the CLabel
     */
    private fun selectedProperty(): BooleanProperty = selectedProperty
    /**
     * @see selectedProperty
     */
    var isSelected: Boolean by selectedProperty

    private val groupNameProperty: StringProperty = SimpleStringProperty("")
    /**
     * The name of the group to which the label belongs
     */
    fun groupNameProperty(): StringProperty = groupNameProperty
    /**
     * @see groupNameProperty
     */
    var groupName: String by groupNameProperty

    private val groupNameVisibleProperty: BooleanProperty = SimpleBooleanProperty(false)
    /**
     * Whether the group name is visible
     */
    fun groupNameVisibleProperty(): BooleanProperty = groupNameVisibleProperty
    /**
     * @see groupNameVisibleProperty
     */
    var isGroupNameVisible: Boolean by groupNameVisibleProperty

    private val auxiliaryTextProperty: StringProperty = SimpleStringProperty("")
    /**
     * The auxiliary proofreading text of the label.
     */
    fun auxiliaryTextProperty(): StringProperty = auxiliaryTextProperty
    /**
     * @see auxiliaryTextProperty
     */
    var auxiliaryText: String by auxiliaryTextProperty

    private val auxiliaryVisibleProperty: BooleanProperty = SimpleBooleanProperty(false)
    /**
     * Whether the auxiliary proofreading text is visible.
     */
    fun auxiliaryVisibleProperty(): BooleanProperty = auxiliaryVisibleProperty
    /**
     * @see auxiliaryVisibleProperty
     */
    var isAuxiliaryVisible: Boolean by auxiliaryVisibleProperty

    private val translationOrientationProperty: ObjectProperty<Orientation> = SimpleObjectProperty(Orientation.VERTICAL)
    /**
     * The orientation of the translation text. Can be either horizontal or vertical.
     */
    fun translationOrientationProperty(): ObjectProperty<Orientation> = translationOrientationProperty
    /**
     * @see translationOrientationProperty
     */
    var translationOrientation: Orientation by translationOrientationProperty

    val auxiliaryLayoutXProperty: DoubleProperty = SimpleDoubleProperty(0.0)
    /**
     * The layoutX of the auxiliary proofreading text pane, relative to the CLabel.
     * This is intended to be controlled by an external layout manager (e.g., CLabelPane)
     * to resolve overlaps.
     */
    fun auxiliaryLayoutXProperty(): DoubleProperty = auxiliaryLayoutXProperty
    var auxiliaryLayoutX: Double by auxiliaryLayoutXProperty

    private val placeholderTextProperty: StringProperty = SimpleStringProperty("")
    /**
     * The placeholder text of the label, displayed in the center.
     */
    fun placeholderTextProperty(): StringProperty = placeholderTextProperty
    /**
     * @see placeholderTextProperty
     */
    var placeholderText: String by placeholderTextProperty

    private val placeholderVisibleProperty: BooleanProperty = SimpleBooleanProperty(false)
    /**
     * Whether the placeholder text is visible.
     */
    fun placeholderVisibleProperty(): BooleanProperty = placeholderVisibleProperty
    /**
     * @see placeholderVisibleProperty
     */
    var isPlaceholderVisible: Boolean by placeholderVisibleProperty

    private val groupFontSizeProperty = SimpleDoubleProperty(-1.0)
    fun groupFontSizeProperty(): DoubleProperty = groupFontSizeProperty
    var groupFontSize: Double by groupFontSizeProperty

    private val groupTextDirectionProperty = SimpleStringProperty("horizontal")
    fun groupTextDirectionProperty(): StringProperty = groupTextDirectionProperty
    var groupTextDirection: String by groupTextDirectionProperty


    // endregion

    // region Privates

    private val pickerRadiusProperty = radiusProperty.transform { it.coerceAtLeast(MIN_PICK_RADIUS) }.primitive().readonly()
    private val pickerRadius: Double by pickerRadiusProperty

    // endregion

    init {
        prefWidthProperty().bind(pickerRadiusProperty * 2)
        prefHeightProperty().bind(pickerRadiusProperty * 2)
    }

    // region Skin

    /**
     * Create default Skin
     * @see javafx.scene.control.Control.createDefaultSkin
     */
    override fun createDefaultSkin(): Skin<CLabel> = CLabelSkin(this)

    private class CLabelSkin(private val cLabel: CLabel) : Skin<CLabel> {

        private val root = Pane()
        private val text = Text()
        private val groupNameText = Text()
        private val groupNamePane = StackPane(groupNameText)
        private val auxiliaryText = Text()
        private val auxiliaryPane = StackPane(auxiliaryText)
        private val placeholderContainer = HBox() // 用於水平排列多列
        private val placeholderPane = StackPane(placeholderContainer)
        private val circle = Circle()

        private var clip: Shape = circle // just a placeholder to make type non-null

        init {
            root.apply {
                prefWidthProperty().bind(cLabel.pickerRadiusProperty)
                prefHeightProperty().bind(cLabel.pickerRadiusProperty)
            }
            text.apply {
                textOrigin = VPos.CENTER

                textProperty().bind(cLabel.indexProperty.asString())
                fillProperty().bind(Bindings.createObjectBinding(
                    {
                        if (cLabel.isTextOpaque) Color.WHITE else Color.WHITE.opacity(cLabel.colorOpacity)
                    }, cLabel.textOpaqueProperty, cLabel.colorOpacityProperty
                ))
                fontProperty().bind(Bindings.createObjectBinding(
                    {
                        Font.font(MonoFont, FontWeight.BOLD, (if (cLabel.index < 10) 1.7 else 1.3) * cLabel.radius)
                    }, cLabel.indexProperty, cLabel.radiusProperty
                ))
                layoutXProperty().bind(Bindings.createDoubleBinding(
                    {
                        cLabel.pickerRadius - boundsInLocal.width / 2
                    }, cLabel.indexProperty, cLabel.pickerRadiusProperty
                ))
                layoutYProperty().bind(Bindings.createDoubleBinding(
                    {
                        cLabel.pickerRadius
                    }, cLabel.indexProperty, cLabel.pickerRadiusProperty
                ))
            }
            circle.apply {
                radiusProperty().bind(cLabel.radiusProperty)
                centerXProperty().bind(cLabel.pickerRadiusProperty)
                centerYProperty().bind(cLabel.pickerRadiusProperty)
            }
            groupNameText.apply {
                textProperty().bind(cLabel.groupNameProperty().transform { name ->
                    name.toCharArray().joinToString("\n")
                })
                font = Font.font(12.0)
                boundsType = TextBoundsType.VISUAL
            }
            groupNamePane.apply {
                padding = Insets(2.0)
                style = "-fx-background-color:lightgreen; -fx-background-radius: 4; -fx-border-color: lightgreen; -fx-border-width: 1; -fx-border-radius: 4;"
                visibleProperty().bind(cLabel.groupNameVisibleProperty())

                layoutXProperty().bind(cLabel.pickerRadiusProperty.subtract(widthProperty().divide(2)))
                // 在占位翻譯的正下方，留10像素間隙
                layoutYProperty().bind(
                    placeholderPane.layoutYProperty()
                        .add(placeholderPane.heightProperty())
                        .add(10)
                )
            }
            
            // Auxiliary Proofreading Translation (on the side)
            auxiliaryText.apply {
                textProperty().bind(cLabel.auxiliaryTextProperty)
                font = Font.font(18.0)
                boundsType = TextBoundsType.VISUAL
                fill = Color.WHITE
            }
                        auxiliaryPane.apply {
                styleClass.add("auxiliary-pane") // Use a unique style class
                padding = Insets(4.0)
                style = "-fx-background-color: rgba(0,0,0,0.75); -fx-background-radius: 6; -fx-border-color: rgba(255,255,255,0.3); -fx-border-width: 1; -fx-border-radius: 6;"
                // 先隱藏輔助翻譯，不再展示
                isVisible = false

                // Layout logic for side-positioning (adjustable X)
                layoutXProperty().bind(cLabel.auxiliaryLayoutXProperty)
                layoutYProperty().bind(cLabel.pickerRadiusProperty.subtract(heightProperty().divide(2)))
            }

            // Placeholder Translation (in the center)
            // 設置容器間距
            placeholderContainer.spacing = 2.0
            
            placeholderPane.apply {
                styleClass.add("placeholder-pane")
                padding = Insets(4.0)
                style = "-fx-background-color: rgba(20, 20, 40, 0.85); -fx-background-radius: 6; -fx-border-color: lightblue; -fx-border-width: 1; -fx-border-radius: 6;"
                visibleProperty().bind(cLabel.placeholderVisibleProperty())

                // 動態更新文字排列
                fun updatePlaceholderText() {
                    placeholderContainer.children.clear()
                    
                    // 預處理文本：替換雙省略號
                    val preprocessedText = cLabel.placeholderText
                        .replace("……", "︙︙")  // 雙省略號替換為雙竪排省略號
                    
                    if (preprocessedText.isEmpty()) return
                    
                    val fontSize = if (cLabel.groupFontSize > 0) cLabel.groupFontSize else 18.0
                    val font = Font.font(fontSize / 0.75) // Convert from px to pt
                    val isVertical = cLabel.groupTextDirection.equals("vertical", ignoreCase = true)
                    
                    if (isVertical) {
                        // 竪排模式：每個字符竪著排列，從右到左，從上到下
                        val lines = preprocessedText.split('\n')
                        val maxCharsPerColumn = 8 // 每列最多字符數
                        val allColumns = mutableListOf<VBox>() // 收集所有列
                        
                        for (line in lines) {
                            if (line.isEmpty()) continue
                            
                            // 將長行分割成多列
                            val chunks = line.chunked(maxCharsPerColumn)
                            
                            for (chunk in chunks) {
                                val columnBox = VBox().apply { spacing = 1.0 }
                                
                                // 在列開始添加透明的"一"來撐寬
                                val topSpacer = Text("一").apply {
                                    this.font = font
                                    boundsType = TextBoundsType.VISUAL
                                    fill = Color.TRANSPARENT // 透明色
                                }
                                columnBox.children.add(topSpacer)
                                
                                for (char in chunk) {
                                    val displayChar = convertToVerticalChar(char)
                                    val charText = Text(displayChar).apply {
                                        this.font = font
                                        boundsType = TextBoundsType.VISUAL
                                        fill = Color.WHITE    
                                    }
                                    columnBox.children.add(charText)
                                }
                                
                                // 在列結束添加透明的"一"來撐寬
                                val bottomSpacer = Text("一").apply {
                                    this.font = font
                                    boundsType = TextBoundsType.VISUAL
                                    fill = Color.TRANSPARENT // 透明色
                                }
                                columnBox.children.add(bottomSpacer)
                                
                                allColumns.add(columnBox)
                            }
                        }
                        
                        // 從右到左添加列（反向遍歷）
                        for (i in allColumns.size - 1 downTo 0) {
                            placeholderContainer.children.add(allColumns[i])
                        }
                    } else {
                        // 橫排模式：正常橫向排列
                        val textNode = Text(preprocessedText).apply {
                            this.font = font
                            boundsType = TextBoundsType.VISUAL
                            fill = Color.WHITE
                        }
                        val textContainer = VBox(textNode)
                        placeholderContainer.children.add(textContainer)
                    }
                }
                
                // 監聽變化並更新文字排列
                cLabel.placeholderTextProperty().addListener { _ -> updatePlaceholderText() }
                cLabel.groupFontSizeProperty.addListener { _ -> updatePlaceholderText() }
                cLabel.groupTextDirectionProperty().addListener { _ -> updatePlaceholderText() }
                
                // 居中定位，覆蓋在Label上面（Z軸）
                layoutXProperty().bind(cLabel.pickerRadiusProperty.subtract(widthProperty().divide(2)))
                layoutYProperty().bind(cLabel.pickerRadiusProperty.subtract(heightProperty().divide(2)))
                
                // 初始更新
                updatePlaceholderText()
            }


            // Update
            val updateListener = onChange<Any> {
                // Remove old
                clip.fillProperty().unbind()
                root.children.clear() // make circle & text have no parents
                // Create new
                clip = Shape.subtract(circle, text).apply {
                    fillProperty().bind(Bindings.createObjectBinding(
                        {
                            cLabel.color.opacity(cLabel.colorOpacity)
                        }, cLabel.colorProperty, cLabel.colorOpacityProperty,cLabel.selectedProperty
                    ))
                }

                if(cLabel.isSelected) {
                    // Create glow effect with label's own color when selected
                    val glowEffect = DropShadow().apply {
                        blurType = BlurType.GAUSSIAN
                        radius = 25.0  // 增大發光範圍
                        spread = 0.4   // 稍微增加擴散度
                        color = cLabel.color  // 使用標籤自身的顏色
                        offsetX = 0.0
                        offsetY = 0.0
                    }
                    clip.effect = glowEffect
                    // 占位翻譯在最上層展示
                    root.children.setAll(text, clip, groupNamePane, auxiliaryPane, placeholderPane)
                } else {
                    clip.effect = null
                    // 占位翻譯在最上層展示
                    root.children.setAll(text, clip, groupNamePane, auxiliaryPane, placeholderPane)
                }


            }
            cLabel.indexProperty.addListener(updateListener)
            cLabel.radiusProperty.addListener(updateListener)
            cLabel.selectedProperty.addListener(updateListener)

            // Manually update the first time
            updateListener.changed(null, null, null)
        }

        /**
         * 將字符轉換為適合竪排顯示的字符
         * 
         * 📍 **修改位置說明**：
         * 如需調整竪排字符替換規則，請修改此函數中的映射關係
         */
        private fun convertToVerticalChar(char: Char): String {
            return when (char) {
                // 省略號替換
                '…' -> "︙"      // 竪排省略號
                
                // 處理雙省略號（……）
                // 注意：這個需要在字符串處理層面預處理，這裡只處理單個字符
                
                // 括號替換
                '(' -> "︵"      // 竪排左括號
                ')' -> "︶"      // 竪排右括號
                '（' -> "︵"      // 中文左括號
                '）' -> "︶"      // 中文右括號
                '—' -> "︱"      // 竪排長破折號
                '―' -> "︱"      // 竪排全形破折號
                // 引號替換（暫時移除，避免重複條件）
                // '"' -> "﹁"   // 可根據需要添加其他引號處理
                
                // 其他字符保持原樣
                else -> char.toString()
            }
        }

        override fun getSkinnable(): CLabel = cLabel

        override fun getNode(): Node = root

        override fun dispose() {
            clip.fillProperty().unbind()
            root.children.remove(clip)
            root.children.remove(groupNamePane)
            root.children.remove(auxiliaryPane)
            root.children.remove(placeholderPane)

            text.textProperty().unbind()
            text.fillProperty().unbind()
            text.fontProperty().unbind()
            text.layoutXProperty().unbind()
            text.layoutYProperty().unbind()
            root.children.remove(text)
            
            // 清理 placeholderContainer 中的所有子節點
            placeholderContainer.children.clear()

            circle.radiusProperty().unbind()
            circle.centerXProperty().unbind()
            circle.centerYProperty().unbind()

            root.prefWidthProperty().unbind()
            root.prefHeightProperty().unbind()
        }

    }

    // endregion

}
