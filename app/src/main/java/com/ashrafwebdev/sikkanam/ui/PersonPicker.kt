package com.ashrafwebdev.sikkanam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.R
import com.ashrafwebdev.sikkanam.data.Person
import com.ashrafwebdev.sikkanam.data.PersonChoice

/** Saved people whose name or phone matches the text. Phone matches need at least four digits. */
fun matchPeople(people: List<Person>, query: String): List<Person> {
    val text = query.trim()
    if (text.isEmpty()) return emptyList()
    return people
        .filter { it.name.contains(text, ignoreCase = true) || (text.length >= 4 && it.phone.contains(text)) }
        .take(6)
}

/**
 * Search before adding: type a name, pick the saved person if they are already in the list, or keep the
 * typed name to add them as a new person. The same register is used by Dawat, Locality and Jamaat.
 */
@Composable
fun PersonPickerFields(
    people: List<Person>,
    choice: PersonChoice,
    onChoice: (PersonChoice) -> Unit,
    label: String = stringResource(R.string.person_name),
    showPhone: Boolean = true,
    showAddress: Boolean = false,
) {
    val matches = if (choice.personId == 0L) matchPeople(people, choice.name) else emptyList()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = choice.name,
            onValueChange = { onChoice(choice.copy(personId = 0, name = it)) },
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        when {
            choice.personId != 0L -> {
                Text(stringResource(R.string.person_saved_selected), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { onChoice(choice.copy(personId = 0)) }) {
                    Text(stringResource(R.string.person_not_this))
                }
            }
            matches.isNotEmpty() -> {
                Text(stringResource(R.string.person_matches), style = MaterialTheme.typography.bodySmall)
                matches.forEach { person ->
                    TextButton(
                        onClick = {
                            onChoice(PersonChoice(personId = person.id, name = person.name, phone = person.phone, address = person.address))
                        },
                    ) {
                        Text(if (person.phone.isBlank()) person.name else "${person.name} · ${person.phone}")
                    }
                }
            }
            choice.name.isNotBlank() -> {
                Text(stringResource(R.string.person_new_hint), style = MaterialTheme.typography.bodySmall)
            }
        }
        if (showPhone) {
            OutlinedTextField(
                value = choice.phone,
                onValueChange = { onChoice(choice.copy(phone = it)) },
                label = { Text(stringResource(R.string.person_phone)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (showAddress) {
            OutlinedTextField(
                value = choice.address,
                onValueChange = { onChoice(choice.copy(address = it)) },
                label = { Text(stringResource(R.string.person_address)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
