package aeh.heimvisir.ui.screens

import aeh.heimvisir.R
import aeh.heimvisir.core.TagNumber
import aeh.heimvisir.model.LookupResult
import aeh.heimvisir.ui.LookupUiState
import aeh.heimvisir.ui.components.PetCard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun LookupScreen(
    state: LookupUiState,
    onTagChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.lookup_heading),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.lookup_help),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = state.tag,
                    onValueChange = onTagChanged,
                    label = { Text(stringResource(R.string.field_tag)) },
                    placeholder = { Text(stringResource(R.string.tag_placeholder)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Search,
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboard?.hide()
                            onSubmit()
                        },
                    ),
                    // Villan er SÝNILEG. Vefútgáfan skilaði auðu og
                    // notandinn hafði ekkert að fara eftir.
                    isError = state.inputError != null,
                    supportingText = state.inputError?.let { { Text(inputErrorText(it)) } },
                    modifier = Modifier.fillMaxWidth(),
                )

                Button(
                    onClick = {
                        keyboard?.hide()
                        onSubmit()
                    },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(stringResource(R.string.action_search))
                    }
                }
            }
        }

        when (val result = state.result) {
            is LookupResult.Found -> PetCard(
                tagNumber = state.lastTag.orEmpty(),
                pet = result.pet,
            )

            LookupResult.NotFound -> MessageCard(
                text = stringResource(R.string.result_not_found, state.lastTag.orEmpty()),
            )

            LookupResult.NetworkError -> MessageCard(
                text = stringResource(R.string.result_network_error),
                isError = true,
            )

            LookupResult.MalformedResponse -> MessageCard(
                text = stringResource(R.string.result_malformed),
                isError = true,
            )

            is LookupResult.ServerError -> MessageCard(
                text = stringResource(R.string.result_server_error, result.code),
                isError = true,
            )

            null -> Unit
        }
    }
}

@Composable
private fun inputErrorText(check: TagNumber.Check): String = when (check) {
    TagNumber.Check.TooShort -> stringResource(R.string.error_tag_too_short, TagNumber.MIN_LENGTH)
    TagNumber.Check.TooLong -> stringResource(R.string.error_tag_too_long, TagNumber.MAX_LENGTH)
    TagNumber.Check.HasIllegalCharacters -> stringResource(R.string.error_tag_illegal)
    is TagNumber.Check.Valid -> ""
}

@Composable
private fun MessageCard(text: String, isError: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(16.dp),
        )
    }
}
