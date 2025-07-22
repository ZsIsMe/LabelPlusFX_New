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
} 