package com.example.sailfreight.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sailfreight.data.engine.ProcurementEngine
import com.example.sailfreight.data.model.PortCoordinates
import java.util.Locale
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPoint(val lat: Double, val lon: Double)

data class ProjectedPoint(
    val offset: Offset,
    val isVisible: Boolean,
    val depth: Float
)

@Composable
fun InteractiveGlobe(
    origin: String,
    destination: String,
    modifier: Modifier = Modifier
) {
    val origCoord = ProcurementEngine.PORT_COORDINATES[origin]
    val destCoord = ProcurementEngine.PORT_COORDINATES[destination]

    // Calculate midpoint for auto-focusing on the active route
    val defaultTargetLat = if (origCoord != null && destCoord != null) {
        (origCoord.lat + destCoord.lat) / 2.0
    } else 10.0

    val defaultTargetLon = if (origCoord != null && destCoord != null) {
        val oRad = origCoord.lon * PI / 180.0
        val dRad = destCoord.lon * PI / 180.0
        atan2(sin(oRad) + sin(dRad), cos(oRad) + cos(dRad)) * 180.0 / PI
    } else 80.0

    // Manual offset state for interactive dragging
    var manualLatOffset by remember { mutableFloatStateOf(0f) }
    var manualLonOffset by remember { mutableFloatStateOf(0f) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }

    // When origin or destination changes, animate back to center on the new route
    LaunchedEffect(origin, destination) {
        manualLatOffset = 0f
        manualLonOffset = 0f
    }

    val animatedTargetLat by animateFloatAsState(
        targetValue = (defaultTargetLat.toFloat() + manualLatOffset).coerceIn(-75f, 75f),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "globe_lat"
    )

    val animatedTargetLon by animateFloatAsState(
        targetValue = defaultTargetLon.toFloat() + manualLonOffset,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "globe_lon"
    )

    // Animated phase for dash route effect and ship movement
    val infiniteTransition = rememberInfiniteTransition(label = "route_anim")
    val dashPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dash_phase"
    )

    val shipProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ship_progress"
    )

    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(290.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF070B14))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        manualLonOffset -= dragAmount.x * 0.45f
                        manualLatOffset = (manualLatOffset + dragAmount.y * 0.35f).coerceIn(-65f, 65f)
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val baseRadius = min(w, h) * 0.44f
            val globeRadius = baseRadius * zoomScale

            val centerLat = animatedTargetLat.toDouble()
            val centerLon = animatedTargetLon.toDouble()

            // 1. Starry cosmic background grid dots
            drawStarsBackground(w, h)

            // 2. Outer atmospheric halo glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0284C7).copy(alpha = 0.25f),
                        Color(0xFF0284C7).copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = globeRadius * 1.22f
                ),
                radius = globeRadius * 1.22f,
                center = Offset(cx, cy)
            )

            // 3. Deep Ocean globe sphere base with radial depth gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E3A8A), // Light ocean blue core
                        Color(0xFF0F2454), // Deep indigo ocean
                        Color(0xFF08122A), // Dark marine rim
                        Color(0xFF040814)  // Outer shadow
                    ),
                    center = Offset(cx - globeRadius * 0.25f, cy - globeRadius * 0.25f),
                    radius = globeRadius * 1.05f
                ),
                radius = globeRadius,
                center = Offset(cx, cy)
            )

            // 4. Latitude and Longitude Graticule lines
            drawGraticules(cx, cy, globeRadius, centerLat, centerLon)

            // 5. Landmass continents
            drawContinents(cx, cy, globeRadius, centerLat, centerLon)

            // 6. Global SAIL port network beacons
            drawWorldPorts(cx, cy, globeRadius, centerLat, centerLon, origin, destination)

            // 7. Active Maritime Route Great-Circle Arc
            if (origCoord != null && destCoord != null) {
                drawGreatCircleRoute(
                    cx = cx,
                    cy = cy,
                    radius = globeRadius,
                    centerLat = centerLat,
                    centerLon = centerLon,
                    orig = origCoord,
                    dest = destCoord,
                    dashPhase = dashPhase,
                    shipProgress = shipProgress,
                    textMeasurer = textMeasurer
                )
            }

            // 8. Atmospheric glass edge rim and specular highlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Transparent,
                        Color(0xFF38BDF8).copy(alpha = 0.25f),
                        Color(0xFF60A5FA).copy(alpha = 0.60f)
                    ),
                    center = Offset(cx, cy),
                    radius = globeRadius
                ),
                radius = globeRadius,
                center = Offset(cx, cy),
                style = Stroke(width = 2.5f)
            )
        }

        // Top-left HUD status
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        ) {
            Surface(
                color = Color(0xFF0F172A).copy(alpha = 0.85f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "3D Orthographic Globe • Drag to Spin",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE2E8F0),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Top-right control buttons
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                color = Color(0xFF0F172A).copy(alpha = 0.85f),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                IconButton(
                    onClick = {
                        manualLatOffset = 0f
                        manualLonOffset = 0f
                        zoomScale = 1.0f
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = "Center active route",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Surface(
                color = Color(0xFF0F172A).copy(alpha = 0.85f),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                IconButton(
                    onClick = { zoomScale = (zoomScale + 0.15f).coerceAtMost(1.45f) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoom in",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Surface(
                color = Color(0xFF0F172A).copy(alpha = 0.85f),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                IconButton(
                    onClick = { zoomScale = (zoomScale - 0.15f).coerceAtLeast(0.75f) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "Zoom out",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Bottom interactive route legend
        Surface(
            color = Color(0xFF0F172A).copy(alpha = 0.90f),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF38BDF8), CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Origin Port", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE2E8F0), fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF10B981), CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Discharge Port", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE2E8F0), fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp, 3.dp).background(Color(0xFFF59E0B), RoundedCornerShape(1.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Great-Circle Corridor", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF59E0B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SPHERICAL 3D ORTHOGRAPHIC PROJECTION & DRAWING HELPERS
// -------------------------------------------------------------

fun projectOrthographic(
    lat: Double,
    lon: Double,
    centerLat: Double,
    centerLon: Double,
    radius: Float,
    cx: Float,
    cy: Float
): ProjectedPoint {
    val phi = lat * PI / 180.0
    val lambda = lon * PI / 180.0
    val phi0 = centerLat * PI / 180.0
    val lambda0 = centerLon * PI / 180.0

    val dLambda = lambda - lambda0
    val cosC = sin(phi0) * sin(phi) + cos(phi0) * cos(phi) * cos(dLambda)
    val isVisible = cosC >= -0.05 // Slight threshold for smooth horizon edge

    val x = cx + radius * (cos(phi) * sin(dLambda)).toFloat()
    val y = cy - radius * (cos(phi0) * sin(phi) - sin(phi0) * cos(phi) * cos(dLambda)).toFloat()

    return ProjectedPoint(Offset(x, y), isVisible, cosC.toFloat())
}

private fun DrawScope.drawStarsBackground(w: Float, h: Float) {
    val starCoords = arrayOf(
        0.08f to 0.15f, 0.14f to 0.82f, 0.22f to 0.28f, 0.88f to 0.12f,
        0.84f to 0.78f, 0.92f to 0.45f, 0.05f to 0.65f, 0.72f to 0.88f,
        0.35f to 0.08f, 0.62f to 0.06f, 0.45f to 0.94f, 0.18f to 0.48f
    )
    starCoords.forEach { (sx, sy) ->
        drawCircle(
            color = Color(0xFF334155).copy(alpha = 0.5f),
            radius = 1.2f,
            center = Offset(w * sx, h * sy)
        )
    }
}

private fun DrawScope.drawGraticules(
    cx: Float,
    cy: Float,
    radius: Float,
    centerLat: Double,
    centerLon: Double
) {
    val graticuleColor = Color(0xFF38BDF8).copy(alpha = 0.14f)

    // Parallels (Latitudes)
    val latitudes = doubleArrayOf(-60.0, -30.0, 0.0, 30.0, 60.0)
    for (lat in latitudes) {
        val path = Path()
        var started = false
        var prevVisible = false

        for (step in -180..180 step 6) {
            val proj = projectOrthographic(lat, step.toDouble(), centerLat, centerLon, radius, cx, cy)
            if (proj.isVisible) {
                if (!started || !prevVisible) {
                    path.moveTo(proj.offset.x, proj.offset.y)
                    started = true
                } else {
                    path.lineTo(proj.offset.x, proj.offset.y)
                }
                prevVisible = true
            } else {
                prevVisible = false
            }
        }
        drawPath(path, graticuleColor, style = Stroke(width = if (lat == 0.0) 1.2f else 0.8f))
    }

    // Meridians (Longitudes)
    for (lon in -180..150 step 30) {
        val path = Path()
        var started = false
        var prevVisible = false

        for (step in -85..85 step 5) {
            val proj = projectOrthographic(step.toDouble(), lon.toDouble(), centerLat, centerLon, radius, cx, cy)
            if (proj.isVisible) {
                if (!started || !prevVisible) {
                    path.moveTo(proj.offset.x, proj.offset.y)
                    started = true
                } else {
                    path.lineTo(proj.offset.x, proj.offset.y)
                }
                prevVisible = true
            } else {
                prevVisible = false
            }
        }
        drawPath(path, graticuleColor, style = Stroke(width = 0.8f))
    }
}

private fun DrawScope.drawContinents(
    cx: Float,
    cy: Float,
    radius: Float,
    centerLat: Double,
    centerLon: Double
) {
    val landFill = Color(0xFF1E293B)
    val landStroke = Color(0xFF475569)

    for (polygon in CONTINENT_POLYGONS) {
        val path = Path()
        var visibleCount = 0

        val projectedPoints = polygon.map { pt ->
            projectOrthographic(pt.lat, pt.lon, centerLat, centerLon, radius, cx, cy)
        }

        projectedPoints.forEachIndexed { index, proj ->
            if (proj.isVisible) {
                visibleCount++
                if (index == 0 || !projectedPoints[index - 1].isVisible) {
                    path.moveTo(proj.offset.x, proj.offset.y)
                } else {
                    path.lineTo(proj.offset.x, proj.offset.y)
                }
            }
        }

        if (visibleCount >= 3) {
            drawPath(path, landFill, style = Fill)
            drawPath(path, landStroke, style = Stroke(width = 1f))
        }
    }
}

private fun DrawScope.drawWorldPorts(
    cx: Float,
    cy: Float,
    radius: Float,
    centerLat: Double,
    centerLon: Double,
    selectedOrig: String,
    selectedDest: String
) {
    for ((pName, pCoord) in ProcurementEngine.PORT_COORDINATES) {
        if (pName == selectedOrig || pName == selectedDest) continue

        val proj = projectOrthographic(pCoord.lat, pCoord.lon, centerLat, centerLon, radius, cx, cy)
        if (proj.isVisible) {
            drawCircle(
                color = Color(0xFF64748B).copy(alpha = 0.7f),
                radius = 2.8f,
                center = proj.offset
            )
        }
    }
}

private fun DrawScope.drawGreatCircleRoute(
    cx: Float,
    cy: Float,
    radius: Float,
    centerLat: Double,
    centerLon: Double,
    orig: PortCoordinates,
    dest: PortCoordinates,
    dashPhase: Float,
    shipProgress: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    // 3D Cartesian vectors
    val phi1 = orig.lat * PI / 180.0
    val lam1 = orig.lon * PI / 180.0
    val phi2 = dest.lat * PI / 180.0
    val lam2 = dest.lon * PI / 180.0

    val v1x = cos(phi1) * cos(lam1)
    val v1y = cos(phi1) * sin(lam1)
    val v1z = sin(phi1)

    val v2x = cos(phi2) * cos(lam2)
    val v2y = cos(phi2) * sin(lam2)
    val v2z = sin(phi2)

    val dot = (v1x * v2x + v1y * v2y + v1z * v2z).coerceIn(-1.0, 1.0)
    val omega = acos(dot)

    val numSegments = 60
    val routePoints = mutableListOf<ProjectedPoint>()

    for (i in 0..numSegments) {
        val t = i.toDouble() / numSegments
        val sOmega = sin(omega)

        val vx: Double
        val vy: Double
        val vz: Double

        if (sOmega < 1e-4) {
            vx = v1x
            vy = v1y
            vz = v1z
        } else {
            val a = sin((1.0 - t) * omega) / sOmega
            val b = sin(t * omega) / sOmega
            vx = a * v1x + b * v2x
            vy = a * v1y + b * v2y
            vz = a * v1z + b * v2z
        }

        val norm = sqrt(vx * vx + vy * vy + vz * vz)
        val latT = asin((vz / norm).coerceIn(-1.0, 1.0)) * 180.0 / PI
        val lonT = atan2(vy, vx) * 180.0 / PI

        routePoints.add(projectOrthographic(latT, lonT, centerLat, centerLon, radius, cx, cy))
    }

    // Draw visible path segments
    val corridorPath = Path()
    val dashedPath = Path()
    var started = false
    var prevVisible = false

    for (pt in routePoints) {
        if (pt.isVisible) {
            if (!started || !prevVisible) {
                corridorPath.moveTo(pt.offset.x, pt.offset.y)
                dashedPath.moveTo(pt.offset.x, pt.offset.y)
                started = true
            } else {
                corridorPath.lineTo(pt.offset.x, pt.offset.y)
                dashedPath.lineTo(pt.offset.x, pt.offset.y)
            }
            prevVisible = true
        } else {
            prevVisible = false
        }
    }

    // Glowing corridor
    drawPath(
        path = corridorPath,
        color = Color(0xFFF59E0B).copy(alpha = 0.35f),
        style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
    )

    // Animated dashed maritime line
    drawPath(
        path = dashedPath,
        color = Color(0xFFF59E0B),
        style = Stroke(
            width = 2.8.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), dashPhase),
            cap = StrokeCap.Round
        )
    )

    // Moving vessel ship icon on the route
    val shipIdx = (shipProgress * numSegments).toInt().coerceIn(0, numSegments)
    val shipPt = routePoints[shipIdx]
    if (shipPt.isVisible) {
        drawCircle(
            color = Color(0xFFF59E0B).copy(alpha = 0.35f),
            radius = 10.dp.toPx(),
            center = shipPt.offset
        )
        drawCircle(
            color = Color.White,
            radius = 3.8.dp.toPx(),
            center = shipPt.offset
        )
    }

    // Origin Port (Sky Blue beacon)
    val origProj = projectOrthographic(orig.lat, orig.lon, centerLat, centerLon, radius, cx, cy)
    if (origProj.isVisible) {
        drawCircle(color = Color(0xFF0284C7).copy(alpha = 0.35f), radius = 12.dp.toPx(), center = origProj.offset)
        drawCircle(color = Color(0xFF38BDF8), radius = 5.dp.toPx(), center = origProj.offset)
        drawCircle(color = Color.White, radius = 2.dp.toPx(), center = origProj.offset)

        drawText(
            textMeasurer = textMeasurer,
            text = orig.name,
            topLeft = Offset(origProj.offset.x + 8.dp.toPx(), origProj.offset.y - 14.dp.toPx()),
            style = TextStyle(
                color = Color(0xFFBAE6FD),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                background = Color(0xCC0F172A)
            )
        )
    }

    // Destination Port (Emerald beacon)
    val destProj = projectOrthographic(dest.lat, dest.lon, centerLat, centerLon, radius, cx, cy)
    if (destProj.isVisible) {
        drawCircle(color = Color(0xFF10B981).copy(alpha = 0.35f), radius = 12.dp.toPx(), center = destProj.offset)
        drawCircle(color = Color(0xFF34D399), radius = 5.dp.toPx(), center = destProj.offset)
        drawCircle(color = Color.White, radius = 2.dp.toPx(), center = destProj.offset)

        drawText(
            textMeasurer = textMeasurer,
            text = dest.name,
            topLeft = Offset(destProj.offset.x + 8.dp.toPx(), destProj.offset.y - 14.dp.toPx()),
            style = TextStyle(
                color = Color(0xFFA7F3D0),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                background = Color(0xCC0F172A)
            )
        )
    }
}

// -------------------------------------------------------------
// CONTINENT POLYGON DATASETS (Key world landmasses for globe)
// -------------------------------------------------------------

private val CONTINENT_POLYGONS: List<List<GeoPoint>> = listOf(
    // India & South Asia (Key focal region)
    listOf(
        GeoPoint(24.0, 68.5), GeoPoint(20.5, 72.8), GeoPoint(15.0, 73.8), GeoPoint(8.1, 77.5),
        GeoPoint(10.5, 79.8), GeoPoint(13.0, 80.3), GeoPoint(16.0, 82.0), GeoPoint(17.7, 83.3),
        GeoPoint(20.3, 86.6), GeoPoint(22.0, 88.1), GeoPoint(22.5, 89.5), GeoPoint(26.0, 89.0),
        GeoPoint(28.0, 80.0), GeoPoint(32.0, 76.0), GeoPoint(28.0, 70.0), GeoPoint(24.0, 68.5)
    ),

    // Australia (Key origin region)
    listOf(
        GeoPoint(-12.0, 131.0), GeoPoint(-12.0, 136.0), GeoPoint(-16.0, 139.0), GeoPoint(-11.0, 142.0),
        GeoPoint(-20.0, 149.0), GeoPoint(-28.0, 153.0), GeoPoint(-34.0, 151.0), GeoPoint(-38.0, 147.0),
        GeoPoint(-38.0, 141.0), GeoPoint(-32.0, 132.0), GeoPoint(-35.0, 118.0), GeoPoint(-32.0, 115.0),
        GeoPoint(-22.0, 114.0), GeoPoint(-15.0, 124.0), GeoPoint(-12.0, 131.0)
    ),

    // Southeast Asia & Indonesia
    listOf(
        GeoPoint(6.0, 95.0), GeoPoint(-6.0, 106.0), GeoPoint(-8.0, 115.0), GeoPoint(-8.0, 126.0),
        GeoPoint(-4.0, 135.0), GeoPoint(0.0, 131.0), GeoPoint(1.0, 125.0), GeoPoint(4.0, 118.0),
        GeoPoint(6.0, 100.0), GeoPoint(13.0, 100.0), GeoPoint(20.0, 106.0), GeoPoint(6.0, 95.0)
    ),

    // East Asia (China, Korea, Far East Russia)
    listOf(
        GeoPoint(22.0, 108.0), GeoPoint(30.0, 122.0), GeoPoint(37.0, 122.0), GeoPoint(39.0, 128.0),
        GeoPoint(43.0, 132.0), GeoPoint(55.0, 136.0), GeoPoint(60.0, 163.0), GeoPoint(66.0, 170.0),
        GeoPoint(70.0, 178.0), GeoPoint(72.0, 130.0), GeoPoint(77.0, 105.0), GeoPoint(70.0, 80.0),
        GeoPoint(50.0, 85.0), GeoPoint(40.0, 90.0), GeoPoint(30.0, 100.0), GeoPoint(22.0, 108.0)
    ),

    // Japan
    listOf(
        GeoPoint(31.0, 130.5), GeoPoint(35.0, 136.0), GeoPoint(40.0, 140.0), GeoPoint(45.0, 142.0),
        GeoPoint(43.0, 145.0), GeoPoint(36.0, 140.0), GeoPoint(33.0, 132.0), GeoPoint(31.0, 130.5)
    ),

    // Africa & Middle East
    listOf(
        GeoPoint(37.0, 10.0), GeoPoint(32.0, 32.0), GeoPoint(28.0, 34.0), GeoPoint(12.0, 44.0),
        GeoPoint(12.0, 51.0), GeoPoint(2.0, 46.0), GeoPoint(-12.0, 40.0), GeoPoint(-26.0, 33.0),
        GeoPoint(-34.0, 18.0), GeoPoint(-16.0, 12.0), GeoPoint(5.0, 9.0), GeoPoint(5.0, -1.0),
        GeoPoint(15.0, -17.0), GeoPoint(28.0, -13.0), GeoPoint(36.0, -6.0), GeoPoint(37.0, 10.0)
    ),

    // Europe
    listOf(
        GeoPoint(36.0, -9.0), GeoPoint(43.0, -9.0), GeoPoint(48.0, -5.0), GeoPoint(54.0, 8.0),
        GeoPoint(58.0, 6.0), GeoPoint(71.0, 28.0), GeoPoint(68.0, 44.0), GeoPoint(60.0, 30.0),
        GeoPoint(45.0, 29.0), GeoPoint(40.0, 26.0), GeoPoint(36.0, 15.0), GeoPoint(36.0, -9.0)
    ),

    // North America
    listOf(
        GeoPoint(70.0, -160.0), GeoPoint(72.0, -130.0), GeoPoint(60.0, -85.0), GeoPoint(50.0, -55.0),
        GeoPoint(44.0, -65.0), GeoPoint(30.0, -81.0), GeoPoint(25.0, -80.0), GeoPoint(28.0, -96.0),
        GeoPoint(22.0, -97.0), GeoPoint(16.0, -93.0), GeoPoint(8.0, -77.0), GeoPoint(18.0, -105.0),
        GeoPoint(32.0, -117.0), GeoPoint(48.0, -125.0), GeoPoint(60.0, -145.0), GeoPoint(70.0, -160.0)
    ),

    // South America
    listOf(
        GeoPoint(12.0, -72.0), GeoPoint(10.0, -62.0), GeoPoint(-5.0, -35.0), GeoPoint(-23.0, -43.0),
        GeoPoint(-35.0, -57.0), GeoPoint(-54.0, -68.0), GeoPoint(-42.0, -74.0), GeoPoint(-18.0, -71.0),
        GeoPoint(-5.0, -81.0), GeoPoint(8.0, -77.0), GeoPoint(12.0, -72.0)
    )
)
