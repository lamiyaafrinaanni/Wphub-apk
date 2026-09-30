package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.SiteEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteSwitcherSheet(
    sites: List<SiteEntity>,
    currentSite: SiteEntity?,
    onSelectSite: (String) -> Unit,
    onAddNewSiteClick: () -> Unit,
    onOpenLoginClick: () -> Unit = {},
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("site_switcher_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            SiteSelector(
                sites = sites,
                currentSite = currentSite,
                onSelectSite = { siteId ->
                    onSelectSite(siteId)
                    onDismiss()
                },
                onAddNewSiteClick = {
                    onDismiss()
                    onAddNewSiteClick()
                },
                onOpenLoginClick = {
                    onDismiss()
                    onOpenLoginClick()
                }
            )
        }
    }
}
