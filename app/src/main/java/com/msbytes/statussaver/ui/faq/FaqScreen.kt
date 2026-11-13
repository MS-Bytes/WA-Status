package com.msbytes.statussaver.ui.faq

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.msbytes.statussaver.R

private val steps = listOf(R.string.howto_step_1, R.string.howto_step_2, R.string.howto_step_3, R.string.howto_step_4)
private val faqs = listOf(
    R.string.faq_q1 to R.string.faq_a1,
    R.string.faq_q2 to R.string.faq_a2,
    R.string.faq_q3 to R.string.faq_a3,
    R.string.faq_q4 to R.string.faq_a4,
    R.string.faq_q5 to R.string.faq_a5,
    R.string.faq_q6 to R.string.faq_a6,
)

@Composable
fun FaqScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.faq_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.cd_back)) }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).navigationBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Text(stringResource(R.string.menu_how_to_use), style = MaterialTheme.typography.titleMedium) }
            itemsIndexed(steps) { index, step ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(28.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                        Text("${index + 1}", color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Spacer(Modifier.size(12.dp))
                    Text(stringResource(step), style = MaterialTheme.typography.bodyMedium)
                }
            }
            item { Spacer(Modifier.size(8.dp)); Text(stringResource(R.string.faq_heading), style = MaterialTheme.typography.titleMedium) }
            itemsIndexed(faqs) { _, (question, answer) -> FaqCard(question, answer) }
            item {
                Text(
                    stringResource(R.string.disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun FaqCard(question: Int, answer: Int) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().clickable { expanded = !expanded }) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(question), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
            }
            AnimatedVisibility(expanded) {
                Text(stringResource(answer), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
