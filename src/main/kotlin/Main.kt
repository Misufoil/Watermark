package watermark

import java.awt.Color
import java.awt.Transparency
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.system.exitProcess

fun main() {
    var myColor = Color(0, 0, 0);
    var answerUse = "no"
    var answerSet = "no"
    val image = inputImage("image")
    val watermark = inputImage("watermark")

    dimensionCheck(image, watermark)

    if (watermark.transparency == Transparency.TRANSLUCENT) {
        println("Do you want to use the watermark's Alpha channel?")
        answerUse = readln().lowercase()
    } else {
        println("Do you want to set a transparency color?")
        answerSet = readln()
        if (answerSet == "yes") {
            println("Input a transparency color ([Red] [Green] [Blue]):")
            try {
                val x = readln().split(" ").map { it.toInt() }
                if (x.size != 3) {
                    println("The transparency color input is invalid.")
                    exitProcess(1)
                }
                myColor = Color(x[0], x[1], x[2])
            } catch (e: Exception) {
                println("The transparency color input is invalid.")
                exitProcess(1)
            }
        }
    }

    val weight = percentageCheck()

    println("Choose the position method (single, grid):")
    when (readln()) {
        "single" -> createImageSingleWatermark(image, watermark, weight, answerUse, answerSet, myColor)
        "grid" -> createImageGridWatermark(image, watermark, weight, answerUse, answerSet, myColor)
        else -> {
            println("The position method input is invalid.")
            exitProcess(1)
        }
    }
}

fun inputImage(type: String): BufferedImage {
    println("Input the ${if (type != "image") "$type " else ""}image filename:")
    val imageFileName = readln()

    if (!File(imageFileName).exists()) {
        println("The file $imageFileName doesn't exist.")
        exitProcess(1)
    }

    val image = ImageIO.read(File(imageFileName))
    if (image.colorModel.numColorComponents != 3) {
        println("The number of $type color components isn't 3.")
        exitProcess(1)
    }

    if (image.colorModel.pixelSize !in listOf(24, 32)) {
        println("The $type isn't 24 or 32-bit.")
        exitProcess(1)
    }
    return image
}

fun dimensionCheck(image: BufferedImage, watermark: BufferedImage) {
    if (image.width < watermark.width || image.height < watermark.height) {
        println("The watermark's dimensions are larger.")
        exitProcess(1)
    }
}

fun percentageCheck(): Int {
    println("Input the watermark transparency percentage (Integer 0-100):")
    try {
        val x = readln().toInt()
        if (x !in 0..100) {
            println("The transparency percentage is out of range.")
            exitProcess(1)
        }
        return x
    } catch (e: NumberFormatException) {
        println("The transparency percentage isn't an integer number.")
        exitProcess(1)
    }
}

fun outputImageName(): String {
    val pattern = Regex(".*\\.(jpg|png)\$")

    println("Input the output image filename (jpg or png extension):")
    val outputImageName = readln()

    if (!pattern.matches(outputImageName) && outputImageName.substringBeforeLast('.') == "") {
        println("The output file extension isn't \"jpg\" or \"png\".")
        exitProcess(1)
    }

    return outputImageName
}

fun blendedColor(w: Color, i: Color, weight: Int) = Color(
    (weight * w.red + (100 - weight) * i.red) / 100,
    (weight * w.green + (100 - weight) * i.green) / 100,
    (weight * w.blue + (100 - weight) * i.blue) / 100
)

fun createImageSingleWatermark(
    image: BufferedImage,
    watermark: BufferedImage,
    weight: Int,
    answerUse: String,
    answerSet: String,
    myColor: Color
) {
    val imageWidth = image.width
    val imageHeight = image.height
    val watermarkWidth = watermark.width
    val watermarkHeight = watermark.height

    println("Input the watermark position ([x 0-${imageWidth - watermarkWidth}] [y 0-${imageHeight - watermarkHeight}]):")

    val (xStart, yStart) = try {
        val list = readln().split(" ").map { it.toInt() }
        if (list.size != 2) {
            println("The position input is invalid.")
            exitProcess(1)
        }
        Pair(list.first(), list.last())
    } catch (e: Exception) {
        println("The position input is invalid.")
        exitProcess(1)
    }

    if (xStart !in 0..imageWidth - watermarkWidth || yStart !in 0..imageHeight - watermarkHeight) {
        println("The position input is out of range.")
        exitProcess(1)
    }

    val outputImageName = outputImageName()

    val newImage = BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB)

    for (x in 0 until imageWidth) {
        for (y in 0 until imageHeight) {
            if (x in xStart until xStart + watermarkWidth && y in yStart until yStart + watermarkHeight) {
                val i = Color(image.getRGB(x, y))

                if (answerUse == "yes") {
                    val w = Color(watermark.getRGB(x - xStart, y - yStart), true)

                    if (w.alpha == 0) {
                        newImage.setRGB(x, y, Color(image.getRGB(x, y)).rgb)
                    }

                    if (w.alpha == 255) {
                        val color = blendedColor(w, i, weight)
                        newImage.setRGB(x, y, color.rgb)
                    }
                } else {
                    val w = Color(watermark.getRGB(x - xStart, y - yStart))

                    if (w.red == myColor.red && w.green == myColor.green && w.blue == myColor.blue && answerSet == "yes") {
                        newImage.setRGB(x, y, Color(image.getRGB(x, y)).rgb)
                    } else {
                        val color = blendedColor(w, i, weight)
                        newImage.setRGB(x, y, color.rgb)
                    }
                }
            } else {
                newImage.setRGB(x, y, Color(image.getRGB(x, y)).rgb)
            }
        }
    }
    val outputFile = File(outputImageName)
    ImageIO.write(newImage, outputImageName.split(".")[1], outputFile)

    println("The watermarked image $outputImageName has been created.")
}

fun createImageGridWatermark(
    image: BufferedImage,
    watermark: BufferedImage,
    weight: Int,
    answerUse: String,
    answerSet: String,
    myColor: Color
) {
    val imageWidth = image.width
    val imageHeight = image.height
    val watermarkWidth = watermark.width
    val watermarkHeight = watermark.height

    val outputImageName = outputImageName()

    val newImage = BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB)

    for (x in 0 until imageWidth) {
        for (y in 0 until imageHeight) {
            val i = Color(image.getRGB(x, y))

            if (answerUse == "yes") {
                val w = Color(watermark.getRGB(x % watermarkWidth, y % watermarkHeight), true)

                if (w.alpha == 0) {
                    newImage.setRGB(x, y, Color(image.getRGB(x, y)).rgb)
                }

                if (w.alpha == 255) {
                    val color = blendedColor(w, i, weight)
                    newImage.setRGB(x, y, color.rgb)
                }

            } else {
                val w = Color(watermark.getRGB(x % watermarkWidth, y % watermarkHeight))

                if (w.red == myColor.red && w.green == myColor.green && w.blue == myColor.blue && answerSet == "yes") {
                    newImage.setRGB(x, y, Color(image.getRGB(x, y)).rgb)
                } else {
                    val color = blendedColor(w, i, weight)
                    newImage.setRGB(x, y, color.rgb)
                }
            }
        }
    }

    val outputFile = File(outputImageName)
    ImageIO.write(newImage, outputImageName.split(".")[1], outputFile)

    println("The watermarked image $outputImageName has been created.")
}