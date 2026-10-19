package io.pythia.service

import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.xssf.usermodel.XSSFCell
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFFont
import org.apache.poi.xssf.usermodel.XSSFRow
import org.apache.poi.xssf.usermodel.XSSFWorkbook

/**
 * The cell styles of ONE exported workbook (task-185). A style and a font belong
 * to the workbook that created them, and Excel caps a workbook at 64k styles, so
 * [ExcelExporter] builds one holder per export and threads it through its write
 * methods — never a field on the shared exporter, never a style per cell. Every
 * accessor builds its style once and returns the cached instance afterwards.
 *
 * Styles only change how a cell looks, never its value or type: that is what
 * keeps the workbook importable by [ExcelImporter].
 */
@Suppress("MagicNumber")
internal class ExcelStyles(private val workbook: XSSFWorkbook) {

    private companion object {
        /** `--color-brand-green` in src/frontend/src/app.css. */
        val BRAND_GREEN = byteArrayOf(0x00, 0x9E.toByte(), 0x5A)

        /** The light fill that sets group rows apart (#F1F5F9). */
        val GROUP_FILL = byteArrayOf(0xF1.toByte(), 0xF5.toByte(), 0xF9.toByte())

        val WHITE = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())

        const val NUMBER_FORMAT = "#,##0.00"
        const val MONEY_FORMAT = "#,##0.00 \"€\""
        const val PERCENT_FORMAT = "0.0 %"
        const val INTEGER_FORMAT = "0"
    }

    private val dataFormat = workbook.createDataFormat()

    // The only two fonts this workbook gets; everything else uses the default font.
    private val boldFont: XSSFFont by lazy { workbook.createFont().apply { bold = true } }
    private val headerFont: XSSFFont by lazy {
        workbook.createFont().apply {
            bold = true
            setColor(XSSFColor(WHITE, null))
        }
    }

    val header: XSSFCellStyle by lazy {
        workbook.createCellStyle().apply {
            setFont(headerFont)
            fill(BRAND_GREEN)
            wrapText = true
            verticalAlignment = VerticalAlignment.CENTER
        }
    }

    val sectionTitle: XSSFCellStyle by lazy {
        workbook.createCellStyle().apply { setFont(boldFont) }
    }

    private val texts = mutableMapOf<Pair<Int, Boolean>, XSSFCellStyle>()
    private val numbers = mutableMapOf<Boolean, XSSFCellStyle>()
    private val moneys = mutableMapOf<Boolean, XSSFCellStyle>()

    /** Column 0 of the structure plan: the tree depth as real Excel indentation. */
    fun text(indent: Int, group: Boolean): XSSFCellStyle = texts.getOrPut(indent to group) {
        base(group).apply { indention = indent.toShort() }
    }

    fun number(group: Boolean): XSSFCellStyle = numbers.getOrPut(group) { formatted(NUMBER_FORMAT, group) }

    fun money(group: Boolean): XSSFCellStyle = moneys.getOrPut(group) { formatted(MONEY_FORMAT, group) }

    private val percentStyle: XSSFCellStyle by lazy { formatted(PERCENT_FORMAT, group = false) }
    fun percent(): XSSFCellStyle = percentStyle

    private val integerStyle: XSSFCellStyle by lazy { formatted(INTEGER_FORMAT, group = false) }
    fun integer(): XSSFCellStyle = integerStyle

    private val wrappedTextStyle: XSSFCellStyle by lazy {
        workbook.createCellStyle().apply {
            wrapText = true
            verticalAlignment = VerticalAlignment.TOP
        }
    }
    fun wrappedText(): XSSFCellStyle = wrappedTextStyle

    private fun formatted(pattern: String, group: Boolean): XSSFCellStyle =
        base(group).apply { dataFormat = this@ExcelStyles.dataFormat.getFormat(pattern) }

    /** A fresh style; a group row's cells are bold on the light group fill. */
    private fun base(group: Boolean): XSSFCellStyle = workbook.createCellStyle().apply {
        if (group) {
            setFont(boldFont)
            fill(GROUP_FILL)
        }
    }

    /** The header band: every cell of [row] in the header style, the row tall enough for two lines. */
    fun styleHeader(row: XSSFRow, heightInPoints: Float = 32f) {
        row.forEach { it.cellStyle = header }
        row.heightInPoints = heightInPoints
    }

    private fun XSSFCellStyle.fill(rgb: ByteArray) {
        setFillForegroundColor(XSSFColor(rgb, null))
        fillPattern = FillPatternType.SOLID_FOREGROUND
    }
}

/** `setColumnWidth` takes 1/256ths of a character. */
internal const val CHAR_WIDTH = 256

/** Sets the widths of the first columns, in characters — explicit, never `autoSizeColumn`. */
internal fun Sheet.widths(vararg chars: Int) =
    chars.forEachIndexed { idx, w -> setColumnWidth(idx, w * CHAR_WIDTH) }

internal fun XSSFCell.styled(style: XSSFCellStyle): XSSFCell = apply { cellStyle = style }
