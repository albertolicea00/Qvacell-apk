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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.qvacell.app.R
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.model.CubanPhoneNumber
import com.qvacell.app.model.UssdCode
import com.qvacell.app.ui.components.SimSelectorIcon
import com.qvacell.app.ui.components.BackNavigationIcon
import com.qvacell.app.ui.components.rememberDirectDialAction

/**
 * Options › Cuenta › Gestionar Plan Amigo — activate/deactivate/status query up top, then two
 * independent add/remove forms below (each dials a different code, so kept as separate forms
 * rather than one control smart enough to handle both) — ported from iOS's FriendsPlanManageView.
 */
@Composable
fun FriendsPlanManageScreen(onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val repository = remember { CatalogRepository(context) }
    val dialDirect = rememberDirectDialAction()

    var addFriendNumber by remember { mutableStateOf("") }
    var removeFriendNumber by remember { mutableStateOf("") }
    var showNonCubanError by remember { mutableStateOf(false) }

    val addContactLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val n = pickedNumber(context, result)
        if (n != null) addFriendNumber = n else if (result.data?.data != null) showNonCubanError = true
    }
    val removeContactLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val n = pickedNumber(context, result)
        if (n != null) removeFriendNumber = n else if (result.data?.data != null) showNonCubanError = true
    }

    fun dial(code: UssdCode?, input: String? = null) {
        if (code == null) return
        dialDirect(code.resolvedCode(input))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 12.dp),
                title = { Text(stringResource(R.string.title_friends_plan)) },
                navigationIcon = { if (onBack != null) BackNavigationIcon(onBack) },
                actions = { SimSelectorIcon() }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(vertical = 8.dp)) {
            Card(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column {
                    ActionRow(stringResource(R.string.friends_plan_activate), Icons.Filled.PersonAdd) { dial(repository.findCodeById("friends-plan-activate")) }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ActionRow(stringResource(R.string.friends_plan_deactivate), Icons.Filled.PersonRemove) { dial(repository.findCodeById("friends-plan-deactivate")) }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ActionRow(stringResource(R.string.friends_plan_check), Icons.Filled.Groups) { dial(repository.findCodeById("friends-plan-status-settings")) }
                }
            }
            Text(
                stringResource(R.string.friends_plan_cost_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp)
            )

            SectionHeader(stringResource(R.string.friends_plan_add_section))
            NumberFormCard(
                number = addFriendNumber,
                onNumberChange = { addFriendNumber = it },
                onPickContact = { addContactLauncher.launch(pickContactIntent()) },
                actionLabel = stringResource(R.string.friends_plan_action_add),
                onSubmit = {
                    dial(repository.findCodeById("friends-plan-add-member"), addFriendNumber)
                    addFriendNumber = ""
                }
            )

            SectionHeader(stringResource(R.string.friends_plan_remove_section))
            NumberFormCard(
                number = removeFriendNumber,
                onNumberChange = { removeFriendNumber = it },
                onPickContact = { removeContactLauncher.launch(pickContactIntent()) },
                actionLabel = stringResource(R.string.friends_plan_action_remove),
                onSubmit = {
                    dial(repository.findCodeById("friends-plan-remove-member"), removeFriendNumber)
                    removeFriendNumber = ""
                }
            )
        }
    }

    if (showNonCubanError) {
        AlertDialog(
            onDismissRequest = { showNonCubanError = false },
            title = { Text(stringResource(R.string.dialog_invalid_number)) },
            text = { Text(stringResource(R.string.friends_plan_non_cuban_error)) },
            confirmButton = {
                TextButton(onClick = { showNonCubanError = false }) { Text(stringResource(R.string.action_accept)) }
            }
        )
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
private fun ActionRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, color = MaterialTheme.colorScheme.primary)
        }
        Icon(Icons.Filled.ArrowOutward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
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
                modifier = Modifier.fillMaxWidth().height(52.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = number,
                    onValueChange = onNumberChange,
                    modifier = Modifier.weight(1f).padding(start = 16.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(accentColor),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    decorationBox = { inner ->
                        if (number.isEmpty()) {
                            Text(
                                stringResource(R.string.transfer_number_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        inner()
                    }
                )
                IconButton(onClick = onPickContact, modifier = Modifier.size(40.dp)) {
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
