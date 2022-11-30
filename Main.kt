package calculator
import java.math.BigInteger

val regexAllSigns = "[-+*/]".toRegex()
val regexPrimarySignString = "\\w+[*/]\\w+".toRegex()

val regexEquality = "=".toRegex()
val regexInt = "[+-]?\\d+".toRegex()
val regexLatinLetters = "[A-Za-z]+".toRegex()
val regexVariable = "[+-]?[A-Za-z]+".toRegex()
val regexNameEqualsName = "[A-Za-z]+\\s*[=]\\s*[+-]?[A-Za-z]+".toRegex() // a = b
val regexNameEqualsInt = "[A-Za-z]+\\s*[=]\\s*[+-]?\\d+".toRegex() // a = 10

val regexParenthesized = "[(][^()]+[)]".toRegex()
val regexParentheses = "[()]".toRegex()

val variables = mutableMapOf<String, BigInteger>()

fun main() {
    while (true) {
        var input = readln().trim()
        input = arrangeSigns(input)
        try {
            if (input.isBlank()) {                          // BLANK INPUT
                continue
            } else if (input.contains(regexEquality)) {     // SOME EQUALITY
                processSomeEquality(input)
            } else if (input.split(regexAllSigns).filter { it.isNotBlank() }.size == 1) {  // SINGLE VALUE
                println(processSingleValue(input))
            } else if (input.split(regexAllSigns).filter { it.isNotBlank() }.size > 1) {  // MULTIPLE VALUES
                println(processExpression(input))
            }
        } catch (e: Exception) {
            when (input.first()) {
                '/' -> {
                    if (input == "/exit") {
                        println("Bye!")
                        break
                    } else if (input == "/help") {
                        println("The program calculates the given expression")
                    } else {
                        println("Unknown command")
                    }
                }
                else -> {
                    println(e.message)
                }
            }
        }
    }
}

fun getSum(expressionToSum: String): String {
    val outputComponents = mutableListOf<BigInteger>()
    for (i in expressionToSum.split(" ")) {
        outputComponents.add(processSingleValue(i))
    }
    return outputComponents.sumOf { it }.toString()
}

fun calculatePrimarySigns(input: String): String {
    var input = input
    if (input.contains(regexPrimarySignString)) {
        while (true) {
            if (!input.contains(regexPrimarySignString)) {
                break
            }
            var result = ""
            val firstPrimarySignExpression = regexPrimarySignString.find(input)!!.value
            if (firstPrimarySignExpression.contains("*")) {
                result = ((processSingleValue(firstPrimarySignExpression.split("*").first()))
                        * processSingleValue(firstPrimarySignExpression.split("*").last())).toString()
                input = input.replace(firstPrimarySignExpression, result)
            } else {
                result = ((processSingleValue(firstPrimarySignExpression.split("/").first()))
                        / processSingleValue(firstPrimarySignExpression.split("/").last())).toString()
                input = input.replace(firstPrimarySignExpression, result)
            }
        }
    }
    input = input.replace("+", " +")
    input = input.replace("-", " -")
    return input
}

fun processExpression(input: String): String {
    var arrangedInput = arrangeSigns(input)
    do {
        val listOfParenthesized = mutableMapOf<String, String>()
        regexParenthesized.findAll(arrangedInput)
            .forEach { listOfParenthesized[it.value] = it.value.replace(regexParentheses, "") }

        for (i in listOfParenthesized.keys) {
            listOfParenthesized[i] = calculatePrimarySigns(listOfParenthesized.getValue(i))
        }

        for (i in listOfParenthesized.keys) {
            listOfParenthesized[i] = getSum(listOfParenthesized.getValue(i))
        }

        for (i in listOfParenthesized.keys) {
            arrangedInput = arrangedInput.replace(i, listOfParenthesized[i]!!)
        }
    } while (arrangedInput.contains(regexParenthesized))

    return getSum(calculatePrimarySigns(arrangedInput))
}

fun processSingleValue(singleValueInput: String): BigInteger {
    if (singleValueInput.matches(regexInt) || singleValueInput.matches(regexVariable)) {
        return if (singleValueInput.matches(regexInt)) { // single integer
            singleValueInput.toBigInteger()
        } else if (singleValueInput.matches(regexVariable)) { // single name
            if (variables.containsKey(separateSign(singleValueInput).last())) {
                if (separateSign(singleValueInput).first() == "-") { // -a
                    variables.getValue(separateSign(singleValueInput).last()) * -BigInteger.ONE
                } else {
                    variables.getValue(separateSign(singleValueInput).last())
                }
            } else {
                throw Exception("Unknown variable")
            }
        } else {
            throw Exception("Invalid identifier1")
        }
    } else {
        throw Exception("Invalid expression")
    }
}

fun checkExisting(s: String): Boolean {
    return variables.keys.contains(s.replace(regexAllSigns, ""))
}

fun separateSign(s: String): List<String> {
    return if (s.first().toString().matches("[+-]".toRegex())) {
        listOf(s.first().toString(), s.substring(1))
    } else {
        listOf(s)
    }
}

fun processAssignmentToVariable(frstPt: String, scndPt: String) {
    if (separateSign(scndPt)[0] == "-") {           // a = -b
        if (checkExisting(scndPt)) {                // assignment to stored variable
            variables[frstPt] = variables.getValue(scndPt[1].toString()) * -BigInteger.ONE
        } else {                                    // assignment to not stored variable
            throw Exception("Unknown variable")
        }
    } else {                                        // a = b
        if (checkExisting(scndPt)) {                // assignment to stored variable
            variables[frstPt] = variables.getValue(scndPt)
        } else {                                    // assignment to not stored variable
            throw Exception("Unknown variable")
        }
    }
}

fun processAssignmentToInteger(frstPt: String, scndPt: String) {
    variables[frstPt] = scndPt.toBigInteger()
}

fun processSomeEquality(input: String) {
    val equalityParts = input.split(regexEquality, 2)
    val frstPt = equalityParts.first().trim()
    val scndPt = equalityParts.last().trim()

    if (input.matches(regexNameEqualsInt)) {            // name = Int
        processAssignmentToInteger(frstPt, scndPt)
    } else if (input.matches(regexNameEqualsName)) {    // name = Name
        processAssignmentToVariable(frstPt, scndPt)
    } else {                                            // alert invalid equality
        if (!frstPt.matches(regexLatinLetters)) {
            throw Exception("Invalid identifier")
        } else if (!scndPt.matches(regexVariable)) {
            throw Exception("Invalid assignment")
        }
    }
}

fun arrangeSigns(input: String): String {
    var arrangedInput = input
    arrangedInput = arrangedInput.replace("\\s+".toRegex(), "") // delete all spaces
    arrangedInput = arrangedInput.replace("[-]{2}".toRegex(), "+") // get + from --
    arrangedInput = arrangedInput.replace("[+]+".toRegex(), "+") // delete redundant +
    arrangedInput = arrangedInput.replace("[-]+".toRegex(), "-") // delete redundant -
    arrangedInput = arrangedInput.replace("[+]\\s*[-]".toRegex(), "-") // get - from +-
    arrangedInput = arrangedInput.replace("[-]\\s*[+]".toRegex(), "-") // get - from -+
    return arrangedInput
}
