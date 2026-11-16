package com.wallora.app.ui.screens

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wallora.app.ui.theme.Pink
import com.wallora.app.ui.theme.TealDark
import com.wallora.app.ui.theme.VioletDark
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** True when the user switched system animations off; the intro is skipped then. */
fun animationsDisabled(context: android.content.Context): Boolean =
    runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }.getOrDefault(false)

private class Half(private val left: Boolean) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val mid = size.width / 2f
        return Outline.Rectangle(if (left) Rect(0f, 0f, mid, size.height) else Rect(mid, 0f, size.width, size.height))
    }
}

private class IntroState(
    val seam: Float,      // the day/night seam draws down the middle
    val sunMoon: Float,   // sun and moon fade in
    val draw: Float,      // the W is drawn
    val shine: Float,     // a spark runs along the finished W
    val name: Float,      // letters of the name appear one by one
    val time: Float,      // 0..1 looping clock for twinkling and drifting
)

private val stars = Random(7).let { r -> List(34) { Triple(r.nextFloat(), r.nextFloat() * 0.62f, r.nextFloat()) } }

/**
 * Wallora's signature is a wallpaper that is both day and night, so the welcome is too: the screen starts split in
 * two, a bright morning on the left and a starry night on the right. A "W" draws itself across the seam (dark ink by
 * day, glowing gradient by night), the name appears, and the two halves slide apart like curtains to reveal the app.
 * About 2.4 seconds; a tap skips it.
 */
@Composable
fun IntroSplash(onFinished: () -> Unit) {
    val seam = remember { Animatable(0f) }
    val sunMoon = remember { Animatable(0f) }
    val draw = remember { Animatable(0f) }
    val shine = remember { Animatable(0f) }
    val name = remember { Animatable(0f) }
    val time = remember { Animatable(0f) }
    val split = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { time.animateTo(1f, tween(2600, easing = LinearEasing)) }
            launch { seam.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
            launch { sunMoon.animateTo(1f, tween(600, delayMillis = 150)) }
            launch { draw.animateTo(1f, tween(900, delayMillis = 330, easing = FastOutSlowInEasing)) }
            launch { shine.animateTo(1f, tween(700, delayMillis = 1230, easing = LinearEasing)) }
            launch { name.animateTo(1f, tween(700, delayMillis = 1000, easing = LinearEasing)) }
        }
        delay(150)
        split.animateTo(1f, tween(520, easing = CubicBezierEasing(0.7f, 0f, 0.84f, 0f)))
        onFinished()
    }

    val state = IntroState(seam.value, sunMoon.value, draw.value, shine.value, name.value, time.value)
    Box(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onFinished() },
    ) {
        for (left in listOf(true, false)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        shape = Half(left)
                        clip = true
                        translationX = (if (left) -1f else 1f) * (size.width / 2f + 4f) * split.value
                    },
            ) {
                IntroScene(left, state)
            }
        }
    }
}

@Composable
private fun IntroScene(left: Boolean, s: IntroState) {
    Canvas(Modifier.fillMaxSize()) {
        if (left) dayBackdrop(s) else nightBackdrop(s)
        drawLetter(left, s)
    }
    val letters = "Wallora"
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Row(Modifier.offset(y = 112.dp)) {
            letters.forEachIndexed { i, c ->
                val a = (s.name * (letters.length + 2) - i).coerceIn(0f, 1f)
                Text(
                    c.toString(),
                    modifier = Modifier
                        .alpha(a)
                        .graphicsLayer { translationY = (1f - a) * 16.dp.toPx() },
                    color = if (left) Color(0xFF2B1F73) else Color(0xFFF2F0FA),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                )
            }
        }
    }
}

private fun DrawScope.dayBackdrop(s: IntroState) {
    drawRect(
        Brush.verticalGradient(
            listOf(Color(0xFF7DB6FF), Color(0xFFBFDDFF), Color(0xFFFFE0C2), Color(0xFFFFB79D)),
        ),
    )
    // two soft clouds drifting right
    val drift = s.time * 60.dp.toPx()
    for ((x, y, r) in listOf(Triple(0.1f, 0.58f, 1f), Triple(0.34f, 0.7f, 0.7f))) {
        val cx = x * size.width + drift * r
        val cy = y * size.height
        for ((dx, dy, rr) in listOf(Triple(0f, 0f, 30f), Triple(30f, -9f, 24f), Triple(-28f, 4f, 20f), Triple(58f, 6f, 18f))) {
            drawCircle(Color.White.copy(alpha = 0.55f * s.sunMoon), rr.dp.toPx() * r, Offset(cx + dx.dp.toPx() * r, cy + dy.dp.toPx() * r))
        }
    }
    // the sun, with slowly turning rays
    val c = Offset(size.width * 0.24f, size.height * 0.18f)
    val a = s.sunMoon
    val spin = s.time * 0.5f * PI.toFloat()
    for (i in 0 until 12) {
        val ang = spin + i * (PI.toFloat() * 2 / 12)
        val r0 = 46.dp.toPx()
        val r1 = (62 + if (i % 2 == 0) 16 else 6).dp.toPx()
        drawLine(
            Color(0xFFFFC94D).copy(alpha = a), c + Offset(cos(ang) * r0, sin(ang) * r0), c + Offset(cos(ang) * r1, sin(ang) * r1),
            strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round,
        )
    }
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF3B0).copy(alpha = 0.7f * a), Color.Transparent), c, 120.dp.toPx()), 120.dp.toPx(), c)
    drawCircle(Brush.linearGradient(listOf(Color(0xFFFFE27A), Color(0xFFFF9F43)), c - Offset(40f, 40f), c + Offset(40f, 40f)), 38.dp.toPx() * (0.6f + 0.4f * a), c, alpha = a)
}

private fun DrawScope.nightBackdrop(s: IntroState) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF05050F), Color(0xFF1B1448), Color(0xFF45247A))))
    for ((i, st) in stars.withIndex()) {
        val (nx, ny, ph) = st
        val tw = 0.35f + 0.65f * abs(sin((s.time * 2f + ph) * PI.toFloat() * 2f))
        val r = (0.8f + 1.6f * ph).dp.toPx() * if (i % 7 == 0) 1.5f else 1f
        drawCircle(Color.White.copy(alpha = tw * s.sunMoon), r, Offset(nx * size.width, ny * size.height))
    }
    // the moon, with a halo
    val c = Offset(size.width * 0.76f, size.height * 0.18f)
    val a = s.sunMoon
    drawCircle(Brush.radialGradient(listOf(Color(0xFFBFD0FF).copy(alpha = 0.5f * a), Color.Transparent), c, 130.dp.toPx()), 130.dp.toPx(), c)
    drawCircle(Color(0xFFF4F1E4), 34.dp.toPx() * (0.6f + 0.4f * a), c, alpha = a)
    for ((dx, dy, r) in listOf(Triple(-10f, -8f, 8f), Triple(12f, 8f, 6f), Triple(-4f, 16f, 4f))) {
        drawCircle(Color(0xFFD9D3BE).copy(alpha = a), r.dp.toPx(), c + Offset(dx.dp.toPx(), dy.dp.toPx()))
    }
    // a shooting star crossing once while the name appears
    val t = ((s.time - 0.35f) / 0.3f).coerceIn(0f, 1f)
    if (t in 0.01f..0.99f) {
        val head = Offset(size.width * (1.05f - 0.7f * t), size.height * (0.06f + 0.28f * t))
        drawLine(
            Brush.linearGradient(listOf(Color.Transparent, Color.White), head + Offset(70.dp.toPx(), -34.dp.toPx()), head),
            head + Offset(70.dp.toPx(), -34.dp.toPx()), head, strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round,
        )
    }
}

/** The W and the seam, drawn for one half: ink-dark on the day side, glowing gradient on the night side. */
private fun DrawScope.drawLetter(left: Boolean, s: IntroState) {
    val mid = size.width / 2f
    // the seam grows from the middle outwards
    val seamHalf = size.height / 2f * s.seam
    drawLine(
        Color.White.copy(alpha = 0.55f), Offset(mid, size.height / 2f - seamHalf), Offset(mid, size.height / 2f + seamHalf),
        strokeWidth = 1.dp.toPx(),
    )

    val box = 170.dp.toPx()
    val cx = mid
    val cy = size.height / 2f - 36.dp.toPx()
    fun p(x: Float, y: Float) = Offset(cx + (x - 0.5f) * box, cy + (y - 0.5f) * box)
    val pts = listOf(p(0.04f, 0.14f), p(0.26f, 0.88f), p(0.5f, 0.34f), p(0.74f, 0.88f), p(0.96f, 0.14f))
    val full = Path().apply {
        moveTo(pts[0].x, pts[0].y)
        pts.drop(1).forEach { lineTo(it.x, it.y) }
    }
    val measure = PathMeasure().apply { setPath(full, false) }
    val part = Path()
    measure.getSegment(0f, measure.length * s.draw, part, true)
    val width = 18.dp.toPx()
    val round = { w: Float -> Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round) }
    if (left) {
        drawPath(part, Color(0xFF2B1F73), style = round(width))
    } else {
        val brush = Brush.linearGradient(listOf(VioletDark, Pink, TealDark), Offset(cx - box / 2, cy - box / 2), Offset(cx + box / 2, cy + box / 2))
        drawPath(part, brush, alpha = 0.28f, style = round(width * 2f))
        drawPath(part, brush, style = round(width))
    }
    // a spark runs along the finished letter
    if (s.draw >= 1f && s.shine in 0.01f..0.99f) {
        val d = measure.getPosition(measure.length * s.shine)
        val r = 26.dp.toPx()
        drawCircle(Brush.radialGradient(listOf(Color.White, Color.Transparent), d, r), r, d)
    }
}
