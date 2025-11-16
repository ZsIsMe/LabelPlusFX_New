package ink.meodinger.lpfx.io

import ink.meodinger.lpfx.Config
import ink.meodinger.lpfx.State
import ink.meodinger.lpfx.component.dialog.showError
import ink.meodinger.lpfx.component.dialog.showInfo
import ink.meodinger.lpfx.options.Logger
import ink.meodinger.lpfx.options.Settings
import ink.meodinger.lpfx.type.TransLabel
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.VPos
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import java.io.File
import javax.imageio.ImageIO

/**
 * 带标签图片导出器
 * 用于将当前页面的图片与标签合成并导出
 */
object LabeledImageExporter {
    
    /**
     * 导出当前页面带标签的图片
     */
    fun exportCurrentPageWithLabels(state: State) {
        Logger.info("开始导出当前页面带标签图片", "LabeledImageExporter")
        
        try {
            // 获取当前翻译文件所在目录
            val currentDir = state.transFile.projectFolder
            
            // 创建导出目录
            val exportDir = currentDir.resolve("export_label_pics")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            
            // 获取当前图片文件
            val currentPicFile = state.getPicFileNow()
            if (!currentPicFile.exists()) {
                showError(state.stage, "当前图片文件不存在")
                return
            }
            
            // 获取当前页面的标签
            val labels = state.transFile.getTransList(state.currentPicName)
            
            // 创建带标签的图片
            val labeledImage = createImageWithLabels(currentPicFile, labels, state)
            
            // 保存图片
            val outputFile = exportDir.resolve(state.currentPicName)
            saveLabeledImage(labeledImage, outputFile)
            
            showInfo(state.stage, "导出完成: ${outputFile.absolutePath}")
            Logger.info("成功导出带标签图片到: ${outputFile.path}", "LabeledImageExporter")
            
        } catch (e: Exception) {
            Logger.error("导出带标签图片失败", "LabeledImageExporter")
            Logger.exception(e)
            showError(state.stage, "导出失败: ${e.message}")
        }
    }
    
    /**
     * 创建带标签的图片
     */
    private fun createImageWithLabels(imageFile: File, labels: List<TransLabel>, state: State): WritableImage {
        Logger.info("開始創建帶標籤圖片: ${imageFile.name}, 標籤數量: ${labels.size}", "LabeledImageExporter")
        
        // 加载原始图片
        val originalImage = Image(imageFile.toURI().toString())
        if (originalImage.isError) {
            throw RuntimeException("無法加載原始圖片: ${originalImage.exception?.message}")
        }
        
        val width = originalImage.width.toInt()
        val height = originalImage.height.toInt()
        Logger.info("圖片尺寸: ${width}x${height}", "LabeledImageExporter")
        
        // 创建Canvas来绘制整个图片（包括原图和标签）
        val canvas = Canvas(width.toDouble(), height.toDouble())
        val gc = canvas.graphicsContext2D
        
        // 设置标签样式
        val labelRadius = Settings.labelRadius
        val font = Font.font(Config.MonoFont, FontWeight.BOLD, labelRadius * 1.5)
        gc.font = font
        gc.textBaseline = VPos.CENTER
        Logger.info("標籤樣式設置完成，半徑: $labelRadius", "LabeledImageExporter")
        
        // 先在Canvas上繪製原始圖片
        Logger.info("繪製原始圖片到Canvas...", "LabeledImageExporter")
        gc.drawImage(originalImage, 0.0, 0.0)
        
        // 繪製標籤（在原圖之上）
        Logger.info("在原圖上繪製標籤...", "LabeledImageExporter")
        for (label in labels) {
            val x = label.x * width
            val y = label.y * height
            val group = state.transFile.groupList[label.groupId]
            
            Logger.info("繪製標籤 ${label.index} 位置: ($x, $y), 分組: ${group.name}", "LabeledImageExporter")
            
            // 繪製標籤圓圈背景
            gc.fill = group.color
            gc.fillOval(x - labelRadius, y - labelRadius, labelRadius * 2, labelRadius * 2)
            
            // 繪製標籤數字
            gc.fill = Color.WHITE
            val labelText = label.index.toString()
            val textNode = Text(labelText)
            textNode.font = font
            val textWidth = textNode.boundsInLocal.width
            gc.fillText(labelText, x - textWidth / 2.0, y)
        }
        
        // 從Canvas獲取最終圖片
        val finalImage = canvas.snapshot(null, null)
        
        Logger.info("圖片合成完成，使用Canvas快照", "LabeledImageExporter")
        return finalImage
    }
    
    /**
     * 保存带标签的图片
     */
    private fun saveLabeledImage(image: WritableImage, outputFile: File) {
        Logger.info("開始保存圖片到: ${outputFile.absolutePath}", "LabeledImageExporter")
        
        // 使用SwingFXUtils转换为BufferedImage
        var bufferedImage = SwingFXUtils.fromFXImage(image, null)
        if (bufferedImage == null) {
            throw RuntimeException("無法將JavaFX圖片轉換為BufferedImage")
        }
        
        Logger.info("圖片轉換成功，尺寸: ${bufferedImage.width}x${bufferedImage.height}", "LabeledImageExporter")
        
        // 根据原始文件扩展名确定输出格式
        val extension = outputFile.extension.lowercase()
        val format = when (extension) {
            "png" -> "PNG"
            "jpg", "jpeg" -> "JPEG"
            "bmp" -> "BMP"
            "gif" -> "GIF"
            else -> "PNG" // 默认使用PNG
        }
        
        Logger.info("使用格式: $format", "LabeledImageExporter")
        
        // 對於JPEG格式，需要處理透明度問題
        if (format == "JPEG") {
            Logger.info("轉換為JPEG格式，移除透明度", "LabeledImageExporter")
            val jpegImage = java.awt.image.BufferedImage(
                bufferedImage.width, 
                bufferedImage.height, 
                java.awt.image.BufferedImage.TYPE_INT_RGB
            )
            val g2d = jpegImage.createGraphics()
            g2d.color = java.awt.Color.WHITE
            g2d.fillRect(0, 0, bufferedImage.width, bufferedImage.height)
            g2d.drawImage(bufferedImage, 0, 0, null)
            g2d.dispose()
            bufferedImage = jpegImage
        }
        
        // 确保父目录存在
        if (!outputFile.parentFile.exists()) {
            outputFile.parentFile.mkdirs()
            Logger.info("創建目錄: ${outputFile.parentFile.absolutePath}", "LabeledImageExporter")
        }
        
        // 保存图片
        val success = ImageIO.write(bufferedImage, format, outputFile)
        if (!success) {
            throw RuntimeException("ImageIO.write 返回 false，可能不支持該格式: $format")
        }
        
        Logger.info("圖片保存成功，文件大小: ${outputFile.length()} bytes", "LabeledImageExporter")
    }

    /**
     * 导出全部图片带标签
     */
    fun exportAllPagesWithLabels(state: State) {
        Logger.info("开始导出全部图片带标签", "LabeledImageExporter")
        
        try {
            // 获取当前翻译文件所在目录
            val currentDir = state.transFile.projectFolder
            
            // 创建导出目录
            val exportDir = currentDir.resolve("export_label_pics")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            
            // 获取所有图片文件
            val allPicFiles = state.transFile.sortedPicNames.map { picName ->
                state.transFile.getFile(picName)
            }.filter { it.exists() }
            
            if (allPicFiles.isEmpty()) {
                showError(state.stage, "没有找到图片文件")
                return
            }
            
            Logger.info("找到 ${allPicFiles.size} 张图片，开始批量处理", "LabeledImageExporter")
            
            var successCount = 0
            var failedCount = 0
            val failedFiles = mutableListOf<String>()
            
            // 逐张处理图片
            allPicFiles.forEachIndexed { index, picFile ->
                try {
                    Logger.info("处理第 ${index + 1}/${allPicFiles.size} 张图片: ${picFile.name}", "LabeledImageExporter")
                    
                    // 获取该图片对应的所有标签
                    val labelsForPic = state.transFile.getTransList(picFile.name)
                    
                    // 创建带标签的图片
                    val labeledImage = if (labelsForPic.isNotEmpty()) {
                        createImageWithLabels(picFile, labelsForPic, state)
                    } else {
                        // 如果没有标签，创建一个Canvas只包含原图
                        Logger.info("图片 ${picFile.name} 没有标签，直接复制原图", "LabeledImageExporter")
                        val originalImage = javafx.scene.image.Image(picFile.toURI().toString())
                        val canvas = javafx.scene.canvas.Canvas(originalImage.width, originalImage.height)
                        val gc = canvas.graphicsContext2D
                        gc.drawImage(originalImage, 0.0, 0.0)
                        canvas.snapshot(null, null)
                    }
                    
                    // 保存图片
                    val outputFile = exportDir.resolve(picFile.name)
                    saveLabeledImage(labeledImage, outputFile)
                    
                    successCount++
                    Logger.info("成功处理图片 ${index + 1}/${allPicFiles.size}: ${picFile.name}", "LabeledImageExporter")
                    
                } catch (e: Exception) {
                    failedCount++
                    failedFiles.add(picFile.name)
                    Logger.error("处理图片失败: ${picFile.name}, 错误: ${e.message}", "LabeledImageExporter")
                }
            }
            
            // 显示完成信息
            val message = buildString {
                appendLine("批量导出完成！")
                appendLine("成功: $successCount 张")
                if (failedCount > 0) {
                    appendLine("失败: $failedCount 张")
                    if (failedFiles.size <= 5) {
                        appendLine("失败文件: ${failedFiles.joinToString(", ")}")
                    } else {
                        appendLine("失败文件: ${failedFiles.take(3).joinToString(", ")} 等...")
                    }
                }
                appendLine("导出目录: ${exportDir.absolutePath}")
            }
            
            Logger.info("批量导出完成，成功: $successCount, 失败: $failedCount", "LabeledImageExporter")
            showInfo(state.stage, message)
            
        } catch (e: Exception) {
            Logger.error("批量导出失败", "LabeledImageExporter")
            Logger.exception(e)
            showError(state.stage, "批量导出失败: ${e.message}")
        }
    }

    /**
     * 导出全部图片带占位翻译
     */
    fun exportAllPagesWithPlaceholderTranslation(state: State, paneScale: Double, placeholderScale: Double) {
        Logger.info("开始导出全部图片带占位翻译", "LabeledImageExporter")

        try {
            // 获取当前翻译文件所在目录
            val currentDir = state.transFile.projectFolder

            // 创建导出目录
            val exportDir = currentDir.resolve("export_placeholder_translation_pics")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            // 获取所有图片文件
            val allPicFiles = state.transFile.sortedPicNames.map { picName ->
                state.transFile.getFile(picName)
            }.filter { it.exists() }

            if (allPicFiles.isEmpty()) {
                showError(state.stage, "没有找到图片文件")
                return
            }

            Logger.info("找到 ${allPicFiles.size} 张图片，开始批量处理", "LabeledImageExporter")

            var successCount = 0
            var failedCount = 0
            val failedFiles = mutableListOf<String>()

            // 逐张处理图片
            allPicFiles.forEachIndexed { index, picFile ->
                try {
                    Logger.info("处理第 ${index + 1}/${allPicFiles.size} 张图片: ${picFile.name}", "LabeledImageExporter")

                    // 获取该图片对应的所有标签
                    val labelsForPic = state.transFile.getTransList(picFile.name)

                    // 创建带标签和占位翻译的图片
                    val labeledImage = if (labelsForPic.isNotEmpty()) {
                        createImageWithLabelsAndPlaceholderText(picFile, labelsForPic, state, paneScale, placeholderScale)
                    } else {
                        // 如果没有标签，创建一个Canvas只包含原图
                        Logger.info("图片 ${picFile.name} 没有标签，直接复制原图", "LabeledImageExporter")
                        val originalImage = javafx.scene.image.Image(picFile.toURI().toString())
                        val canvas = javafx.scene.canvas.Canvas(originalImage.width, originalImage.height)
                        val gc = canvas.graphicsContext2D
                        gc.drawImage(originalImage, 0.0, 0.0)
                        canvas.snapshot(null, null)
                    }

                    // 保存图片
                    val outputFile = exportDir.resolve(picFile.name)
                    saveLabeledImage(labeledImage, outputFile)

                    successCount++
                    Logger.info("成功处理图片 ${index + 1}/${allPicFiles.size}: ${picFile.name}", "LabeledImageExporter")

                } catch (e: Exception) {
                    failedCount++
                    failedFiles.add(picFile.name)
                    Logger.error("处理图片失败: ${picFile.name}, 错误: ${e.message}", "LabeledImageExporter")
                }
            }

            // 显示完成信息
            val message = buildString {
                appendLine("批量导出完成！")
                appendLine("成功: $successCount 张")
                if (failedCount > 0) {
                    appendLine("失败: $failedCount 张")
                    if (failedFiles.size <= 5) {
                        appendLine("失败文件: ${failedFiles.joinToString(", ")}")
                    } else {
                        appendLine("失败文件: ${failedFiles.take(3).joinToString(", ")} 等...")
                    }
                }
                appendLine("导出目录: ${exportDir.absolutePath}")
            }

            Logger.info("批量导出完成，成功: $successCount, 失败: $failedCount", "LabeledImageExporter")
            showInfo(state.stage, message)

        } catch (e: Exception) {
            Logger.error("批量导出失败", "LabeledImageExporter")
            Logger.exception(e)
            showError(state.stage, "批量导出失败: ${e.message}")
        }
    }

    /**
     * 创建带标签和占位翻译的图片
     * 注意：此函数目前仅支持横排文本导出
     */
    private fun createImageWithLabelsAndPlaceholderText(
        imageFile: File,
        labels: List<TransLabel>,
        state: State,
        paneScale: Double,
        placeholderScale: Double
    ): WritableImage {
        Logger.info("開始創建帶標籤和佔位翻譯的圖片: ${imageFile.name}, 標籤數量: ${labels.size}", "LabeledImageExporter")

        // 加载原始图片
        val originalImage = Image(imageFile.toURI().toString())
        if (originalImage.isError) {
            throw RuntimeException("無法加載原始圖片: ${originalImage.exception?.message}")
        }

        val width = originalImage.width
        val height = originalImage.height
        Logger.info("圖片尺寸: ${width}x${height}", "LabeledImageExporter")

        // 创建Canvas来绘制整个图片
        val canvas = Canvas(width, height)
        val gc = canvas.graphicsContext2D

        // 先在Canvas上繪製原始圖片
        gc.drawImage(originalImage, 0.0, 0.0)

        // 繪製占位翻译
        for (label in labels) {
            val group = state.transFile.groupList[label.groupId]
            if (label.text.isNotEmpty()) {
                val x = label.x * width
                val y = label.y * height

                val placeholderText = label.text
                val baseFontSize = if (group.fontSize > 0) group.fontSize else 41.0
                val finalFontSize = baseFontSize * paneScale * placeholderScale
                val placeholderFont = Font.font(finalFontSize)
                val isVertical = group.textDirection.equals("vertical", ignoreCase = true)

                // 提前计算整个文本块的尺寸
                val textBlockBounds = calculateTextBlockBounds(placeholderText, placeholderFont, isVertical)
                val textBlockWidth = textBlockBounds.first
                val textBlockHeight = textBlockBounds.second

                val padding = 8.0 * paneScale * placeholderScale // 内边距也需要缩放
                val rectWidth = textBlockWidth + padding * 2
                val rectHeight = textBlockHeight + padding * 2
                
                // 将背景和文字的中心点定位在 (x, y)
                val rectX = x - rectWidth / 2.0
                val rectY = y - rectHeight / 2.0
                
                // 绘制背景矩形 (样式参考 CLabel.kt 中的 placeholderPane)
                gc.fill = Color.rgb(20, 20, 40, 0.85)
                gc.fillRoundRect(rectX, rectY, rectWidth, rectHeight, 12.0 * paneScale * placeholderScale, 12.0 * paneScale * placeholderScale)

                // 绘制边框 (样式参考 CLabel.kt 中的 placeholderPane)
                gc.stroke = Color.LIGHTBLUE
                gc.lineWidth = 1.0 // 边框宽度保持1px，不缩放，以确保清晰
                gc.strokeRoundRect(rectX, rectY, rectWidth, rectHeight, 12.0 * paneScale * placeholderScale, 12.0 * paneScale * placeholderScale)

                // --- 繪製文字 ---
                gc.font = placeholderFont
                gc.fill = Color.WHITE
                
                val startDrawX = x - textBlockWidth / 2.0
                val startDrawY = y - textBlockHeight / 2.0

                if (isVertical) {
                    drawVerticalText(gc, placeholderText, placeholderFont, startDrawX, startDrawY)
                } else {
                    drawHorizontalText(gc, placeholderText, placeholderFont, startDrawX, startDrawY)
                }
            }
        }

        // 從Canvas獲取最終圖片
        val finalImage = canvas.snapshot(null, null)
        Logger.info("圖片合成完成，使用Canvas快照", "LabeledImageExporter")
        return finalImage
    }

    private fun drawHorizontalText(gc: javafx.scene.canvas.GraphicsContext, text: String, font: Font, startX: Double, startY: Double) {
        val lines = text.split('\n')
        var currentY = startY
        val textNode = Text()
        textNode.font = font
        
        for ((index, line) in lines.withIndex()) {
            textNode.text = line
            val lineHeight = textNode.boundsInLocal.height
            // 对于第一行，我们定位到基线，对于后续行，我们简单地增加行高
            if (index == 0) {
                 // 垂直居中对齐，需要向上移动半个行高
                currentY += lineHeight / 1.5 // 经验值，对齐文本
            } else {
                currentY += lineHeight
            }
            gc.fillText(line, startX, currentY)
        }
    }

    private fun drawVerticalText(gc: javafx.scene.canvas.GraphicsContext, text: String, font: Font, startX: Double, startY: Double) {
        val preprocessedText = text.replace("……", "︙︙")
        val lines = preprocessedText.split('\n')
        
        val textNode = Text()
        textNode.font = font
        textNode.text = "一" // 用于获取标准字符宽度和高度
        val charWidth = textNode.boundsInLocal.width
        val charHeight = textNode.boundsInLocal.height
        
        var currentX = startX + (lines.size - 1) * charWidth // 从最右边的列开始

        for (line in lines) {
            var currentY = startY
            for (char in line) {
                val displayChar = convertToVerticalChar(char)
                gc.fillText(displayChar, currentX, currentY + charHeight / 1.5) // 往下微调对齐
                currentY += charHeight
            }
            currentX -= charWidth // 移到左边一列
        }
    }
    
    private fun calculateTextBlockBounds(text: String, font: Font, isVertical: Boolean): Pair<Double, Double> {
        val textNode = Text()
        textNode.font = font
        val lines = text.split('\n')

        if (isVertical) {
            val maxCharsPerColumn = lines.maxOfOrNull { it.length } ?: 0
            textNode.text = "一" // 用标准字符计算尺寸
            val charWidth = textNode.boundsInLocal.width
            val charHeight = textNode.boundsInLocal.height
            val totalWidth = lines.size * charWidth
            val totalHeight = maxCharsPerColumn * charHeight
            return Pair(totalWidth, totalHeight)
        } else {
            val totalWidth = lines.maxOfOrNull { line ->
                textNode.text = line
                textNode.boundsInLocal.width
            } ?: 0.0
            val totalHeight = lines.sumOf { line ->
                textNode.text = line
                textNode.boundsInLocal.height
            }
            return Pair(totalWidth, totalHeight)
        }
    }

    /**
     * 将字符转换为适合竖排显示的字符 (逻辑来自 CLabel.kt)
     */
    private fun convertToVerticalChar(char: Char): String {
        return when (char) {
            '…' -> "︙"
            '(' -> "︵"
            ')' -> "︶"
            '（' -> "︵"
            '）' -> "︶"
            '—' -> "︱"
            '―' -> "︱"
            else -> char.toString()
        }
    }
} 