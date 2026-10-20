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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import com.qvacell.app.R
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.model.CubanPhoneNumber
import com.qvacell.app.model.UssdCode
import com.qvacell.app.service.DashboardCapture
import com.qvacell.app.service.DashboardDataRepository
import com.qvacell.app.service.TransferPinStore
import com.qvacell.app.ui.components.CodeRow
import com.qvacell.app.ui.components.rememberDirectDialAction
import com.qvacell.app.ui.resolveAndroidIcon

private data class QuickTile(val codeId: String, @StringRes val labelRes: Int)

private val QUICK_TILES = listOf(
    QuickTile("main-balance",            R.string.dashboard_tile_balance),
    QuickTile("national-recharge-limit", R.string.dashboard_tile_limit),
    QuickTile("corp-balance",            R.string.dashboard_tile_corp),
    QuickTile("data-plan",               R.string.dashboard_tile_data),
    QuickTile("voice-balance",           R.string.dashboard_tile_voice),
    QuickTile("sms-balance",             R.string.dashboard_tile_sms),
    QuickTile("bonus-usd-plans",         R.string.dashboard_tile_bonus),
    QuickTile("friends-plan",            R.string.dashboard_tile_friends),
    QuickTile("tfa",                     R.string.dashboard_tile_tfa),
)

private const val TILE_COLUMNS = 3
private val TILE_GAP = 16.dp
private val TILE_MIN = 44.dp

@Composable
fun ManualDashboardContent(
    repository: CatalogRepository,
    dashboardRepository: DashboardDataRepository,
    ussdCaptureEnabled: Boolean,
    tileStyle: String = "filled",
    hiddenSections: Set<String> = emptySet()
) {
    val showAsList = tileStyle == "list"
    val context = LocalContext.current
    val dialDirect = rememberDirectDialAction()
    val pinStore = remember { TransferPinStore(context) }
    val resolvedTiles = remember(repository) {
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
    var showNonCubanError by remember { mutableStateOf(false) }

    val pickContactLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (idx >= 0) {
                    val raw = cursor.getString(idx)
                    val normalized = CubanPhoneNumber.normalize(raw)
                    if (normalized != null) {
                        transferNumber = normalized
                    } else {
                        showNonCubanError = true
                    }
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
        if (resolvedTiles.isNotEmpty() && "consultas" !in hiddenSections) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.dashboard_consultas),
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
                                            label = stringResource(tile.labelRes),
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
        if ("transferir" !in hiddenSections) {
        val transferEnabled = transferNumber.isNotBlank() && transferPin.isNotBlank() && transferAmount.isNotBlank()
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.transfer_title).uppercase(),
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
                                if (transferNumber.isEmpty()) Text(stringResource(R.string.transfer_number_hint), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
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
                                contentDescription = stringResource(R.string.transfer_choose_contact),
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
                                    if (transferPin.isEmpty()) Text(stringResource(R.string.contacts_pin_hint), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
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
                                if (transferAmount.isEmpty()) Text(stringResource(R.string.contacts_amount_hint), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
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
                                dialDirect("*234*1*$transferNumber*$transferPin*$transferAmount#")
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.contacts_transfer_action), style = MaterialTheme.typography.bodyMedium, color = transferTint)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Filled.ArrowOutward, contentDescription = null, tint = transferTint, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        }

        // Recargar por Llamada — standalone card (above tarjeta)
        if ("recargar" !in hiddenSections) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.recharge_title).uppercase(),
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
                        .clickable { dialDirect("*88#") }
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
                            stringResource(R.string.recharge_by_call),
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
                            if (cardNumber.isEmpty()) Text(stringResource(R.string.recharge_card_hint), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            inner()
                        }
                    )
                    IconButton(onClick = { /* TODO: camera scan */ }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = stringResource(R.string.recharge_scan_card),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                val rechargeTint = if (rechargeEnabled) MaterialTheme.colorScheme.primary
                                  else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = rechargeEnabled) {
                            dialDirect("*662*$cardNumber#")
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.recharge_with_card), style = MaterialTheme.typography.bodyMedium, color = rechargeTint)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Filled.ArrowOutward, contentDescription = null, tint = rechargeTint, modifier = Modifier.size(16.dp))
                }
            }
        }
        }

        // Advance balance section
        if (advanceBalanceCodes.isNotEmpty() && "adelanta" !in hiddenSections) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.recharge_advance_section),
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
                            onClick = { dialDirect(code.resolvedCode()) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(code.price ?: code.title.value)
                        }
                    }
                }
            }
        }
    }

    if (showNonCubanError) {
        AlertDialog(
            onDismissRequest = { showNonCubanError = false },
            title = { Text(stringResource(R.string.transfer_invalid_number_title)) },
            text = { Text(stringResource(R.string.transfer_invalid_number_desc)) },
            confirmButton = {
                TextButton(onClick = { showNonCubanError = false }) { Text(stringResource(R.string.action_ok)) }
            }
        )
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

