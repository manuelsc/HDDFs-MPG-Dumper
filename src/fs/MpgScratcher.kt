package fs

import fs.HDDFsImage.Companion.findIn
import logger.ProgressLogger
import utils.sectorAsPosition
import java.io.File
import java.io.FileOutputStream

@Suppress("ConstantConditionIf", "SpellCheckingInspection")
class MpgScratcher(
    private val source: HDDFsImage,
    private val position: Pair<UInt, UInt>,
    private val logger: ProgressLogger
) {

    fun extractTo(outPath: String) {
        val outputPath = File(outPath)
        if (!outputPath.exists()) outputPath.mkdirs()

        exportAll(outPath, 0, 1)
    }

    private fun exportAll(outPath: String, offset: Int, fileOffset: Int) {
        val nextOffset = write("$outPath/$fileOffset.mpg", offset)
        if (nextOffset == COMPLETE) {
            logger.complete()
            return
        }
        exportAll(outPath, nextOffset, fileOffset + 1)
    }

    private fun write(outFile: String, offset: Int): Int {
        val file = File(outFile)
        var fos: FileOutputStream? = null

        try {
            fos = FileOutputStream(file)

            var i = offset
            while (i in offset until position.second.toInt() / HDDFs.MPG_ALLOC_BLOCK_SECTORS) {
                logger.progress(i.toLong(), file.name)

                val absolutePosition = (position.first.toLong() + (i * HDDFs.MPG_ALLOC_BLOCK_SECTORS)).sectorAsPosition
                val chunk = source.readBytes(absolutePosition, HDDFs.MPG_ALLOC_BLOCK_SIZE.toInt())

                // Reached end of mpg
                if (HDDFs.EMPTY.findIn(chunk, 0)) {
                    i = COMPLETE
                    break
                }

                // Reached end of file, write next one
                if (i != offset && HDDFs.MPG_NEW_SIGNATURE.findIn(chunk, 4)) {
                    break
                }

                fos.write(chunk)
                i++
            }

            fos.flush()
            return i
        } finally {
            fos?.close()
        }
    }

    companion object {
        const val COMPLETE = -1
    }
}