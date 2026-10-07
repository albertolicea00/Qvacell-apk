package com.qvacell.app.ui.screens

import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.model.CubanPhoneNumber
import com.qvacell.app.model.UssdCode
import com.qvacell.app.service.DialService
import com.qvacell.app.ui.components.BackNavigationIcon

/**
 * Ajustes › Cuenta › Gestionar Plan Amigo — activate/deactivate/status query up top, then two
 * independent add/remove forms below (each dials a different code, so kept as separate forms
 * rather than one control smart enough to handle both) — ported from iOS's FriendsPlanManageView.
 */
@Composable
fun FriendsPlanManageScreen(onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val repository = remember { CatalogRepository(context) }

    var addFriendNumber by remember { mutableStateOf("") }
    var removeFriendNumber by remember { mutableStateOf("") }

    val addContactLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        pickedNumber(context, result)?.let { addFriendNumber = it }
    }
    val removeContactLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        pickedNumber(context, result)?.let { removeFriendNumber = it }
    }

    fun dial(code: UssdCode?, input: String? = null) {
        if (code == null) return
        DialService.dial(context, code.resolvedCode(input))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 12.dp),
                title = { Text("Gestionar Plan Amigo") },
                navigationIcon = { if (onBack != null) BackNavigationIcon(onBack) }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(vertical = 8.dp)) {
            Card(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column {
                    ActionRow("Activar Plan Amigo") { dial(repository.findCodeById("friends-plan-activate")) }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ActionRow("Desactivar Plan Amigo") { dial(repository.findCodeById("friends-plan-deactivate")) }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ActionRow("Consultar Plan Amigo") { dial(repository.findCodeById("friends-plan-status-settings")) }
                }
            }
            Text(
                "Activar el Plan Amigos tiene un costo de $25.00.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp)
            )

            SectionHeader("Agregar Amigo")
            NumberFormCard(
                number = addFriendNumber,
                onNumberChange = { addFriendNumber = it },
                onPickContact = { addContactLauncher.launch(pickContactIntent()) },
                actionLabel = "Agregar",
                onSubmit = {
                    dial(repository.findCodeById("friends-plan-add-member"), addFriendNumber)
                    addFriendNumber = ""
                }
            )

            SectionHeader("Eliminar Amigo")
            NumberFormCard(
                number = removeFriendNumber,
                onNumberChange = { removeFriendNumber = it },
                onPickContact = { removeContactLauncher.launch(pickContactIntent()) },
                actionLabel = "Eliminar",
                onSubmit = {
                    dial(repository.findCodeById("friends-plan-remove-member"), removeFriendNumber)
                    removeFriendNumber = ""
                }
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.primary)
        Icon(Icons.Filled.ArrowOutward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun NumberFormCard(
    number: String,
    onNumberChange: (String) -> Unit,
    onPickContact: () -> Unit,
    actionLabel: String,
    onSubmit: () -> Unit
) {
    val active = number.isNotBlank()
    val accentColor = MaterialTheme.colorScheme.primary
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)

    Card(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp).clickable(onClick = onPickContact)
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = "Elegir de contactos",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                BasicTextField(
                    value = number,
                    onValueChange = onNumberChange,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(accentColor),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    decorationBox = { inner ->
                        if (number.isEmpty()) {
                            Text(
                                "Número (+53 ...)",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        inner()
                    }
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = active, onClick = onSubmit)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    actionLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (active) accentColor else mutedColor
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Filled.ArrowOutward,
                    contentDescription = null,
                    tint = if (active) accentColor else mutedColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun pickContactIntent(): Intent =
    Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)

private fun pickedNumber(context: android.content.Context, result: androidx.activity.result.ActivityResult): String? {
    val uri = result.data?.data ?: return null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            if (numberIndex >= 0) {
                return CubanPhoneNumber.normalize(cursor.getString(numberIndex))
            }
        }
    }
    return null
}
