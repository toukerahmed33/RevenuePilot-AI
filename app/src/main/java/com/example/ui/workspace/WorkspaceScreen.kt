package com.example.ui.workspace

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.InspectionNoteEntity
import com.example.data.local.entity.TaskEntity
import com.example.ui.WorkspaceTab
import com.example.ui.components.PriorityBadge
import com.example.ui.components.VerificationStatusBadge
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TaskFilterTab {
    ALL,
    TODAY,
    PENDING,
    COMPLETED
}

enum class InspectionTab {
    ALL,
    DRAFTS,
    FINAL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    activeTab: WorkspaceTab,
    tasks: List<TaskEntity>,
    inspectionNotes: List<InspectionNoteEntity>,
    onSelectTab: (WorkspaceTab) -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onOpenTaskDialog: (TaskEntity?) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onOpenEndOfDaySummary: () -> Unit,
    onOpenInspectionDialog: (InspectionNoteEntity?) -> Unit,
    onDeleteInspectionNote: (InspectionNoteEntity) -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (activeTab == WorkspaceTab.TASKS) {
                        onOpenTaskDialog(null)
                    } else {
                        onOpenInspectionDialog(null)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag(
                    if (activeTab == WorkspaceTab.TASKS) "add_task_fab" else "add_inspection_fab"
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = if (activeTab == WorkspaceTab.TASKS) "নতুন টাস্ক" else "নতুন পরিদর্শন নোট"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("workspace_screen")
        ) {
            // Minimalist Segmented Switcher
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                SegmentedButton(
                    selected = activeTab == WorkspaceTab.TASKS,
                    onClick = { onSelectTab(WorkspaceTab.TASKS) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("workspace_tab_tasks")
                ) {
                    Text("দৈনিক টাস্ক (${tasks.size})")
                }
                SegmentedButton(
                    selected = activeTab == WorkspaceTab.INSPECTIONS,
                    onClick = { onSelectTab(WorkspaceTab.INSPECTIONS) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("workspace_tab_inspections")
                ) {
                    Text("মাঠ পরিদর্শন (${inspectionNotes.size})")
                }
            }

            if (activeTab == WorkspaceTab.TASKS) {
                TasksTabContent(
                    tasks = tasks,
                    onToggleTask = onToggleTask,
                    onOpenTaskDialog = onOpenTaskDialog,
                    onDeleteTask = onDeleteTask,
                    onOpenEndOfDaySummary = onOpenEndOfDaySummary
                )
            } else {
                InspectionsTabContent(
                    notes = inspectionNotes,
                    onOpenDialog = onOpenInspectionDialog,
                    onDeleteNote = onDeleteInspectionNote
                )
            }
        }
    }
}

@Composable
private fun TasksTabContent(
    tasks: List<TaskEntity>,
    onToggleTask: (TaskEntity) -> Unit,
    onOpenTaskDialog: (TaskEntity?) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onOpenEndOfDaySummary: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf(TaskFilterTab.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    val filteredTasks = remember(tasks, selectedFilter, searchQuery) {
        tasks.filter { task ->
            val matchesSearch = searchQuery.isBlank() ||
                task.title.contains(searchQuery, ignoreCase = true) ||
                task.holdingId.contains(searchQuery, ignoreCase = true) ||
                task.description.contains(searchQuery, ignoreCase = true)

            val matchesTab = when (selectedFilter) {
                TaskFilterTab.ALL -> true
                TaskFilterTab.TODAY -> task.dueDate == todayDateStr
                TaskFilterTab.PENDING -> task.status != "COMPLETED"
                TaskFilterTab.COMPLETED -> task.status == "COMPLETED"
            }
            matchesSearch && matchesTab
        }
    }

    val completedCount = tasks.count { it.status == "COMPLETED" }
    val pendingCount = tasks.count { it.status != "COMPLETED" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tasks_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp, top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "কর্মপরিকল্পনা সারসংক্ষেপ",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "সম্পন্ন: $completedCount টি • বাকি: $pendingCount টি",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onOpenEndOfDaySummary,
                        modifier = Modifier.testTag("end_of_day_summary_button")
                    ) {
                        Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("দিনশেষে রিপোর্ট")
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("টাস্ক বা হোল্ডিং নং খুঁজুন...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedFilter == TaskFilterTab.ALL,
                        onClick = { selectedFilter = TaskFilterTab.ALL },
                        label = { Text("সকল (${tasks.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == TaskFilterTab.TODAY,
                        onClick = { selectedFilter = TaskFilterTab.TODAY },
                        label = { Text("আজকের কাজ") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == TaskFilterTab.PENDING,
                        onClick = { selectedFilter = TaskFilterTab.PENDING },
                        label = { Text("বাকি ($pendingCount)") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == TaskFilterTab.COMPLETED,
                        onClick = { selectedFilter = TaskFilterTab.COMPLETED },
                        label = { Text("সম্পন্ন ($completedCount)") }
                    )
                }
            }
        }

        if (filteredTasks.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "কোনো টাস্ক পাওয়া যায়নি। নতুন টাস্ক যোগ করতে (+) বাটনে চাপুন।",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(filteredTasks, key = { it.id }) { task ->
                WorkspaceTaskCard(
                    task = task,
                    onToggle = { onToggleTask(task) },
                    onEdit = { onOpenTaskDialog(task) },
                    onDelete = { onDeleteTask(task) }
                )
            }
        }
    }
}

@Composable
private fun WorkspaceTaskCard(
    task: TaskEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                IconButton(
                    onClick = onToggle,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (task.status == "COMPLETED") Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = "Toggle Complete",
                        tint = if (task.status == "COMPLETED") StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (task.status == "COMPLETED") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        PriorityBadge(task.priority)
                    }
                    if (task.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (task.holdingId.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "হোল্ডিং: ${task.holdingId}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onPrimaryContainer),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "তারিখ: ${task.dueDate}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusDanger, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun InspectionsTabContent(
    notes: List<InspectionNoteEntity>,
    onOpenDialog: (InspectionNoteEntity?) -> Unit,
    onDeleteNote: (InspectionNoteEntity) -> Unit
) {
    var selectedFilter by remember { mutableStateOf(InspectionTab.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredNotes = remember(notes, selectedFilter, searchQuery) {
        notes.filter { note ->
            val matchesSearch = searchQuery.isBlank() ||
                note.holdingRef.contains(searchQuery, ignoreCase = true) ||
                note.inspectionRef.contains(searchQuery, ignoreCase = true) ||
                note.assignedArea.contains(searchQuery, ignoreCase = true) ||
                note.observations.contains(searchQuery, ignoreCase = true)

            val matchesTab = when (selectedFilter) {
                InspectionTab.ALL -> true
                InspectionTab.DRAFTS -> note.isDraft
                InspectionTab.FINAL -> !note.isDraft
            }
            matchesSearch && matchesTab
        }
    }

    val draftsCount = notes.count { it.isDraft }
    val finalCount = notes.count { !it.isDraft }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("inspection_notes_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp, top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("হোল্ডিং, রেফারেন্স বা এলাকা খুঁজুন...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedFilter == InspectionTab.ALL,
                        onClick = { selectedFilter = InspectionTab.ALL },
                        label = { Text("সকল (${notes.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == InspectionTab.DRAFTS,
                        onClick = { selectedFilter = InspectionTab.DRAFTS },
                        label = { Text("খসড়া ($draftsCount)") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == InspectionTab.FINAL,
                        onClick = { selectedFilter = InspectionTab.FINAL },
                        label = { Text("চূড়ান্ত ($finalCount)") }
                    )
                }
            }
        }

        if (filteredNotes.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "কোনো পরিদর্শন নোট পাওয়া যায়নি। নতুন নোট যোগ করতে (+) বাটনে চাপুন।",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(filteredNotes, key = { it.id }) { note ->
                WorkspaceInspectionCard(
                    note = note,
                    onEdit = { onOpenDialog(note) },
                    onDelete = { onDeleteNote(note) }
                )
            }
        }
    }
}

@Composable
private fun WorkspaceInspectionCard(
    note: InspectionNoteEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "হোল্ডিং: ${note.holdingRef}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = note.inspectionRef,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (note.isDraft) StatusWarning.copy(alpha = 0.14f) else StatusSuccess.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (note.isDraft) "খসড়া" else "চূড়ান্ত",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (note.isDraft) StatusWarning else StatusSuccess,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = note.assignedArea,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = note.inspectionDate,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                VerificationStatusBadge(note.verificationStatus)
                if (note.locationLabel.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = note.locationLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                            )
                        }
                    }
                }
                if (note.photoUri.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ছবি", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                        }
                    }
                }
            }

            if (note.photoUri.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = note.photoUri,
                    contentDescription = "মাঠ পরিদর্শনের ছবি",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = note.observations,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (note.followUpAction.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "পদক্ষেপ: ${note.followUpAction}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusDanger, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
