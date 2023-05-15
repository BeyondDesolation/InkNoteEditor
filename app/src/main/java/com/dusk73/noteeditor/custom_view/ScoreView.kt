package com.dusk73.noteeditor.custom_view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.dusk73.noteeditor.R
import com.dusk73.noteeditor.custom_view.models.*
import com.dusk73.musicxmltools.MusicEditor
import com.dusk73.musicxmltools.enums.Accidental
import com.dusk73.musicxmltools.enums.NoteType
import com.dusk73.musicxmltools.models.*
import com.dusk73.musicxmltools.models.base.MeasureElement
import com.dusk73.musicxmltools.models.tools.ElementPosition
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt


class ScoreView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var viewWidth = 1000
    private var scoreWidth = 1000
    private var scoreHeight = 200

    private var size = 0f
    private var scoreHorizontalPadding = 25
    private var scoreVerticalPadding = 50
    private var measureHorizontalPaddings = 0f
    private var staffVerticalPaddings = 0f
    private var staffsGroupPadding = 0f
    private var staffSpace = 0f

    private var staffLineHeight = 0f
    private var noteVerticalStep = 0f

    private var noteWidth = 0f
    private var stemWidth = 0f
    private var noteFlagsWidth = 0f
    private var accidentalWidth = 0f

    private var clefWidth = 0f
    private var timeWidth = 0f

    private var minNotesInterval = 0f

    private lateinit var restNotesWidth: Map<NoteType, Float>

    private val intervalWeights = mapOf(
        NoteType.WHOLE to 10f,
        NoteType.HALF to 5f,
        NoteType.QUARTER to 2.5f,
        NoteType.EIGHTH to 1.5f,
        NoteType._16TH to 1.25f,
        NoteType._32EN to 1f,
        NoteType._64TH to 1f,
    )

    private val lineOrderOfSharps = listOf(8, 5, 9, 6, 3, 7, 4)
    private val lineOrderOfFlats = listOf(4, 7, 3, 6, 2, 5, 1)

    @SuppressLint("ResourceType")
    private val paintRect = Paint().apply {
        isAntiAlias = true
        color = Color.parseColor(resources.getString(R.color.main_2))
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }
    private val paintLeland = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
        textAlign = Paint.Align.LEFT
        typeface = ResourcesCompat.getFont(context, R.font.leland)
    }
    private val paintLelandText = Paint().apply {
        color = Color.BLUE
        textAlign = Paint.Align.LEFT
        typeface = ResourcesCompat.getFont(context, R.font.leland_text)
    }

    // i - part index, j - measure index
    private val measuresDrawInfo = ArrayList<ArrayList<MeasureDrawInfo>>()

    private var musicEditor = MusicEditor()
    private var scorePartwise = musicEditor.scorePartwise

    var touchInfo: TouchInfo? = null

    init {
        musicEditor.addNote(ElementPosition(0, 0, 0), 1, NoteType.QUARTER)
        recalcSize(110f, scoreWidth)
        update()
    }

    fun updateMusicEditor(musicEditor: MusicEditor) {
        this.musicEditor = musicEditor
        scorePartwise = musicEditor.scorePartwise
    }

    fun update() {
        measuresDrawInfo.clear()

        for (i in 0 until scorePartwise.parts.size) measuresDrawInfo.add(ArrayList())

        val measureWidths = ArrayList<MeasureWidthInfo>(scorePartwise.parts.size)
        val measuresCount = scorePartwise.parts[0].measures.size

        var firstInRowMeasureIndex = 0
        var staffsGroup = 0
        var totalWidths = 0f

        var isClefAdded: Boolean
        var isKeyAdded: Boolean
        var lastKeyWidth = 0f

        fun addDrawInfo(measureWidthsInfo: List<MeasureWidthInfo>) {
            val widthsAndMinIntervals = fillSpaceForMeasures(measureWidthsInfo)
            addMeasuresDrawInfo(
                firstInRowMeasureIndex,
                widthsAndMinIntervals.map { it.first },
                staffsGroup
            )
            addElementsDrawInfo(widthsAndMinIntervals.map { it.second }, firstInRowMeasureIndex)
        }

        for (measureIndex in 0 until measuresCount) {
            isClefAdded = false
            isKeyAdded = false
            var attributesWidth = 0f

            if (isThereTimePresent(0, measureIndex)) attributesWidth += timeWidth

            val keyWidth = calcKeyWidth(0, measureIndex)
            if (keyWidth > 0) {
                attributesWidth += keyWidth
                lastKeyWidth = keyWidth
                isKeyAdded = true
            }

            if (isThereClefPresent(0, measureIndex)) {
                attributesWidth += clefWidth
                isClefAdded = true
            }

            val measuresWidthInfo = calcMeasuresMinWidth2(measureIndex)
            measuresWidthInfo.attributesWidth = attributesWidth

            if (totalWidths + attributesWidth + measuresWidthInfo.width > scoreWidth) {
                addDrawInfo(measureWidths)
                measureWidths.clear()

                firstInRowMeasureIndex = measureIndex
                totalWidths = 0f
                staffsGroup++

                if (!isClefAdded) attributesWidth += clefWidth

                if (!isKeyAdded) attributesWidth += lastKeyWidth
            }
            totalWidths += attributesWidth + measuresWidthInfo.width
            measuresWidthInfo.attributesWidth = attributesWidth
            measureWidths.add(measuresWidthInfo)
        }
        if (totalWidths > 0)
            addDrawInfo(measureWidths)

        if(measuresDrawInfo.last().last().bottom.toInt() != scoreHeight) {
            scoreHeight = measuresDrawInfo.last().last().bottom.toInt() + 100
            requestLayout()
        }

        touchInfo = null
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)

        val newViewWidth = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> scoreWidth.coerceAtMost(widthSize)
            else -> scoreWidth
        }
        if(newViewWidth != viewWidth) {
            viewWidth = newViewWidth
            scoreWidth = newViewWidth - scoreHorizontalPadding * 3
            recalcSize(size, scoreWidth)
            update()
        }
        setMeasuredDimension(newViewWidth, scoreHeight)
    }

    override fun onDraw(canvas: Canvas?) {
        drawScore(canvas!!)
        drawTouch(canvas)
        super.onDraw(canvas)
    }

    private var prevX = 0f
    private var prevY = 0f
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                prevX = event.x
                prevY = event.y
            }
            MotionEvent.ACTION_UP -> {
                if (abs(prevX - event.x) < 100f && abs(prevY - event.y) < 100f) processTouch(
                    event.x,
                    event.y
                )
            }
        }
        return true
    }

    private fun processTouch(x: Float, y: Float) {
        val touchedMeasurePos = findTouchedMeasure(x, y)
        if (touchedMeasurePos.first < 0) {
            touchInfo = null
        } else {
            val partIndex = touchedMeasurePos.first
            val measureIndex = touchedMeasurePos.second
            val measure = measuresDrawInfo[partIndex][measureIndex]

            val zeroLineY = measure.bottom - staffVerticalPaddings + staffLineHeight
            val line = ((zeroLineY - y + noteVerticalStep / 2) / noteVerticalStep).toInt()

            val nearestNote = findNearestNote(measure.elements, x, y)
            val note =
                scorePartwise.parts[partIndex].measures[measureIndex].elements[nearestNote.elementIndex] as Note

            val isElementTouched = note.line != null && note.line!! == line

            touchInfo = TouchInfo(
                partIndex, measureIndex, line, measure, nearestNote, isElementTouched
            )
        }
        invalidate()
    }

    private fun findTouchedMeasure(x: Float, y: Float): Pair<Int, Int> {
        for (partIndex in 0 until measuresDrawInfo.size) {
            for (measureIndex in 0 until measuresDrawInfo[partIndex].size) {
                val measure = measuresDrawInfo[partIndex][measureIndex]

                if (measure.left < x && measure.right > x && measure.top < y && measure.bottom > y) return Pair(
                    partIndex,
                    measureIndex
                )
            }
        }
        return Pair(-1, -1)
    }

    private fun findNearestNote(
        elements: List<ElementDrawInfo>,
        x: Float,
        y: Float,
    ): ElementDrawInfo {
        var minDistance = Float.MAX_VALUE

        var nearestElement: ElementDrawInfo? = null

        for (element in elements) {
            if(element.elementIndex < 0)
                continue
            val distance = sqrt(abs(element.y - y).pow(2) + abs(element.x - x).pow(2))
            if(distance < minDistance) {
                minDistance = distance
                nearestElement = element
            }
        }
        return nearestElement!!
    }


    private fun drawScore(canvas: Canvas) {
        for (partIndex in 0 until measuresDrawInfo.size) {
            var currentStaffsGroup = -1

            for (measureInfo in measuresDrawInfo[partIndex]) {
                // drawMeasureFrame(measureInfo, canvas)

                drawMeasure(measureInfo, canvas)
                drawSingleBarLines(measureInfo, partIndex, canvas)

                if (currentStaffsGroup != measureInfo.staffsGroup) {
                    drawFiveLineStaff(measureInfo, canvas)
                    currentStaffsGroup = measureInfo.staffsGroup
                }
            }
        }
    }

    private fun drawMeasureFrame(measureDrawInfo: MeasureDrawInfo, canvas: Canvas) {
        paintLeland.style = Paint.Style.STROKE
        canvas.drawRect(
            measureDrawInfo.left,
            measureDrawInfo.top,
            measureDrawInfo.right,
            measureDrawInfo.bottom,
            paintLeland
        )
        paintLeland.style = Paint.Style.FILL
    }

    private fun drawMeasure(measureDrawInfo: MeasureDrawInfo, canvas: Canvas) {
        for (elementInfo in measureDrawInfo.elements) {
            canvas.drawText(
                elementInfo.view, elementInfo.x, elementInfo.y, paintLeland
            )
        }
        // Тактовая черта
        canvas.drawText(
            "\uD834\uDD00",
            measureDrawInfo.left,
            measureDrawInfo.top + staffVerticalPaddings + size,
            paintLeland
        )
    }

    private fun drawSingleBarLines(
        measureDrawInfo: MeasureDrawInfo, partIndex: Int, canvas: Canvas
    ) {
        if (scorePartwise.parts.size >= 2) {
            if (partIndex < scorePartwise.parts.size - 1) {
                canvas.drawText(
                    "\uD834\uDD00",
                    measureDrawInfo.left,
                    measureDrawInfo.top + staffVerticalPaddings * 2 + size,
                    paintLeland
                )
            }
            if (partIndex > 0) {
                canvas.drawText(
                    "\uD834\uDD00",
                    measureDrawInfo.left,
                    measureDrawInfo.top + staffVerticalPaddings,
                    paintLeland
                )
            }
        }
    }

    private fun drawFiveLineStaff(measureDrawInfo: MeasureDrawInfo, canvas: Canvas) {
        for (i in 0 until (scoreWidth / size).toInt()) {
            canvas.drawText(
                "\uD834\uDD1A",
                measureDrawInfo.left + i * size,
                measureDrawInfo.bottom - staffVerticalPaddings,
                paintLeland
            )
        }
    }

    private fun drawTouch(canvas: Canvas) {
        if (touchInfo == null) return

        val measure = touchInfo!!.measureDrawInfo
        val y = measure.bottom - staffVerticalPaddings + staffLineHeight -
                touchInfo!!.line * noteVerticalStep - noteVerticalStep * 1.25f

        canvas.drawRoundRect(
            measure.left, measure.top, measure.right, measure.bottom, 10f, 10f, paintRect
        )

        canvas.drawRoundRect(
            touchInfo!!.nearestElement.x,
            y,
            touchInfo!!.nearestElement.x + noteWidth,
            y + noteWidth,
            4f,
            4f,
            paintRect
        )
    }


    private fun recalcSize(newSize: Float, newScoreWidth: Int) {
        size = newSize
        scoreWidth = newScoreWidth
        measureHorizontalPaddings = size / 5f
        staffVerticalPaddings = size * 0.8f
        staffsGroupPadding = size / 3f
        staffSpace = size + staffVerticalPaddings * 2

        staffLineHeight = size / 4.098360f
        noteVerticalStep = staffLineHeight / 2f

        noteWidth = staffLineHeight * 1.2f
        stemWidth = noteWidth / 10f
        noteFlagsWidth = staffLineHeight * 1.0667f
        accidentalWidth = noteWidth / 1.1f

        clefWidth = noteWidth * 3.2f
        timeWidth = noteWidth * 2.5f

        minNotesInterval = staffLineHeight / 2.25f

        paintLeland.textSize = size
        paintLelandText.textSize = size

        restNotesWidth = mapOf(
            NoteType.WHOLE to noteWidth * 2.3f,
            NoteType.HALF to noteWidth * 2.3f,
            NoteType.QUARTER to noteWidth * 0.862f,
            NoteType.EIGHTH to noteWidth * 0.882f,
            NoteType._16TH to noteWidth * 1.1f,
            NoteType._32EN to noteWidth * 1.35f,
            NoteType._64TH to noteWidth * 1.586f,
        )
    }

    private fun isThereClefPresent(partIndex: Int, measureIndex: Int): Boolean {
        if (scorePartwise.parts[partIndex].measures[measureIndex].attributes?.clef != null) return true
        return false
    }

    private fun isThereTimePresent(partIndex: Int, measureIndex: Int): Boolean {
        if (scorePartwise.parts[partIndex].measures[measureIndex].attributes?.time != null) return true
        return false
    }

    private fun calcKeyWidth(partIndex: Int, measureIndex: Int): Float {
        val attributes =
            scorePartwise.parts[partIndex].measures[measureIndex].attributes ?: return 0f
        return abs(attributes.key!!.fifths) * accidentalWidth
    }

    private fun calcMeasuresMinWidth2(measureIndex: Int): MeasureWidthInfo {
        val elapsedDurations = ArrayList<Int>(scorePartwise.parts.size)
        val currentIndexes = ArrayList<Int>(scorePartwise.parts.size)

        val lastChordWidths = ArrayList<Pair<Float, Boolean>>(scorePartwise.parts.size)
        val lastChordX = ArrayList<Float>(scorePartwise.parts.size)

        for (part in scorePartwise.parts) {
            elapsedDurations.add(0)
            currentIndexes.add(0)
            lastChordWidths.add(Pair(0f, false))
            lastChordX.add(0f)
        }

        var mainX = measureHorizontalPaddings
        var mainElapsedDuration = 0

        var totalExtraSpace = 0f
        var totalIntervalWeights = 0f

        for (breaker in 0 until 1024) {
            var isThereWideChordWithStemDown = false

            for (partIndex in 0 until scorePartwise.parts.size) {
                val elements = scorePartwise.parts[partIndex].measures[measureIndex].elements

                if (currentIndexes[partIndex] >= elements.size)
                    continue

                if (elapsedDurations[partIndex] == mainElapsedDuration) {
                    val chord = getChord(elements, currentIndexes[partIndex])
                    val chordWidthInfo = calcChordWidth(elements, chord)

                    if (!isThereWideChordWithStemDown)
                        isThereWideChordWithStemDown = chordWidthInfo.second

                    lastChordWidths[partIndex] = chordWidthInfo
                }
            }

            var minDurationStep = Int.MAX_VALUE
            for (partIndex in 0 until scorePartwise.parts.size) {
                if (elapsedDurations[partIndex] == mainElapsedDuration) {
                    val elements = scorePartwise.parts[partIndex].measures[measureIndex].elements

                    val noteIndex = findNextNote(elements, currentIndexes[partIndex])
                    if (noteIndex < 0) continue

                    currentIndexes[partIndex] = noteIndex
                    val note = elements[currentIndexes[partIndex]] as Note

                    val xOffset =
                        if (isThereWideChordWithStemDown && !lastChordWidths[partIndex].second)
                            noteWidth
                        else
                            0f
                    lastChordX[partIndex] = mainX + xOffset

                    elapsedDurations[partIndex] += note.duration
                    currentIndexes[partIndex]++
                }
                if (elapsedDurations[partIndex] - mainElapsedDuration < minDurationStep) {
                    minDurationStep = elapsedDurations[partIndex] - mainElapsedDuration
                }
            }

            if (minDurationStep == Int.MAX_VALUE)
                break

            var maxNewX = -1f
            var extraSpace = 0f
            var intervalWeight = 0f
            var lastInterval = 0f
            for (partIndex in 0 until scorePartwise.parts.size) {
                val elements = scorePartwise.parts[partIndex].measures[measureIndex].elements

                if (elapsedDurations[partIndex] - mainElapsedDuration == minDurationStep) {
                    val interval = calcIntervalForNote(
                        elements, currentIndexes[partIndex] - 1, minNotesInterval
                    )
                    val newX = lastChordX[partIndex] + lastChordWidths[partIndex].first + interval.first

                    if (newX > maxNewX) {
                        maxNewX = newX
                        extraSpace = interval.second
                        intervalWeight =
                            intervalWeights[(elements[currentIndexes[partIndex] - 1] as Note).type]!!
                        lastInterval = interval.first
                    }
                }
            }
            // Если предыдущей нотой в партии была нота с точкой
            if(maxNewX < mainX)
                maxNewX = mainX + lastInterval

            mainElapsedDuration += minDurationStep
            totalExtraSpace += extraSpace
            totalIntervalWeights += intervalWeight
            mainX = maxNewX
        }
        return MeasureWidthInfo(mainX, totalExtraSpace, totalIntervalWeights)
    }

    private fun findNextNote(elements: List<MeasureElement>, startIndex: Int): Int {
        for (i in startIndex until elements.size) if ((elements[i] as Note?)?.chord == false) return i
        return -1
    }

    // return: first is width, second is extra space
    private fun calcIntervalForNote(
        elements: List<MeasureElement>,
        noteIndex: Int,
        minInterval: Float
    ): Pair<Float, Float> {
        val note = elements[noteIndex] as Note

        if (note.dots > 0) return Pair(note.dots * intervalWeights[note.type]!! * minInterval, 0f)

        val nextNoteIndex = findNextNote(elements, noteIndex + 1)
        if (nextNoteIndex < 0) return Pair(intervalWeights[note.type]!! * minInterval, 0f)

        var totalAccidentalWidth = 0f
        val chord = getChord(elements, nextNoteIndex)
        for (i in chord){
            if ((elements[i] as Note).accidental != null) {
                totalAccidentalWidth += accidentalWidth
                if(totalAccidentalWidth >= accidentalWidth * 3)
                    break
            }
        }
        val noteTypeInterval = intervalWeights[note.type]!! * minInterval
        val requiredInterval = minNotesInterval + totalAccidentalWidth
        if (noteTypeInterval >= requiredInterval) return Pair(noteTypeInterval, 0f)

        return Pair(requiredInterval, requiredInterval - noteTypeInterval)
    }

    private fun getChord(
        elements: List<MeasureElement>,
        startChordIndex: Int,
    ): List<Int> {
        val chord = ArrayList<Int>()
        chord.add(startChordIndex)
        for (i in startChordIndex + 1 until elements.size) {
            val nextChordNote = elements[i]
            if (nextChordNote is Note && nextChordNote.chord) {
                chord.add(i)
            } else {
                break
            }
        }
        return chord
    }


    private fun calcChordWidth(
        elements: List<MeasureElement>,
        chord: List<Int>
    ): Pair<Float, Boolean> {
        val note = elements[chord[0]] as Note

        if (note.rest)
            return Pair(restNotesWidth[note.type]!!, false)

        if (note.dots > 0)
            return Pair(noteWidth, false)

        val sorted = sortChordByLines(elements, chord)
        val isWideChord = isWideChord(sorted)

        if (isWideChord)
            return Pair(noteWidth * 2, note.stem == "down")

        if (note.type.divisions > 1 && note.stem == "up")
            return Pair(noteWidth + noteFlagsWidth, false)

        return Pair(noteWidth, false)
    }

    private fun sortChordByLines(
        elements: List<MeasureElement>,
        chord: List<Int>
    ): List<Pair<Int, Int>> {
        val chordWithLines = ArrayList<Pair<Int, Int>>()
        for (i in chord)
            chordWithLines.add(Pair(i, (elements[i] as Note).line!!))

        chordWithLines.sortWith(compareBy { it.second })
        return chordWithLines
    }

    private fun isWideChord(sortedChord: List<Pair<Int, Int>>): Boolean {
        for (i in 0..sortedChord.size - 2) {
            if (sortedChord[i + 1].second - sortedChord[i].second == 1) {
                return true
            }
        }
        return false
    }

    private fun findLastClef(partIndex: Int, measureIndex: Int): Clef {
        var clef: Clef? = null
        val measures = measuresDrawInfo[partIndex]
        for (i in measureIndex - 1 downTo 0) {
            if (measures[i].clef != null) {
                clef = measures[i].clef
                break
            }
        }
        return clef!!
    }

    private fun findLastKey(partIndex: Int, measureIndex: Int): Key {
        var fifths: Int? = null
        val part = measuresDrawInfo[partIndex]
        for (i in measureIndex - 1 downTo 0) {
            if (part[i].fifths != null) {
                fifths = part[i].fifths
                break
            }
        }
        return Key(fifths ?: 0)
    }

    private fun calcTopForStaffsGroup(groupNumber: Int): Float {
        return scoreVerticalPadding + (staffsGroupPadding + scorePartwise.parts.size * staffSpace) * groupNumber
    }

    private fun fillSpaceForMeasures(measuresWidthsInfo: List<MeasureWidthInfo>): List<Pair<Float, Float>> {
        val widthsAndMinIntervals = ArrayList<Pair<Float, Float>>(measuresWidthsInfo.size)

        val sumOfMeasureWidth = measuresWidthsInfo.sumOf { it.width.toDouble() }.toFloat()
        val freeSpace = scoreWidth -
                sumOfMeasureWidth - measuresWidthsInfo.sumOf { it.attributesWidth.toDouble() }
            .toFloat()

        for (i in measuresWidthsInfo.indices) {
            val additionalSpace =
                freeSpace * (measuresWidthsInfo[i].width / sumOfMeasureWidth)

            val newMeasureWidth =
                measuresWidthsInfo[i].width + additionalSpace + measuresWidthsInfo[i].attributesWidth
            val newMinInterval = minNotesInterval +
                    (additionalSpace + measuresWidthsInfo[i].extraSpace) / measuresWidthsInfo[i].sumOfIntervalWeights
            widthsAndMinIntervals.add(Pair(newMeasureWidth, newMinInterval))
        }
        return widthsAndMinIntervals
    }

    private fun addMeasuresDrawInfo(
        firstMeasure: Int, measuresWidths: List<Float>, staffsGroup: Int
    ) {
        val createMode =
            measuresDrawInfo[0].size == 0 || measuresDrawInfo[0].last().staffsGroup < staffsGroup

        val top = calcTopForStaffsGroup(staffsGroup)
        var left = scoreHorizontalPadding.toFloat()

        for (i in measuresWidths.indices) {
            val measureIndex = firstMeasure + i

            for (partIndex in 0 until scorePartwise.parts.size) {
                val measure = scorePartwise.parts[partIndex].measures[measureIndex]

                val measureDrawInfo = MeasureDrawInfo(
                    left = left,
                    top = top + partIndex * staffSpace,
                    right = left + measuresWidths[i],
                    bottom = top + (partIndex + 1) * staffSpace,
                    elements = ArrayList(),
                    staffsGroup = staffsGroup,
                    attributesWidth = 0f,
                )
                val y = top + partIndex * staffSpace + staffVerticalPaddings + size
                var x = left + measureHorizontalPaddings

                var clef: Clef? = measure.attributes?.clef
                var key: Key? = measure.attributes?.key
                val time: Time? = measure.attributes?.time
                var clefForKey: Clef? = clef
                if (i == 0) {
                    clef = clef ?: findLastClef(partIndex, firstMeasure)
                    key = key ?: findLastKey(partIndex, firstMeasure)
                    clefForKey = clef
                } else if (key != null) {
                    clefForKey = clef ?: findLastClef(partIndex, measureIndex)
                }

                val startX = x
                x = addClefDrawInfo(measureDrawInfo, x, y, clef)
                x = addKeyDrawInfo(measureDrawInfo, x, y, clefForKey, key)
                x = addTimeDrawInfo(measureDrawInfo, x, y, time)

                measureDrawInfo.attributesWidth = x - startX

                if (createMode) measuresDrawInfo[partIndex].add(measureIndex, measureDrawInfo)
                else measuresDrawInfo[partIndex][measureIndex] = measureDrawInfo
            }
            left += measuresWidths[i]
        }
    }

    private fun addClefDrawInfo(
        measureDrawInfo: MeasureDrawInfo,
        x: Float,
        y: Float,
        clef: Clef?,
    ): Float {
        if (clef == null) return x

        measureDrawInfo.elements.add(
            ElementDrawInfo(
                elementIndex = -1,
                view = "\uD834\uDD1E",
                x = x, y = y,
            )
        )
        measureDrawInfo.clef = clef
        return x + clefWidth
    }

    private fun addKeyDrawInfo(
        measureDrawInfo: MeasureDrawInfo,
        x: Float,
        y: Float,
        clef: Clef?,
        key: Key?,
    ): Float {
        if (key == null || clef == null) return x

        val fifths = key.fifths
        var currentX = x

        if (fifths != 0) {
            var noteCLine = musicEditor.calcLineOfNoteC(clef)
            if (clef.sign == "F") noteCLine -= 7

            val view: String?
            val lineOrder: List<Int>?

            if (fifths < 0) {
                view = ScoreViewResources.flat
                lineOrder = lineOrderOfFlats
            } else {
                view = ScoreViewResources.sharp
                lineOrder = lineOrderOfSharps
            }

            for (k in 1..abs(fifths)) {
                measureDrawInfo.elements.add(
                    ElementDrawInfo(
                        elementIndex = -1,
                        view = view,
                        x = currentX,
                        y = y - (lineOrder[k] + noteCLine) * noteVerticalStep,
                    )
                )
                currentX += accidentalWidth
            }
            currentX += accidentalWidth
        }
        measureDrawInfo.fifths = fifths
        return currentX
    }

    private fun addTimeDrawInfo(
        measureDrawInfo: MeasureDrawInfo,
        x: Float,
        y: Float,
        time: Time?,
    ): Float {
        if (time == null) return x

        measureDrawInfo.elements.add(
            ElementDrawInfo(
                elementIndex = -1,
                view = ScoreViewResources.noteTypes[time.beats]!!,
                x = x,
                y = y - staffLineHeight * 3,
            )
        )
        measureDrawInfo.elements.add(
            ElementDrawInfo(
                elementIndex = -1,
                view = ScoreViewResources.noteTypes[time.beatType]!!,
                x = x,
                y = y - staffLineHeight,
            )
        )
        return x + timeWidth
    }

    private fun addElementsDrawInfo(minNoteIntervals: List<Float>, firstMeasureIndex: Int) {
        for (i in minNoteIntervals.indices) {
            val measureIndex = firstMeasureIndex + i

            val elapsedDurations = ArrayList<Int>(scorePartwise.parts.size)
            val currentIndexes = ArrayList<Int>(scorePartwise.parts.size)

            val lastChordWidths = ArrayList<Pair<Float, Boolean>>(scorePartwise.parts.size)
            val lastChordX = ArrayList<Float>(scorePartwise.parts.size)

            for (part in scorePartwise.parts) {
                elapsedDurations.add(0)
                currentIndexes.add(0)
                lastChordWidths.add(Pair(0f, false))
                lastChordX.add(0f)
            }

            var mainX = measuresDrawInfo[0][measureIndex].left +
                    measuresDrawInfo[0][measureIndex].attributesWidth +
                    measureHorizontalPaddings

            var mainElapsedDuration = 0

            for (breaker in 0 until 1024) {
                var isThereWideChordWithStemDown = false

                for (partIndex in 0 until scorePartwise.parts.size) {
                    val elements = scorePartwise.parts[partIndex].measures[measureIndex].elements

                    if (currentIndexes[partIndex] >= elements.size)
                        continue

                    if (elapsedDurations[partIndex] == mainElapsedDuration) {
                        val chord = getChord(elements, currentIndexes[partIndex])
                        val chordWidthInfo = calcChordWidth(elements, chord)

                        if (!isThereWideChordWithStemDown)
                            isThereWideChordWithStemDown = chordWidthInfo.second

                        lastChordWidths[partIndex] = chordWidthInfo
                    }
                }

                var minDurationStep = Int.MAX_VALUE

                for (partIndex in 0 until scorePartwise.parts.size) {
                    val elements = scorePartwise.parts[partIndex].measures[measureIndex].elements

                    if (currentIndexes[partIndex] >= elements.size)
                        continue

                    if (elements[currentIndexes[partIndex]] is Note) {
                        if (elapsedDurations[partIndex] == mainElapsedDuration) {
                            val measureDrawInfo = measuresDrawInfo[partIndex][measureIndex]
                            val chord = getChord(elements, currentIndexes[partIndex])

                            val xOffset =
                                if (isThereWideChordWithStemDown && !lastChordWidths[partIndex].second)
                                    noteWidth
                                else
                                    0f

                            lastChordX[partIndex] = mainX + xOffset
                            addNotesDrawInfo(measureDrawInfo, elements, chord, mainX + xOffset)

                            elapsedDurations[partIndex] += (elements[chord[0]] as Note).duration
                            currentIndexes[partIndex] = currentIndexes[partIndex] + chord.size
                        }
                        if (elapsedDurations[partIndex] - mainElapsedDuration < minDurationStep)
                            minDurationStep = elapsedDurations[partIndex] - mainElapsedDuration
                    }
                }

                if (minDurationStep == Int.MAX_VALUE)
                    break

                var maxNewX = -1f
                var lastInterval = 0f
                for (partIndex in 0 until scorePartwise.parts.size) {
                    val elements = scorePartwise.parts[partIndex].measures[measureIndex].elements

                    if (elapsedDurations[partIndex] - mainElapsedDuration == minDurationStep) {
                        val interval = calcIntervalForNote(
                            elements, currentIndexes[partIndex] - 1,
                            minNoteIntervals[i]
                        )
                        val newX = lastChordX[partIndex] + lastChordWidths[partIndex].first + interval.first

                        if (newX > maxNewX) {
                            maxNewX = newX
                            lastInterval = interval.first
                        }
                    }
                }
                // Если предыдущей нотой в партии была нота с точкой
                if(maxNewX < mainX)
                    maxNewX = mainX + lastInterval

                mainElapsedDuration += minDurationStep
                mainX = maxNewX
            }
        }
    }

    private fun addNotesDrawInfo(
        measureDrawInfo: MeasureDrawInfo,
        elements: List<MeasureElement>,
        chord: List<Int>,
        x: Float,
    ) {
        val y = measureDrawInfo.bottom - staffVerticalPaddings
        val firstNote = elements[chord[0]] as Note

        if (firstNote.rest) {
            measureDrawInfo.elements.add(
                ElementDrawInfo(
                    elementIndex = chord[0],
                    view = ScoreViewResources.restNotes[firstNote.type]!!,
                    x = x, y = y,
                )
            )
            return
        }

        val noStemNoteView =
            if (firstNote.type == NoteType.WHOLE || firstNote.type == NoteType.HALF)
                ScoreViewResources.noStemNotes[firstNote.type]!!
            else
                ScoreViewResources.noStemNotes[NoteType.QUARTER]!!


        val sortedChord = sortChordByLines(elements, chord)
        val isWideChord = isWideChord(sortedChord)

        val isStemUp = firstNote.stem == "up"
        val stemXOffset = if (isWideChord || isStemUp) noteWidth else 0f
        val stemYOffset = if (isStemUp) noteVerticalStep * 0.1f else noteVerticalStep * 7.25f

        val flag = if (isStemUp)
            ScoreViewResources.flagsUp[firstNote.type]
        else
            ScoreViewResources.flagsDown[firstNote.type]

        var lastChordLine = Int.MIN_VALUE
        var chordXOffset = 0f

        var accidentalOffset = 0f
        for (noteInfo in sortedChord) {
            val note = elements[noteInfo.first] as Note

            chordXOffset =
                if (note.line!! - lastChordLine == 1 && chordXOffset == 0f) noteWidth
                else 0f

            lastChordLine = note.line!!

            measureDrawInfo.elements.add(
                ElementDrawInfo(
                    elementIndex = noteInfo.first,
                    view = noStemNoteView,
                    x = x + chordXOffset,
                    y = y - (note.line!! - 2) * noteVerticalStep
                )
            )
            if (firstNote.stem != null) {
                measureDrawInfo.elements.add(
                    ElementDrawInfo(
                        elementIndex = -2,
                        view = ScoreViewResources.stem,
                        x = x + stemXOffset,
                        y = y - (note.line!! - 3) * noteVerticalStep + stemYOffset
                    )
                )
            }
            if (note.accidental != null){
                val accidentalView = when(note.accidental) {
                    Accidental.SHARP -> ScoreViewResources.sharp
                    Accidental.FLAT -> ScoreViewResources.flat
                    else -> ScoreViewResources.natural
                }
                measureDrawInfo.elements.add(
                    ElementDrawInfo(
                        elementIndex = -2,
                        view = accidentalView,
                        x = x - accidentalWidth + accidentalOffset,
                        y = y - (note.line!! - 2) * noteVerticalStep
                    )
                )
                accidentalOffset -= accidentalWidth
                if(accidentalOffset < accidentalWidth * -2)
                    accidentalOffset = 0f
            }
        }
        if (flag != null) {
            val flagLine: Int
            val yOffset: Float
            if (isStemUp) {
                flagLine = sortedChord.last().second
                yOffset = -(flagLine + 5) * noteVerticalStep + noteVerticalStep * 0.3f
            } else {
                flagLine = sortedChord[0].second
                yOffset = -(flagLine - 9) * noteVerticalStep - noteVerticalStep * 0.3f
            }

            measureDrawInfo.elements.add(
                ElementDrawInfo(
                    elementIndex = -1, view = flag, x = x + stemXOffset, y = y + yOffset
                )
            )
        }
    }
}




