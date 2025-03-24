package com.calculatorapp


import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import net.objecthunter.exp4j.ExpressionBuilder
import org.json.JSONArray
import org.json.JSONException


class MainActivity : AppCompatActivity() {


    private lateinit var inputTextView: EditText
    private lateinit var outputTextView: TextView
    private var input: String = ""
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var historySpinner: Spinner


    // History list to store operations and results
    private val historyList = mutableListOf<String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)


        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("CalculatorPrefs", MODE_PRIVATE)


        // Initialize Views
        inputTextView = findViewById(R.id.input)
        outputTextView = findViewById(R.id.output)
        historySpinner = findViewById(R.id.history_spinner)  // Spinner for history dropdown


        inputTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // Always move the cursor to the end after text change
                inputTextView.setSelection(inputTextView.text.length)
            }
        })

        val buttons = listOf<Button>(
            findViewById(R.id.button0),
            findViewById(R.id.button1),
            findViewById(R.id.button2),
            findViewById(R.id.button3),
            findViewById(R.id.button4),
            findViewById(R.id.button5),
            findViewById(R.id.button6),
            findViewById(R.id.button7),
            findViewById(R.id.button8),
            findViewById(R.id.button9),
            findViewById(R.id.button_dot),
            findViewById(R.id.button_add),
            findViewById(R.id.button_sub),
            findViewById(R.id.button_multi),
            findViewById(R.id.button_devide),
            findViewById(R.id.button_LeftParenthesis),
            findViewById(R.id.button_RightParenthesis),
            findViewById(R.id.button_clear),
            findViewById(R.id.button_equal),
            findViewById(R.id.button_power),
            findViewById(R.id.button_backspace),
            findViewById(R.id.button_para1),  // Button for Toggle Sign (+/-)
            findViewById(R.id.button_para2)   // Button for Percentage
        )


        buttons.forEach { button ->
            button.setOnClickListener(View.OnClickListener { v: View ->
                handleButtonClick(button.text.toString())
            })
        }

        inputTextView.setShowSoftInputOnFocus(false) // Suppress the keyboard

// Alternatively, use reflection for older Android versions
        try {
            val method = TextView::class.java.getMethod("setShowSoftInputOnFocus", Boolean::class.java)
            method.invoke(inputTextView, false)
        } catch (e: Exception) {
            e.printStackTrace()
        }


        // Load the calculation history when the app starts
        loadHistory()

        }


    private fun appendInput(value: String) {
        val cursorPosition = inputTextView.selectionStart

        // Insert the value at the current cursor position
        input = input.substring(0, cursorPosition) + value + input.substring(cursorPosition)

        // Update the text and move the cursor to the new position
        inputTextView.setText(input)
        inputTextView.setSelection(cursorPosition + value.length)
    }


    private fun updateInputText(text: String) {
        input = text
        inputTextView.setText(input)
        inputTextView.setSelection(input.length)  // Move the cursor to the end
    }


    private fun appendDecimal() {
        val lastNumber = input.split(" ").last()
        if (!lastNumber.contains(".")) {
            input += "."
            inputTextView.setText(input)
        }
    }



    private fun handleOperator(op: String) {
        val cursorPosition = inputTextView.selectionStart

        // Check if the input is not empty and the last character is valid for an operator
        if (input.isNotEmpty() && (input.last().isDigit() || input.last() == ')' || input.last() == '%')) {
            // Insert the operator at the current cursor position
            input = input.substring(0, cursorPosition) + " $op " + input.substring(cursorPosition)

            // Update the text and move the cursor to the new position
            inputTextView.setText(input)
            inputTextView.setSelection(cursorPosition + op.length + 2)  // +2 to account for spaces
        }
    }


    private fun calculateResult() {
        try {
            if (input.isNotEmpty()) {
                // Prepare the expression for evaluation
                val preparedInput = input.replace("x", "*").replace("÷", "/").replace("%", "/100")


                // Build and evaluate the expression
                val expression = ExpressionBuilder(preparedInput).build()
                val result = expression.evaluate()


                // Format the result to avoid decimal places if it's a whole number
                val formattedResult = if (result == result.toInt().toDouble()) {
                    result.toInt().toString()
                } else {
                    result.toString()
                }


                // Save the result and the corresponding operation in the history
                val operationWithResult = "$input = $formattedResult"
                saveResultInHistory(operationWithResult)


                // Show result
                outputTextView.text = formattedResult
                input = formattedResult
                inputTextView.setText(input)
            }
        } catch (e: Exception) {
            outputTextView.text = "Syntax Error"
        }
    }


    private fun saveResultInHistory(operationWithResult: String) {
        // Log the history being saved
        Log.d("CalculatorApp", "Saving to history: $operationWithResult")


        // Add the operation with the result to the history list (most recent first)
        historyList.add(0, operationWithResult)  // Add to the front of the list to maintain recent first order


        // Ensure the history list does not exceed 23 entries
        if (historyList.size > 23) {
            historyList.removeAt(historyList.size - 1)  // Remove the oldest (last) entry if the list exceeds 23
        }


        // Save updated history back to SharedPreferences using JSONArray
        try {
            val jsonArray = JSONArray(historyList)
            val editor = sharedPreferences.edit()
            editor.putString("history", jsonArray.toString()) // Save as JSON string
            editor.apply()
        } catch (e: JSONException) {
            Log.e("CalculatorApp", "Failed to save history", e)
        }


        // Update the displayed history
        loadHistory()
    }


    private fun loadHistory() {
        val historyJson = sharedPreferences.getString("history", "[]") ?: "[]"
        try {
            val jsonArray = JSONArray(historyJson)

            // Clear the current history list and add the new data
            historyList.clear()
            for (i in 0 until jsonArray.length()) {
                historyList.add(jsonArray.getString(i))
            }

            Log.d("CalculatorApp", "Loaded History: $historyList")


            // Update the Spinner (dropdown) with the history
            val historyListDisplay = historyList.toList()
            val adapter =
                ArrayAdapter(this, android.R.layout.simple_spinner_item, historyListDisplay)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            historySpinner.adapter = adapter

        } catch (e: JSONException) {
            Log.e("CalculatorApp", "Failed to load history", e)
        }
    }




    private fun clearInput() {
        input = ""
        inputTextView.setText("")
        outputTextView.text = ""
    }


    private fun handlePercentage() {
        if (input.isNotEmpty()) {
            if (input.last().isDigit()) {
                input += "%"
                inputTextView.setText(input)
            }
        }
    }


    private fun toggleSign() {
        if (input.isNotEmpty()) {
            val parts = input.split(" ").toMutableList()
            var lastPart = parts.last()

            // Check for the last standalone number outside parentheses
            val outerNumberRegex = Regex("""(-?\d+\.?\d*)$""")
            val outerMatch = outerNumberRegex.find(lastPart)

            if (outerMatch != null) {
                val number = outerMatch.value

                // Check if the number is inside parentheses
                val innerNumberRegex = Regex("""\((\-?\d+\.?\d*)$""")
                val innerMatch = innerNumberRegex.find(lastPart)

                if (innerMatch != null) {
                    // Toggle the sign of the number inside the parentheses
                    val numberInside = innerMatch.groupValues[1]
                    val toggledNumber = if (numberInside.startsWith("-")) {
                        numberInside.substring(1)  // Remove negative sign
                    } else {
                        "-$numberInside"  // Add negative sign
                    }
                    parts[parts.size - 1] = lastPart.replace(numberInside, toggledNumber)
                } else {
                    // Toggle the sign of the number outside parentheses
                    val toggledNumber = if (number.startsWith("-")) {
                        number.substring(1)  // Remove negative sign
                    } else {
                        "-$number"  // Add negative sign
                    }
                    parts[parts.size - 1] = lastPart.replace(number, toggledNumber)
                }
            }

            // Reassemble the input and update the TextView
            input = parts.joinToString(" ")
            inputTextView.setText(input)
        }
    }


    private fun handleButtonClick(value: String) {
        when {
            value.isNumeric() -> appendInput(value)
            value == "." -> appendDecimal()
            value in setOf("+", "-", "x", "÷", "^") -> handleOperator(value)
            value == "=" -> calculateResult()
            value == "AC" -> clearInput()
            value == "+/-" -> toggleSign()  // Toggle sign for the most recent number
            value == "⌫" -> handleBackspace()
            value == "(" || value == ")" -> handleParenthesis(value)
            value == "%" -> handlePercentage()
        }
    }


    private fun String.isNumeric(): Boolean {
        return try {
            this.toDouble()
            true
        } catch (e: NumberFormatException) {
            false
        }
    }


    private fun handleBackspace() {
        val cursorPosition = inputTextView.selectionStart

        if (cursorPosition > 0) {
            // Remove the character before the cursor position
            input = input.removeRange(cursorPosition - 1, cursorPosition)
            inputTextView.setText(input)

            // Update the cursor position after backspace
            inputTextView.setSelection(cursorPosition - 1)
        }
    }



    private fun handleParenthesis(value: String) {
        if (value == "(") {
            input += "("
        } else if (value == ")") {
            if (input.isNotEmpty() && input.last().isDigit()) {
                input += ")"
            }
        }
        inputTextView.setText(input)
    }
}




