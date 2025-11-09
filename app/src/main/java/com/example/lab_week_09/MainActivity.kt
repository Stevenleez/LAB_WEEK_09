package com.example.lab_week_09

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.util.copy
import com.example.lab_week_09.ui.theme.LAB_WEEK_09Theme
import com.example.lab_week_09.ui.theme.OnBackgroundItemText
import com.example.lab_week_09.ui.theme.OnBackgroundTitleText
import com.example.lab_week_09.ui.theme.PrimaryTextButton
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
//import java.sql.Types
import com.squareup.moshi.Types


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LAB_WEEK_09Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    App(
                        navController = navController
                    )
                }
            }
        }
    }
}

//Declare a data class called Student
data class Student(
    var name: String
)

@Composable
fun App(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            Home { listAsText ->
                navController.navigate(
                    "resultContent/?listData=${Uri.encode(listAsText)}"
                )
            }
        }
        composable(
            "resultContent/?listData={listData}",
            arguments = listOf(navArgument("listData") { type = NavType.StringType })
        ) { backStackEntry ->
            ResultContent(backStackEntry.arguments?.getString("listData").orEmpty())
        }
    }
}

// --- Home screen state + actions ---
@Composable
fun Home(
    navigateFromHomeToResult: (String) -> Unit
) {
    val listData = remember {
        mutableStateListOf(
            Student("Tanu"),
            Student("Tina"),
            Student("Tono")
        )
    }
    var inputField by remember { mutableStateOf(Student("")) }

    // Moshi setup (remember so it’s not rebuilt each recomposition)
    val moshi = remember { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }
    val listType = remember { Types.newParameterizedType(List::class.java, Student::class.java) }
    val listAdapter = remember { moshi.adapter<List<Student>>(listType) }

    HomeContent(
        listData = listData,
        inputField = inputField,
        onInputValueChange = { input -> inputField = inputField.copy(name = input) },
        onButtonClick = {
            if (inputField.name.isNotBlank()) {
                listData.add(inputField)
                inputField = inputField.copy(name = "")
            }
        },
        navigateFromHomeToResult = {
            val json = listAdapter.toJson(listData.toList())
            navigateFromHomeToResult(Uri.encode(json))   // pass encoded JSON
        }
    )
}

// --- Home UI ---
@Composable
fun HomeContent(
    listData: SnapshotStateList<Student>,
    inputField: Student,
    onInputValueChange: (String) -> Unit,
    onButtonClick: () -> Unit,
    navigateFromHomeToResult: () -> Unit
) {
    LazyColumn {
        item {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OnBackgroundTitleText(text = stringResource(id = R.string.enter_item))

                TextField(
                    value = inputField.name,
                    onValueChange = onInputValueChange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                Row {
                    PrimaryTextButton(text = stringResource(id = R.string.button_click)) {
                        onButtonClick()
                    }
                    PrimaryTextButton(text = stringResource(id = R.string.button_navigate)) {
                        navigateFromHomeToResult()
                    }
                }
            }
        }

        items(listData) { item ->
            Column(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OnBackgroundItemText(text = item.name)
            }
        }
    }
}

// --- Result screen ---
@Composable
fun ResultContent(listData: String) {
    val decodedJson = remember(listData) { Uri.decode(listData) }

    // Moshi
    val moshi = remember { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }
    val listType = remember { Types.newParameterizedType(List::class.java, Student::class.java) }
    val adapter = remember { moshi.adapter<List<Student>>(listType) }

    // Parse JSON -> always return a non-null List
    val students: List<Student> = remember(decodedJson) {
        // fromJson() returns List<Student>?
        // make it non-null BEFORE returning from remember
        adapter.fromJson(decodedJson).orEmpty()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(16.dp)
    ) {
        item { OnBackgroundTitleText(text = "Students") }

        if (students.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OnBackgroundItemText(text = "No data")
                }
            }
        } else {
            // ✅ Use the items(list) overload; list is NON-NULL now
            items(students) { s ->
                OnBackgroundItemText(text = s.name)  // 'name' resolves correctly
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}


// --- Previews ---
@Preview(showBackground = true)
@Composable
fun PreviewHome() {
    LAB_WEEK_09Theme {
        Home(navigateFromHomeToResult = {})
    }
}