package aeh.heimvisir.ui.components

import aeh.heimvisir.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Merki um að dýrið sé skráð týnt — það er aðalatriðið á skjánum. */
@Composable
fun LostBadge(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.badge_lost),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onError,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.error)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}
