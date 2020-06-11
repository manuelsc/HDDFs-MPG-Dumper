import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import fs.HDDFs
import fs.HDDFsImage
import logger.ConsoleLogger
import logger.EmptyLogger
import utils.blocksToMpgSize
import utils.format
import java.io.File
import kotlin.system.exitProcess

const val VERSION = "0.21"

@Suppress("SpellCheckingInspection")
class Main : CliktCommand(printHelpOnEmptyArgs = true) {

    private val input: File by option("-i", "--input", envvar = "HDDFS_DUMP_IN", help = "HDDFs Image").file(
        mustExist = true,
        canBeFile = true,
        canBeDir = false
    ).required()

    private val output: File by option("-o", "--output", envvar = "HDDFS_DUMP_OUT", help = "MPG output directory").file(
        mustExist = false,
        canBeFile = false,
        canBeDir = true
    ).required()

    private val debug: Boolean by option(
        "--debug", envvar = "HDDFS_DUMP_DEBUG",
        help = "Dont write to disk, debug output on console"
    ).flag(default = false)

    private val noMpgCleanup: Boolean by option(
        "--no-mpg-cleanup", envvar = "HDDFS_DUMP_NO_CLEANUP",
        help = "Cleaning up MPGs means skipping non sequential blocks. " +
                "Some VCRs simply override deleted MPGs with new ones, resulting in an overlap at the end of the mpg." +
                "If you encounter issues with the cleanup you can use this option to disable it."
    ).flag(default = false)

    private val recover: Boolean by option(
        "--recover", envvar = "HDDFS_DUMP_RECOVER",
        help = "Dump even deleted data. You might want to use this in combination with --no-mpg-cleanup"
    ).flag(default = false)

    override fun run() {
        intro()

        val pathToFile = input.toString()
        val outPath = output.toString()

        val hddfs = loadHDDFSImage(pathToFile, debug)
        checkValidHDDFs(hddfs)

        val mpgPosition = hddfs.findMpgSectionPosition().also {
            println("MPG sector found at: ${it.first}, length: ${it.second} ")
        }

        val mpgBlocks = getMPGBlockCount(hddfs, mpgPosition, recover)

        println("Extracting to \"$outPath\"${ if(noMpgCleanup) " (no cleanup)" else "" }...")
        hddfs.extractAllMpgs(
            outPath,
            mpgPosition,
            if (debug) EmptyLogger(mpgBlocks) else ConsoleLogger(mpgBlocks),
            noMpgCleanup,
            recover
        )
    }

    private fun intro() {
        println("\nHDDFs MPG Dumper v$VERSION by Manuel S. Caspari, June 2020")
        println("Bitcoin: 1Ju9G5U4iwJekRvUnG1JDme11vjxJ889Ux")
        println("Ethereum: 0xa9981a33f6b1A18da5Db58148B2357f22B44e1e0")
        println("Tip me a beer if you like this tool :)\n")
        println("Licences: ")
        println("- Clikt, Copyright 2018-2020 AJ Alt, Apache License Version 2.0 (https://github.com/ajalt/clikt)")
        println("- Progressbar, Copyright 2015-2020 Tongfei Chen, The MIT License (https://github.com/ctongfei/progressbar)\n")
    }

    private fun getMPGBlockCount(hddfs: HDDFsImage, mpgPosition: Pair<UInt, UInt>, recover: Boolean): Long {
        print("Analysing MPG sector (this might take a while)... ")

        val mpgBlocks = if(recover){
            mpgPosition.second.toLong() / HDDFs.MPG_ALLOC_BLOCK_SECTORS
        } else {
            hddfs.getMpgBlockCount(mpgPosition)
        }
        val mpgSize = mpgBlocks.blocksToMpgSize

        println("OK")
        println("Found ${mpgSize.toULong().format()} bytes of MPG data (Blocks: $mpgBlocks)\n")
        return mpgBlocks
    }

    private fun loadHDDFSImage(pathToFile: String, debug: Boolean): HDDFsImage {
        print("Reading \"$pathToFile\"... ")
        return HDDFsImage(pathToFile, debug).also {
            println("OK")
        }
    }

    private fun checkValidHDDFs(hddfs: HDDFsImage) {
        print("Check for HDDFs file system... ")
        if (!hddfs.isValidHDDFs()) {
            println("NOK")
            println("No valid HDDFs Image found")
            exitProcess(-1)
        }
        println("OK")
    }
}

fun main(args: Array<String>) = Main().main(args)