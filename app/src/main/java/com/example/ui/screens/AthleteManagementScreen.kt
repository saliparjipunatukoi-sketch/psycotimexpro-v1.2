package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.model.AthleteEntity

/**
 * Compatibility wrapper that delegates to [AthleteListScreen].
 */
@Composable
fun AthleteManagementScreen(
    athletes: List<AthleteEntity>,
    onOpenAddAthlete: () -> Unit,
    onSelectAthlete: (AthleteEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    AthleteListScreen(
        athletes = athletes,
        onOpenAddAthlete = onOpenAddAthlete,
        onSelectAthlete = onSelectAthlete,
        modifier = modifier
    )
}
