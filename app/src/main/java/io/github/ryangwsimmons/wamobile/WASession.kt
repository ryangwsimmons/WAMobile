@file:Suppress("BlockingMethodInNonBlockingContext")

package io.github.ryangwsimmons.wamobile

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.json.JSONObject
import org.jsoup.Connection.Method
import org.jsoup.Connection.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

import kotlin.collections.Map

@Parcelize
class WASession(private val username: String, private val password: String, private var homeCookies: Map<String, String>): Parcelable {

    @Throws(Exception::class)
    fun initConnection() {
        //Make the initial connection to the WebAdvisor main page
        var res: Response = Jsoup.connect("https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TYPE=M&PID=CORE-WBMAIN&TOKENIDX=").followRedirects(true).execute()

        //Collect the cookies from the previous connection
        val cookies: MutableMap<String, String> = res.cookies()

        //Make the login request to the server
        res = Jsoup.connect("https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TOKENIDX="
                + cookies["LASTTOKEN"]
                + "&SS=LGRQ&URL=https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TYPE=M%26PID=CORE-WBMAIN%26TOKENIDX="
                + cookies["LASTTOKEN"]
                + "%26WARN=Y")
            .data("USER.NAME", this.username, "CURR.PWD", this.password, "RETURN.URL", "https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TYPE=M&PID=CORE-WBMAIN", "SUBMIT_OPTIONS", "")
            .cookies(cookies)
            .method(Method.POST)
            .followRedirects(true)
            .execute()

        //Update the cookies from the initial connection with the cookies from the login request
        cookies.putAll(res.cookies())

        //Parse the resulting document
        val doc: Document = res.parse()

        //Check that the series of connections resulted in the user getting the main page, and not the log in page or some other page
        val docTitle: String = doc.getElementsByTag("title").text()
        if (!(docTitle.contains("Main Menu"))) {
            throw Exception("Unable to log into WebAdvisor. Check that your login credentials are correct.")
        }

        //Copy the cookies from the home page into the homeCookies property
        this.homeCookies = cookies.toMap()
    }

    @Throws(Exception::class)
    fun getName(): String {
        //Check that a connection has been initialized
        if (this.homeCookies.isEmpty()) {
            throw Exception("Error: The connection has not yet been initialized.")
        }
        //Connect to the WebAdvisor main page
        val res: Response = Jsoup.connect("https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TYPE=M&PID=CORE-WBMAIN&TOKENIDX=" + this.homeCookies["LASTTOKEN"])
                                 .cookies(this.homeCookies)
                                 .followRedirects(true)
                                 .execute()

        //Parse the response document's HTML
        val doc: Document = res.parse()

        //Get the name
        var name: String = doc.getElementById("global")
            .getElementsByClass("container")[0]
            .getElementsByTag("h1")[0]
            .getElementsByClass("department")[0]
                              .getElementsByTag("a").text()
        name = "(?<=WebAdvisor for ).*$".toRegex(RegexOption.IGNORE_CASE).find(name)!!.value

        //Return the name
        return name
    }

    @Throws(Exception::class)
    fun getTermsResponse(): Response {
        //Check that a connection has been initialized
        if (this.homeCookies.isEmpty()) {
            throw Exception("Error: The connection has not yet been initialized.")
        }

        //Connect to the WebAdvisor grades page for the signed-in user

        return Jsoup.connect("https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TOKENIDX="
                + homeCookies["LASTTOKEN"] + "&CONSTITUENCY=WBST&type=P&pid=ST-WESTS02A")
            .cookies(homeCookies)
            .followRedirects(true)
            .execute()
    }

    @Throws(Exception::class)
    fun getTerms(): ArrayList<Term> {
        //Check that a connection has been initialized
        if (this.homeCookies.isEmpty()) {
            throw Exception("Error: The connection has not yet been initialized.")
        }

        //Create a new ArrayList to hold all the terms obtained from the request
        val terms: ArrayList<Term> = ArrayList()

        //Connect to the WebAdvisor grades page for the signed-in user
        val res: Response = this.getTermsResponse()

        //Parse the response document's HTML
        val doc: Document = res.parse()

        //Add all the terms to the ArrayList
        for(row: Element in doc.getElementById("GROUP_Grp_LIST_VAR1").getElementsByTag("tr")) {
            //If the current row in the table contains body cells (as opposed to header cells), add a new Term to the ArrayList
            if (row.getElementsByTag("td").size != 0) {
                val shortName: String = row.getElementsByClass("LIST_VAR2")[0].getElementsByTag("p")[0].text()
                val longName: String = row.getElementsByClass("LIST_VAR3")[0].getElementsByTag("p")[0].text()
                val startDate: String = row.getElementsByClass("DATE_LIST_VAR1")[0].getElementsByTag("p")[0].text()
                val endDate: String = row.getElementsByClass("DATE_LIST_VAR2")[0].getElementsByTag("p")[0].text()

                terms.add(Term(shortName, longName, startDate, endDate))
            }
        }

        //Return the ArrayList of Terms
        return terms
    }

    @Throws(Exception::class)
    fun getGrades(termPosition: Int, terms: List<Term>, grades: ArrayList<Grade>, advisor: StringBuilder, termGPA: StringBuilder) {
        //Check that a connection has been initialized
        if (this.homeCookies.isEmpty()) {
            throw Exception("Error: The connection has not yet been initialized.")
        }

        //Connect to the WebAdvisor terms page for the signed-in user
        var res: Response = this.getTermsResponse()

        //Create the request form for the next request using a map
        val gradesForm: HashMap<String, String> = HashMap()

        //Construct the form
        //Add the parameters that stay the same for every request
        gradesForm["LIST.VAR1_CONTROLLER"] = "LIST.VAR1"
        gradesForm["LIST.VAR1_MEMBERS"] = "LIST.VAR1*LIST.VAR2*LIST.VAR3*DATE.LIST.VAR1*DATE.LIST.VAR2"
        gradesForm["LIST.VAR1_MAX"] = terms.size.toString()
        gradesForm["LIST.VAR2_MAX"] = terms.size.toString()
        gradesForm["LIST.VAR3_MAX"] = terms.size.toString()
        gradesForm["DATE.LIST.VAR1_MAX"] = terms.size.toString()
        gradesForm["DATE.LIST.VAR2_MAX"] = terms.size.toString()
        //Add the parameter that determines which term has been selected
        gradesForm["LIST.VAR1_RADIO"] = "LIST.VAR1_" + (termPosition + 1).toString()
        //Add the parameters that repeat for every term the signed-in user has attended at Guelph
        for (i in terms.indices) {
            gradesForm["LIST.VAR2_" + (i + 1).toString()] = terms[i].shortName
            gradesForm["LIST.VAR3_" + (i + 1).toString()] = terms[i].longName.replace(' ', '+')
            gradesForm["DATE.LIST.VAR1_" + (i + 1).toString()] = terms[i].startDate
            gradesForm["DATE.LIST.VAR2_" + (i + 1).toString()] = terms[i].endDate
        }
        //Add the other miscellaneous parameters
        gradesForm["VAR1"] = ""
        gradesForm["RETURN.URL"] = "https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TOKENIDX=" + this.homeCookies["LASTTOKEN"] + "&type=M&constituency=WBST&pid=CORE-WBST"
        gradesForm["SUBMIT_OPTIONS"] = ""

        //Connect to the view grades page for the selected term
        res = Jsoup.connect(res.url().toString())
            .data(gradesForm)
            .cookies(res.cookies())
            .method(Method.POST)
            .followRedirects(true)
            .execute()

        //Parse the resulting document's HTML
        val doc: Document = res.parse()

        //Get the advisor and the term GPA
        advisor.append(doc.getElementById("GROUP_Grp_LIST_VAR1").getElementsByTag("p")[0].text())
        //Converting termGPA to float, and then to string again removes any unnecessary zeros
        termGPA.append(doc.getElementById("GROUP_Grp_VAR1").getElementById("LIST_VAR2").text().toFloat().toString() + "%")

        //Loop through all the courses for the selected term, adding course and grade info to the ArrayList of grades
        for(row: Element in doc.getElementById("GROUP_Grp_LIST_VAR3").getElementsByTag("tr")) {
            //If the current row isn't a header row, and actually contains data
            if (row.getElementsByTag("td").size != 0) {
                //Parse the HTML for the desired values
                val courseSection: String = row.getElementsByClass("LIST_VAR3")[0].getElementsByTag("p")[0].text()
                val courseTitle: String = row.getElementsByClass("VAR_STC_TITLE")[0].getElementsByTag("p")[0].text()
                var finalGrade: String = row.getElementsByClass("VAR_STC_VERIFIED_GRADE")[0].getElementsByTag("p")[0].text()
                //Check to see if the final grade is numeric, and set it's value accordingly
                finalGrade = when(finalGrade.toIntOrNull()) {
                    null -> finalGrade
                    else -> finalGrade.toInt().toString() + "%" //Converting finalGrade to integer and then back to string removes any leading zeros that may be present
                }
                val credits: String = row.getElementsByClass("VAR_STC_CRED")[0].getElementsByTag("p")[0].text()

                //Add the grade to the ArrayList
                grades.add(Grade(courseSection, courseTitle, finalGrade, credits))
            }
        }
    }

    @Throws(Exception::class)
    fun getNews(): NewsData {
        //Check that a connection has been initialized
        if (this.homeCookies.isEmpty()) {
            throw Exception("Error: The connection has not yet been initialized.")
        }

        // Create a string to hold the news HTML
        var html = ""

        // Make a connection to the WebAdvisor main page for students (the one with the news)
        var res: Response =
            Jsoup.connect("https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TOKENIDX=" + this.homeCookies["LASTTOKEN"] + "&type=M&constituency=WBST&pid=CORE-WBST")
                .cookies(this.homeCookies)
                .followRedirects(true)
                .execute()

        // Parse the resulting document's HTML
        val doc: Document = res.parse()

        // Find the news HTML in the page body
        for (customText: Element in doc.getElementById("content")
            .getElementsByClass("customText")) {
            if (customText.getElementsByTag("h1").size > 0 && customText.getElementsByTag("h2").size > 0 && customText.getElementsByTag(
                    "p"
                ).size > 0
            ) {
                html = customText.html()
                break
            }
        }

        // Add the outer divs back to the HTML, so that CSS styling works correctly
        html = "<div id=\"main\"><div id=\"content\">$html</div></div>"

        // Get the CSS for the news HTML
        res = Jsoup.connect("https://webadvisor.uoguelph.ca/WebAdvisor/stylesheets/themes/GUELPH/template.css")
                .cookies(this.homeCookies)
                .followRedirects(true)
                .ignoreContentType(true)
                .execute()

        var css: String = res.body()

        res = Jsoup.connect("https://webadvisor.uoguelph.ca/WebAdvisor/stylesheets/themes/GUELPH/ug-override.css")
            .cookies(this.homeCookies)
            .followRedirects(true)
            .ignoreContentType(true)
            .execute()

        css += res.body()

        // Return the news data
        return NewsData(html, css)
    }

    @Throws(Exception::class)
    private fun getSearchSectionsResponse(): Response {
        //Check that a connection has been initialized
        if (this.homeCookies.isEmpty()) {
            throw Exception("Error: The connection has not yet been initialized.")
        }

        //Connect to the WebAdvisor search for sections filter page

        //Return the response
        return Jsoup.connect("https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TOKENIDX=" + homeCookies["LASTTOKEN"] + "&CONSTITUENCY=WBST&type=P&pid=ST-WESTS12A")
            .cookies(homeCookies)
            .followRedirects(true)
            .execute()
    }

    @Throws(Exception::class)
    fun getSearchSectionsFilterValues(): SearchSectionsData {
        // Make a request to the Catalog Advanced Search options endpoint
        val res = Jsoup.connect("https://colleague-ss.uoguelph.ca/Student/Courses/GetCatalogAdvancedSearchAsync")
            .ignoreContentType(true)
            .execute()

        // Parse the request JSON
        val dataJson = JSONObject(res.body())

        // Create a new SearchSectionsData object to hold the response data
        val searchSectionsData = SearchSectionsData()

        // Parse the subjects array to get all available subjects
        val subjectsArray = dataJson.getJSONArray("Subjects")
        for (i in 0 until subjectsArray.length()) {
            val subjectJson = subjectsArray.getJSONObject(i)
            if (subjectJson.getBoolean("ShowInCourseSearch")) {
                searchSectionsData.subjects.add(DropdownOption(subjectJson.getString("Description"), subjectJson.getString("Code")))
            }
        }

        // Parse the terms array to get all available terms
        val termsArray = dataJson.getJSONArray("Terms")
        for (i in 0 until termsArray.length()) {
            val termJson = termsArray.getJSONObject(i)
            searchSectionsData.terms.add(DropdownOption(termJson.getString("Item2"), termJson.getString("Item1")))
        }

        // Parse the locations array to get all available locations
        val locationsArray = dataJson.getJSONArray("Locations")
        for (i in 0 until locationsArray.length()) {
            val locationJson = locationsArray.getJSONObject(i)
            searchSectionsData.locations.add(DropdownOption(locationJson.getString("Item2"), locationJson.getString("Item1")))
        }

        // Parse the academic levels array to get all available academic levels
        val academicLevelsArray = dataJson.getJSONArray("AcademicLevels")
        for (i in 0 until academicLevelsArray.length()) {
            val academicLevelJson = academicLevelsArray.getJSONObject(i)
            searchSectionsData.academicLevels.add(DropdownOption(academicLevelJson.getString("Item2"), academicLevelJson.getString("Item1")))
        }

        // Parse the meeting times array to get all available meeting times
        val meetingTimesArray = dataJson.getJSONArray("TimeRanges")
        for (i in 0 until meetingTimesArray.length()) {
            val meetingTimeJson = meetingTimesArray.getJSONObject(i)
            val beginningTime = meetingTimeJson.getInt("Item2")
            val endTime = meetingTimeJson.getInt("Item3")
            searchSectionsData.meetingTimes.add(DropdownOption(
                meetingTimeJson.getString("Item1"),
                "$beginningTime,$endTime"
            ))
        }

        return searchSectionsData
    }


    @Throws(Exception::class)
    fun getSearchResults(
        term: String,
        meetingStartDate: String,
        meetingEndDate: String,
        subjects: ArrayList<String>,
        courseNums: ArrayList<String>,
        sections: ArrayList<String>,
        timeOfDay: String,
        timeStartsBy: String,
        timeEndsBy: String,
        days: ArrayList<Boolean>,
        location: String,
        academicLevel: String,
        page: Int = 1,
        existingCookies: MutableMap<String, String>? = null,
        existingReqVerToken: String? = null
    ): SearchResultsData {
        var res: Response
        var cookies: MutableMap<String, String>
        var reqVerToken: String

        if (existingCookies != null && existingReqVerToken != null) {
            cookies = existingCookies
            reqVerToken = existingReqVerToken
        } else {
            // Make a request to the Search for Sections page to get a request verification code and cookies
            res = Jsoup.connect("https://colleague-ss.uoguelph.ca/Student/Courses")
                .followRedirects(true)
                .execute()

            // Store the cookies in a variable
            cookies = res.cookies()

            // Parse the response text to get the HTML
            val doc = res.parse()

            // Store the request verification token in a variable
            reqVerToken = doc.select("input[name='__RequestVerificationToken']").attr("value")
        }

        // Build the POST body JSON for the next request
        val postBodyJson = """
            {
                "searchParameters": "{
                    \"terms\": [${if (term == "") "" else "\\\"$term\\\""}],
                    \"startTime\": ${if (timeOfDay.split(',')[0] == "") "null" else timeOfDay.split(',')[0]},
                    \"endTime\": ${if (timeOfDay.split(',')[1] == "") "null" else timeOfDay.split(',')[1]},
                    \"academicLevels\": [${if (academicLevel == "") "" else "\\\"$academicLevel\\\""}],
                    \"days\": [${days.mapIndexedNotNull { index: Int, day: Boolean -> if (day) "\\\"$index\\\"" else null}.joinToString(", ")}],
                    \"locations\": [${if (location == "") "" else "\\\"$location\\\""}],
                    \"keywordComponents\": [${subjects.mapIndexedNotNull { index: Int, subject: String ->
                        if (subject != "" || courseNums[index] != "" || sections[index] != "") {
                            """
                                {
                                    \"subject\": \"$subject\",
                                    \"courseNumber\": \"${courseNums[index]}\",
                                    \"section\": \"${sections[index]}\",
                                    \"synonym\": \"\"
                                }
                            """.trimIndent()
                        } else null
                    }.joinToString(", ")}],
                    \"startDate\": ${if (meetingStartDate == "") "null" else "\\\"$meetingStartDate\\\""},
                    \"endDate\": ${if (meetingEndDate == "") "null" else "\\\"$meetingEndDate\\\""},
                    \"startsAtTime\": ${if (timeStartsBy == "") "null" else "\\\"$timeStartsBy\\\""},
                    \"endsByTime\": ${if (timeEndsBy == "") "null" else "\\\"$timeEndsBy\\\""},
                    \"pageNumber\": $page,
                    \"sortOn\": \"None\",
                    \"sortDirection\": \"Ascending\",
                    \"quantityPerPage\": 30,
                    \"searchResultsView\": \"SectionListing\"
                }"
            }
        """.trimIndent().replace("\n", "")

        res = Jsoup.connect("https://colleague-ss.uoguelph.ca/Student/Courses/SearchAsync")
            .method(Method.POST)
            .ignoreContentType(true)
            .cookies(cookies)
            .header("Content-Type", "application/json, charset=utf-8")
            .header("X-Requested-With", "XMLHttpRequest")
            .header("__RequestVerificationToken", reqVerToken)
            .requestBody(postBodyJson)
            .execute()

        return SearchResultsData(cookies, reqVerToken, res.body())
    }

    @SuppressLint("DefaultLocale")
    fun getSectionDetails(cookies: Map<String, String>, result: SearchResult): SectionDetails {
        //Make the connection to the server
        var res: Response = Jsoup.connect("https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor")
            .cookies(cookies)
            .followRedirects(true)
            .execute()

        val doc: Document = res.parse()

        //Get the course name and split it into sections
        val courseNameSections: List<String> = doc.getElementById("VAR2").text().split("*")

        //Get the course name without the section, in all lowercase
        val courseName: String = (courseNameSections[0] + courseNameSections[1]).toLowerCase()

        //Create a variable that holds the url for the course calendar, and populate it based on the academic level of the course
        var calendarURL = ""
        when (result.academicLevel) {
            "Diploma" -> calendarURL = "https://www.uoguelph.ca/registrar/calendars/diploma/current/courses/"
            "Graduate" -> calendarURL = "https://www.uoguelph.ca/registrar/calendars/graduate/current/courses/"
            "Undergraduate" -> calendarURL = "https://www.uoguelph.ca/registrar/calendars/undergraduate/current/courses/"
            "Undergraduate Guelph-Humber" -> calendarURL = "https://www.uoguelph.ca/registrar/calendars/guelphhumber/current/courses/"
        }

        //Connect to the calendar page
        res = Jsoup.connect("$calendarURL$courseName.shtml")
            .followRedirects(true)
            .execute()

        //Parse the calendar document
        val calDoc: Document = res.parse()

        //Get the section details, and store them in variables
        val title: String = calDoc.getElementById("content").getElementsByClass("title")[0].text()
        val description: String = calDoc.getElementById("content").getElementsByClass("description")[0].text()
        var offerings = ""
        if (calDoc.getElementById("content").getElementsByClass("offerings").size > 0) {
            offerings = calDoc.getElementById("content").getElementsByClass("offerings")[0].getElementsByClass("text")[0].text()
        }
        var restrictions = ""
        if (calDoc.getElementById("content").getElementsByClass("restrictions").size > 0) {
            restrictions = calDoc.getElementById("content").getElementsByClass("restrictions")[0].getElementsByClass("text")[0].text()
        }
        var prereqs = ""
        if (calDoc.getElementById("content").getElementsByClass("prereqs").size > 0){
            prereqs = calDoc.getElementById("content").getElementsByClass("prereqs")[0].getElementsByClass("text")[0].text()
        }
        val departments: String = calDoc.getElementById("content").getElementsByClass("departments")[0].getElementsByClass("text")[0].text()
        val startDate: String = doc.getElementById("VAR6").text()
        val endDate: String = doc.getElementById("VAR7").text()
        val facultyName: String = doc.getElementById("LIST_VAR7_1").text()
        val facultyEmail: String = doc.getElementById("LIST_VAR10_1").text()
        val facultyPhone: String = doc.getElementById("LIST_VAR8_1").text()
        val facultyExtension: String = doc.getElementById("LIST_VAR9_1").text()

        //Return a new SectionDetails object containing the data
        return SectionDetails(title, description, offerings, restrictions, prereqs, departments, startDate, endDate, facultyName, facultyEmail, facultyPhone, facultyExtension)
    }

    @Throws(Exception::class)
    fun getEllucianCookies(): MutableMap<String, String>{
        //Check that a connection has been initialized
        if (this.homeCookies.isEmpty()) {
            throw Exception("Error: The connection has not yet been initialized.")
        }

        //Connect to the WebAdvisor students page
        var res: Response = Jsoup.connect("https://webadvisor.uoguelph.ca/WebAdvisor/WebAdvisor?TYPE=M&CONSTITUENCY=WBST&PID=CORE-WBST&TOKENIDX=" + this.homeCookies["LASTTOKEN"])
            .cookies(this.homeCookies)
            .followRedirects(true)
            .execute()

        // Parse the HTML
        val doc: Document = res.parse()

        // Get the <a> tag for the account view, extract the token
        val token = "Token=(.*)\$".toRegex().find(
            doc.getElementsByClass("subnav")[0]
                .select("h3:contains(Financial Profile) + ul li a")[0]
                .attr("href")
        )!!.groupValues[1]

        // Connect to the Ellucian portal by submitting the token retrieved from WebAdvisor
        res = Jsoup.connect("https://colleague-ss.uoguelph.ca/Student/Finance?Token=$token")
            .followRedirects(true)
            .execute()

        // Get the cookies from the response
        return res.cookies()
    }

    @Throws(Exception::class)
    fun getAccountViewInfo(ellucianCookies: MutableMap<String, String>, timeFrameID: String = ""): String{
        // Navigate to the account activity page
        var res = Jsoup.connect("https://colleague-ss.uoguelph.ca/Student/Finance/AccountActivity")
            .cookies(ellucianCookies)
            .followRedirects(true)
            .execute()

        // Parse the response text to get the HTML
        val doc = res.parse()

        // Get the request verification token
        val reqVerToken = doc.select("input[name='__RequestVerificationToken']").attr("value")

        // If a time frame ID has been passed in, form the request body
        val bodyJSON = if (timeFrameID != "") {
            "{\"timeframeId\":\"$timeFrameID\"}"
        } else {
            "{}"
        }

        // Make a request to the Account Activity Info endpoint
        res = Jsoup.connect("https://colleague-ss.uoguelph.ca/Student/Finance/AccountActivity/GetAccountActivityViewModel")
            .method(Method.POST)
            .ignoreContentType(true)
            .cookies(ellucianCookies)
            .header("Content-Type", "application/json")
            .header("__RequestVerificationToken", reqVerToken)
            .header("X-Requested-With", "XMLHttpRequest")
            .requestBody(bodyJSON)
            .execute()

        return res.body()
    }
}