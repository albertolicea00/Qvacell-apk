package com.qvacell.app.ui.screens

import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowOutward
import com.qvacell.app.ui.components.ScanCardButton
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.model.CubanPhoneNumber
import com.qvacell.app.model.UssdCode
import com.qvacell.app.service.DashboardCapture
import com.qvacell.app.service.DashboardDataRepository
import com.qvacell.app.service.DialService
import com.qvacell.app.service.TransferPinStore
import com.qvacell.app.ui.components.CodeRow
import com.qvacell.app.ui.resolveAndroidIcon

private data class QuickTile(val codeId: String, val label: String)

private val QUICK_TILES = listOf(
    QuickTile("main-balance",            "Saldo"),
    QuickTile("national-recharge-limit", "Límite"),
    QuickTile("bonus-usd-plans",         "Bono"),
    QuickTile("data-plan",               "Datos"),
    QuickTile("voice-balance",           "Voz"),
    QuickTile("sms-balance",             "SMS"),
    QuickTile("friends-plan",            "Amigo"),
    QuickTile("tfa",                     "TFA"),
    QuickTile("postpaid-balance",        "Pospago"),
)

private const val TILE_COLUMNS = 3
private val TILE_GAP = 16.dp
private val TILE_MIN = 44.dp

@Composable
fun ManualDashboardContent(
    repository: CatalogRepository,
    dashboardRepository: DashboardDataRepository,
    ussdCaptureEnabled: Boolean,
    tileStyle: String = "filled"
) {
    val showAsList = tileStyle == "list"
    val context = LocalContext.current
    val pinStore = remember { TransferPinStore(context) }

    val resolvedTiles = remember {
        QUICK_TILES.mapNotNull { tile ->
            repository.findCodeById(tile.codeId)?.let { tile to it }
        }
    }
    val advanceBalanceCodes = remember {
        listOf("advance-balance-25", "advance-balance-50").mapNotNull { repository.findCodeById(it) }
    }

    var transferNumber by remember { mutableStateOf("") }
    var transferPin by remember { mutableStateOf(pinStore.load() ?: "") }
    var transferAmount by remember { mutableStateOf("") }
    var transferPinVisible by remember { mutableStateOf(false) }
    var cardNumber by remember { mutableStateOf("") }

    val pickContactLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (idx >= 0) {
                    CubanPhoneNumber.normalize(cursor.getString(idx))?.let { transferNumber = it }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick-action queries — tile grid or contact list
        if (resolvedTiles.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "CONSULTAS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
                if (showAsList) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        resolvedTiles.forEachIndexed { index, (_, code) ->
                            CodeRow(
                                code = code,
                                onClick = {
                                    DashboardCapture.captureOrDial(context, code, dashboardRepository, ussdCaptureEnabled)
                                },
                                contactStyle = true,
                                showDescription = true,
                                plainPrice = true
                            )
                            if (index != resolvedTiles.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                } else {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val rawSize = (maxWidth - TILE_GAP * (TILE_COLUMNS - 1)) / TILE_COLUMNS
                        val tileSize = maxOf(TILE_MIN, rawSize)

                        Column(verticalArrangement = Arrangement.spacedBy(TILE_GAP)) {
                            resolvedTiles.chunked(TILE_COLUMNS).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(TILE_GAP)
                                ) {
                                    row.forEach { (tile, code) ->
                                        QuickActionTile(
                                            code = code,
                                            label = tile.label,
                                            size = tileSize,
                                            outline = tileStyle == "outline",
                                            onClick = {
                                                DashboardCapture.captureOrDial(
                                                    context, code, dashboardRepository, ussdCaptureEnabled
                                                )
                                            }
                                        )
                                    }
                                    repeat(TILE_COLUMNS - row.size) {
                                        Spacer(Modifier.size(tileSize))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Transfer section
        val transferEnabled = transferNumber.isNotBlank() && transferPin.isNotBlank() && transferAmount.isNotBlank()
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "TRANSFERIR SALDO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column {
                    // Number row
                    Row(
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = transferNumber,
                            onValueChange = { transferNumber = it },
                            modifier = Modifier.weight(1f).padding(start = 16.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            decorationBox = { inner ->
                                if (transferNumber.isEmpty()) Text("Número (+53 ...)", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                inner()
                            }
                        )
                        IconButton(
                            onClick = {
                                pickContactLauncher.launch(
                                    Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                                )
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = "Elegir de contactos",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    // PIN + Amount row
                    Row(
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).padding(start = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = transferPin,
                                onValueChange = { transferPin = it },
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                visualTransformation = if (transferPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                decorationBox = { inner ->
                                    if (transferPin.isEmpty()) Text("Clave", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                    inner()
                                }
                            )
                            IconButton(onClick = { transferPinVisible = !transferPinVisible }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    if (transferPinVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Box(Modifier.width(1.dp).height(28.dp).background(MaterialTheme.colorScheme.outlineVariant))
                        BasicTextField(
                            value = transferAmount,
                            onValueChange = { transferAmount = it },
                            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            decorationBox = { inner ->
                                if (transferAmount.isEmpty()) Text("Monto", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                inner()
                            }
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    val transferTint = if (transferEnabled) MaterialTheme.colorScheme.primary
                                      else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = transferEnabled) {
                                pinStore.save(transferPin)
                                DialService.dialDirect(context, "*234*1*$transferNumber*$transferPin*$transferAmount#")
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Transferir", style = MaterialTheme.typography.bodyMedium, color = transferTint)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Filled.ArrowOutward, contentDescription = null, tint = transferTint, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Recargar por Llamada — standalone card (above tarjeta)
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "RECARGAR SALDO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { DialService.dialDirect(context, "*88#") }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Recargar por Llamada",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Recharge with card section
        val rechargeEnabled = cardNumber.isNotBlank()
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = cardNumber,
                        onValueChange = { cardNumber = it },
                        modifier = Modifier.weight(1f).padding(start = 16.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        decorationBox = { inner ->
                            if (cardNumber.isEmpty()) Text("Número de tarjeta", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            inner()
                        }
                    )
                    ScanCardButton(onCodeScanned = { cardNumber = it })
                    Spacer(Modifier.width(4.dp))
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                val rechargeTint = if (rechargeEnabled) MaterialTheme.colorScheme.primary
                                  else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = rechargeEnabled) {
                            DialService.dialDirect(context, "*662*$cardNumber#")
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recargar con Tarjeta", style = MaterialTheme.typography.bodyMedium, color = rechargeTint)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Filled.ArrowOutward, contentDescription = null, tint = rechargeTint, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Advance balance section
        if (advanceBalanceCodes.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "ADELANTA SALDO",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    advanceBalanceCodes.forEach { code ->
                        androidx.compose.material3.OutlinedButton(
                            onClick = { DialService.dialDirect(context, code.resolvedCode()) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(code.price ?: code.title.value)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    code: UssdCode,
    label: String,
    size: Dp,
    outline: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(percent = 20)
    val contentColor = if (outline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
    Surface(
        modifier = Modifier
            .width(size)
            .height(size * 0.72f)
            .then(if (outline) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .clickable(onClick = onClick),
        shape = shape,
        color = if (outline) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = resolveAndroidIcon(code.icon),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size((size.value * 0.28f).dp)
            )
            Spacer(Modifier.height(5.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

