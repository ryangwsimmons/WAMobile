package ca.ryangwsimmons.wamobile

data class SectionDetails(
    val title: String,
    val description: String,
    val facultyName: String,
    val facultyEmail: String,
    val facultyPhone: String,
    val courseSpecs: HashMap<String, String>
)