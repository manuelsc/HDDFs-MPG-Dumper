package fs

import fs.HDDFsImage.Companion.findIn
import logger.ProgressLogger
import utils.sectorAsPosition
import utils.toUInt32BigEndian
import java.io.File
import java.io.FileOutputStream

@Suppress("ConstantConditionIf", "SpellCheckingInspection")
class MpgScratcher(
    private val source: HDDFsImage,
    private val position: Pair<UInt, UInt>,
    private val logger: ProgressLogger,
    private val allowSequenceJumps: Boolean, // noMpgCleanup
    private val recover: Boolean,
    private val debug: Boolean = false
) {

    fun extractTo(outPath: String) {
        val outputPath = File(outPath)
        if (!outputPath.exists()) outputPath.mkdirs()

        exportAll(outPath, 0, 1)
    }

    private fun exportAll(outPath: String, offset: Int, fileOffset: Int) {
        if(debug) println("$fileOffset.mpg starts at index $offset")

        val nextOffset = write("$outPath/$fileOffset.mpg", offset)
        if (nextOffset == COMPLETE || nextOffset == position.second.toInt() / HDDFs.MPG_ALLOC_BLOCK_SECTORS.toInt()) {
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
            var lastSequence = HDDFs.MPG_NEW_SIGNATURE.toUInt32BigEndian()
            var lastGap = 7u * HDDFs.MPG_ALLOC_BLOCK_SECTORS.toUInt()

            while (i in offset until position.second.toInt() / HDDFs.MPG_ALLOC_BLOCK_SECTORS) {
                logger.progress(i.toLong(), file.name)

                // Current Block
                val chunk = readChunk(i)
                val isMpgBlock = hasMpgHeader(chunk)
                val sequenceNumber = getSequenceNumber(chunk)

                // Next Block
                val nextChunkHeader = readChunk(i + 1, 12)
                val nextIsMpgBlock = hasMpgHeader(nextChunkHeader)
                val nextSequence = getSequenceNumber(nextChunkHeader)

                // Abort criterium 1: Reached end of mpg, we are done
                val isEmpty = HDDFs.EMPTY.findIn(chunk, 0)
                if (!recover && isEmpty) {
                    i = COMPLETE
                    break
                }

                // Abort criterium 2: Reached end of title, finish this write up and continue with next one
                if (i != offset && HDDFs.MPG_NEW_SIGNATURE.findIn(chunk, 4)) {
                    break
                }

                // Protect against ghost data (VCR overrides previously deleted mpg)
                // Ignore those sudden sequence jumps if allowSequenceJumps is not set
                val margin = lastGap + (lastGap * 4u).toUInt()
                val changeToNow = sequenceNumber - lastSequence
                if(!isMpgBlock || isSequenceSequential(changeToNow, margin) || allowSequenceJumps){
                    if(debug){
                        println("$isMpgBlock (" + sequenceNumber + ") " + debugGetHeader(chunk) +" || margin: $margin || change: $changeToNow")
                    } else {
                        // Cleanup last block
                        if(nextIsMpgBlock && !isSequenceSequential(nextSequence - sequenceNumber, margin * 3u) && !allowSequenceJumps) {
                            fos.write(cleanupLastBlock(chunk, sequenceNumber))
                        } else {
                            if(!isEmpty) fos.write(chunk)
                        }
                    }

                    if(isMpgBlock){
                        lastGap = sequenceNumber - lastSequence
                        lastSequence = sequenceNumber
                    }
                }

                i++
            }

            if(!debug) fos.flush()
            return i
        } finally {
            fos?.close()
        }
    }

    /**
     * So the VCR overrides old deleted MPGs with new onces which is why we need to pay attention
     * to the sequence number. Since we read in bigger chunks for performance reasons we manually
     * clean up the last block if we find a non sequential subblock within the last block
     * @param chunk read last block
     * @param sequenceNumber of last block
     * @return either the whole chunk if no sequential inconsistency has been found
     * or a subset of chunk of all sequential subblocks if it is inconsistent
     */
    private fun cleanupLastBlock(chunk: ByteArray, sequenceNumber: UInt): ByteArray{
        var lastGap = HDDFs.HDD_SECTOR_SIZE.toUInt()
        var lastSequence = sequenceNumber

        for(i in 1 until chunk.size / HDDFs.HDD_SECTOR_SIZE){
            val sequence = getSequenceNumber(chunk, (i * HDDFs.HDD_SECTOR_SIZE) + 4)
            val isMpgBlock = hasMpgHeader(chunk, i * HDDFs.HDD_SECTOR_SIZE)
            val changeToNow = sequence - lastSequence

            val margin = lastGap + (lastGap * 4u).toUInt()
            if(isMpgBlock && ! isSequenceSequential(changeToNow, margin)){
                val newChunk = ByteArray(i * HDDFs.HDD_SECTOR_SIZE)
                chunk.copyInto(newChunk, 0, 0, newChunk.size)
                if(debug) println("Last block was cleaned up")
                return newChunk
            }

            if(isMpgBlock){
                lastGap = sequence - lastSequence
                lastSequence = sequence
            }
        }
        return chunk
    }

    /**
     * Has the following block a valid MPG signature
     * @param chunk read block
     * @param offset optional offset
     * @return true if chunk has a valid MPG signature at given offset
     */
    private fun hasMpgHeader(chunk: ByteArray, offset: Int = 0): Boolean {
        return HDDFs.MPG_SIGNATURE.findIn(chunk, offset)
    }

    /**
     * @param change difference between last block and this block
     * @param margin allowed margin to be considered a sequential block
     * @returns true if difference is within the margin (sequential)
     * or false if not
     */
    private fun isSequenceSequential(change: UInt, margin: UInt): Boolean {
        return change <= margin && change > 0u
    }

    /**
     * Read a block at a given offset with given length
     * @param offset in sectors
     * @param length in size
     * @returns block of data at offset with length
     */
    private fun readChunk(offset: Int, length: Int = HDDFs.MPG_ALLOC_BLOCK_SIZE.toInt()): ByteArray {
        val absolutePosition = (position.first.toLong() + (offset * HDDFs.MPG_ALLOC_BLOCK_SECTORS)).sectorAsPosition
        return source.readBytes(absolutePosition, length)
    }

    /**
     * Get the sequence number of a MPG block
     * @param chunk mpg block
     * @param offset on where the sequence number is located in the mpg block. Default offset of 4
     * @return an UInt interpretation of the 4 most significant bytes of the mpg sequence
     */
    private fun getSequenceNumber(chunk: ByteArray, offset: Int = 4): UInt {
        val sequenceNumber = ByteArray(4)
        chunk.copyInto(sequenceNumber, 0, offset, endIndex = sequenceNumber.size + offset)
        return sequenceNumber.toUInt32BigEndian()
    }

    private fun debugGetHeader(chunk: ByteArray): String {
        var erg = ""
        val until = if(chunk.size > 13) 13 else chunk.size
        for(i in 0 until until){
            erg += String.format("%02x", chunk[i]) + " "
        }
        return erg
    }

    companion object {
        const val COMPLETE = -1
    }
}