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
        private val placeholderText = Text()
        private val placeholderPane = StackPane(placeholderText)
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
                layoutYProperty().bind(cLabel.radiusProperty.multiply(2).add(55))
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
                visibleProperty().bind(cLabel.auxiliaryVisibleProperty())

                // Layout logic for side-positioning (adjustable X)
                layoutXProperty().bind(cLabel.auxiliaryLayoutXProperty)
                layoutYProperty().bind(cLabel.pickerRadiusProperty.subtract(heightProperty().divide(2)))
            }

            // Placeholder Translation (in the center)
            placeholderText.apply {
                textProperty().bind(cLabel.placeholderTextProperty())
                font = Font.font(18.0)
                boundsType = TextBoundsType.VISUAL
                fill = Color.WHITE
            }
            placeholderPane.apply {
                styleClass.add("placeholder-pane")
                padding = Insets(4.0)
                style = "-fx-background-color: rgba(20, 20, 40, 0.85); -fx-background-radius: 6; -fx-border-color: lightblue; -fx-border-width: 1; -fx-border-radius: 6;"
                visibleProperty().bind(cLabel.placeholderVisibleProperty())
                
                // Dynamic layout for centered positioning and rotation
                fun updatePlaceholderLayout() {
                    if (cLabel.translationOrientation == Orientation.VERTICAL) {
                        rotate = 90.0
                    } else {
                        rotate = 0.0
                    }
                    // Always centered on the label's circle
                    layoutXProperty().bind(cLabel.pickerRadiusProperty.subtract(widthProperty().divide(2)))
                    layoutYProperty().bind(cLabel.pickerRadiusProperty.subtract(heightProperty().divide(2)))
                }
                
                cLabel.translationOrientationProperty().addListener { _ -> updatePlaceholderLayout() }
                updatePlaceholderLayout() // Initial setup
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
                    // Draw placeholderPane first, so it's in the background
                    root.children.setAll(placeholderPane, text, clip, groupNamePane, auxiliaryPane)
                } else {
                    clip.effect = null
                    // Draw placeholderPane first, so it's in the background
                    root.children.setAll(placeholderPane, text, clip, groupNamePane, auxiliaryPane)
                }


            }
            cLabel.indexProperty.addListener(updateListener)
            cLabel.radiusProperty.addListener(updateListener)
            cLabel.selectedProperty.addListener(updateListener)

            // Manually update the first time
            updateListener.changed(null, null, null)
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

            circle.radiusProperty().unbind()
            circle.centerXProperty().unbind()
            circle.centerYProperty().unbind()

            root.prefWidthProperty().unbind()
            root.prefHeightProperty().unbind()
        }

    }

    // endregion

}
